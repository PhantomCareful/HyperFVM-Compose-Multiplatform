package com.careful.hyperfvm.compose.navigation

import top.yukonga.miuix.kmp.nav.core.NavKey

/**
 * 应用内的全部导航目标。
 *
 * 五个一级界面在 [Main] 里由 Pager 承载，不参与压栈；这里只定义需要叠加在
 * 一级界面之上的二级页面。栈用 `navBackStackOf` 建在内存里，要跨 Activity 重建
 * 存活时靠 [com.careful.hyperfvm.compose.navigation.NavBackStackSaver] 存成 [id] 列表。
 */
sealed interface Route : NavKey {

    /** 主框架：仪表盘 / 防御卡 / 数据中心 / 实用工具 / 设置。 */
    data object Main : Route

    /**
     * 温馨礼包链接管理页：账号列表、添加链接、礼包清单与使用说明。
     *
     * 账号数据存在 [com.careful.hyperfvm.compose.data.meishi.MeishiAccounts] 里，
     * 路由不带参数；仪表盘卡片的领取结果也直接读同一份状态。
     */
    data object MeishiWechat : Route

    /**
     * 收件箱：云端通知的本地存档，入口在仪表盘顶栏左上角。
     *
     * 列表全部来自 [com.careful.hyperfvm.compose.data.notification.NotificationRepository]，
     * 路由不带参数；收件发生在启动与仪表盘刷新时，不在本页。
     */
    data object NotificationInbox : Route

    /** 关于页，同时也是「转场样式」设置的观察入口。 */
    data object About : Route

    /** 授权检查页：四项系统授权的跳转入口与授予状态。 */
    data object Permission : Route

    /** 共建团页：致谢参与开发的同学名单。 */
    data object CoContributorTeam : Route

    /** 致谢页（游戏相关）。 */
    data object ThanksGame : Route

    /** 致谢页（App相关）。 */
    data object ThanksApp : Route

    /** 今日运势页：4 个不同速率播放的海星动图，定格算分。 */
    data object TodayLucky : Route

    /** 资源储备计算器页：按道具数量折算威望 / 金币 / 礼券 / 魅力币的储备总量。 */
    data object ResourceCalculator : Route

    /** 强卡成功率计算器页：按主卡/副卡/四叶草/VIP/公会组合估算强化成功率。 */
    data object CardOddsCalculator : Route

    /** 概率公示合集目录页：宝箱概率 / 抽奖概率两页，按类别分列全部条目。 */
    data object OddsDisclosure : Route

    /**
     * 单个宝箱的概率公示详情页。
     *
     * Java 版用 Intent 传整个 Parcelable 数组，这里只存两个下标：内容从静态的
     * [com.careful.hyperfvm.compose.data.oddsDisclosure.OddsDataRepository] 里取，
     * 返回栈恢复时只要有下标就能拿到同一份数据。
     */
    data class OddsDisclosureInfo(val categoryIndex: Int, val boxIndex: Int) : Route

    /**
     * 单场抽奖的概率公示详情页。
     *
     * 与 [OddsDisclosureInfo] 同构：只存两个下标，内容从静态的
     * [com.careful.hyperfvm.compose.data.oddsDisclosure.OddsDataRepository] 里取，
     * 返回栈恢复时只要有下标就能拿到同一份数据。
     */
    data class LotteryOddsInfo(val categoryIndex: Int, val drawIndex: Int) : Route

    /** 数据图合集页：全部数据图的瀑布流，点开图片在看器覆盖层里浏览。 */
    data object DataImages : Route

    /**
     * 数据包更新页：与首次更新共用同一套界面，只是标题与副标题不同。
     *
     * 由仪表盘的「数据包有新版本」提示卡进入；用户点下载之前可以返回，
     * 一旦开始下载就锁在本页，直到「开启新世界」才回到仪表盘。
     */
    data object DataPackUpdate : Route

    /**
     * 套装合集页：全部套装的瀑布流，顶栏筛选性别、下方搜索，
     * 点卡片在同一页的弹层里看基础信息与续费价格。
     */
    data object ClothingCollection : Route

    /**
     * 翅膀合集页：与套装合集同款布局，顶栏筛选性别、下方搜索，
     * 点卡片在弹层里看基础信息与续费价格。
     */
    data object WingCollection : Route

    /** 食神谱合集目录页：按上等佳肴 / 秘制佳肴 / 极品佳肴分组列出全部食神谱。 */
    data object CookeryIndex : Route

    /**
     * 查黑系统的骗子详情页，对应 Java 版 `IcuFraudActivity`。
     *
     * 只存 QQ 号：内容优先从 [com.careful.hyperfvm.compose.data.fraud.FraudRepository]
     * 的查询缓存取，进程回收后缓存为空时详情页会按这个号重查。
     */
    data class IcuFraud(val qq: String) : Route

    /**
     * 查黑系统的名单页：展示全部已标记的 QQ 号，在本地名单里搜索。
     *
     * 名单不随路由传递，进页时从 [com.careful.hyperfvm.compose.data.fraud.FraudRepository]
     * 拉取；详情页与名单页之间不互相携带数据。
     */
    data object FraudList : Route

    /**
     * 更新公告页：在 App 内直接浏览云端仓库里那条 B 站图文公告。
     *
     * 不带参数：页面自己拉最新链接，同一期公告的正文与图片都有本地缓存。
     */
    data object Announcement : Route

    /**
     * 更新公告合集页：B 站「版本更新」文集里的全部历史公告，一期一张卡。
     *
     * 列表与正文都由页面自己拉；合集与每期的正文都有磁盘缓存。
     */
    data object AnnouncementArchive : Route

