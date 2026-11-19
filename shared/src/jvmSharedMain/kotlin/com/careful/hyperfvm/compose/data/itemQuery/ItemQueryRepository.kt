package com.careful.hyperfvm.compose.data.itemQuery

import com.careful.hyperfvm.compose.data.datapack.DataPackRepository
import com.careful.hyperfvm.compose.platform.AppEnv
import com.careful.hyperfvm.compose.platform.lowerCurrentThreadPriority
import com.careful.hyperfvm.compose.platform.restoreThreadPriority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

/**
 * 道具查询的数据源：解析数据包内的 card_desc.xml（游戏全量道具表）。
 *
 * 整表约 5 MB、一万五千余条，正则全表解析在桌面 JVM 上不到 100 ms，
 * 解析结果在进程内保留一份，之后的检索都是内存里的子串匹配。
 *
 * 解析结果还会由 [ItemQueryCache] 落一份到磁盘：数据包没换时下次启动直接读回，
 * 不再扫整份 XML；数据包更新后由 [DataPackRepository] 在「准备数据」阶段调 [prebuild]
 * 重新生成，通常走不到「第一次查询才解析」这条路。
 * 换新包时调 [invalidate] 同时作废内存与磁盘缓存。
 *
 * 检索口径：只匹配道具名称（忽略大小写），结果保持源文件顺序；
 * 名称不含关键词的条目不返回。
 *
 * 礼包（描述首行提到「获得 / 开出」并以冒号结尾，如「打开有几率获得以下道具之一：」
 * 「随机开出以下物品之一:」）在解析时拆成 [ItemBag]，子项按名字反查道具表拿到图片 id，
 * 界面就能按「图标 + 名字 + 数量」逐行展示；行首的「0星」这类星级标记会在展示时去掉。
 *
 * 整表解析只在一处发生，预热与首次查询共用一把锁：预热会延迟 2 s 并降到后台线程优先级，
 * 避免启动首帧与解析抢 CPU。
 */
object ItemQueryRepository {

    private const val CardDescFileName = "card_desc.xml"

