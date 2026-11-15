package com.careful.hyperfvm.compose.data.dashboard

import androidx.compose.runtime.Immutable
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * 仪表盘卡片的编号。
 *
 * 枚举顺序就是 Java 版 `fragment_dashboard.xml` 里卡片自上而下的顺序，
 * 首次进入时按这个顺序展示；用户拖动排序后以本地保存的顺序为准。
 * [title] 与 Java 版 `title_dashboard_*` 文案一致。
 *
 * 温馨礼包与更新公告固定并排占一行（见 [DashboardSlot.GiftPair]），
 * 其余卡片各占一行。App 公告（`global_notification`）后续会换新实现，本轮不迁移；
 * 声望翻倍也不单独成卡，它的状态并进了美食悬赏的副标题。
 */
enum class DashboardCardId(val title: String) {
    MonthEnd("月末提醒"),
    SweetGift("温馨礼包"),
    UpdateNotice("更新公告"),
    DoubleExplosion("双爆信息"),
    DailyCheckIn("每日签到"),
    Fertilization("施肥活动"),
    BirthdayActivity("福利打卡"),
    Bounty("美食悬赏"),
    LuckyMoney("抢红包"),
    ThreeIslands("三岛福利"),
    FoodContest("美食大赛"),
    CampTask("营地任务"),
    CrossServerTeamUp("助人为乐"),
    HappyHoliday("欢乐假期"),
    WorldBoss("巅峰对决"),
    AnimalActivity("走马观花"),
    SweetIsland("甜蜜岛"),
    TransferDiscount("二转打折"),
    CryStoneDiscount("结晶强化打折"),
    WeddingDiscount("豪华婚礼打折"),
    DailyRecharge("日氪"),
    MillionConsumption("百万消费"),
}

/** 与温馨礼包同处一行、固定展示、不能单独隐藏的卡片。 */
val DashboardCardId.isPairedWithGift: Boolean
    get() = this == DashboardCardId.SweetGift || this == DashboardCardId.UpdateNotice

/**
 * 仪表盘上的一行。
 *
 * 绝大多数卡独占一行；温馨礼包与更新公告并排放在同一行里，作为整体参与排序，
 * 所以在顺序、拖动和卡片管理里都只算一项。
 */
sealed interface DashboardSlot {

    /** 独占一行的普通卡片。 */
    data class Single(val id: DashboardCardId) : DashboardSlot

    /** 温馨礼包 + 更新公告的并排行：固定显示，不参与显隐。 */
    data object GiftPair : DashboardSlot

    companion object {

        /** 存进 SharedPreferences 的标记，普通卡直接存枚举名。 */
        const val PairToken = "giftPair"

        /** 默认顺序：按枚举顺序排，两张成对的卡归到同一个 [GiftPair]。 */
        val defaults: List<DashboardSlot> = DashboardCardId.entries
            .map(::of)
            .distinct()

        fun of(id: DashboardCardId): DashboardSlot =
            if (id.isPairedWithGift) GiftPair else Single(id)

        /** 旧版本把两张成对的卡分别存成 `SweetGift` / `UpdateNotice`，这里一并归成 [GiftPair]。 */
        fun fromToken(token: String): DashboardSlot? {
            if (token == PairToken) return GiftPair
            val id = DashboardCardId.entries.firstOrNull { it.name == token } ?: return null
            return of(id)
        }
    }
}

/** 存盘用的字符串：成对的行用固定标记，其余用卡片枚举名。 */
val DashboardSlot.token: String
    get() = when (this) {
        DashboardSlot.GiftPair -> DashboardSlot.PairToken
        is DashboardSlot.Single -> id.name
    }

/** 成对的行永远显示；普通卡看它有没有被关掉。 */
fun DashboardSlot.isVisible(hidden: Set<DashboardCardId>): Boolean = when (this) {
    DashboardSlot.GiftPair -> true
    is DashboardSlot.Single -> id !in hidden
}

/** 卡片状态文案里的固定值，与 Java 版逐字一致。 */
object DashboardStatus {
    /** 首屏数据还没回来时的占位，同 Java 版刷新前的「请等待...」。 */
    const val Pending = "请等待…"

    /** 数据源返回缺失或格式对不上时的兜底。 */
    const val Failed = "获取失败"

    /** 这张卡当前没有活动；弹层标题会因此加上「（上一轮）」。 */
    const val None = "暂无"
}

/** 「12/21」这类进行中的数字状态，也是唯一会在卡片背景上画进度的状态。 */
private val RatioStatus = Regex("""^(\d+)/(\d+)$""")

