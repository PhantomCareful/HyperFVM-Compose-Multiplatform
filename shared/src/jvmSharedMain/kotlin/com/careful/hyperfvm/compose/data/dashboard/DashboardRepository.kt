package com.careful.hyperfvm.compose.data.dashboard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * 仪表盘数据仓库：五个网络源并行拉取，加上两张纯本地卡片，合并成 id → 状态 的表。
 *
 * 每个源自己兜底（失败只是它对应的几张卡缺项），所以这里不需要 try：
 * 缺项由界面按 [DashboardStatus.Failed] 显示。
 */
object DashboardRepository {

    /** 月末提醒卡的文案，同 Java 版 `text_dashboard_last_day_of_month`（去掉「月末了，」前缀）。 */
    private const val MonthEndText = "请注意清空积分和金券"

    /** 每日签到满 25 天后的状态，带状态卡绿底。 */
    private const val CheckInClaimable = "可以领奖"

    /** 上一次刷新出来的结果：切换 Tab 回来先用它渲染，不必等网络。 */
    @Volatile
    private var cache: Map<DashboardCardId, DashboardEntry>? = null

    fun cached(): Map<DashboardCardId, DashboardEntry>? = cache

    /** 全量刷新一次，返回值同时写进 [cached]。 */
    suspend fun refresh(): Map<DashboardCardId, DashboardEntry> = withContext(Dispatchers.IO) {
        val merged = coroutineScope {
            // 五个请求各自独立，并行发出去。全部在 Dispatchers.IO 上跑，
            // 阻塞式的 HttpURLConnection 不会占用主线程。
            val git = async { DashboardGitSource.fetch() }
            val doubleExplosion = async { DashboardGameSources.doubleExplosion() }
            val fertilization = async { DashboardGameSources.fertilization() }
            val newYear = async { DashboardGameSources.newYear() }
            val sweetIsland = async { DashboardGameSources.sweetIsland() }
            val sources = listOf(
                git.await(),
                doubleExplosion.await(),
                fertilization.await(),
                newYear.await(),
                sweetIsland.await(),
            )
            val result = LinkedHashMap<DashboardCardId, DashboardEntry>()
            sources.forEach(result::putAll)
            result[DashboardCardId.DailyCheckIn] = dailyCheckIn()
            if (isMonthEnd()) result[DashboardCardId.MonthEnd] = DashboardEntry(MonthEndText)
            result
        }
        cache = merged
        merged
    }

    /** 每日签到纯本地算：1-24 号显示月签道具进度，25 号起到本月结束是「可以领奖」。 */
    private fun dailyCheckIn(): DashboardEntry {
        val day = LocalDate.now().dayOfMonth
        return if (day in 1..24) {
            DashboardEntry("$day/25")
        } else {
            DashboardEntry(CheckInClaimable, style = DashboardStyle.Green)
        }
    }
}
