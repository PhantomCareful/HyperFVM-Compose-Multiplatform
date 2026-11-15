package com.careful.hyperfvm.compose.data.dashboard

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 游戏配置源，移植自 Java 版 `ActivityCatcher` / `FertilizationTaskCatcher` /
 * `NewYearCatcher` / `SweetIslandCatcher`。
 *
 * 同一份字段现在既算卡片状态，也拼出点击卡片后的详情弹层：
 * 双爆时段、活动起止时间、抢红包与甜蜜岛的开放场次。
 * 单个数据源失败时返回空 Map，由 [DashboardRepository] 决定是否整体判失败。
 */
internal object DashboardGameSources {

    private const val ActivityUrl = "https://cdn-qq-ms.123u.com/cdn.qq.123u.com/config/activity.xml"
    private const val FertilizationUrl = "https://cdn-qq-ms.123u.com/cdn.qq.123u.com/config/newtask.xml"
    private const val NewYearUrl = "https://cdn-qq-ms.123u.com/cdn.qq.123u.com/config/new_year.xml"
    private const val SweetIslandUrl = "https://cdn-qq-ms.123u.com/cdn.qq.123u.com/config/sweetisland.xml"

    /** 场次时间戳按北京时间解释：数据源就是照这个时区生成的，不随设备时区变化。 */
    private val BeiJing: ZoneId = ZoneId.of("GMT+8")

    /** 场次时段只显示时分，日期单独成段（场次卡的标题与摘要分工）。 */
    private val SessionClockFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /** 弹层里场次卡下方的提示，与 Java 版弹窗同义。 */
    private const val CalendarNote = "长按场次可添加到系统日历"

    /** 双爆最多预告三段，再多弹层就太长了。 */
    private const val MaxDoubleSegments = 3

    /** 双爆的两种形式：限时与全天（00:00-23:59）。 */
    private enum class DoubleKind { Timed, Full }