/**
 * 卡片的整体外观，把「底色」与「强调色」两个维度归并成四种组合，
 * 具体的取色规则全在 `dashboardCardPalette` 里，数据层只需要选型号。
 *
 * - [Neutral]：普通白底卡，绝大多数卡都是这种；
 * - [Green]：状态绿底卡 —— 全天双爆、今日开启的走马观花与甜蜜岛、可以领奖的每日签到；
 * - [GreenProgress]：白底，只有进度条与 X/Y 用绿 —— 声望翻倍开启的美食悬赏；
 * - [Red]：勘误卡同款的醒目红 —— 今日抢红包（恭喜发财）。
 */
enum class DashboardStyle { Neutral, Green, GreenProgress, Red }

/** 弹层里的一行时间。
 *
 * [label] 是左侧标签（开始时间 / 全天双爆等），[value] 是日期或时间段。
 * [highlight] 只对整行时间卡（[DashboardTimesLayout.Full]）生效：这一行值得强调时
 * 换成绿底卡，文字走版本卡那套配色。
 */
@Immutable
data class DashboardTimeLine(
    val label: String,
    val value: String,
    val highlight: Boolean = false,
)

/** 弹层时间区的排布：两列概览卡（默认）或每行一张标准卡（双爆）。 */
enum class DashboardTimesLayout { Grid, Full }

/**
 * 弹层里的一场活动（抢红包 / 走马观花 / 甜蜜岛）。
 *
 * [date] 是场次日期（`yyyy-MM-dd`），[time] 是场次时段（`HH:mm - HH:mm`），
 * 场次卡拿它们当标题与摘要；[description] 是额外的场次信息（抢红包的红包金额与个数），
 * 手机上是两行、PAD 上合成一行，其余活动为 null。
 * 起止时间戳留着给「长按添加到系统日历」用，同样按北京时间解释。
 */
@Immutable
data class DashboardSession(
    val beginEpochSecond: Long,
    val endEpochSecond: Long,
    val date: String,
    val time: String,
    val description: String? = null,
)

/** 场次卡片的排布：两列网格（岛类活动）或整行大卡（抢红包，跟随 Java 版布局）。 */
enum class DashboardSessionLayout { Grid, Full }

/**
 * 弹层底部的按钮：营地任务顺序表这类外链，以及巅峰对决的「赛季规则 / 赛季奖励」
 * （指向 B 站图文动态，点开就在 App 内解析阅读）。
 */
@Immutable
data class DashboardLink(val label: String, val url: String)

/**
 * 卡片点击后弹出的详情。
 *
 * 各卡按需填字段：时间概览卡、卡片名单（SmallTitle + 防御卡 Card）、
 * 「米鼠的图」预览卡、场次、底部链接按钮。活动状态不在这里重复展示——卡片上已经有了。
 * [title] 已经算好「（上一轮）」后缀，界面直接拿来当弹层标题。
 */
@Immutable
data class DashboardSheet(
    val title: String,
    /** 这一轮没有活动、弹层里看到的是上一轮安排；弹层顶部补一句说明。 */
    val previousRound: Boolean = false,
    /** 错误色提示卡的文案（百万消费用它放理性消费提醒）；为空则不显示。 */
    val errata: String? = null,
    val times: List<DashboardTimeLine> = emptyList(),
    val timesLayout: DashboardTimesLayout = DashboardTimesLayout.Grid,
    val sectionTitle: String? = null,
    val cards: List<String> = emptyList(),
    /** 本地卡片目录版本不够时的提示卡文案，与 Java 版逐字一致。 */
    val tips: String? = null,
    /** 这张卡相关的「米鼠的图」，存数据图目录里的文件名，界面直接铺预览卡。 */
    val images: List<String> = emptyList(),
    val sessions: List<DashboardSession> = emptyList(),
    val sessionLayout: DashboardSessionLayout = DashboardSessionLayout.Grid,
    val note: String? = null,
    val links: List<DashboardLink> = emptyList(),
    /**
     * 抢红包专用：今天有场次时点卡片弹这句提示，不打开弹层；
     * 其余情况为 null，点卡片走正常弹层。
     */
    val todayTip: String? = null,
)

