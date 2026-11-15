package com.careful.hyperfvm.compose.data.dashboard

import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

/**
 * 仪表盘几个数据源共用的 GET 请求，移植自 Java 版 `XMLHelper`：
 * URL 末尾拼一个随机数绕开 CDN 缓存，带上 XML 的 Accept 与自定义 User-Agent。
 *
 * 任何失败（网络异常、非 2xx、空响应）都返回 null，由调用方决定兜底文案。
 * 仪表盘只需要「拿到了」和「没拿到」两种结果，不在这里区分错误类型。
 */
internal object DashboardNetwork {

    private const val ConnectTimeoutMs = 10_000
    private const val ReadTimeoutMs = 15_000

    fun fetch(url: String): String? = try {
        val connection = URL(withCacheBuster(url)).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = ConnectTimeoutMs
            connection.readTimeout = ReadTimeoutMs
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/xml")
            connection.setRequestProperty("User-Agent", "CustomXMLClient")
            if (connection.responseCode !in 200..299) {
                null
            } else {
                connection.inputStream.bufferedReader().use { it.readText() }
                    .takeUnless { it.isBlank() }
            }
        } finally {
            connection.disconnect()
        }
    } catch (_: Exception) {
        null
    }

    /** 同 Java 版：`?` + 1..1e9 的随机数，避免运营商或 CDN 把旧配置缓存住。 */
    private fun withCacheBuster(url: String): String =
        "$url?${Random.nextInt(1, 1_000_000_000)}"
}
