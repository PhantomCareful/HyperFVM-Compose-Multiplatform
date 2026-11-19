package com.careful.hyperfvm.compose.data.catalog

import com.careful.hyperfvm.compose.data.datapack.DataPackRepository
import com.careful.hyperfvm.compose.platform.AppEnv
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** 合集页的加载状态。 */
sealed interface CatalogLoadState {

    /**
     * 首次加载中。[total] 为 0 表示还在下载、解析条目文件（进度未知）；
     * 大于 0 时表示正在下载预览图，[done]/[total] 用于环形进度。
     */
    data class Loading(val done: Int = 0, val total: Int = 0) : CatalogLoadState

    /** 资源就位，可以展示瀑布流。 */
    data class Ready(val items: List<CatalogItem>) : CatalogLoadState

    /** 加载失败（多为断网）：界面给错误与重试入口，不闪退。 */
    data class Error(val message: String) : CatalogLoadState
}

/** 一次「更新」的结果：完成时带上男女条目各新增了多少。 */
sealed interface CatalogUpdateResult {
    data class Done(val maleAdded: Int, val femaleAdded: Int) : CatalogUpdateResult
    data class Failed(val message: String) : CatalogUpdateResult
}

/**
 * 服装 / 翅膀合集的本地数据仓储。
 *
 * 条目数据来自数据包内的两个官方配置文件（card_desc 与 market_item，由
 * [DataPackRepository] 随数据包一起下发），预览图按道具 id 从道具 CDN 下载；
 * 条目与图片都缓存到 `filesDir/catalog`，之后进页面直接读本地。
 * 数据包更新后由 [DataPackRepository] 删掉条目缓存，下一次进页面自动重建。
 *
 * 状态按 [CatalogKind] 分开存：两个合集共用同一份条目文件，但图片与就位判断各自独立，
 * 第一次进哪个页面就先下哪个页面的资源。
 *
 * 断网、超时、HTTP 错误、解析异常全部收敛成 [CatalogLoadState.Error] 或
 * [CatalogUpdateResult.Failed]，界面不会因为网络问题闪退。
 */
object CatalogRepository {

    private const val ImageUrlBase = "https://cdn-qq-ms.123u.com/cdn.qq.123u.com/images"

    private const val ConnectTimeoutMs = 10_000
    private const val ReadTimeoutMs = 30_000

    /** 图片下载并发：8 路能把一般带宽跑满，也不会让几百张小图同时抢 IO。 */
    private const val ImageConcurrency = 8

    /** 图片下载共用的调度器：合集整批下载与单张按需下载都受同一个并发上限约束。 */
    private val ImageDispatcher = Dispatchers.IO.limitedParallelism(ImageConcurrency)

    /** 单张按需下载的去重表：同一张图只下一次（概率公示页可能多处引用同一张图）。 */
    private val pendingImageJobs = ConcurrentHashMap<String, Deferred<Unit>>()

    private val imageScope = CoroutineScope(SupervisorJob() + ImageDispatcher)

    /** 续费档的固定展示顺序。 */
    private val RenewalOrder = listOf("7天", "30天", "永久")

    /** market_item 的 i_expiry_date → 展示名；只收录用户要的三档。 */
    private val DurationLabels = mapOf("604800" to "7天", "2592000" to "30天", "-1" to "永久")

    /** market_item 的 i_comm_currency_type → 货币名。 */
    private val CurrencyLabels = mapOf("1" to "点券", "3" to "功勋")

    /** 源数据里重名、需要按 id 升序编 1/2/3 后缀的款式。 */
    private val VariantNames = setOf("魔影圣衣", "天神之影")

    /** 已知坏条目：图片 404 / 重复变体，目录里剔除。 */
    private val KnownBrokenIds = setOf("0x14d03f10", "0x14d03f20")

    private val HpRegex = Regex("""\+\s*(\d+)""")
    private val ItemTagRegex = Regex("""<item\b[^>]*>""")
    private val AttrRegex = Regex("(\\w+)=\"([^\"]*)\"")
    private val MarketBlockRegex = Regex("""<item\b.*?</item>""", RegexOption.DOT_MATCHES_ALL)
    private val ElementTagRegex = Regex("""<element\b[^>]*>""")

    /** 每个合集的加载状态；进程内共享，页面重建不丢。 */
    private val states = ConcurrentHashMap<CatalogKind, MutableStateFlow<CatalogLoadState>>()

    /** 每个合集的串行锁：进页面自动加载与手动更新不能同时跑。 */
    private val mutexes = ConcurrentHashMap<CatalogKind, Mutex>()

