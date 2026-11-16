package com.careful.hyperfvm.compose.data.notification

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.careful.hyperfvm.compose.data.dashboard.DashboardNetwork
import com.careful.hyperfvm.compose.platform.AppEnv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * 收件箱里的一条通知，即已经落到本地的那一份。
 *
 * [id] 是云端分配的递增编号：既用来判断「是不是新通知」，也当列表项的稳定 key。
 * [read] 只服务于卡片标题前的「【未读】」前缀，与仪表盘角标不是一回事——角标看的是
 * [NotificationRepository.newCount]（上次看过收件箱之后又到了几条）。
 * [links] 是这条通知带的跳转链接，一条通知可以带多个。
 */
data class StoredNotification(
    val id: Long,
    val title: String,
    val content: String,
    val links: List<NotificationLink>,
    val read: Boolean,
    val receivedAt: Long,
)

/**
 * 通知里的一个跳转链接：一条通知可以带多个。
 *
 * 按钮文案就是 [button] 与 [title] 直接拼起来。
 */
data class NotificationLink(
    val button: String,
    val title: String,
    val url: String,
) {
    val label: String get() = button + title
}

/**
 * 收件箱数据仓库：云端通知清单的抓取、版本筛选、本地持久化与已读/删除状态。
 *
 * 收件规则（与 Java 版无关，是本版新增的）：
 * - 云端清单是一个 JSON 数组，每条带唯一且递增的 id；
 * - 本地记一个「收到过的最新 id」，只有比它大的才算新通知，其余一律跳过；
 * - 新通知只增不减：云端把某条删掉，本地那份照旧保留，除非用户在收件箱里手动删除。
 *
 * 状态都是 Compose 状态：仪表盘角标与收件箱列表读同一份，改完立刻生效。
 */
object NotificationRepository {

    /** 云端通知清单。 */
    private const val NotificationUrl =
        "https://raw.giteeusercontent.com/phantom-careful/hyper-fvm-updater/raw/main/notification.m3u"

    private const val PrefsName = "hyperfvm_notification"
    private const val KeyItems = "items"
    private const val KeyLatestId = "latestReceivedId"
    private const val KeyLastSeenId = "lastSeenId"

    /** 需求约定：这个取值表示「全版本都能收到」。 */
    private const val AllVersions = "0"

    /** 版本规则里的通配段，大小写与星号都认。 */
    private val Wildcards = setOf("X", "x", "*")

    /** 收到的全部通知，按 id 升序（最早的在最前），界面按需反转。 */
    var items: List<StoredNotification> by mutableStateOf(emptyList())
        private set

    /** 本地收到过的最新通知 id：云端只认比它更大的那些。 */
    var latestReceivedId: Long by mutableStateOf(0L)
        private set

    /**
     * 上一次进入收件箱时的 [latestReceivedId] 快照。
     *
     * 仪表盘角标数是「比它大的通知条数」，所以进一次收件箱（[markInboxSeen]）就归零；
     * 卡片上的红点另算，要点开那条通知的详情才会清。
     */
    private var lastSeenId: Long by mutableStateOf(0L)

    /** 仪表盘入口上的角标：上次看过收件箱之后又到了几条。 */
    val newCount: Int get() = items.count { it.id > lastSeenId }

    /** 是否还有没点开过的通知（「一键已读」据此禁用）。 */
    val hasUnread: Boolean get() = items.any { !it.read }

    private var loaded = false

    /** 等 Snackbar 定案的删除：批次号 + 被删的那几条，恢复时按 id 排序就回到原位。 */
    private var pendingRemoval: PendingRemoval? = null

    /** 删除批次号：连着删两批时，前一条 Snackbar 收尾不能定案后一批。 */
    private var removalToken = 0L

