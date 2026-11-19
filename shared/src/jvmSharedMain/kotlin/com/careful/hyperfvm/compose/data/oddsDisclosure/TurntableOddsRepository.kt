package com.careful.hyperfvm.compose.data.oddsDisclosure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * 幸运转转概率公示的动态数据源。
 *
 * 官方把公示放在一个静态 HTML 里，物品与概率会随活动轮换，所以这里不做本地数据表：
 * 每次进入「抽奖概率」页都重新拉一次并解析，解析结果组织成「幸运转转」一个分节，
 * 下面按页面顺序放四个抽奖——普通转转、35000 保底奖励、140000 保底奖励、豪华转转。
 *
 * 解析只认小节标题里的关键字，不依赖表格顺序；对不上号的小节直接跳过，
 * 一条都解析不出来时返回 [Result.ParseError]，由界面提示失败而不是显示半截数据。
 */
object TurntableOddsRepository {

    /** 官方公示页；物品轮换后内容会原地更新。 */
    private const val Url = "https://td-qqgame-ms.123u.com/ms_truntableRate.html"

    private const val Errata = "幸运转转概率公示数据直接解析自官方概率公示界面，因此不提供道具图片展示。\n未来奖励轮换时，可能会出现解析失败的情况，届时开发者会第一时间发布版本更新解决问题。"

    private const val ConnectTimeoutMs = 10_000
    private const val ReadTimeoutMs = 15_000

    /** 解析结果里唯一的分节标题。 */
    private const val CategoryTitle = "幸运转转"

    /** 四个抽奖的目录行标题，同时也是 [DrawDescriptions] 的键。 */
    private const val NormalDraw = "普通转转"
    private const val Guarantee35000Draw = "35000保底奖励"
    private const val Guarantee140000Draw = "140000保底奖励"
    private const val LuxuryDraw = "豪华转转"

    // 说明文案没有默认值：没写的场次，详情页直接不显示说明卡。

    /**
     * 四个抽奖各自的说明文字（详情页第一张 TipsCard），按 [drawName] 的返回值取用。
     *
     * 新增抽奖时在这里补一段；没写的场次详情页不显示说明卡。
     */
    private val DrawDescriptions = mapOf(
        NormalDraw to "消耗200点券购买四叶草，同时可以获赠幸运转转1次。\n" +
                "每次幸运转转可以获得一份超值奖励。奖励包括：马年生肖卡片、其他生肖卡片、转职凭证、技能书、高级香料和高级四叶草等。\n" +
                "每次抽中稀有道具，额外可以获得幸运星。幸运星可以在豪华转转中抽取或者兑换特殊道具。\n" +
                "马年幸运转转保底累计金额将在马年活动结束后清空。",
        Guarantee35000Draw to "每累计转转35000点券，可触发一次小保底。",
        Guarantee140000Draw to "每累计转转140000点券，可触发一次大保底。",
        LuxuryDraw to "豪华转转可以消耗幸运星随机获取奖励，也可以点击兑换消耗幸运星直接获取物品。豪华转转中的物品可以重复获取。\n" +
                "幸运星可在普通转转中获取。在普通转转每次抽中稀有道具，额外可以获得幸运星。\n" +
                "豪华转转中的物品将定期进行更换。\n" +
                "幸运星数量将在马年活动结束之后清空，请注意及时使用。",
    )

    /**
     * 个别抽奖对勘误卡文案的覆写；没列出的场次按默认文案 [OddsErrataTips] 处理。
     *
     * 键与 [DrawDescriptions] 一致；写成空串用默认文案，写成 null 则不显示勘误卡。
     */
    private val DrawErrata = mapOf<String, String?>(
        NormalDraw to Errata,
        Guarantee35000Draw to Errata,
        Guarantee140000Draw to Errata,
        LuxuryDraw to Errata,
    )

    /** 一次拉取的三种结局，供界面分别映射成不同的提示。 */
    sealed interface Result {
        data class Success(val categories: List<LotteryCategory>) : Result
        data object NetworkError : Result
        data object ParseError : Result
    }

    /**
     * 最近一次解析成功的结果，进程内共享。
     *
     * 详情页不随导航参数传数据，靠它按下标取回同一份内容；进程重建后为空，
     * 详情页会自行补拉一次。刷新失败时保留上一次的结果，界面继续显示旧数据。
     */
    @Volatile
    var cached: List<LotteryCategory> = emptyList()
        private set

    /** 拉取并解析公示页；成功时同时更新 [cached]。必须在协程里调用，内部切到 IO 线程。 */
    suspend fun refresh(): Result = withContext(Dispatchers.IO) {
        val html = fetchHtml() ?: return@withContext Result.NetworkError
        val categories = parse(html)
        if (categories.isEmpty()) return@withContext Result.ParseError
        cached = categories
        Result.Success(categories)
    }

    private fun fetchHtml(): String? = try {
        // 带时间戳绕开 CDN 缓存：拿到旧页面等于拿到过期的概率。
        val url = "$Url?_=${System.currentTimeMillis()}"
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = ConnectTimeoutMs
            connection.readTimeout = ReadTimeoutMs
            connection.requestMethod = "GET"
            if (connection.responseCode !in 200..299) {
                null
            } else {
                // 页面声明 UTF-8，按字符串读即可，不要用平台默认编码。
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
        } finally {
            connection.disconnect()
        }
    } catch (_: Exception) {
        null
    }