    fun state(kind: CatalogKind): StateFlow<CatalogLoadState> = flowOf(kind).asStateFlow()

    private fun flowOf(kind: CatalogKind): MutableStateFlow<CatalogLoadState> =
        states.computeIfAbsent(kind) { MutableStateFlow(CatalogLoadState.Loading()) }

    private fun mutexOf(kind: CatalogKind): Mutex =
        mutexes.computeIfAbsent(kind) { Mutex() }

    /** 预览图落盘路径：`filesDir/catalog/images/x14d00120.png`。 */
    fun imageFile(env: AppEnv, id: String): File = File(imageDir(env), "$id.png")

    private fun imageDir(env: AppEnv): File = File(env.filesDir, "catalog/images")

    /**
     * 按需确保一张预览图落盘（概率公示页的服装 / 翅膀奖品用）。
     *
     * 本地已有就直接返回；下载失败不抛异常——界面按没有图标展示，不弹错误。
     *
     * 0x14 开头的道具图在 CDN 上有两套资源：个别 0x14 条目缺图，但同 id 的 0x24 版在；
     * 个别 0x14 图分辨率偏低。候选顺序由 [imageIdCandidates] 给出，逐个试到成功为止。
     */
    suspend fun ensureImage(env: AppEnv, id: String, preferHighRes: Boolean = false) {
        val target = imageFile(env, id)
        if (target.exists()) return
        val candidates = imageIdCandidates(id, preferHighRes)
        pendingImageJobs.computeIfAbsent(id) { key ->
            imageScope.async {
                try {
                    for (candidate in candidates) {
                        if (target.exists()) break
                        runCatching { downloadToFile(imageUrl(candidate), target) }
                    }
                } finally {
                    pendingImageJobs.remove(key)
                }
            }
        }.await()
    }

    /**
     * 一张道具图的候选 id：0x14 与 0x24 是同一批图的低清 / 高清两套资源。
     *
     * 普通道具先试 0x14，缺图（404）时再退 0x24；武器类要求高清，顺序反过来，
     * 0x24 也失败时再退 0x14。其它前缀没有 0x24 对应资源，原样返回。
     */
    private fun imageIdCandidates(id: String, preferHighRes: Boolean): List<String> {
        if (!id.startsWith("x14") || id.length <= 3) return listOf(id)
        val highRes = "x24" + id.substring(3)
        return if (preferHighRes) listOf(highRes, id) else listOf(id, highRes)
    }

    /**
     * 加载合集：本地缓存完备就直接就绪，否则下载条目与缺的预览图。
     *
     * 数据包安装时由 [DataPackRepository] 经 [ensureAll] 提前调一次；页面进入时再调
     * 只是兜底（状态已是 Ready 时直接返回，不会重复下载）。
     * 只补缺的图片：上次下载到一半中断时，条目文件已经写盘，重进页面不用重新解析。
     * 全部图片都落盘后写就绪标记，[isPrepared] 用它判断数据包是否已经准备完毕。
     */
    suspend fun ensureLoaded(env: AppEnv, kind: CatalogKind) {
        val flow = flowOf(kind)
        if (flow.value is CatalogLoadState.Ready) return
        mutexOf(kind).withLock {
            if (flow.value is CatalogLoadState.Ready) return
            withContext(Dispatchers.IO) {
                try {
                    val cached = readCachedItems(env, kind)
                    if (cached != null) {
                        val missing = cached.filter { !imageFile(env, it.id).exists() }
                        val failed = if (missing.isEmpty()) {
                            0
                        } else {
                            downloadImages(env, missing) { done ->
                                flow.value = CatalogLoadState.Loading(done, missing.size)
                            }
                        }
                        flow.value = CatalogLoadState.Ready(cached)
                        if (failed == 0) markPrepared(env, kind)
                        return@withContext
                    }

                    flow.value = CatalogLoadState.Loading()
                    val items = fetchCatalog(env, kind)
                    if (items.isEmpty()) error("条目数据解析失败，请稍后重试")
                    // 先把条目写盘再下图：中途被杀掉时下次只需补图，不用重拉 9 MB 的文件。
                    writeCachedItems(env, kind, items)
                    val failed = downloadImages(env, items) { done ->
                        flow.value = CatalogLoadState.Loading(done, items.size)
                    }
                    flow.value = CatalogLoadState.Ready(items)
                    if (failed == 0) markPrepared(env, kind)
                } catch (e: CancellationException) {
                    // 页面 / Activity 被销毁：保持当前状态，下次重入时重新补。
                    throw e
                } catch (e: Exception) {
                    flow.value = CatalogLoadState.Error(friendlyMessage(e))
                }
            }
        }
    }

