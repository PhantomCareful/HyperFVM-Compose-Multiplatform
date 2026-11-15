package com.careful.hyperfvm.compose.data.fraud

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.cert.CertificateException
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.SSLHandshakeException

/**
 * 查黑系统的数据层，移植自 Java 版 `IcuHelper`，并接入作者后来提供的接口：
 * - 详情：GET `<详情接口>?qq=<QQ号>`，解析 `data.status / data.fraudAccount / data.fraudRecordList`；
 * - 名单：GET [ListApiUrl]（必须鉴权）或 [LimitedListApiUrl]（不需要鉴权、限流），
 *   `data` 直接是全部已标记账号的数组。
 *
 * 请求都带上 `Authorization`（KEY 存在本机 local.properties，由宿主启动时注入到 [FraudAuthKey]），
 * 可以享受不限流权限；开源仓库里没有 KEY：详情感化为受限流的普通请求，
 * 名单则退回不需要鉴权但限流的旧接口（新接口无 KEY 会直接返回 400）。
 *
 * 转换规则：
 * - 金额按「分 / 10」换算成元，0 显示「未知」；
 * - ISO 8601 时间转成设备本地时区的 `yyyy-MM-dd HH:mm:ss`，解析失败保留原串；
 * - 受害者QQ / 平台 / 区服 为空或字面量 "null" 时显示「未知」；
 * - platform（1-5）与 victimServer 拼成「平台-区服」（如「4399-75」），
 *   type（0-6）换成中文行骗类型。
 */
object FraudRepository {

    /** 与 Java 版 `IcuHelper.API_URL` 相同。 */
    private const val ApiUrl = "https://www.msdzlsch.cn/fraud/getFraudAccountDetail?qq="

    /** 全部已标记账号的简要名单，不限流；必须带 `Authorization`，无 KEY 时会返回 400。 */
    private const val ListApiUrl = "https://www.msdzlsch.cn/fraud/getFraudList"

    /** 名单的限流接口：不需要鉴权，开源构建没有 KEY 时用它兜底；返回结构与 [ListApiUrl] 相同。 */
    private const val LimitedListApiUrl = "https://www.msdzlsch.cn/fraud/getFraudAccountList"

    /** 接口按浏览器请求校验，不带 User-Agent 会被拒绝；与 Java 版一致。 */
    private const val UserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

    private const val ConnectTimeoutMs = 15_000
    private const val ReadTimeoutMs = 15_000

    /**
     * 查询成功的账号缓存，用于名单页 / 列表进出详情页的交接：
     * 路由里仅存 QQ 号，详情页优先从这里取整份数据，取不到（进程被回收等）再自己重查。
     */
    private val accountCache = ConcurrentHashMap<String, FraudAccount>()

    fun cachedAccount(qq: String): FraudAccount? = accountCache[qq]

    /** 名单缓存：从详情页返回时先用旧数据渲染，不必每次重新拉名单。 */
    @Volatile
    private var listCache: List<FraudAccountBrief>? = null

    fun cachedList(): List<FraudAccountBrief>? = listCache

