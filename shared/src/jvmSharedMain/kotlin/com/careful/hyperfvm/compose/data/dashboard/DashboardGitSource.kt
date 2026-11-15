package com.careful.hyperfvm.compose.data.dashboard

import com.careful.hyperfvm.compose.data.cardCatalog.CardCatalogDataVersion
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Git 配置源，移植自 Java 版 `DashboardGitCatcher`。
 *
 * [GitUrl] 是一个 JSON 数组，每项形如
 * `{"name": "日氪", "startDate": "2026-01-30", "endDate": "2026-02-05"}`，
 * 带卡表的活动还会多一个 `cardList`（`|` 分隔），福利打卡多一个 `closeDate`，
 * 世界BOSS 多 `challengeDate` / `settlementDate`，营地任务多 `urlTask`。
 *
 * 同一份字段现在既算卡片状态，也拼出点击卡片后的详情弹层：
 * 起止时间、活动关闭时间、返场卡片名单、底部按钮。
 */
internal object DashboardGitSource {

    /** 与 Java 版 `DashboardGitCatcher.GIT_URL` 相同。 */
    private const val GitUrl = "https://gitee.com/phantom-careful/hyper-fvm-updater/raw/main/dashboard.m3u"

    /** 整个数据源拿不到时，这些卡统一显示「网络异常」，同 Java 版 `buildFailResult()`。 */
    private const val NetworkError = "网络异常"

    /** 带卡表的活动统一用这个分组标题。 */
    private const val CardsSection = "返场卡片名单"

    /** 福利打卡的新卡分组标题。 */
    private const val NewCardSection = "本期新卡"