    /**
     * 数据包安装 / 启动补齐阶段调用：两个合集一起准备，界面停在 Preparing 等它完成。
     *
     * 两类共用同一个图片下载调度器（并发上限仍是 [ImageConcurrency]），不会重复抢 IO。
     */
    suspend fun ensureAll(env: AppEnv) {
        coroutineScope {
            launch { ensureLoaded(env, CatalogKind.Clothing) }
            launch { ensureLoaded(env, CatalogKind.Wing) }
        }
    }

    /**
     * 两个合集是否都已备齐（条目缓存 + 全部预览图）。
     *
     * 数据包刚装上、或上次准备被中断时返回 false，[DataPackRepository] 据此把状态
     * 停回 Preparing 补齐，避免进合集页才现下载。
     * 标记里记的是准备时的数据包版本：包更新后旧标记自动失效。
     */
    fun isPrepared(env: AppEnv, kind: CatalogKind): Boolean {
        val ready = readyFile(env, kind)
        if (!cacheFile(env, kind).isFile || !ready.isFile) return false
        val version = runCatching { ready.readText().trim() }.getOrNull()
        return version == DataPackRepository.installedVersion(env).toString()
    }

    /** 新数据包安装后调用：条目与就绪标记一起作废，等下一次准备重建（图片按 id 复用）。 */
    fun invalidate(env: AppEnv) {
        CatalogKind.entries.forEach { kind ->
            cacheFile(env, kind).delete()
            readyFile(env, kind).delete()
        }
    }

    private fun readyFile(env: AppEnv, kind: CatalogKind): File =
        File(env.filesDir, "catalog/${cacheName(kind)}.ready")

    /** 写就绪标记；写失败只影响下次启动的快速判断，不影响本次展示。 */
    private fun markPrepared(env: AppEnv, kind: CatalogKind) {
        runCatching {
            val file = readyFile(env, kind)
            file.parentFile?.mkdirs()
            file.writeText(DataPackRepository.installedVersion(env).toString())
        }
    }

    /**
     * 拉取最新条目文件并与本地对比：新条目按性别计数上报，顺带补齐本地缺的图片。
     *
     * 条目文件拉取/解析失败返回 [CatalogUpdateResult.Failed]；单张图片失败不阻断，
     * 对应卡片先显示占位，下次更新会再补。
     */
    suspend fun update(env: AppEnv, kind: CatalogKind): CatalogUpdateResult {
        val flow = flowOf(kind)
        return mutexOf(kind).withLock {
            withContext(Dispatchers.IO) {
                try {
                    val fresh = fetchCatalog(env, kind)
                    if (fresh.isEmpty()) return@withContext CatalogUpdateResult.Failed("条目数据解析失败，请稍后重试")

                    val oldIds = when (val current = flow.value) {
                        is CatalogLoadState.Ready -> current.items.map { it.id }.toSet()
                        else -> readCachedItems(env, kind)?.map { it.id }?.toSet() ?: emptySet()
                    }
                    val added = fresh.filter { it.id !in oldIds }

                    writeCachedItems(env, kind, fresh)
                    val missing = fresh.filter { !imageFile(env, it.id).exists() }
                    if (missing.isNotEmpty()) downloadImages(env, missing) {}
                    flow.value = CatalogLoadState.Ready(fresh)

                    CatalogUpdateResult.Done(
                        maleAdded = added.count { it.sex == CatalogSex.Male },
                        femaleAdded = added.count { it.sex == CatalogSex.Female },
                    )
                } catch (e: Exception) {
                    CatalogUpdateResult.Failed(friendlyMessage(e))
                }
            }
        }
    }

    // ==================== 条目文件 ====================

    /** 读数据包内的两个配置文件，产出该合集的条目列表（按 id 升序）。 */
    private fun fetchCatalog(env: AppEnv, kind: CatalogKind): List<CatalogItem> {
        val cardDesc = DataPackRepository.dataFile(env, "card_desc.xml")
            ?: error("数据包缺少 card_desc.xml，请重新下载数据包")
        val marketItem = DataPackRepository.dataFile(env, "market_item.xml")
            ?: error("数据包缺少 market_item.xml，请重新下载数据包")
        val cardItems = parseCardItems(cardDesc.readText())
        val marketItems = parseMarketItems(marketItem.readText())
        return buildCatalog(kind, cardItems, marketItems)
    }