    /**
     * 查询 [qq] 号的行骗记录；接口明确回答「没有记录」时返回 null。
     *
     * 网络、证书、解析等失败统一抛 [FraudQueryException]，message 可直接展示。
     */
    suspend fun query(qq: String): FraudAccount? = withContext(Dispatchers.IO) {
        try {
            parseAccount(fetch(ApiUrl + qq), qq)?.also { accountCache[qq] = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw FraudQueryException(toUserMessage(e))
        }
    }

    /** 拉取全部已标记账号的简要名单，成功后会更新 [cachedList]。 */
    suspend fun queryList(): List<FraudAccountBrief> = withContext(Dispatchers.IO) {
        try {
            // 带 KEY 走不限流的新接口；开源构建没有 KEY 时退回不需要鉴权的旧接口（限流）。
            val url = if (FraudAuthKey.value.isNotEmpty()) ListApiUrl else LimitedListApiUrl
            parseAccountBriefs(fetch(url)).also { listCache = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw FraudQueryException(toUserMessage(e))
        }
    }

    private fun fetch(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = ConnectTimeoutMs
            connection.readTimeout = ReadTimeoutMs
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", UserAgent)
            // 带 KEY 时享受不限流权限；开源构建没有 KEY，不带这个头也能用，只是受限流。
            val authKey = FraudAuthKey.value
            if (authKey.isNotEmpty()) {
                connection.setRequestProperty("Authorization", authKey)
            }
            if (connection.responseCode !in 200..299) {
                // 交给统一的错误映射，按网络异常提示。
                throw IOException("HTTP ${connection.responseCode}")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    /** 解析名单：`code=200`，`data` 直接是数组；没有有效项时返回空列表。 */
    private fun parseAccountBriefs(body: String): List<FraudAccountBrief> {
        val root = JSONObject(body)
        if (root.optInt("code", -1) != 200) throw JSONException("名单接口返回异常")
        val data = root.optJSONArray("data") ?: return emptyList()
        return buildList {
            for (index in 0 until data.length()) {
                val item = data.optJSONObject(index) ?: continue
                val qq = item.optString("qq", "")
                if (qq.isEmpty()) continue
                add(
                    FraudAccountBrief(
                        qq = qq,
                        fraudCount = item.optInt("fraudCount", 0),
                        lastFraudTime = formatToLocalDateTime(item.optString("lastFraudTime", "")),
                    ),
                )
            }
        }
    }

    /** 解析接口响应；没有记录返回 null。结构异常由 [JSONObject] 抛 JSONException。 */
    private fun parseAccount(body: String, queriedQq: String): FraudAccount? {
        val data = JSONObject(body).optJSONObject("data") ?: return null
        // status != 1 表示该 QQ 号没有行骗记录，与 Java 版一致。
        if (data.optInt("status", -1) != 1) return null
        val account = data.optJSONObject("fraudAccount") ?: return null

        val victims = buildList {
            val records = data.optJSONArray("fraudRecordList") ?: return@buildList
            for (index in 0 until records.length()) {
                val record = records.optJSONObject(index) ?: continue
                add(
                    FraudVictim(
                        victimQq = textOrUnknown(record.optString("victimQq", "")),
                        serverArea = formatServerArea(
                            platform = (record.opt("platform") as? Number)?.toInt(),
                            server = record.optString("victimServer", ""),
                        ),
                        fraudType = formatFraudType((record.opt("type") as? Number)?.toInt()),
                        fraudTime = formatToLocalDateTime(record.optString("time", "")),
                        remark = record.optString("remark", ""),
                        amount = formatAmount(record.optInt("fraudAmount", 0)),
                    ),
                )
            }
        }

        return FraudAccount(
            // 接口偶尔不带 qq 字段，用查询号兜底，详情页不会出现空标题值。
            qq = account.optString("qq", "").ifEmpty { queriedQq },
            recordTime = formatToLocalDateTime(account.optString("createTime", "")),
            lastFraudTime = formatToLocalDateTime(account.optString("lastFraudTime", "")),
            fraudCount = account.optInt("fraudCount", 0),
            fraudAmount = formatAmount(account.optInt("fraudAmount", 0)),
            uncertainAmountCount = account.optInt("uncertainAmountCount", 0),
            victims = victims,
        )
    }

    /** 空串与字面量 "null" 都视为没有填写，显示「未知」。 */
    private fun textOrUnknown(value: String): String =
        value.takeUnless { it.isEmpty() || it == "null" } ?: "未知"

    /** platform 取值：1-4399 / 2-QQ大厅 / 3-3366 / 4-QQ空间 / 5-7k7k。 */
    private val FraudPlatformNames = mapOf(
        1 to "4399",
        2 to "QQ大厅",
        3 to "3366",
        4 to "QQ空间",
        5 to "7k7k",
    )

    /** type 取值：0-骗号 / 1-出d诈骗 / 2-出道具诈骗 / 3-代肝跑路 / 4-代氪诈骗 / 5-欠钱跑路 / 6-其他。 */
    private val FraudTypeNames = mapOf(
        0 to "骗号",
        1 to "出d诈骗",
        2 to "出道具诈骗",
        3 to "代肝跑路",
        4 to "代氪诈骗",
        5 to "欠钱跑路",
        6 to "其他",
    )

    /** 「平台-区服」拼成一条展示串：1 + "75" → "4399-75"；缺一边就只显示另一边。 */
    private fun formatServerArea(platform: Int?, server: String): String {
        val channel = platform?.let { FraudPlatformNames[it] ?: it.toString() }.orEmpty()
        val serverText = server.takeUnless { it.isEmpty() || it == "null" } ?: ""
        return when {
            channel.isEmpty() && serverText.isEmpty() -> "未知"
            channel.isEmpty() -> serverText
            serverText.isEmpty() -> channel
            else -> "$channel-$serverText"
        }
    }

    /** 数字换成中文行骗类型；未知数字保留原值，未填显示「未知」。 */
    private fun formatFraudType(type: Int?): String =
        type?.let { FraudTypeNames[it] ?: it.toString() } ?: "未知"

    /** 与 Java 版一致：接口给的是「分」，展示时除以 10 换算成元；0 表示金额未知。 */
    private fun formatAmount(amountInCents: Int): String {
        if (amountInCents == 0) return "未知"
        return "${amountInCents / 10.0}元"
    }

    /** 接口时间统一按本地时区展示，精确到秒。 */
    private val LocalDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /**
     * "2026-05-29T02:56:30.000+00:00" → 设备本地时区的 "2026-05-29 10:56:30"；
     * 不带时区的 "2026-05-29T10:56:30" 直接按本地时间补到秒；其余解析失败保留原串。
     */
    private fun formatToLocalDateTime(raw: String): String {
        if (raw.isEmpty()) return ""
        return try {
            OffsetDateTime.parse(raw)
                .atZoneSameInstant(ZoneId.systemDefault())
                .format(LocalDateTimeFormatter)
        } catch (_: Exception) {
            try {
                LocalDateTime.parse(raw).format(LocalDateTimeFormatter)
            } catch (_: Exception) {
                raw
            }
        }
    }

    private fun toUserMessage(error: Throwable): String = when {
        error.isCertificateError() -> "服务器安全证书校验未通过（可能已过期），请稍后再试。"
        error is IOException -> "网络异常，请检查网络连接或稍后重试。"
        error is JSONException -> "解析数据失败，请稍后重试。"
        else -> "查询异常，请稍后重试。"
    }

    /** 沿异常链判断是否为 SSL 证书校验失败，与 Java 版 `IcuHelper.isCertificateError` 相同。 */
    private fun Throwable.isCertificateError(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is SSLHandshakeException || current is CertificateException) return true
            current = current.cause
        }
        return false
    }
}