    fun fetch(): Map<DashboardCardId, DashboardEntry> {
        val body = DashboardNetwork.fetch(GitUrl) ?: return networkError()
        val array = try {
            JSONArray(body)
        } catch (_: Exception) {
            return networkError()
        }
        val result = mutableMapOf<DashboardCardId, DashboardEntry>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            // 单条解析失败不能拖垮整张表：数据源偶尔会漏字段，跳过让它走「获取失败」即可。
            try {
                parseItem(item)?.let { (id, entry) -> result[id] = entry }
            } catch (_: Exception) {
                // 留空，界面按 DashboardStatus.Failed 兜底。
            }
        }
        return result
    }

    /** 按 `name` 分发；返回 null 表示本轮不展示的条目（如 App 通知）。 */
    private fun parseItem(item: JSONObject): Pair<DashboardCardId, DashboardEntry>? =
        when (item.optString("name")) {
            "二转打折" -> DashboardCardId.TransferDiscount to transferDiscount(item)
            "三岛" -> DashboardCardId.ThreeIslands to cardListRatio(
                id = DashboardCardId.ThreeIslands,
                item = item,
                images = listOf("tiramisu_image_2_1"),
            )
            "美食大赛" -> DashboardCardId.FoodContest to cardListRatio(
                id = DashboardCardId.FoodContest,
                item = item,
                images = listOf("tiramisu_image_2_3", "tiramisu_image_2_3_1"),
            )
            "欢乐假期" -> DashboardCardId.HappyHoliday to happyHoliday(item)
            "福利打卡" -> DashboardCardId.BirthdayActivity to birthdayActivity(item)
            "日氪" -> DashboardCardId.DailyRecharge to dailyRecharge(item)
            "助人为乐" -> DashboardCardId.CrossServerTeamUp to generic(DashboardCardId.CrossServerTeamUp, item)
            "营地任务" -> DashboardCardId.CampTask to campTask(item)
            "豪华婚礼" -> DashboardCardId.WeddingDiscount to generic(DashboardCardId.WeddingDiscount, item)
            "结晶打折" -> DashboardCardId.CryStoneDiscount to generic(DashboardCardId.CryStoneDiscount, item)
            "世界BOSS" -> DashboardCardId.WorldBoss to worldBoss(item)
            // App 通知及其它本轮不迁移的条目。
            else -> null
        }

    /**
     * 通用活动：助人为乐 / 豪华婚礼 / 结晶打折。
     *
     * 结束当天过了 10 点就当活动结束 —— Java 版这条规则对日氪和世界BOSS 也不适用，
     * 所以这两张卡各自单独实现。
     */
    private fun generic(id: DashboardCardId, item: JSONObject): DashboardEntry {
        val (today, start, end) = range(item)
        val status = when {
            today.isBefore(start) -> "即将开始"
            today.isAfter(end) -> DashboardStatus.None
            today == end && DashboardDates.hour() >= 10 -> DashboardStatus.None
            else -> "${DashboardDates.daysBetween(start, today)}/${DashboardDates.daysBetween(start, end)}"
        }
        return dashboardEntryWithSheet(id = id, status = status, times = timesOf(start, end))
    }

    /** 营地任务：通用活动加上两个底部入口（顺序表跳浏览器、解谜图直接铺在弹层里）。 */
    private fun campTask(item: JSONObject): DashboardEntry {
        val (today, start, end) = range(item)
        val status = when {
            today.isBefore(start) -> "即将开始"
            today.isAfter(end) -> DashboardStatus.None
            today == end && DashboardDates.hour() >= 10 -> DashboardStatus.None
            else -> "${DashboardDates.daysBetween(start, today)}/${DashboardDates.daysBetween(start, end)}"
        }
        val url = item.optString("urlTask").trim()
        // 链接缺失时不放一个点了报错的按钮。
        val links = if (url.isEmpty()) emptyList() else listOf(DashboardLink("营地任务顺序表", url))
        return dashboardEntryWithSheet(
            id = DashboardCardId.CampTask,
            status = status,
            times = timesOf(start, end),
            images = listOf("tiramisu_image_2_5"),
            links = links,
        )
    }

    /** 日氪：状态按道具数算进度，每天 8 个；同样没有结束当天 10 点的截断。 */
    private fun dailyRecharge(item: JSONObject): DashboardEntry {
        val (today, start, end) = range(item)
        val length = DashboardDates.daysBetween(start, end)
        val status = when {
            today.isBefore(start) -> "即将开始"
            today.isAfter(end) -> DashboardStatus.None
            else -> {
                val during = DashboardDates.daysBetween(start, today)
                "${during * 8}/${length * 8}"
            }
        }
        return dashboardEntryWithSheet(
            id = DashboardCardId.DailyRecharge,
            status = status,
            times = timesOf(start, end),
        )
    }

    /**
     * 世界BOSS：一个赛季四个阶段（预热 → 挑战 → 结算 → 领奖），
     * 且没有「结束当天 10 点后算结束」这条规则。
     *
     * 预热期一定只有一天；挑战期与结算期都按天数画进度，最后一天是领奖。
     *
     * 弹层底部是 Java 版那两个按钮：「赛季规则」「赛季奖励」，各对应一条 B 站
     * 图文动态（云端 `urlRule` / `urlReward`）。链接为空时就不放那个按钮。
     */
    private fun worldBoss(item: JSONObject): DashboardEntry {
        val (today, start, end) = range(item)
        val challenge = parseDate(item, "challengeDate")
        val settlement = parseDate(item, "settlementDate")
        val status = when {
            today.isBefore(start) -> "即将开始"
            today.isAfter(end) -> DashboardStatus.None
            today.isBefore(challenge) -> "预热中"
            today.isBefore(settlement) -> "${DashboardDates.daysBetween(challenge, today)}/" +
                "${DashboardDates.daysBetween(challenge, settlement) - 1}"
            today.isBefore(end) -> "${DashboardDates.daysBetween(settlement, today)}/" +
                "${DashboardDates.daysBetween(settlement, end)}"
            // 赛季最后一天：结算已过，当晚领奖。
            else -> "领奖时间"
        }
        val times = listOf(
            DashboardTimeLine("预热期", start.toString()),
            DashboardTimeLine("挑战开始", challenge.toString()),
            DashboardTimeLine("结算开始", settlement.toString()),
            DashboardTimeLine("领奖日期", end.toString()),
        )
        val links = buildList {
            val rule = item.optString("urlRule").trim()
            if (rule.isNotEmpty()) add(DashboardLink("赛季规则", rule))
            val reward = item.optString("urlReward").trim()
            if (reward.isNotEmpty()) add(DashboardLink("赛季奖励", reward))
        }
        return dashboardEntryWithSheet(
            id = DashboardCardId.WorldBoss,
            status = status,
            times = times,
            links = links,
        )
    }

    /**
     * 三岛 / 美食大赛：带卡表、进度天数要减 1 的那一类。
     *
     * 「尚未开始」与通用活动的「即将开始」不同，是 Java 版原文如此。
     */
    private fun cardListRatio(
        id: DashboardCardId,
        item: JSONObject,
        images: List<String>,
    ): DashboardEntry {
        val (today, start, end) = range(item)
        val length = DashboardDates.daysBetween(start, end) - 1
        val status = when {
            today.isBefore(start) -> "即将开始"
            today.isAfter(end) -> DashboardStatus.None
            today == end && DashboardDates.hour() >= 10 -> DashboardStatus.None
            else -> "${DashboardDates.daysBetween(start, today)}/$length"
        }
        return dashboardEntryWithSheet(
            id = id,
            status = status,
            times = timesOf(start, end),
            sectionTitle = CardsSection,
            cards = cardsOf(item),
            images = images,
        )
    }

    /** 二转打折：状态同 [cardListRatio]，卡表张数接在标题后面（「二转打折 - 6张」）。 */
    private fun transferDiscount(item: JSONObject): DashboardEntry {
        val cards = cardsOf(item)
        val (today, start, end) = range(item)
        val length = DashboardDates.daysBetween(start, end) - 1
        val status = when {
            today.isBefore(start) -> "即将开始"
            today.isAfter(end) -> DashboardStatus.None
            today == end && DashboardDates.hour() >= 10 -> DashboardStatus.None
            else -> "${DashboardDates.daysBetween(start, today)}/$length"
        }
        return dashboardEntryWithSheet(
            id = DashboardCardId.TransferDiscount,
            status = status,
            times = timesOf(start, end),
            sectionTitle = CardsSection,
            cards = cards,
        ).copy(title = "二转打折 - ${cards.size}张")
    }

    /** 欢乐假期：进度天数不减 1，且没有结束当天 10 点的截断。 */
    private fun happyHoliday(item: JSONObject): DashboardEntry {
        val (today, start, end) = range(item)
        val status = when {
            today.isBefore(start) -> "即将开始"
            today.isAfter(end) -> DashboardStatus.None
            else -> "${DashboardDates.daysBetween(start, today)}/${DashboardDates.daysBetween(start, end)}"
        }
        return dashboardEntryWithSheet(
            id = DashboardCardId.HappyHoliday,
            status = status,
            times = timesOf(start, end),
            sectionTitle = CardsSection,
            cards = cardsOf(item),
            images = listOf("tiramisu_image_1_7"),
        )
    }

    /**
     * 福利打卡：活动结束后还有一段「领奖期」，一直到页面关闭为止，
     * 所以它的结束判断看 `closeDate` 而不是 `endDate`。
     *
     * 本期新卡要核对本地卡片目录的版本：版本够就展示卡 Card，不够就换成
     * Java 版同款文案的提示卡。
     */
    private fun birthdayActivity(item: JSONObject): DashboardEntry {
        val (today, start, end) = range(item)
        val close = parseDate(item, "closeDate")
        val length = DashboardDates.daysBetween(start, end)
        val status = when {
            today.isBefore(start) -> "即将开始"
            today.isAfter(end) && today.isBefore(close) -> "领奖期"
            // 结束当天 10 点后进入领奖期；这条要排在关闭判断前面，
            // 否则 closeDate 与 endDate 同一天时会被当成已关闭。
            today == end && DashboardDates.hour() >= 10 -> "领奖期"
            // Java 版在页面关闭当天会落进进度分支，算出一个超过总天数的比值；
            // 这里把关闭当天也算作结束，避免出现「16/15」这种文案。
            !today.isBefore(close) -> DashboardStatus.None
            else -> "${DashboardDates.daysBetween(start, today)}/$length"
        }
        val times = timesOf(start, end) + DashboardTimeLine("活动关闭", close.toString())
        val newCard = item.optString("newCardName").trim()
        val requiredText = item.optString("requiredDatabaseVersion").trim()
        // 云端版本号可能非数字；解析失败按 0 处理，同 Java 版（0 不会触发版本不足提示）。
        val required = requiredText.toIntOrNull() ?: 0
        val satisfied = (CardCatalogDataVersion.toIntOrNull() ?: 0) >= required
        return dashboardEntryWithSheet(
            id = DashboardCardId.BirthdayActivity,
            status = status,
            times = times,
            sectionTitle = if (newCard.isNotEmpty() && satisfied) NewCardSection else null,
            cards = if (newCard.isNotEmpty() && satisfied) listOf(newCard) else emptyList(),
            tips = if (newCard.isNotEmpty() && !satisfied) {
                "$newCard\n\n当前数据库尚未包含此卡片\n请更新数据库版本到$requiredText"
            } else {
                null
            },
        )
    }

    /** 读出今天与活动起止日期；字段缺失或格式不对时抛异常，由 [fetch] 跳过这张卡。 */
    private fun range(item: JSONObject): Triple<LocalDate, LocalDate, LocalDate> = Triple(
        DashboardDates.today(),
        parseDate(item, "startDate"),
        parseDate(item, "endDate"),
    )

    private fun parseDate(item: JSONObject, key: String): LocalDate =
        DashboardDates.parse(item.getString(key)) ?: error("$key 格式不对")

    /** `cardList` 是 `|` 分隔的卡片名，空段直接丢掉。 */
    private fun cardsOf(item: JSONObject): List<String> =
        item.optString("cardList")
            .split("|")
            .map(String::trim)
            .filter(String::isNotEmpty)

    private fun timesOf(start: LocalDate, end: LocalDate): List<DashboardTimeLine> = listOf(
        DashboardTimeLine("开始时间", start.toString()),
        DashboardTimeLine("结束时间", end.toString()),
    )

    private fun networkError(): Map<DashboardCardId, DashboardEntry> = listOf(
        DashboardCardId.DailyRecharge,
        DashboardCardId.HappyHoliday,
        DashboardCardId.CrossServerTeamUp,
        DashboardCardId.ThreeIslands,
        DashboardCardId.FoodContest,
        DashboardCardId.TransferDiscount,
        DashboardCardId.CampTask,
        DashboardCardId.WeddingDiscount,
        DashboardCardId.CryStoneDiscount,
        DashboardCardId.BirthdayActivity,
        // Java 版网络失败时直接隐藏世界BOSS，这里保留它并给出同样的提示，
        // 因为 Compose 版的卡片显隐已经交给用户自己控制。
        DashboardCardId.WorldBoss,
    ).associateWith { DashboardEntry(NetworkError) }
}
