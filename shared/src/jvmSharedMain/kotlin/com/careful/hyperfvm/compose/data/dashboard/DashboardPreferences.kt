package com.careful.hyperfvm.compose.data.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.careful.hyperfvm.compose.platform.AppEnv

/**
 * 仪表盘各行的展示顺序与显隐，存在 SharedPreferences 里。
 *
 * 两个字段都是 Compose 状态：拖动排序或拨开关后界面立刻生效，
 * 同时写回本地，下次启动照旧。用户还没排过序时按 [DashboardSlot.defaults] 的顺序。
 *
 * 顺序以「行」为单位：温馨礼包与更新公告并排占一行（[DashboardSlot.GiftPair]），
 * 拖动与存盘都只算一项，所以这两张卡永远挨在一起，也不会被拆开隐藏。
 *
 * 显隐改动是「草稿」式的：拨开关只记 hidden，卡片立即从仪表盘消失，但顺序不动——
 * 弹层还没关，用户随时能把开关拨回来，卡片就还在原来的位置。关掉弹层时
 * [settleOrder] 才把隐藏的卡正式排到末尾并落盘。仪表盘与卡片管理读同一份顺序，
 * 所以管理列表的开关顺序始终与仪表盘一致。
 * 其中月末提醒是重要通知，不管开关是否打开都永远排在第一位，不参与拖动排序。
 */
class DashboardPreferences(env: AppEnv) {

    private val prefs = env.prefs(PrefsName)

    /** 当前顺序，已经补齐后续版本新增、但没存过顺序的卡片。 */
    var order: List<DashboardSlot> by mutableStateOf(readOrder())
        private set

    /** 被用户关掉的卡片；不在集合里的默认展示。成对的温馨礼包 / 更新公告不在其中。 */
    var hidden: Set<DashboardCardId> by mutableStateOf(readHidden())
        private set

    fun saveOrder(newOrder: List<DashboardSlot>) {
        if (newOrder == order) return
        order = newOrder
        prefs.putString(KeyOrder, newOrder.joinToString(separator = ",") { it.token })
        prefs.commit()
    }

    /**
     * 拨开关：只改显隐，不动顺序。
     *
     * 卡片立即从仪表盘消失，但顺序保持原样——弹层还没关，用户随时可以把开关拨回来，
     * 卡片就还在原来的位置。正式沉底由 [settleOrder] 在弹层关闭时完成。
     *
     * 成对的温馨礼包 / 更新公告固定显示，没有开关，不会走到这里。
     */
    fun setVisible(id: DashboardCardId, visible: Boolean) {
        if (id.isPairedWithGift) return
        val nextHidden = if (visible) hidden - id else hidden + id
        if (nextHidden == hidden) return
        hidden = nextHidden
        // 键值存储两端都只保证字符串，这里用逗号拼；枚举名里不会有逗号。
        prefs.putString(KeyHidden, nextHidden.joinToString(separator = ",") { it.name })
        prefs.commit()
    }

    /**
     * 卡片管理弹层关闭时调用：隐藏的卡片正式沉到末尾并落盘。
     *
     * 月末提醒始终置顶，即使被关掉也不参与沉底。
     */
    fun settleOrder() {
        val monthEnd = DashboardSlot.Single(DashboardCardId.MonthEnd)
        val current = order
        val settled = current.filter { it == monthEnd } +
            current.filter { it != monthEnd && it.isVisible(hidden) } +
            current.filter { it != monthEnd && !it.isVisible(hidden) }
        saveOrder(settled)
    }

    /**
     * 存过的顺序在前，新版新增的卡片按 [DashboardSlot.defaults] 接在末尾，
     * 关掉的统一沉到列表末尾。旧版本把成对的两张卡分别存成两行，读出来会归成同一行。
     *
     * 月末提醒是重要通知，不管有没有被关掉都置顶；旧版本存下的顺序里它可能在别处。
     */
    private fun readOrder(): List<DashboardSlot> {
        val saved = prefs.getString(KeyOrder, null)
            .orEmpty()
            .split(",")
            .mapNotNull { DashboardSlot.fromToken(it) }
            .distinct()
        val all = saved + DashboardSlot.defaults.filterNot { it in saved }
        // 旧版本关掉的卡片可能夹在中间，读出来先稳定分区。
        val off = readHidden()
        val monthEnd = DashboardSlot.Single(DashboardCardId.MonthEnd)
        // 稳定分区：月末提醒（不管开关）-> 其余启用的（保持原顺序）-> 关掉的。
        return all.filter { it == monthEnd } +
            all.filter { it != monthEnd && it.isVisible(off) } +
            all.filter { it != monthEnd && !it.isVisible(off) }
    }

    /** 忽略已经删掉的卡片名；成对的卡不允许隐藏，旧版本存下的残留一并丢掉。 */
    private fun readHidden(): Set<DashboardCardId> {
        val saved = prefs.getString(KeyHidden, null)
            .orEmpty()
            .split(",")
            .filter { it.isNotEmpty() }
            .toSet()
        return DashboardCardId.entries
            .filter { it.name in saved && !it.isPairedWithGift }
            .toSet()
    }

    private companion object {
        const val PrefsName = "hyperfvm_dashboard"
        const val KeyOrder = "cardOrder"
        const val KeyHidden = "hiddenCards"
    }
}