    /**
     * 双爆信息：仪表盘卡照旧——标题换成「限时双爆 / 全天双爆」，右侧写「持续到…，共 XX 天」，
     * 全天双爆整张卡转绿（见 [DashboardStyle.Green]）。
     *
     * 活动表每天都有一条双爆条目，只是类型在「限时 / 全天」之间切，
     * 所以从今天往后找连续同类型的那一段：段尾就是「持续到」，
     * 从今天到段尾的天数（含今天）就是「共 XX 天」。
     *
     * 弹层里从今天起至多给三个双爆时段，每个时段一整行的「标题 + 摘要」卡：
     * 标题是全/限时，摘要是整段的「开始日期 至 结束日期，共 XX 天」；
     * 全天双爆且整段超过 2 天的那一行转绿底（文字改走版本卡配色）。
     * 今天完全不在双爆段里（多是跨年时数据提供方还没更新次年配置）就整张卡不展示。
     */
    fun doubleExplosion(): Map<DashboardCardId, DashboardEntry> {
        val body = DashboardNetwork.fetch(ActivityUrl) ?: return emptyMap()
        return try {
            val activities = parseActivities(body)
            val today = DashboardDates.today()
            val segments = doubleSegments(activities, today)
            val current = segments.firstOrNull { !it.start.isAfter(today) && !it.end.isBefore(today) }
            val entry = when {
                segments.isEmpty() -> DashboardEntry(DashboardStatus.None, hidden = true)
                current != null -> dashboardEntryWithSheet(
                    id = DashboardCardId.DoubleExplosion,
                    status = "持续到${current.end}，共${DashboardDates.daysBetween(today, current.end)}天",
                    title = current.title,
                    style = if (current.kind == DoubleKind.Full) DashboardStyle.Green else DashboardStyle.Neutral,
                    times = segments.map { it.timeLine },
                    timesLayout = DashboardTimesLayout.Full,
                )
                // 今天不是双爆、后面还有预告：卡片还是「即将开始」。
                else -> dashboardEntryWithSheet(
                    id = DashboardCardId.DoubleExplosion,
                    status = "即将开始",
                    title = segments.first().title,
                    times = segments.map { it.timeLine },
                    timesLayout = DashboardTimesLayout.Full,
                )
            }
            mapOf(DashboardCardId.DoubleExplosion to entry)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /** 一段连续的双爆。[title] 是全/限时，[timeLine] 是弹层里那一行的时间卡。 */
    private data class DoubleSegment(val kind: DoubleKind, val start: LocalDate, val end: LocalDate) {
        val title: String get() = if (kind == DoubleKind.Full) "全天双爆" else "限时双爆"

        /** 整段天数（含首尾）。 */
        val days: Int get() = DashboardDates.daysBetween(start, end)

        /** 弹层里的整行时间卡：全天双爆且整段超过 2 天才转绿。 */
        val timeLine: DashboardTimeLine
            get() = DashboardTimeLine(
                label = title,
                value = when (start) {
                    end -> "$start，共1天"
                    else -> "$start 至 $end，共${days}天"
                },
                highlight = kind == DoubleKind.Full && days > 2,
            )
    }

    /**
     * 从今天起最多取 [MaxDoubleSegments] 段连续双爆。
     *
     * 今天落在某段中间时先把段首向前补全，弹层里给的是完整时段；
     * 今天不是双爆时从之后第一个双爆日开始。日期断档（配置里缺那天）同样算作段结束。
     */
    private fun doubleSegments(activities: Map<LocalDate, String>, today: LocalDate): List<DoubleSegment> {
        var cursor = if (doubleKind(activities[today]) != null) {
            var start = today
            // 只往前补到同类型的相邻日期：跨到上一段就停，段与段之间不会粘在一起。
            while (doubleKind(activities[start.minusDays(1)]) == doubleKind(activities[start])) {
                start = start.minusDays(1)
            }
            start
        } else {
            activities.keys
                .filter { it.isAfter(today) && doubleKind(activities[it]) != null }
                .minOrNull() ?: return emptyList()
        }
        val segments = mutableListOf<DoubleSegment>()
        while (segments.size < MaxDoubleSegments) {
            val kind = doubleKind(activities[cursor]) ?: break
            var end = cursor
            // 只往后连：往前连会把已经过去的同类型日子也算进去。
            while (doubleKind(activities[end.plusDays(1)]) == kind) end = end.plusDays(1)
            segments += DoubleSegment(kind, cursor, end)
            cursor = generateSequence(end.plusDays(1)) { it.plusDays(1) }
                .firstOrNull { activities[it] == null || doubleKind(activities[it]) != null } ?: break
        }
        return segments
    }

    /** 当天内容是双爆就返回它的形式；不是双爆或没有配置返回 null。 */
    private fun doubleKind(content: String?): DoubleKind? {
        // 同 Java 版：只看按 <br> 分开的第二段。
        val second = content?.split("<br>")?.getOrNull(1)?.trim().orEmpty()
        if (!second.contains("双爆")) return null
        return if (second.contains("00:00-23:59开启双倍双爆")) DoubleKind.Full else DoubleKind.Timed
    }

    /** 施肥任务 `<task id="39">` 的起止时间；`.*?` 需要跨行匹配。 */
    private val FertilizationTask = Regex(
        """<task id="39".*?startTime="(\d+)".*?endTime="(\d+)".*?</task>""",
        RegexOption.DOT_MATCHES_ALL,
    )

    /** 施肥活动：任务 id=39 的起止时间戳，进度固定按 21 天算。 */
    fun fertilization(): Map<DashboardCardId, DashboardEntry> {
        val body = DashboardNetwork.fetch(FertilizationUrl) ?: return emptyMap()
        return try {
            val match = FertilizationTask.find(body) ?: return emptyMap()
            val start = timestampToDate(match.groupValues[1]) ?: return emptyMap()
            val end = timestampToDate(match.groupValues[2]) ?: return emptyMap()
            val today = DashboardDates.today()
            val status = when {
                today.isBefore(start) -> "即将开始"
                today.isAfter(end) -> DashboardStatus.None
                else -> "${DashboardDates.daysBetween(start, today)}/21"
            }
            val entry = dashboardEntryWithSheet(
                id = DashboardCardId.Fertilization,
                status = status,
                times = timesOf(start, end),
            )
            mapOf(DashboardCardId.Fertilization to entry)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /**
     * 美食悬赏 / 百万消费 / 抢红包：三张卡共用一个 `new_year.xml`。
     *
     * 声望翻倍不再单独成卡，它的状态挂在美食悬赏的副标题上，开启时整张卡转绿底。
     * 各卡各自 try 一次，某一段格式变了不影响另外两张。
     */
    fun newYear(): Map<DashboardCardId, DashboardEntry> {
        val body = DashboardNetwork.fetch(NewYearUrl) ?: return emptyMap()
        val result = mutableMapOf<DashboardCardId, DashboardEntry>()
        val bountyDouble = runCatching { bountyDouble(body) }.getOrNull()
        result.putIfPresent(DashboardCardId.Bounty, runCatching { bounty(body, bountyDouble) }.getOrNull())
        result.putIfPresent(DashboardCardId.MillionConsumption, runCatching { millionConsumption(body) }.getOrNull())
        result.putIfPresent(DashboardCardId.LuckyMoney, runCatching { luckyMoney(body) }.getOrNull())
        return result
    }

    /** 甜蜜岛 / 走马观花：共用 `sweetisland.xml`，也是各自 try 一次。 */
    fun sweetIsland(): Map<DashboardCardId, DashboardEntry> {
        val body = DashboardNetwork.fetch(SweetIslandUrl) ?: return emptyMap()
        val result = mutableMapOf<DashboardCardId, DashboardEntry>()
        result.putIfPresent(
            DashboardCardId.SweetIsland,
            runCatching { sessionStatus(DashboardCardId.SweetIsland, body, SystemOpenBlock, firstMapOnly = false) }
                .getOrNull(),
        )
        result.putIfPresent(
            DashboardCardId.AnimalActivity,
            runCatching { sessionStatus(DashboardCardId.AnimalActivity, body, TjbzOpenBlock, firstMapOnly = true) }
                .getOrNull(),
        )
        return result
    }

    // ---- 双爆信息 ----

    /** `activity` 节点的 `time` 是日期、`content` 里用 `<br>` 分段。 */
    private fun parseActivities(body: String): Map<LocalDate, String> {
        val document = XmlDom.parse(body) ?: return emptyMap()
        val result = mutableMapOf<LocalDate, String>()
        XmlDom.elements(document, "activity").forEach { element ->
            val date = DashboardDates.parse(XmlDom.attr(element, "time"))
            val content = XmlDom.attr(element, "content")
            if (date != null && !content.isNullOrBlank()) result[date] = content
        }
        return result
    }

    // ---- 声望翻倍 ----

    /**
     * `dayMax` 超过 220 就是开了翻倍。这一段不再单独成卡：
     * [text] 作为美食悬赏的副标题，[enabled] 决定它的卡面要不要转绿。
     */
    private data class DoubleSummary(val enabled: Boolean, val text: String)

    private fun bountyDouble(body: String): DoubleSummary? {
        val dayMax = body.substringAfter("dayMax=\"", "")
            .substringBefore("\"", "")
            .toIntOrNull() ?: return null
        val enabled = dayMax > 220
        return DoubleSummary(
            enabled = enabled,
            text = "声望翻倍${if (enabled) "已开启" else "未开启"}，单日上限 $dayMax",
        )
    }

    // ---- 美食悬赏 / 百万消费 ----

    private val BountyTime = Regex("""活动时间:\s*(\d{1,2})月(\d{1,2})日-(\d{1,2})月(\d{1,2})日10:00""")

    /**
     * 百万消费的日期后面紧跟着活动说明，用这段说明把它和美食悬赏区分开。
     * 只保留开头的「以下方式消费点券可领取豪礼：」，后面那串奖励清单改起来太勤。
     */
    private val MillionConsumptionTime = Regex(
        """(\d{1,2})月(\d{1,2})日-(\d{1,2})月(\d{1,2})日10:00"\s*reddes="以下方式消费点券可领取豪礼：""",
    )

    private fun bounty(body: String, double: DoubleSummary?): DashboardEntry? {
        val match = BountyTime.find(body) ?: return null
        val range = resolveMonths(match) ?: return null
        return rangeEntry(
            id = DashboardCardId.Bounty,
            start = range.first,
            end = range.second,
            detail = double?.text.orEmpty(),
            // 翻倍只在活动进行中才有意义：打开时只把进度条与 X/Y 换成绿色，卡面仍是白底。
            activeStyle = if (double?.enabled == true) DashboardStyle.GreenProgress else DashboardStyle.Neutral,
            images = listOf("tiramisu_image_2_2"),
        )
    }

    private fun millionConsumption(body: String): DashboardEntry? {
        val match = MillionConsumptionTime.find(body) ?: return null
        val range = resolveMonths(match) ?: return null
        return rangeEntry(
            id = DashboardCardId.MillionConsumption,
            start = range.first,
            end = range.second,
            // 消费类活动专属提醒：竖在弹层最顶部的错误色卡里。
            errata = "适度游戏，理性消费",
            images = listOf(
                "tiramisu_image_1_3_1",
                "tiramisu_image_1_3_2",
                "tiramisu_image_1_3_3",
            ),
        )
    }

    /**
     * 配置里的日期只有「X月X日」，需要按当前月份补年份。
     * 跨年活动（开始月大于结束月）分两种情况，规则与 Java 版一致：
     * 现在是开始月 → 结束在明年；现在是结束月 → 开始在去年；都不是就判失败。
     */
    private fun resolveMonths(match: MatchResult): Pair<LocalDate, LocalDate>? {
        val startMonth = match.groupValues[1].toInt()
        val startDay = match.groupValues[2].toInt()
        val endMonth = match.groupValues[3].toInt()
        val endDay = match.groupValues[4].toInt()
        val today = DashboardDates.today()
        val years = when {
            startMonth > endMonth -> when (today.monthValue) {
                startMonth -> today.year to today.year + 1
                endMonth -> today.year - 1 to today.year
                else -> return null
            }
            else -> today.year to today.year
        }
        return try {
            LocalDate.of(years.first, startMonth, startDay) to
                LocalDate.of(years.second, endMonth, endDay)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 悬赏与百万消费共用：进度天数要减 1，没有结束当天 10 点的截断；
     * 弹层里给起止日期与各卡自己的米鼠图。
     */
    private fun rangeEntry(
        id: DashboardCardId,
        start: LocalDate,
        end: LocalDate,
        detail: String = "",
        activeStyle: DashboardStyle = DashboardStyle.Neutral,
        images: List<String> = emptyList(),
        errata: String? = null,
    ): DashboardEntry {
        val today = DashboardDates.today()
        val active = !today.isBefore(start) && !today.isAfter(end)
        val status = when {
            today.isBefore(start) -> "即将开始"
            today.isAfter(end) -> DashboardStatus.None
            else -> {
                val length = DashboardDates.daysBetween(start, end) - 1
                "${DashboardDates.daysBetween(start, today)}/$length"
            }
        }
        return dashboardEntryWithSheet(
            id = id,
            status = status,
            detail = detail,
            style = if (active) activeStyle else DashboardStyle.Neutral,
            times = timesOf(start, end),
            images = images,
            errata = errata,
        )
    }

    // ---- 抢红包 ----

    private val LuckyMoneyBlock = Regex("""<LuckyMoney\b[^>]*>[\s\S]*?</LuckyMoney>""")
    private val LiTag = Regex("""<li\b[^>]*>""")
    private val BeginTimeAttr = Regex("""beginTime\s*=\s*"([^"]*)"""")
    private val EndTimeAttr = Regex("""endTime\s*=\s*"([^"]*)"""")
    private val TotalAttr = Regex("""total\s*=\s*"([^"]*)"""")
    private val UserNumAttr = Regex("""userNum\s*=\s*"([^"]*)"""")
    private val MinGetAttr = Regex("""minGet\s*=\s*"([^"]*)"""")
    private val MaxGetAttr = Regex("""maxGet\s*=\s*"([^"]*)"""")

    /**
     * 今天有场次就是「今日开启」（勘误卡同款的醒目红），点卡片先弹提示、
     * 说明今天的场次时段；今天没有则显示下一场的日期，未来一场都没有就不展示。
     *
     * 场次卡跟随 Java 版：整行大卡，除日期时段外还带红包总金额、个数与单个金额区间。
     */
    private fun luckyMoney(body: String): DashboardEntry? {
        val block = LuckyMoneyBlock.find(body)?.value ?: return null
        val sessions = parseLuckyMoneySessions(block) ?: return null
        val now = Instant.now().epochSecond
        val today = LocalDate.now(BeiJing)
        // 今天有场次（哪怕已经结束）就仍算「今日开启」，提示取还在进行或还没开始的那一场。
        val todaySessions = sessions.filter { beijingDate(it.begin) == today }
        val todaySession = todaySessions.firstOrNull { it.end > now } ?: todaySessions.firstOrNull()
        val upcoming = sessions.filter { it.end > now }.map(::sessionOf)
        if (todaySession != null) {
            val tip = "今日${beijingClock(todaySession.begin)}到${beijingClock(todaySession.end)}期间有抢红包活动，" +
                "不同服务器开启时刻不同，请进入游戏查看具体开始时刻"
            return dashboardEntryWithSheet(
                id = DashboardCardId.LuckyMoney,
                status = "今日开启",
                style = DashboardStyle.Red,
                sessions = upcoming,
                sessionLayout = DashboardSessionLayout.Full,
                note = CalendarNote,
                todayTip = tip,
            )
        }
        val next = sessions.firstOrNull { it.begin > now } ?: return DashboardEntry(
            DashboardStatus.None,
            hidden = true,
        )
        return dashboardEntryWithSheet(
            id = DashboardCardId.LuckyMoney,
            status = beijingDate(next.begin).toString(),
            sessions = upcoming,
            sessionLayout = DashboardSessionLayout.Full,
            note = CalendarNote,
        )
    }

    // ---- 甜蜜岛 / 走马观花 ----

    private val SystemOpenBlock = Regex("""<systemOpen\b[^>]*>[\s\S]*?</systemOpen>""")
    private val TjbzOpenBlock = Regex("""<tjbz_Open\b[^>]*>[\s\S]*?</tjbz_Open>""")
    private val MapBlock = Regex("""<map\b[^>]*>[\s\S]*?</map>""")
    private val OpenTag = Regex("""<open\b[^>]*>""")
    private val StartTimeAttr = Regex("""startTime\s*=\s*"([^"]*)"""")

    /**
     * 两个活动共用一套状态判断：今天那场还没结束就是「今日开启」（状态卡绿底），
     * 否则显示接下来最近的开启日期，一场都不剩就整张卡不展示。
     * 走马观花只看 `tjbz_Open` 里第一个 `map`。
     */
    private fun sessionStatus(
        id: DashboardCardId,
        body: String,
        blockPattern: Regex,
        firstMapOnly: Boolean,
    ): DashboardEntry? {
        val block = blockPattern.find(body)?.value ?: return null
        val scope = if (firstMapOnly) MapBlock.find(block)?.value ?: return null else block
        val sessions = parseSessions(scope, OpenTag, StartTimeAttr, EndTimeAttr) ?: return null
        val now = Instant.now().epochSecond
        val today = LocalDate.now(BeiJing)
        val upcoming = sessions.filter { it.second > now }.map(::sessionOf)
        // 今天还有没结束的场次（正在进行的、或今天稍后开放的）就算「今日开启」。
        val activeToday = sessions.any {
            it.second > now && (it.first <= now || beijingDate(it.first) == today)
        }
        return when {
            activeToday -> dashboardEntryWithSheet(
                id = id,
                status = "今日开启",
                style = DashboardStyle.Green,
                sessions = upcoming,
                note = CalendarNote,
            )
            // 今天没开、以后也没有场次：这张卡本轮不展示。
            upcoming.isEmpty() -> DashboardEntry(DashboardStatus.None, hidden = true)
            else -> {
                // 全部场次里排在今天之后的第一场；跨零点的场次把日期往后再推一格。
                val next = sessions.firstOrNull { it.first > now && beijingDate(it.first) != today }
                    ?: sessions.firstOrNull { it.first > now }
                    ?: return null
                dashboardEntryWithSheet(
                    id = id,
                    status = beijingDate(next.first).toString(),
                    sessions = upcoming,
                    note = CalendarNote,
                )
            }
        }
    }

    // ---- 公共工具 ----

    /** 秒级时间戳列表（已按开始时间升序）→ 起止对；一条都解析不出来返回 null。 */
    private fun parseSessions(
        scope: String,
        tagPattern: Regex,
        beginPattern: Regex,
        endPattern: Regex,
    ): List<Pair<Long, Long>>? {
        val sessions = tagPattern.findAll(scope).mapNotNull { tag ->
            val begin = attribute(tag.value, beginPattern)?.toLongOrNull() ?: return@mapNotNull null
            val end = attribute(tag.value, endPattern)?.toLongOrNull() ?: return@mapNotNull null
            if (begin <= 0L || end < begin) return@mapNotNull null
            begin to end
        }.sortedBy { it.first }.toList()
        return sessions.takeIf { it.isNotEmpty() }
    }

    /** 抢红包的一场：除起止时间戳外还带红包总金额、个数与单个金额区间（单位 D）。 */
    private data class LuckyMoneySession(
        val begin: Long,
        val end: Long,
        val total: Int,
        val userNum: Int,
        val minGet: Int,
        val maxGet: Int,
    )

    /** 抢红包场次：起止时间与四个红包字段都齐全才算一场，按开始时间升序。 */
    private fun parseLuckyMoneySessions(scope: String): List<LuckyMoneySession>? {
        val sessions = LiTag.findAll(scope).mapNotNull { tag ->
            val begin = attribute(tag.value, BeginTimeAttr)?.toLongOrNull() ?: return@mapNotNull null
            val end = attribute(tag.value, EndTimeAttr)?.toLongOrNull() ?: return@mapNotNull null
            val total = attribute(tag.value, TotalAttr)?.toIntOrNull() ?: return@mapNotNull null
            val userNum = attribute(tag.value, UserNumAttr)?.toIntOrNull() ?: return@mapNotNull null
            val minGet = attribute(tag.value, MinGetAttr)?.toIntOrNull() ?: return@mapNotNull null
            val maxGet = attribute(tag.value, MaxGetAttr)?.toIntOrNull() ?: return@mapNotNull null
            if (begin <= 0L || end < begin) return@mapNotNull null
            LuckyMoneySession(begin, end, total, userNum, minGet, maxGet)
        }.sortedBy { it.begin }.toList()
        return sessions.takeIf { it.isNotEmpty() }
    }

    /**
     * 场次卡的数据：日期与时段分开存，界面拿日期当标题、时段当摘要。
     * 抢红包额外带上红包信息；手机双行、PAD 单行由界面层决定。
     */
    private fun sessionOf(range: Pair<Long, Long>): DashboardSession = DashboardSession(
        beginEpochSecond = range.first,
        endEpochSecond = range.second,
        date = beijingDate(range.first).toString(),
        time = "${beijingClock(range.first)} - ${beijingClock(range.second)}",
    )

    private fun sessionOf(session: LuckyMoneySession): DashboardSession = DashboardSession(
        beginEpochSecond = session.begin,
        endEpochSecond = session.end,
        date = beijingDate(session.begin).toString(),
        time = "${beijingClock(session.begin)} - ${beijingClock(session.end)}",
        description = "共${session.total}D，${session.userNum}个红包\n" +
            "每个红包最少${session.minGet}D，最多${session.maxGet}D",
    )

    private fun attribute(tag: String, pattern: Regex): String? =
        pattern.find(tag)?.groupValues?.get(1)?.trim()

    private fun beijingDateTime(epochSeconds: Long) =
        Instant.ofEpochSecond(epochSeconds).atZone(BeiJing)

    /** 场次时段：只取 `HH:mm`，日期由 [beijingDate] 单独给，与 Java 版场次类一致。 */
    private fun beijingClock(epochSeconds: Long): String =
        beijingDateTime(epochSeconds).format(SessionClockFormatter)

    private fun beijingDate(epochSeconds: Long): LocalDate =
        Instant.ofEpochSecond(epochSeconds).atZone(BeiJing).toLocalDate()

    private fun timesOf(start: LocalDate, end: LocalDate): List<DashboardTimeLine> = listOf(
        DashboardTimeLine("开始时间", start.toString()),
        DashboardTimeLine("结束时间", end.toString()),
    )

    /** 秒级时间戳 → 设备本地时区的自然日，同 Java 版 `convertTimeStampToDate`。 */
    private fun timestampToDate(text: String): LocalDate? =
        text.toLongOrNull()?.let {
            Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault()).toLocalDate()
        }

    private fun MutableMap<DashboardCardId, DashboardEntry>.putIfPresent(
        id: DashboardCardId,
        entry: DashboardEntry?,
    ) {
        if (entry != null) this[id] = entry
    }
}