    /** 首次读取本地存档；页面进组合时先调它，之后状态就是现成的。 */
    fun ensureLoaded(env: AppEnv) {
        if (loaded) return
        loaded = true
        val prefs = env.prefs(PrefsName)
        latestReceivedId = prefs.getLong(KeyLatestId, 0L)
        lastSeenId = prefs.getLong(KeyLastSeenId, 0L)
        items = readItems(prefs.getString(KeyItems, null))
    }

    /**
     * 拉一次云端清单并收件。
     *
     * 网络与解析放在 IO 上，写状态与落盘留在调用方线程（页面协程本来就是主线程）。
     * 任何失败都当作「这次没收到」：收件箱是锦上添花，不能影响仪表盘刷新。
     */
    suspend fun refresh(env: AppEnv) {
        ensureLoaded(env)
        val remote = withContext(Dispatchers.IO) { fetchRemote() }
        if (remote.isEmpty()) return
        val accepted = remote.asSequence()
            .filter { it.id > latestReceivedId }
            // 版本不符的通知不推进 latestReceivedId：等用户升级到符合的版本时还能收到它。
            .filter { matchesVersion(env.appVersionName, it.versionRule) }
            .filterNot { cloud -> items.any { it.id == cloud.id } }
            .toList()
        if (accepted.isEmpty()) return
        val receivedAt = System.currentTimeMillis()
        val added = accepted.map { cloud ->
            StoredNotification(
                id = cloud.id,
                title = cloud.title,
                content = cloud.content,
                links = cloud.links,
                read = false,
                receivedAt = receivedAt,
            )
        }
        items = (items + added).sortedBy { it.id }
        latestReceivedId = maxOf(latestReceivedId, added.maxOf { it.id })
        persist(env)
    }

    /** 进一次收件箱就清掉仪表盘角标：角标的语义固定是「上次看过之后又到了几条」。 */
    fun markInboxSeen(env: AppEnv) {
        ensureLoaded(env)
        if (lastSeenId == latestReceivedId) return
        lastSeenId = latestReceivedId
        env.prefs(PrefsName).apply {
            putLong(KeyLastSeenId, lastSeenId)
            commit()
        }
    }

    /** 点开一条通知的详情：清掉它左侧的红点。 */
    fun markRead(env: AppEnv, id: Long) {
        ensureLoaded(env)
        if (items.none { it.id == id && !it.read }) return
        items = items.map { if (it.id == id) it.copy(read = true) else it }
        persist(env)
    }

    /** 一键已读：清掉全部红点；仪表盘角标不归它管。 */
    fun markAllRead(env: AppEnv) {
        ensureLoaded(env)
        if (!hasUnread) return
        items = items.map { if (it.read) it else it.copy(read = true) }
        persist(env)
    }

    /**
     * 批量删除：界面立刻少这几条，**落盘推迟**到 [commitRemove]。
     *
     * 收件箱的「删除通知」后面跟着一个可撤销的 Snackbar：点撤销由 [undoRemove] 按 id
     * 顺序放回原位；提示自己消失（或用户退出收件箱）时才真正写进存储。
     * 云端有没有这条都不影响：[latestReceivedId] 也不回退，删掉的通知不会被重新收一次。
     *
     * @return 本批次的编号，交给 [commitRemove] / [undoRemove] 用；没删到东西时为 null。
     */
    fun remove(env: AppEnv, ids: Set<Long>): Long? {
        ensureLoaded(env)
        val removed = items.filter { it.id in ids }
        if (removed.isEmpty()) return null
        // 上一批删除还在等定案就又删了一批：先把旧的落盘，一个待定项只能记一批。
        commitRemove(env)
        val token = ++removalToken
        pendingRemoval = PendingRemoval(token, removed)
        items = items.filterNot { it.id in ids }
        return token
    }