    /** 把公示页拆成「幸运转转」一个分节；任何一张表都没解析出来时返回空表。 */
    private fun parse(html: String): List<LotteryCategory> {
        val draws = mutableListOf<LotteryDraw>()
        // h2 是小节大标题（一、二、三），h3 是「点券保底奖励」里的 35000 / 140000 子标题。
        var sectionTitle: String? = null
        var subTitle: String? = null
        HeadingOrTable.findAll(html).forEach { match ->
            val level = match.groupValues[1]
            if (level.isNotEmpty()) {
                val text = cleanHtml(match.groupValues[2])
                if (level == "2") {
                    sectionTitle = text
                    subTitle = null
                } else {
                    subTitle = text
                }
                return@forEach
            }
            // 表格归属最近一个标题：有 h3 用 h3，否则用 h2。
            val heading = subTitle ?: sectionTitle ?: return@forEach
            val name = drawName(heading) ?: return@forEach
            parseTable(match.groupValues[3], name)?.let { draws += it }
        }
        return if (draws.isEmpty()) emptyList() else listOf(LotteryCategory(CategoryTitle, draws))
    }

    /**
     * 小节标题 → 目录行标题。
     *
     * 页面上的标题依次是「一、幸运转转概率」「35000 点券保底」「140000 点券保底」「三、豪华转转」，
     * 这里按关键字映射成用户熟悉的四个名字；认不出的标题（如新增的说明性小节）返回 null 跳过。
     */
    private fun drawName(heading: String): String? {
        val text = SectionPrefix.replace(heading, "")
        return when {
            text.contains("豪华") -> LuxuryDraw
            text.contains("35000") -> Guarantee35000Draw
            text.contains("140000") -> Guarantee140000Draw
            text.contains("幸运转转") -> NormalDraw
            else -> null
        }
    }

    /**
     * 解析一张三列或四列的概率表；表头行、合计行与解析不了的行都跳过。
     *
     * 「送幸运星」列的值（0/1）映射成 [OddsItem.expected]：送 1 颗星的奖励是值得关注的好东西，
     * 详情页用 Important 卡突出；列表里没有这一列时（如豪华转转）全部按普通卡展示。
     */
    private fun parseTable(tableHtml: String, name: String): LotteryDraw? {
        val prizes = mutableListOf<OddsItem>()
        // 列位置从表头里找，不写死下标：目前「送幸运星」在第四列，找不到时退到第 4 列。
        var starColumn = -1
        TableRow.findAll(tableHtml).forEach { rowMatch ->
            val row = rowMatch.groupValues[1]
            // 表头（<th>）与整行「合计」不是单条奖品；class="total" 写在 <tr> 上，要查整段匹配。
            if (row.contains("<th", ignoreCase = true)) {
                starColumn = TableCell.findAll(row)
                    .map { cleanHtml(it.groupValues[1]) }
                    .indexOfFirst { it.contains("幸运星") }
                return@forEach
            }
            if (TotalRowClass.containsMatchIn(rowMatch.value)) return@forEach
            val cells = TableCell.findAll(row).map { cleanHtml(it.groupValues[1]) }.toList()
            if (cells.size < 3 || cells[0] == "合计") return@forEach
            val count = cells[1].toIntOrNull()?.takeIf { it > 0 } ?: 1
            val odds = cells[2].removeSuffix("%").trim().toDoubleOrNull()?.div(100.0)
                ?: return@forEach
            if (odds < 0.0 || odds > 1.0) return@forEach
            val stars = cells.getOrNull(if (starColumn >= 0) starColumn else 3)
                ?.toIntOrNull()
                ?: 0
            prizes += OddsItem(
                name = cells[0],
                count = count,
                // 转转条目没有图标：详情页的卡片会自动省掉左侧图标位。
                image = null,
                expected = stars > 0,
                odds = odds,
            )
        }
        if (prizes.isEmpty()) return null
        return LotteryDraw(
            name = name,
            image = null,
            description = DrawDescriptions[name],
            prizes = prizes,
            // 键不存在时按默认文案处理；显式写 null 才隐藏勘误卡。
            errata = DrawErrata.getOrElse(name) { "" },
        )
    }

    /** 去掉标签与实体，压平空白：单元格里通常只有纯文本，这里只是兜底。 */
    private fun cleanHtml(raw: String): String =
        unescapeEntities(Tag.replace(raw, " ")).replace(Whitespace, " ").trim()

    private fun unescapeEntities(raw: String): String {
        if ('&' !in raw) return raw
        return raw
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
    }

    /** 一次扫描出所有 h2/h3 与表格，按出现顺序处理：group1 = 标题级别，group2 = 标题，group3 = 表格。 */
    private val HeadingOrTable = Regex(
        "<h([23])[^>]*>(.*?)</h\\1>|<table[^>]*>(.*?)</table>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    /** 标题前的「一、」「二、」之类序号。 */
    private val SectionPrefix = Regex("^\\s*[一二三四五六七八九十]+、\\s*")

    private val TableRow = Regex(
        "<tr[^>]*>(.*?)</tr>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    private val TableCell = Regex(
        "<t[dh][^>]*>(.*?)</t[dh]>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    /** 「合计」行的标记：`<tr class="total">`。 */
    private val TotalRowClass = Regex("class\\s*=\\s*\"[^\"]*\\btotal\\b", RegexOption.IGNORE_CASE)

    private val Tag = Regex("<[^>]+>")

    private val Whitespace = Regex("\\s+")
}
