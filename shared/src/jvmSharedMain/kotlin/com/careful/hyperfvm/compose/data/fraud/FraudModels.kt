package com.careful.hyperfvm.compose.data.fraud

/**
 * 一条行骗记录，对应 Java 版 `IcuHelper.VictimInfo`。
 *
 * 字段都已在数据层转成可直接展示的字符串：QQ / 区服空值换成「未知」，
 * 时间换成设备本地日期，金额换成「x元」或「未知」；platform 与 victimServer
 * 拼成「4399-75」这种「平台-区服」形式，type 换成中文行骗类型。
 */
data class FraudVictim(
    val victimQq: String,
    /** 平台与区服拼好的展示串，如「4399-75」；两边都缺失时为「未知」。 */
    val serverArea: String,
    /** 行骗类型，接口的数字已换成中文（骗号 / 出d诈骗 / …）。 */
    val fraudType: String,
    val fraudTime: String,
    /** 备注可能为空串：为空时详情页不渲染备注块。 */
    val remark: String,
    val amount: String,
)

/**
 * 名单页的一条简要记录，对应 `getFraudAccountList` 返回的一项。
 * 时间已转成设备本地时区、精确到秒。
 */
data class FraudAccountBrief(
    val qq: String,
    val fraudCount: Int,
    val lastFraudTime: String,
)

/**
 * 一条骗子账号，对应 Java 版 `IcuHelper.FraudResult`。
 *
 * 只表示「接口确认有行骗记录」的情况；没有记录由查询层返回 null。
 */
data class FraudAccount(
    val qq: String,
    val recordTime: String,
    val lastFraudTime: String,
    val fraudCount: Int,
    val fraudAmount: String,
    val uncertainAmountCount: Int,
    val victims: List<FraudVictim>,
)

/** 查询失败。[message] 已是可直接展示给用户的中文提示。 */
class FraudQueryException(message: String) : Exception(message)