    /** card_desc 的道具表。id 先 trim 再小写：源数据里个别 id 带前导空格（如蟾宫仙子）。 */
    private fun parseCardItems(xml: String): Map<String, Map<String, String>> {
        val result = HashMap<String, Map<String, String>>()
        for (match in ItemTagRegex.findAll(xml)) {
            val attrs = parseAttrs(match.value)
            val id = attrs["id"]?.trim()?.lowercase() ?: continue
            result[id] = attrs
        }
        return result
    }

    /** market_item 的道具表：key 同 [parseCardItems]，value 是全部续费档位。 */
    private fun parseMarketItems(xml: String): Map<String, List<Map<String, String>>> {
        val result = HashMap<String, List<Map<String, String>>>()
        for (match in MarketBlockRegex.findAll(xml)) {
            val block = match.value
            val head = parseAttrs(block.substringBefore('>') + ">")
            val id = head["i_id"]?.trim()?.lowercase() ?: continue
            result[id] = ElementTagRegex.findAll(block)
                .map { parseAttrs(it.value) }
                .toList()
        }
        return result
    }

    private fun parseAttrs(tag: String): Map<String, String> =
        AttrRegex.findAll(tag).associate { it.groupValues[1] to it.groupValues[2] }

    /**
     * 把两张道具表合成条目列表。
     *
     * 筛选口径与老版本的离线目录一致：男女款、排除功能道具、按 id 前缀区分合集；
     * 服装只要体力大于 20 的套装，翅膀全部收录。
     */
    private fun buildCatalog(
        kind: CatalogKind,
        cardItems: Map<String, Map<String, String>>,
        marketItems: Map<String, List<Map<String, String>>>,
    ): List<CatalogItem> {
        val prefix = "0x${kind.idPrefix}"
        val items = mutableListOf<CatalogItem>()
        for ((rawId, attrs) in cardItems) {
            val id = rawId.lowercase()
            if (!id.startsWith(prefix) || id in KnownBrokenIds) continue
            if (attrs["type"] == "功能道具") continue
            val sex = when (attrs["sex"]) {
                "男" -> CatalogSex.Male
                "女" -> CatalogSex.Female
                else -> continue
            }
            val hp = HpRegex.find(attrs["hp"].orEmpty())?.groupValues?.get(1)?.toIntOrNull() ?: 0
            if (kind == CatalogKind.Clothing && hp <= 20) continue
            items += CatalogItem(
                id = "x" + id.removePrefix("0x"),
                name = attrs["name"].orEmpty(),
                sex = sex,
                hp = hp,
                description = attrs["desc"]?.trim()?.takeIf { it.isNotEmpty() },
                renewals = buildRenewals(marketItems[id]),
            )
        }
        items.sortBy { it.id }
        return applyVariantSuffix(items)
    }

    /** 三档续费按「7天 / 30天 / 永久」排序；源数据里查不到的档位跳过。 */
    private fun buildRenewals(elements: List<Map<String, String>>?): List<CatalogRenewal> {
        if (elements.isNullOrEmpty()) return emptyList()
        val byLabel = LinkedHashMap<String, String>()
        for (element in elements) {
            val label = DurationLabels[element["i_expiry_date"]] ?: continue
            if (label in byLabel) continue
            val currency = CurrencyLabels[element["i_comm_currency_type"]] ?: "点券"
            val price = element["i_comm_price"] ?: "0"
            byLabel[label] = "$price$currency"
        }
        return RenewalOrder.mapNotNull { label ->
            byLabel[label]?.let { CatalogRenewal(duration = label, price = it) }
        }
    }

    /** 重名款式按 id 升序编 1/2/3 后缀，界面上才能分清是哪一套变体。 */
    private fun applyVariantSuffix(items: List<CatalogItem>): List<CatalogItem> {
        val groups = items.filter { it.name in VariantNames }.groupBy { it.sex to it.name }
        if (groups.isEmpty()) return items
        val renamed = HashMap<String, String>()
        for ((_, group) in groups) {
            group.sortedBy { it.id }.forEachIndexed { index, item ->
                renamed[item.id] = "${item.name}${index + 1}"
            }
        }
        return items.map { item -> renamed[item.id]?.let { item.copy(name = it) } ?: item }
    }

    // ==================== 网络与图片 ====================

    /** 预览图的 CDN 地址：图片路径按道具 id 每两位分一层，如 images/1/4/0x14d00120.png。 */
    private fun imageUrl(id: String): String {
        val rid = id.removePrefix("x")
        require(rid.length >= 2) { "非法的道具 id：$id" }
        return "$ImageUrlBase/${rid[0]}/${rid[1]}/0x$rid.png"
    }