    /** 一条 `<item ... />`，与 [com.careful.hyperfvm.compose.data.catalog.CatalogRepository] 的解析口径一致。 */
    private val ItemTagRegex = Regex("<item\\s[^>]*/>")
    private val AttrRegex = Regex("([\\w-]+)=\"([^\"]*)\"")
    private val LineBreakRegex = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)

    /** 除换行外的其余 HTML 标签（如 `<font color=...>`），只保留里面的文字。 */
    private val HtmlTagRegex = Regex("</?[a-zA-Z][^<>]*>")

    /** XML 实体：命名实体（`&amp;`）与数字引用（`&#10;` / `&#x0a;`）。 */
    private val EntityRegex = Regex("&(#x?[0-9a-fA-F]+|[a-zA-Z][a-zA-Z0-9]*);")

    private val NamedEntities = mapOf(
        "lt" to "<",
        "gt" to ">",
        "amp" to "&",
        "quot" to "\"",
        "apos" to "'",
        "nbsp" to " ",
    )

    /** 实体最多解三层：源数据里既有 `&lt;br&gt;`，个别条目还多叠一层 `&amp;lt;br&amp;gt;`。 */
    private const val MaxDecodeLayers = 3

    /**
     * 礼包首行的关键字：说明「获得 / 开出」什么、并以冒号结尾，如
     * 「打开可随机获得以下道具之一：」「随机开出以下物品之一:」「可以获得图鉴专属徽章:」。
     */
    private val BagHeaderKeywords = listOf("获得", "开出")

    /** 首行长度上限：真实数据里最长的是「打开有几率获得雅典娜守护合成材料能量宝石或以下任意道具：」。 */
    private const val BagHeaderMaxLength = 40

    /** 礼包行首的星级标记，如「0星四叶草」里的「0星」；只是礼包内的星级，不是名字。 */
    private val StarPrefixRegex = Regex("^\\d+星")

    /** 末尾半截的左括号：数据里有「8周年蛋糕（30天，不绑」这种漏写右括号的写法。 */
    private val UnclosedQualifierRegex = Regex("[（(][^（）()]*$")

    /** 礼包行尾的 `xN`，后面允许再跟 `（30天，绑定）` / `【男】` 这类后缀。 */
    private val BagQuantityRegex = Regex(
        "^(.*?)x(\\d+)\\s*(?:[（(][^（）()]*[）)]|[【\\[][^【】\\[\\]]*[】\\]])*$",
        RegexOption.IGNORE_CASE,
    )

    /** 货币行：`10点券` / `50礼券`，没有 `xN` 写法。 */
    private val BagCurrencyRegex = Regex("^\\d+(点券|礼券|金币)$")

    /** 道具名末尾的限定后缀，如 `(30天)`、`（35格）`、`(永久)`。 */
    private val TrailingQualifierRegex = Regex("[（(][^（）()]*[）)]\\s*$")

    /** 道具名末尾的方括号备注，如 `【男】`、`【2013端午纪念卡】`。 */
    private val TrailingBracketRegex = Regex("[【\\[][^【】\\[\\]]*[】\\]]\\s*$")

    /** 一次解析里最多剥几层装饰：星级 + 末尾括号 + 方括号 + 半截括号 + 冒号，剥完还查不到就是没这个道具。 */
    private const val MaxResolveSteps = 6

    /** 初始容量按实际条数给，避免整表解析时反复扩容。 */
    private const val EstimateCount = 16_000

    private val mutex = Mutex()

    @Volatile
    private var entries: List<ItemEntry>? = null

    /**
     * 缓存代次：数据包换新时 [clear] 会 +1。
     *
     * 预热解析可能正好跨过换包，解析结束时发现代次变了就不写回缓存，
     * 避免把旧包的数据当成新包用。
     */
    @Volatile
    private var cacheEpoch = 0

    /** 进程级预热协程：不跟随页面生命周期，数据包就绪后就在后台把整表解析完。 */
    private val warmUpScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 预热前的等待时间：启动瞬间要跑首帧、主题初始化、图片解码，整表解析再挤进来会掉帧。
     */
    private const val WarmUpDelayMillis = 2_000L

    /** 解析循环每多少条让出一次 CPU：换包 / 退出时能及时取消，也不长时间占满线程。 */
    private const val ParseYieldInterval = 2_000

    /** 读并解析整张道具表；并发调用只解析一次，数据包未装或缺少文件时抛异常。 */
    suspend fun ensureLoaded(env: AppEnv): List<ItemEntry> = load(env, lowPriority = false)

    /**
     * 真正的加载：先看内存，再试磁盘缓存，最后才解析 XML。
     *
     * 磁盘缓存按「数据包 version + XML 大小 + 修改时间」校验，数据包没换就是一次读盘；
     * 解析完成后再把结果写回去，下次启动继续受益。
     * 解析在 IO 线程上跑，[lowPriority] 为 true 时顺带把线程降到后台优先级。
     *
     * 只有预热走低优先级——那是「反正待会儿用得上」的提前量，不该和界面抢 CPU；
     * 用户点了查询的加载要保持正常优先级，宁可多占一点也要尽快出结果。
     */
    private suspend fun load(env: AppEnv, lowPriority: Boolean): List<ItemEntry> = withContext(Dispatchers.IO) {
        entries?.let { return@withContext it }
        mutex.withLock {
            entries?.let { return@withLock it }
            val file = DataPackRepository.dataFile(env, CardDescFileName)
                ?: error("数据包缺少 card_desc.xml，请先下载数据包")
            val epoch = cacheEpoch
            val packVersion = DataPackRepository.installedVersion(env)
            val xmlLength = file.length()
            val xmlModified = file.lastModified()
            // 数据包没换：上次编好的索引直接读回，跳过整次 XML 解析。
            val cached = ItemQueryCache.read(env, packVersion, xmlLength, xmlModified)
            if (cached != null) {
                if (epoch == cacheEpoch) entries = cached
                return@withLock cached
            }
            val xml = file.readText()
            val parsed = if (lowPriority) {
                lowerCurrentThreadPriority()
                try {
                    parse(xml)
                } finally {
                    restoreThreadPriority()
                }
            } else {
                parse(xml)
            }
            // 解析期间数据包被换掉（下载完成 / clear），这份结果作废，也不写回缓存。
            if (epoch == cacheEpoch) {
                entries = parsed
                ItemQueryCache.write(env, packVersion, xmlLength, xmlModified, parsed)
            }
            parsed
        }
    }

    /** 只按道具名称检索，忽略大小写；空白关键词返回空列表。 */
    fun search(all: List<ItemEntry>, keyword: String): List<ItemEntry> {
        val text = keyword.trim()
        if (text.isEmpty()) return emptyList()
        return all.filter { it.name.contains(text, ignoreCase = true) }
    }

    /**
     * 只清内存里的那份：磁盘缓存仍然有效。
     *
     * 启动时的 [DataPackRepository.check] 会走这里，此时数据包没换，
     * 下次加载还能直接读盘；换包请用 [invalidate]。
     */
    fun clear() {
        cacheEpoch++
        entries = null
    }

    /** 数据包换新后调用：内存与磁盘缓存一起作废，下次加载重建。 */
    fun invalidate(env: AppEnv) {
        clear()
        ItemQueryCache.delete(env)
    }

    /**
     * 后台预热：数据包就绪后调用，把「第一次查询才加载」提前到启动之后。
     *
     * 两个限流手段：延迟 [WarmUpDelayMillis] 才开始（启动首帧不被抢 CPU），
     * 并且跑在后台优先级的线程上（不跟界面线程抢时间片）。
     * 通常到这时只剩一次读盘；缓存缺失（系统清了缓存目录、刚加缓存功能）才会真解析。
     * 失败（如包内缺文件）静默忽略，留给真正的查询去报错；与 [ensureLoaded] 共用一把锁，不会重复解析。
     */
    fun warmUp(env: AppEnv) {
        warmUpScope.launch {
            delay(WarmUpDelayMillis)
            runCatching { load(env, lowPriority = true) }
        }
    }

    /**
     * 数据包刚装好时调用：在下载页的「准备数据」阶段就把整表加载完并写进磁盘缓存。
     *
     * 与 [warmUp] 的区别是「有人正在等」：下载页此刻本来就停在 Preparing，
     * 索引在这里做完，Ready 之后第一次检索就是读盘命中，而不是等 2 s 后再后台补。
     * 因此不用低优先级。失败（如包内缺文件）静默忽略，留给真正的查询去报错。
     */
    suspend fun prebuild(env: AppEnv) {
        runCatching { load(env, lowPriority = false) }
    }

    private suspend fun parse(xml: String): List<ItemEntry> {
        val result = ArrayList<ItemEntry>(EstimateCount)
        var index = 0
        for (match in ItemTagRegex.findAll(xml)) {
            // 整表解析要几十到几百毫秒，隔一段让出一次：换包 / 退出时能及时取消。
            if (index++ % ParseYieldInterval == 0) yield()
            val attrs = parseAttrs(match.value)
            val rawId = attrs["id"]?.trim()?.lowercase() ?: continue
            // 源数据里个别 id 带前导空格（如蟾宫仙子），trim 后统一成 x + 十六进制。
            val hex = rawId.removePrefix("0x")
            if (hex.isEmpty()) continue
            val name = attrs["name"]?.trim().orEmpty()
            if (name.isEmpty()) continue
            result += ItemEntry(
                id = "x$hex",
                name = name,
                description = cleanDescription(attrs["desc"]),
                type = attrs["type"]?.trim().orEmpty(),
                energy = attrs["energy"]?.trim()?.takeIf { it.isNotEmpty() },
                hp = attrs["hp"]?.trim()?.takeIf { it.isNotEmpty() },
            )
        }
        // 礼包子项要按名字反查图片 id，先建一份名称索引（同名条目取先出现的那个）。
        val byName = HashMap<String, ItemEntry>(result.size)
        for (entry in result) byName.putIfAbsent(entry.name, entry)
        return result.map { entry -> entry.copy(bag = parseBag(entry.description, byName)) }
    }

    /**
     * 把一个礼包的描述拆成「首行 + 候选列表 + 末尾说明」。
     *
     * 首行不限固定句式，只要提到「获得 / 开出」并以冒号结尾就算（实测共 46 种写法）。
     * 末尾说明从后往前识别：不像道具行的才当说明，因为 `钢钻`、`永久保鲜袋` 这类
     * 真道具在礼包里可能没有 `xN`，只能拿名称索引区分。一行道具都没有或描述太短时
     * 返回 null，界面按普通描述展示。
     */
    private fun parseBag(description: String?, itemsByName: Map<String, ItemEntry>): ItemBag? {
        if (description == null) return null
        val lines = description
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
        if (lines.size < 3) return null
        val header = lines.first()
        if (!isBagHeader(header)) return null
        var lastItem = lines.lastIndex
        val notes = ArrayDeque<String>()
        while (lastItem >= 1 && !isBagItemLine(lines[lastItem], itemsByName)) {
            notes.addFirst(lines[lastItem])
            lastItem--
        }
        if (lastItem < 1) return null
        return ItemBag(
            header = header,
            note = notes.joinToString("").takeIf { it.isNotEmpty() },
            entries = (1..lastItem).map { parseBagEntry(lines[it], itemsByName) },
        )
    }

    /** 首行是不是礼包说明：提到「获得 / 开出」并以冒号结尾，且不超过长度上限。 */
    private fun isBagHeader(line: String): Boolean =
        line.length <= BagHeaderMaxLength &&
            (line.endsWith('：') || line.endsWith(':')) &&
            BagHeaderKeywords.any { line.contains(it) }

    /** 礼包行是不是候选道具：带 `xN`、是货币、或者能在道具表里找到这个名字。 */
    private fun isBagItemLine(line: String, itemsByName: Map<String, ItemEntry>): Boolean =
        BagQuantityRegex.matches(line) ||
            BagCurrencyRegex.matches(line) ||
            resolveBagItem(line, itemsByName) != null

    /** 拆一行候选道具：展示名、数量 `xN`、对应道具 id。 */
    private fun parseBagEntry(line: String, itemsByName: Map<String, ItemEntry>): BagEntry {
        val quantity = BagQuantityRegex.matchEntire(line)
        val rawName = quantity?.groupValues?.get(1)?.trim() ?: line
        val item = resolveBagItem(rawName, itemsByName)
        return BagEntry(
            name = bagDisplayName(rawName = rawName, resolved = item != null),
            quantity = quantity?.groupValues?.get(2),
            itemId = item?.id,
            isWeapon = item?.type?.contains("武器") == true,
        )
    }

    /**
     * 列表里展示的名字：反查到道具时去掉「0星」这类星级前缀（名字本身不含它），
     * 再截掉漏写右括号的半截说明与末尾方括号备注；没反查到就原样留着，免得把真名字改坏。
     */
    private fun bagDisplayName(rawName: String, resolved: Boolean): String {
        var name = if (resolved) rawName.replace(StarPrefixRegex, "").trim() else rawName
        name = name.replace(UnclosedQualifierRegex, "").trim()
        name = name.replace(TrailingBracketRegex, "").trim()
        return name.ifEmpty { rawName }
    }

    /**
     * 按名称找道具：依次剥掉礼包行里的装饰再匹配——行首「0星」、末尾「（30天，绑定）」、
     * 末尾「【男】」、漏写右括号的「（30天，不绑」，最后把半角冒号换成全角。
     * 一次只剥一种，避免把名字自带的括号也啃掉。
     * 找不到返回 null（如图标不存在的点券、其它区服道具），界面就不画图标。
     */
    private fun resolveBagItem(name: String, itemsByName: Map<String, ItemEntry>): ItemEntry? {
        var current = name
        repeat(MaxResolveSteps) {
            itemsByName[current]?.let { return it }
            current = nextBagName(current) ?: return null
        }
        return null
    }

    /**
     * 下一个候选名：按优先级依次尝试剥星级、末尾括号、方括号备注、半截括号、
     * 最后把半角冒号换成全角再试一次；没有可剥的返回 null。
     */
    private fun nextBagName(name: String): String? {
        val noStar = name.replace(StarPrefixRegex, "").trim()
        if (noStar != name && noStar.isNotEmpty()) return noStar
        val noQualifier = name.replace(TrailingQualifierRegex, "").trim()
        if (noQualifier != name && noQualifier.isNotEmpty()) return noQualifier
        val noBracket = name.replace(TrailingBracketRegex, "").trim()
        if (noBracket != name && noBracket.isNotEmpty()) return noBracket
        val noUnclosed = name.replace(UnclosedQualifierRegex, "").trim()
        if (noUnclosed != name && noUnclosed.isNotEmpty()) return noUnclosed
        // 道具表里「食神谱：毛血旺」是全角冒号，礼包里偶有写成半角的。
        if (name.contains(':')) {
            val fullWidth = name.replace(':', '：')
            if (fullWidth != name) return fullWidth
        }
        return null
    }

    private fun parseAttrs(tag: String): Map<String, String> =
        AttrRegex.findAll(tag).associate { it.groupValues[1] to it.groupValues[2] }

    /**
     * 把源数据里的 HTML 片段整理成纯文本：先解 XML 实体，再把 `<br>`（含 `&lt;br&gt;` 形式）
     * 与 `|` 换成换行，最后去掉其余标签（如 `<font>`），只留文字。
     */
    private fun cleanDescription(raw: String?): String? {
        val decoded = decodeEntities(raw ?: return null)
        return decoded
            .let { LineBreakRegex.replace(it, "\n") }
            .replace(HtmlTagRegex, "")
            .replace('|', '\n')
            .trim()
            .takeIf { it.isNotEmpty() }
    }

    /**
     * 逐层解 XML 实体，直到不再变化或达到 [MaxDecodeLayers] 上限。
     * 换行在源数据里写作 `&lt;br&gt;`，不先解码就会把实体原样显示出来。
     */
    private fun decodeEntities(text: String): String {
        var current = text
        repeat(MaxDecodeLayers) {
            val next = EntityRegex.replace(current) { decodeEntity(it) }
            if (next == current) return current
            current = next
        }
        return current
    }

    /** 解一个实体；不认识的实体（如 `&foo;`）原样保留。 */
    private fun decodeEntity(match: MatchResult): String {
        val body = match.groupValues[1]
        val decoded = when {
            body.startsWith("#x", ignoreCase = true) ->
                body.drop(2).toIntOrNull(16)?.let { codePointToString(it) }

            body.startsWith("#") -> body.drop(1).toIntOrNull()?.let { codePointToString(it) }
            else -> NamedEntities[body.lowercase()]
        }
        return decoded ?: match.value
    }

    /** 按码点转字符，兼容 BMP 之外的字符（如 emoji）。 */
    private fun codePointToString(codePoint: Int): String? =
        if (codePoint in 1..0x10FFFF) String(Character.toChars(codePoint)) else null
}