    /**
     * 合集里的某一期公告，对应 Java 版的 cv 专栏。
     *
     * 只存 cv 号：标题与正文由页面自己拉，进程回收后重新打开也能照常加载。
     */
    data class AnnouncementArticle(val cvId: String) : Route

    /**
     * 任意一条哔哩哔哩图文动态（opus）的阅读页，目前用于巅峰对决弹层里的
     * 「赛季规则 / 赛季奖励」。
     *
     * 正文复用更新公告那套解析与缓存；只存 opus id 与顶栏标题，进程回收后
     * 重新打开也能照常加载。
     */
    data class BilibiliOpus(val opusId: String, val title: String) : Route

    /** 写进 Bundle 时用的稳定标识；新增页面必须在这里补一条。 */
    val id: String
        get() = when (this) {
            Main -> "main"
            MeishiWechat -> "meishiWechat"
            NotificationInbox -> "notificationInbox"
            About -> "about"
            Announcement -> "announcement"
            AnnouncementArchive -> "announcementArchive"
            is AnnouncementArticle -> "announcementArticle/$cvId"
            is BilibiliOpus -> "bilibiliOpus/$opusId/$title"
            Permission -> "permission"
            CoContributorTeam -> "coContributorTeam"
            ThanksGame -> "thanksGame"
            ThanksApp -> "thanksApp"
            TodayLucky -> "todayLucky"
            ResourceCalculator -> "resourceCalculator"
            CardOddsCalculator -> "cardOddsCalculator"
            OddsDisclosure -> "oddsDisclosure"
            is OddsDisclosureInfo -> "oddsDisclosureInfo/$categoryIndex/$boxIndex"
            is LotteryOddsInfo -> "lotteryOddsInfo/$categoryIndex/$drawIndex"
            DataImages -> "dataImages"
            DataPackUpdate -> "dataPackUpdate"
            ClothingCollection -> "clothingCollection"
            WingCollection -> "wingCollection"
            CookeryIndex -> "cookeryIndex"
            is IcuFraud -> "icuFraud/$qq"
            FraudList -> "fraudList"
        }

    companion object {
        /** [OddsDisclosureInfo] 的 id 前缀，后面跟「类别下标/宝箱下标」。 */
        private const val OddsDisclosureInfoIdPrefix = "oddsDisclosureInfo/"

        /** [LotteryOddsInfo] 的 id 前缀，后面跟「类别下标/抽奖下标」。 */
        private const val LotteryOddsInfoIdPrefix = "lotteryOddsInfo/"

        /** [IcuFraud] 的 id 前缀，后面跟 QQ 号。 */
        private const val IcuFraudIdPrefix = "icuFraud/"

        /** [AnnouncementArticle] 的 id 前缀，后面跟 cv 号。 */
        private const val AnnouncementArticleIdPrefix = "announcementArticle/"

        /** [BilibiliOpus] 的 id 前缀，后面跟「opus id/标题」。 */
        private const val BilibiliOpusIdPrefix = "bilibiliOpus/"

        fun fromId(id: String): Route? {
            // 带参数的页面得先把下标拆出来，不能像 data object 那样直接比字符串。
            if (id.startsWith(OddsDisclosureInfoIdPrefix)) {
                val args = id.removePrefix(OddsDisclosureInfoIdPrefix).split('/')
                val categoryIndex = args.getOrNull(0)?.toIntOrNull() ?: return null
                val boxIndex = args.getOrNull(1)?.toIntOrNull() ?: return null
                return OddsDisclosureInfo(categoryIndex, boxIndex)
            }
            if (id.startsWith(LotteryOddsInfoIdPrefix)) {
                val args = id.removePrefix(LotteryOddsInfoIdPrefix).split('/')
                val categoryIndex = args.getOrNull(0)?.toIntOrNull() ?: return null
                val drawIndex = args.getOrNull(1)?.toIntOrNull() ?: return null
                return LotteryOddsInfo(categoryIndex, drawIndex)
            }
            if (id.startsWith(IcuFraudIdPrefix)) {
                val qq = id.removePrefix(IcuFraudIdPrefix)
                return qq.takeIf { it.isNotEmpty() }?.let(::IcuFraud)
            }
            if (id.startsWith(AnnouncementArticleIdPrefix)) {
                val cvId = id.removePrefix(AnnouncementArticleIdPrefix)
                return cvId.takeIf { it.isNotEmpty() }?.let(::AnnouncementArticle)
            }
            if (id.startsWith(BilibiliOpusIdPrefix)) {
                // 标题里万一带上斜杠，只拆第一段，剩下的整段还给标题。
                val args = id.removePrefix(BilibiliOpusIdPrefix).split('/', limit = 2)
                val opusId = args.getOrNull(0)?.takeIf { it.isNotEmpty() } ?: return null
                val title = args.getOrNull(1)?.takeIf { it.isNotEmpty() } ?: "哔哩哔哩"
                return BilibiliOpus(opusId = opusId, title = title)
            }
            return when (id) {
                "main" -> Main
                "meishiWechat" -> MeishiWechat
                "notificationInbox" -> NotificationInbox
                "about" -> About
                "announcement" -> Announcement
                "announcementArchive" -> AnnouncementArchive
                "permission" -> Permission
                "coContributorTeam" -> CoContributorTeam
                "thanksGame" -> ThanksGame
                "thanksApp" -> ThanksApp
                "todayLucky" -> TodayLucky
                "resourceCalculator" -> ResourceCalculator
                "cardOddsCalculator" -> CardOddsCalculator
                "oddsDisclosure" -> OddsDisclosure
                "dataImages" -> DataImages
                "dataPackUpdate" -> DataPackUpdate
                "clothingCollection" -> ClothingCollection
                "wingCollection" -> WingCollection
                "cookeryIndex" -> CookeryIndex
                "fraudList" -> FraudList
                else -> null
            }
        }
    }
}