    /**
     * 并发下载一批预览图，返回失败张数。已存在的文件直接跳过。
     *
     * 单张失败只计一次，不抛异常：缺图的卡片先显示占位，下次更新会再补。
     */
    private suspend fun downloadImages(
        env: AppEnv,
        items: List<CatalogItem>,
        onProgress: (Int) -> Unit,
    ): Int = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext 0
        val dir = imageDir(env).apply { mkdirs() }
        val done = AtomicInteger(0)
        val failed = AtomicInteger(0)
        coroutineScope {
            items.forEach { item ->
                launch(ImageDispatcher) {
                    val target = File(dir, "${item.id}.png")
                    if (!target.exists()) {
                        try {
                            downloadToFile(imageUrl(item.id), target)
                        } catch (_: Exception) {
                            failed.incrementAndGet()
                        }
                    }
                    onProgress(done.incrementAndGet())
                }
            }
        }
        failed.get()
    }

    /** 下载单个文件：先写 .part 再改名，中途被杀掉不会留下半张图。 */
    private fun downloadToFile(url: String, target: File) {
        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, "${target.name}.part")
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = ConnectTimeoutMs
            readTimeout = ReadTimeoutMs
            setRequestProperty("User-Agent", "Mozilla/5.0")
        }
        try {
            if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
            connection.inputStream.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            }
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
            }
        } finally {
            connection.disconnect()
            if (temp.exists()) temp.delete()
        }
    }

    // ==================== 本地缓存 ====================

    private fun cacheName(kind: CatalogKind): String =
        if (kind == CatalogKind.Clothing) "clothing" else "wing"

    /** 条目列表落盘为 JSON：首次解析成功后写一次，之后进页面直接读。 */
    private fun cacheFile(env: AppEnv, kind: CatalogKind): File =
        File(env.filesDir, "catalog/${cacheName(kind)}.json")

    private fun readCachedItems(env: AppEnv, kind: CatalogKind): List<CatalogItem>? {
        val file = cacheFile(env, kind)
        if (!file.exists()) return null
        return runCatching {
            val array = JSONObject(file.readText()).getJSONArray("items")
            buildList(array.length()) {
                for (index in 0 until array.length()) {
                    val json = array.getJSONObject(index)
                    val renewals = json.optJSONArray("renewals")?.let { renewalsJson ->
                        buildList(renewalsJson.length()) {
                            for (renewalIndex in 0 until renewalsJson.length()) {
                                val renewal = renewalsJson.getJSONObject(renewalIndex)
                                add(
                                    CatalogRenewal(
                                        duration = renewal.getString("duration"),
                                        price = renewal.getString("price"),
                                    ),
                                )
                            }
                        }
                    } ?: emptyList()
                    add(
                        CatalogItem(
                            id = json.getString("id"),
                            name = json.getString("name"),
                            sex = if (json.optString("sex") == "Female") CatalogSex.Female else CatalogSex.Male,
                            hp = json.optInt("hp"),
                            description = json.optString("description").takeIf { it.isNotEmpty() },
                            renewals = renewals,
                        ),
                    )
                }
            }
        }.getOrNull()
    }

    private fun writeCachedItems(env: AppEnv, kind: CatalogKind, items: List<CatalogItem>) {
        val file = cacheFile(env, kind)
        file.parentFile?.mkdirs()
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("name", item.name)
                    .put("sex", item.sex.name)
                    .put("hp", item.hp)
                    .put("description", item.description ?: "")
                    .put(
                        "renewals",
                        JSONArray().apply {
                            item.renewals.forEach { renewal ->
                                put(
                                    JSONObject()
                                        .put("duration", renewal.duration)
                                        .put("price", renewal.price),
                                )
                            }
                        },
                    ),
            )
        }
        val text = JSONObject().put("items", array).toString()
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(text)
        if (!temp.renameTo(file)) {
            temp.copyTo(file, overwrite = true)
            temp.delete()
        }
    }

    // ==================== 错误文案 ====================

    private fun friendlyMessage(e: Exception): String = when (e) {
        is UnknownHostException, is ConnectException -> "网络连接失败，请检查网络后重试"
        is SocketTimeoutException -> "网络请求超时，请检查网络后重试"
        else -> e.message?.takeIf { it.isNotBlank() } ?: "内容获取失败，请检查网络后重试"
    }
}