    /**
     * 敲定待定的删除：把当前列表写盘；没有待定项时什么都不做。
     * [token] 不为空时只让自己那一批生效——连着删两批时，前一条 Snackbar 收尾
     * 不能把后一批的删除顺手定案。为 null 是页面退出的兜底，谁在等就定谁。
     */
    fun commitRemove(env: AppEnv, token: Long? = null) {
        val pending = pendingRemoval ?: return
        if (token != null && pending.token != token) return
        pendingRemoval = null
        persist(env)
    }

    /** 撤销待定的删除：按 id 顺序放回列表并落盘；[token] 同 [commitRemove]。 */
    fun undoRemove(env: AppEnv, token: Long? = null) {
        val pending = pendingRemoval ?: return
        if (token != null && pending.token != token) return
        pendingRemoval = null
        items = (items + pending.items).sortedBy { it.id }
        persist(env)
    }

    /**
     * 判断当前 App 版本是否要接收这条通知。
     *
     * 规则文本形如 `[{1.X.X}, {2.1.X}, {3.1.5}]`，按逗号拆成多个版本模式，命中任意一个即可：
     * `X` / `x` / `*` 那一段任意，其余按数字比较；段数不同时短的一边按 0 补齐
     * （`1.X.X` 能命中 `1.2.3`，也能命中 `1`）。
     */
    internal fun matchesVersion(versionName: String, rule: String?): Boolean {
        val text = rule?.trim().orEmpty()
        if (text.isEmpty() || text == AllVersions) return true
        val patterns = text.trim('[', ']', ' ', '\t', '\r', '\n')
            .split(',')
            .map { it.trim().trim('{', '}', '"', ' ') }
            .filter { it.isNotEmpty() }
        // 形如「[]」：一个版本都没列，按全版本处理，否则这条通知永远不会被接收。
        if (patterns.isEmpty()) return true
        return patterns.any { matchesPattern(versionName, it) }
    }

    private fun matchesPattern(versionName: String, pattern: String): Boolean {
        val actual = segmentsOf(versionName)
        val wanted = segmentsOf(pattern)
        for (index in 0 until maxOf(actual.size, wanted.size)) {
            val want = wanted.getOrNull(index) ?: "0"
            if (want in Wildcards) continue
            if (!sameSegment(actual.getOrNull(index) ?: "0", want)) return false
        }
        return true
    }

    private fun segmentsOf(version: String): List<String> =
        version.trim().removePrefix("v").removePrefix("V").split('.').map { it.trim() }

    /** 两边都是纯数字就按数值比（`01` 与 `1` 视为同一段），否则按文本比。 */
    private fun sameSegment(actual: String, wanted: String): Boolean {
        val a = actual.toIntOrNull()
        val w = wanted.toIntOrNull()
        return if (a != null && w != null) a == w else actual == wanted
    }

