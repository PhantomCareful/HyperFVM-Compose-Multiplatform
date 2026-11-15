package com.careful.hyperfvm.compose.data.meishi

import androidx.compose.runtime.Immutable

/**
 * 一个已保存的温馨礼包账号。
 *
 * 只存 openid 与解析出来的区服 / 角色名：完整的链接由 [link] 按同一套地址格式重建，
 * 所以存储结构可以退化成三个字符串，将来接设备份还原时字段名也能和 Java 版对齐
 * （openid / serverName / playerName）。
 */
@Immutable
data class MeishiAccount(
    val openId: String,
    val serverName: String,
    val playerName: String,
) {
    /** 微信公众号里复制过来的那种链接，管理界面按原样只读展示。 */
    val link: String get() = "${MeishiLinks.IndexUrl}?openid=$openId"
}

/** 温馨礼包用到的两个接口地址，与 Java 版 `MeishiLinkUtil` / `GiftFetchHelper` 完全一致。 */
object MeishiLinks {

    /** 角色页：打开后从 `<h1 class="title">` 里解析「区服 - 角色」。 */
    const val IndexUrl = "http://meishi.wechat.123u.com/meishi/index"

    /** 领取接口：`ret` 为 0（本次领取）或 1（此前已领）都算成功。 */
    private const val GiftUrl = "http://meishi.wechat.123u.com/meishi/gift"

    fun gift(openId: String): String = "$GiftUrl?openid=$openId"
}

/**
 * 解析角色信息的结果。
 *
 * Java 版对这三类失败给了不同提示，所以不能在数据层就把它们合并成 null：
 * - [NotParsed] → 「解析失败 / 未找到区服和角色信息」
 * - [ServerError] → 「获取信息失败 / 服务器无响应」
 * - [NetworkError] → 「网络错误 / 无法连接服务器」
 */
sealed interface MeishiFetchResult {

    @Immutable
    data class Success(val serverName: String, val playerName: String) : MeishiFetchResult

    /** 页面拿到了，但没找到能拆成「区服 - 角色」的标题行。 */
    data object NotParsed : MeishiFetchResult

    /** 服务器返回非 2xx，或响应体是空的。 */
    data object ServerError : MeishiFetchResult

    /** 请求根本没发出去 / 没有回应（DNS、超时、地址非法）。 */
    data object NetworkError : MeishiFetchResult
}

/** 仪表盘上温馨礼包卡的领取结果，文案映射见 `MeishiClaimResult.text`。 */
sealed interface MeishiClaimResult {

    /** 没有保存任何账号，卡片显示「暂无账号」。 */
    data object NoAccount : MeishiClaimResult

    /** 全部账号都拿到了有效响应，[count] 是成功数。 */
    data class Claimed(val count: Int) : MeishiClaimResult

    /** 某个账号返回了意料之外的 `ret` 或非 2xx，整批就此停下。 */
    data object ServerError : MeishiClaimResult

    /** 请求过程抛了异常。 */
    data object Error : MeishiClaimResult
}

/** 仪表盘卡片上那句话，与 Java 版逐字一致。 */
val MeishiClaimResult.text: String
    get() = when (this) {
        MeishiClaimResult.NoAccount -> "暂无账号"
        is MeishiClaimResult.Claimed -> "${count}个已领取"
        MeishiClaimResult.ServerError -> "领取失败"
        MeishiClaimResult.Error -> "领取异常"
    }

/** 领取失败时卡片右下角换成「去掉的圆圈」，与 Java 版切图标的行为对齐。 */
val MeishiClaimResult.isFailure: Boolean
    get() = this == MeishiClaimResult.ServerError || this == MeishiClaimResult.Error