/**
 * 一张仪表盘卡片的展示数据。
 *
 * [status] 是卡片右侧那句简要文案（「暂无」「即将开始」「12/21」等）；
 * [detail] 是标题下方的补充说明（目前只有美食悬赏用它显示声望翻倍状态）；
 * [title] 用于覆盖枚举里的固定标题：双爆要换成「限时双爆 / 全天双爆」，
 * 二转打折要把卡表张数接在标题后面。
 *
 * [sheet] 是点击后弹出的详情；[hidden] 用于「没有则不展示」的卡片
 * （抢红包 / 走马观花 / 甜蜜岛 / 双爆）：数据源判定整张卡不该出现时置 true。
 */
@Immutable
data class DashboardEntry(
    val status: String,
    val detail: String = "",
    val title: String? = null,
    val style: DashboardStyle = DashboardStyle.Neutral,
    val sheet: DashboardSheet? = null,
    val hidden: Boolean = false,
) {
    /** 0..1 的进度；非「X/Y」状态返回 null，卡片不画进度背景。 */
    val progress: Float?
        get() {
            val match = RatioStatus.matchEntire(status) ?: return null
            val total = match.groupValues[2].toFloatOrNull() ?: return null
            if (total <= 0f) return null
            return (match.groupValues[1].toFloat() / total).coerceIn(0f, 1f)
        }
}

/** 「没有的时候也展示」时，弹层顶部说明看到的是上一轮。 */
internal const val DashboardPreviousRoundHint = "活动已结束，以下是上一轮的安排"

/**
 * 状态 + 详情一起拼成卡片数据。
 *
 * 状态为 [DashboardStatus.None] 时弹层标题补上「（上一轮）」：这一轮没有活动，
 * 弹层里的时间、名单与按钮都是上一轮留下的。
 */
internal fun dashboardEntryWithSheet(
    id: DashboardCardId,
    status: String,
    title: String? = null,
    detail: String = "",
    style: DashboardStyle = DashboardStyle.Neutral,
    times: List<DashboardTimeLine> = emptyList(),
    timesLayout: DashboardTimesLayout = DashboardTimesLayout.Grid,
    sectionTitle: String? = null,
    cards: List<String> = emptyList(),
    tips: String? = null,
    images: List<String> = emptyList(),
    sessions: List<DashboardSession> = emptyList(),
    sessionLayout: DashboardSessionLayout = DashboardSessionLayout.Grid,
    note: String? = null,
    links: List<DashboardLink> = emptyList(),
    todayTip: String? = null,
    errata: String? = null,
    hidden: Boolean = false,
): DashboardEntry = DashboardEntry(
    status = status,
    detail = detail,
    title = title,
    style = style,
    hidden = hidden,
    sheet = DashboardSheet(
        title = if (status == DashboardStatus.None) "${id.title}（上一轮）" else id.title,
        previousRound = status == DashboardStatus.None,
        errata = errata,
        times = times,
        timesLayout = timesLayout,
        sectionTitle = sectionTitle,
        cards = cards,
        tips = tips,
        images = images,
        sessions = sessions,
        sessionLayout = sessionLayout,
        note = note,
        links = links,
        todayTip = todayTip,
    ),
)

/** 今天是不是当月最后一天：月末提醒卡只在今天出现。 */
fun isMonthEnd(): Boolean {
    val today = LocalDate.now()
    return today.dayOfMonth == today.lengthOfMonth()
}

/**
 * 日期计算，等价于 Java 版 `TimeUtil` 里仪表盘用到的几个方法。
 *
 * 全部按设备本地时区的自然日计算；只有 sweetisland / new_year 的场次时间戳
 * 因为数据源按北京时间生成，才单独走 GMT+8（见 [DashboardGameSources]）。
 */
internal object DashboardDates {

    /** `yyyy-M-d`：既能读补零的 "2026-11-05"，也能读游戏配置里不补零的 "2026-11-5"。 */
    private val Formatter = DateTimeFormatter.ofPattern("yyyy-M-d")

    fun today(): LocalDate = LocalDate.now()

    fun todayText(): String = today().toString()

    /** 当前小时（0-23）。活动结束当天的 10 点截断要用它。 */
    fun hour(): Int = LocalTime.now().hour

    /** 含首尾的天数差，同一天返回 1，与 Java 版 `calculateDaysBetween` 一致。 */
    fun daysBetween(start: LocalDate, end: LocalDate): Int =
        (ChronoUnit.DAYS.between(start, end) + 1).toInt()

    /** 解析失败返回 null，交给调用方走「获取失败」兜底。 */
    fun parse(text: String?): LocalDate? {
        if (text.isNullOrBlank()) return null
        return try {
            LocalDate.parse(text.trim(), Formatter)
        } catch (_: Exception) {
            null
        }
    }
}