    /** 抓并解析云端清单；拿不到或不合法都返回空表，等同「这次没收到」。 */
    private fun fetchRemote(): List<CloudNotification> {
        val body = DashboardNetwork.fetch(NotificationUrl) ?: return emptyList()
        val array = try {
            JSONArray(body)
        } catch (_: Exception) {
            // Gitee raw 偶尔会回 HTML 或截断的内容，按「没收到」处理。
            return emptyList()
        }
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                // 单条坏数据只丢它自己，不影响其它通知。
                try {
                    parseCloud(item)?.let(::add)
                } catch (_: Exception) {
                    // 留空。
                }
            }
        }
    }

    private fun parseCloud(item: JSONObject): CloudNotification? {
        val id = item.optLong("id", -1L)
        if (id <= 0L) return null
        val title = item.optString("title").trim().ifEmpty { "系统通知" }
        val content = item.optString("content").trim()
        if (content.isEmpty()) return null
        return CloudNotification(
            id = id,
            title = title,
            content = content,
            links = parseLinks(item),
            versionRule = versionRuleOf(item),
        )
    }

    /**
     * 云端 `urlList` 字段：有链接时是 `[{title, button, url}]` 数组，**没有链接时是字符串 `"null"`**
     * （而不是空数组）。[JSONObject.optJSONArray] 对非数组（含 `"null"`）一律返回 null，
     * 正好当作「没有链接」。
     */
    private fun parseLinks(item: JSONObject): List<NotificationLink> {
        val array = item.optJSONArray("urlList") ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val entry = array.optJSONObject(index) ?: continue
                val url = entry.optString("url").trim()
                // 单条坏链接只丢它自己，不影响其它链接。
                if (url.isEmpty()) continue
                add(
                    NotificationLink(
                        button = entry.optString("button").trim(),
                        title = entry.optString("title").trim(),
                        url = url,
                    ),
                )
            }
        }
    }

    /**
     * 版本规则：0（或缺失）表示全版本；否则可能直接是数组，也可能是一段
     * `[{1.X.X}, {2.1.X}]` 这样的文本。云端字段名在需求里出现过
     * `requiredAppVersionName` / `requiredAppVersionCode` 两种写法，两个都认。
     */
    private fun versionRuleOf(item: JSONObject): String? {
        val raw = item.opt("requiredAppVersionName")
            ?: item.opt("requiredAppVersionCode")
            ?: return null
        if (raw == JSONObject.NULL) return null
        return when (raw) {
            is JSONArray -> buildList {
                for (index in 0 until raw.length()) {
                    raw.optString(index).trim().takeIf { it.isNotEmpty() }?.let(::add)
                }
            }.joinToString(separator = ",", prefix = "[", postfix = "]")

            else -> raw.toString()
        }
    }

    /** 读存档里的链接。 */
    private fun readLinks(item: JSONObject): List<NotificationLink> {
        val array = item.optJSONArray("links") ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val entry = array.optJSONObject(index) ?: continue
                val url = entry.optString("url").trim()
                if (url.isEmpty()) continue
                add(
                    NotificationLink(
                        button = entry.optString("button"),
                        title = entry.optString("title"),
                        url = url,
                    ),
                )
            }
        }
    }

    private fun persist(env: AppEnv) {
        val prefs = env.prefs(PrefsName)
        prefs.putString(KeyItems, itemsToJson())
        prefs.putLong(KeyLatestId, latestReceivedId)
        prefs.putLong(KeyLastSeenId, lastSeenId)
        prefs.commit()
    }

    private fun itemsToJson(): String {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("content", item.content)
                    .put("links", linksToJson(item.links))
                    .put("read", item.read)
                    .put("receivedAt", item.receivedAt),
            )
        }
        return array.toString()
    }

    private fun linksToJson(links: List<NotificationLink>): JSONArray {
        val array = JSONArray()
        links.forEach { link ->
            array.put(
                JSONObject()
                    .put("button", link.button)
                    .put("title", link.title)
                    .put("url", link.url),
            )
        }
        return array
    }

    private fun readItems(raw: String?): List<StoredNotification> {
        if (raw.isNullOrBlank()) return emptyList()
        val array = try {
            JSONArray(raw)
        } catch (_: Exception) {
            // 存档损坏时当作空收件箱，下次收件会重新落一份干净的。
            return emptyList()
        }
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optLong("id", -1L)
                if (id <= 0L) continue
                add(
                    StoredNotification(
                        id = id,
                        title = item.optString("title"),
                        content = item.optString("content"),
                        links = readLinks(item),
                        read = item.optBoolean("read", false),
                        receivedAt = item.optLong("receivedAt", 0L),
                    ),
                )
            }
        }.sortedBy { it.id }
    }
}

/** 等 Snackbar 定案的一批删除：批次号用来区分「撤销 / 定案」是谁的事。 */
private data class PendingRemoval(
    val token: Long,
    val items: List<StoredNotification>,
)

/** 云端清单里的一条，只在收件过程中短暂存在。 */
private data class CloudNotification(
    val id: Long,
    val title: String,
    val content: String,
    val links: List<NotificationLink>,
    val versionRule: String?,
)
