package com.careful.hyperfvm.compose.data.meishi

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 温馨礼包的三个网络动作，移植自 Java 版 `MeishiLinkUtil` / `GiftFetchHelper`：
 * 从链接里摘 openid、拉角色页解析「区服 - 角色」、批量领取。
 *
 * 全部是阻塞调用，必须在 IO 线程上跑；内部不抛异常，每类失败都映射成
 * 调用方可以分辨的结果，界面据此决定提示文案或卡片状态。
 */
object MeishiNetwork {

    private const val ConnectTimeoutMs = 10_000
    private const val ReadTimeoutMs = 15_000

    /** 微信公众号复制的链接形如 `.../meishi/index?openid=xxx&...`。 */
    private val OpenIdPattern = Regex("openid=([^&]+)")

    /** 角色页里那一行标题，同 Java 版；`.*?` 不跨行，只取第一处。 */
    private val TitlePattern = Regex("<h1 class=\"title\">(.*?)</h1>")

    /** 标题里的分隔符，左右各一个空格，与 Java 版的 `split(" - ")` 一致。 */
    private const val TitleSeparator = " - "

    /** 一次请求的三种结局，供上层分别映射。 */
    private sealed interface Response {
        data class Body(val text: String) : Response
        data object HttpError : Response
        data object NetworkError : Response
    }

    /**
     * 从用户给的文本里提取 openid。
     *
     * 与 Java 版 `checkLink` 一致：先要求它长得像个 http(s) 链接，再找 `openid=` 参数。
     * 返回 null 表示这条链接不能用，界面据此提示「链接可能有误」。
     */
    fun parseOpenId(input: String): String? {
        val text = input.trim()
        if (!text.startsWith("http://") && !text.startsWith("https://")) return null
        val match = OpenIdPattern.find(text) ?: return null
        return match.groupValues[1].takeIf { it.isNotEmpty() }
    }

    /**
     * 拉角色页并解析「区服 - 角色」。
     *
     * 解析规则同 Java 版：先抓 `<h1 class="title">` 里的文字，再按 `" - "` 切成两段。
     * 切不出两段就落到 [MeishiFetchResult.NotParsed]（页面改版、openid 失效都会这样）。
     */
    fun fetchPlayerInfo(openId: String): MeishiFetchResult {
        val html = when (val response = request("${MeishiLinks.IndexUrl}?openid=$openId")) {
            is Response.Body -> response.text
            Response.HttpError -> return MeishiFetchResult.ServerError
            Response.NetworkError -> return MeishiFetchResult.NetworkError
        }
        val title = TitlePattern.find(html)?.groupValues?.get(1)?.trim()
            ?: return MeishiFetchResult.NotParsed
        val parts = title.split(TitleSeparator)
        if (parts.size != 2) return MeishiFetchResult.NotParsed
        val serverName = parts[0].trim()
        val playerName = parts[1].trim()
        // 任何一段为空都当解析失败：卡片上留半截信息比直接报错更难排查。
        if (serverName.isEmpty() || playerName.isEmpty()) return MeishiFetchResult.NotParsed
        return MeishiFetchResult.Success(serverName = serverName, playerName = playerName)
    }

    /**
     * 依次领取每个账号的礼包。
     *
     * 与 Java 版一致：只要有一个账号拿到非 2xx 或意料之外的 `ret` 就整批停下，
     * 已领取的数量作废——卡片显示的是「这一轮是否顺利」，而不是部分成功。
     * Java 版把网络异常也算进「领取失败」；这里单独留出 [MeishiClaimResult.Error]，
     * 断网时卡片显示「领取异常」，与服务器出错区分开。
     */
    fun claim(openIds: List<String>): MeishiClaimResult {
        if (openIds.isEmpty()) return MeishiClaimResult.NoAccount
        var claimed = 0
        openIds.forEach { openId ->
            val body = when (val response = request(MeishiLinks.gift(openId))) {
                is Response.Body -> response.text
                Response.HttpError -> return MeishiClaimResult.ServerError
                Response.NetworkError -> return MeishiClaimResult.Error
            }
            val ret = runCatching { JSONObject(body).getInt("ret") }.getOrNull()
                ?: return MeishiClaimResult.ServerError
            // 0 = 本次领取成功，1 = 此前已经领过，两种都算这个账号没问题。
            if (ret != 0 && ret != 1) return MeishiClaimResult.ServerError
            claimed++
        }
        return MeishiClaimResult.Claimed(claimed)
    }

    /** 不带缓存绕行的 GET：这两个接口都按 openid 实时返回，不需要 CDN 参数。 */
    private fun request(url: String): Response = try {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = ConnectTimeoutMs
            connection.readTimeout = ReadTimeoutMs
            connection.requestMethod = "GET"
            if (connection.responseCode !in 200..299) {
                Response.HttpError
            } else {
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                if (text.isBlank()) Response.HttpError else Response.Body(text)
            }
        } finally {
            connection.disconnect()
        }
    } catch (_: Exception) {
        Response.NetworkError
    }
}
