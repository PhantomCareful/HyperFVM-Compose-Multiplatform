package com.careful.hyperfvm.compose.data.announcement

import com.careful.hyperfvm.compose.platform.AppEnv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 公告与 B 站图文动态的数据仓库（最新公告、历史合集、任意 opus 动态）。
 *
 * 链接每次加载都重新取：m3u 只有几十字节，新公告上线后不用升级 App 就能看到。
 * 正文按 opus id 缓存在 `cacheDir/announcement/<id>.json`（存接口原始 JSON），
 * 同一期公告只在第一次打开时请求 B 站，之后进页面直接读磁盘；下拉刷新才重取。
 *
 * 完全离线时退回上一次成功解析的公告，至少还能看历史内容。
 */
object AnnouncementRepository {

    private const val JsonSuffix = ".json"

    /** 合集列表的缓存文件名（不含后缀），与正文缓存挨在同一个目录里。 */
    private const val ArchiveCacheId = "archive"

    /** 上一次解析成功的公告，仪表盘与公告页共享。 */
    @Volatile
    private var cachedAnnouncement: Announcement? = null

    /** 合集列表的内存缓存，与正文分开：列表页进来先上屏，再在后台刷新。 */
    @Volatile
    private var cachedArchive: AnnouncementArchive? = null

    /** 近期看过的历史公告，避免在同一篇上反复读盘与联网。 */
    private val articleMemory = ConcurrentHashMap<String, Announcement>()

    fun cached(): Announcement? = cachedAnnouncement

    fun cachedArchive(): AnnouncementArchive? = cachedArchive

    /**
     * 加载当前公告。
     *
     * @param refresh 为 true 时跳过磁盘缓存重新请求（公告页下拉刷新）；
     *   内存里的同一期公告仍会先返回，避免每次进页面都白屏。
     */
    suspend fun load(env: AppEnv, refresh: Boolean = false): Announcement =
        withContext(Dispatchers.IO) {
            val link = AnnouncementSource.latestLink()
            val id = link?.let(AnnouncementSource::opusId)

            // 链接都拿不到（离线 / 仓库抽风）：有旧的就先看旧的。
            if (id == null) {
                val fallback = cachedAnnouncement ?: readLatestCache(env)
                if (fallback != null) return@withContext fallback
                throw AnnouncementException("拿不到公告链接，请检查网络后重试。")
            }

            val memory = cachedAnnouncement
            if (!refresh && memory?.id == id) return@withContext memory

            if (!refresh) {
                readCache(env, id)?.let { announcement ->
                    cachedAnnouncement = announcement
                    // 换期时先把上一期的图片清掉，后面的下载都归这一期。
                    AnnouncementImages.prune(env, setOf(id))
                    return@withContext announcement
                }
            }

            val body = AnnouncementSource.fetchDetailJson(id)
            val announcement = AnnouncementSource.parse(id, body)
            writeCache(env, id, body)
            cachedAnnouncement = announcement
            AnnouncementImages.prune(env, setOf(id))
            announcement
        }

    /**
     * 加载合集列表（B 站「版本更新」文集，当前 139 篇）。
     *
     * 接口一次就返回全部篇目，磁盘缓存存原始 JSON：下拉刷新才重新请求。
     */
    suspend fun loadArchive(env: AppEnv, refresh: Boolean = false): AnnouncementArchive =
        withContext(Dispatchers.IO) {
            if (!refresh) {
                cachedArchive?.let { return@withContext it }
                readArchiveCache(env)?.let {
                    cachedArchive = it
                    return@withContext it
                }
            }
            val body = AnnouncementSource.fetchReadlistJson()
            val archive = AnnouncementSource.parseReadlist(body)
            writeCache(env, ArchiveCacheId, body)
            cachedArchive = archive
            archive
        }

    /**
     * 打开合集里的某一期。
     *
     * 新版公告的正文在 opus 详情接口里（限频宽松），先试它；老专栏在那里只有跳转信息，
     * 失败后改走正文接口 / 专栏网页（两条路交替重试）——每篇只在第一次打开时拉一次，
     * 解析用的原始 JSON 会落盘，再看直接读本地。
     */
    suspend fun loadArticle(
        env: AppEnv,
        entry: AnnouncementEntry,
        refresh: Boolean = false,
    ): Announcement = withContext(Dispatchers.IO) {
        if (!refresh) {
            articleMemory[entry.cvId]?.let { return@withContext it }
            readArticleCache(env, entry)?.let {
                articleMemory[entry.cvId] = it
                return@withContext it
            }
        }
        val announcement = fetchArticle(env, entry)
        articleMemory[entry.cvId] = announcement
        // 当前这篇与「最新公告」那篇共用图库，两边的缓存 id 都要留住：
        // 新版公告的图片挂在 opus id 下，老专栏解析后 id 是 cv 号（图片也跟着挂在它下面）。
        AnnouncementImages.prune(
            env,
            setOfNotNull(entry.opusId.takeIf { it.isNotEmpty() }, entry.cvId, cachedAnnouncement?.id),
        )
        announcement
    }

    /**
     * 打开任意一条 opus 图文动态（巅峰对决的赛季规则 / 赛季奖励）。
     *
     * 正文与「最新公告」共用同一套接口、解析器和缓存目录：同一篇只看一次网络，
     * 之后进页面直接读盘。内存缓存用 `opus-` 前缀另外存，不把某条活动动态
     * 当成公告的最新媒体。解析不了时抛 [AnnouncementException]，页面照旧给
     * 「在哔哩哔哩中打开」。
     */
    suspend fun loadOpus(
        env: AppEnv,
        opusId: String,
        refresh: Boolean = false,
    ): Announcement = withContext(Dispatchers.IO) {
        val memoryKey = "opus-$opusId"
        if (!refresh) {
            articleMemory[memoryKey]?.let { return@withContext it }
            readCache(env, opusId)?.let {
                articleMemory[memoryKey] = it
                return@withContext it
            }
        }
        val body = AnnouncementSource.fetchDetailJson(opusId)
        val announcement = AnnouncementSource.parse(opusId, body)
        writeCache(env, opusId, body)
        articleMemory[memoryKey] = announcement
        // 与公告页同一口径：图片缓存只留当前这一篇与最新公告那一篇。
        AnnouncementImages.prune(env, setOfNotNull(opusId, cachedAnnouncement?.id))
        announcement
    }

    private fun directory(env: AppEnv): File = File(env.cacheDir, "announcement")

    private fun cacheFile(env: AppEnv, id: String): File =
        File(directory(env), "$id$JsonSuffix")

    private fun readCache(env: AppEnv, id: String): Announcement? {
        val file = cacheFile(env, id)
        if (!file.isFile) return null
        return try {
            AnnouncementSource.parse(id, file.readText())
        } catch (_: Exception) {
            // 缓存坏了（写一半、接口结构变了）就删掉，下次重新请求。
            file.delete()
            null
        }
    }

    private fun readArchiveCache(env: AppEnv): AnnouncementArchive? {
        val file = cacheFile(env, ArchiveCacheId)
        if (!file.isFile) return null
        return try {
            AnnouncementSource.parseReadlist(file.readText())
        } catch (_: Exception) {
            file.delete()
            null
        }
    }

    /**
     * 某篇历史的正文缓存：opus 格式存 `article-<cv>.json`，专栏正文存
     * `article-<cv>-cv.json`。两种结构不同，分开存才能直接选对应的解析器
     * （专栏那份里可能是网页 SSR 的 `detail`，也可能是正文接口的整包，解析器都认）。
     */
    private fun readArticleCache(env: AppEnv, entry: AnnouncementEntry): Announcement? {
        val opusFile = File(directory(env), "article-${entry.cvId}$JsonSuffix")
        if (opusFile.isFile && entry.opusId.isNotEmpty()) {
            val parsed = runCatching { AnnouncementSource.parse(entry.opusId, opusFile.readText()) }.getOrNull()
            if (parsed != null) return parsed
            opusFile.delete()
        }
        val cvFile = File(directory(env), "article-${entry.cvId}-cv$JsonSuffix")
        if (cvFile.isFile) {
            val parsed = runCatching { AnnouncementSource.parseArticle(entry.cvId, cvFile.readText()) }.getOrNull()
            if (parsed != null) return parsed
            cvFile.delete()
        }
        return null
    }

    /** 先 opus 后专栏（正文接口 / 网页交替）；拿到的正文 JSON 顺手写盘，下次打开就不必再联网。 */
    private fun fetchArticle(env: AppEnv, entry: AnnouncementEntry): Announcement {
        if (entry.opusId.isNotEmpty()) {
            try {
                val body = AnnouncementSource.fetchDetailJson(entry.opusId)
                val announcement = AnnouncementSource.parse(entry.opusId, body)
                writeCache(env, "article-${entry.cvId}", body)
                return announcement
            } catch (_: AnnouncementException) {
                // 老式专栏：opus 接口只给跳转信息，改走正文接口 / 网页。
            }
        }
        val body = AnnouncementSource.fetchArticleJson(entry.cvId)
        val announcement = AnnouncementSource.parseArticle(entry.cvId, body)
        writeCache(env, "article-${entry.cvId}-cv", body)
        return announcement
    }

    /** 目录里最近改过的那份正文缓存：离线兜底用（合集列表与历史正文的缓存不算）。 */
    private fun readLatestCache(env: AppEnv): Announcement? =
        directory(env).listFiles()
            ?.filter { it.isFile && it.name.endsWith(JsonSuffix) }
            ?.filter { !it.name.startsWith("article-") && it.name != "$ArchiveCacheId$JsonSuffix" }
            ?.maxByOrNull { it.lastModified() }
            ?.let { file -> readCache(env, file.name.removeSuffix(JsonSuffix)) }

    private fun writeCache(env: AppEnv, id: String, body: String) {
        val file = cacheFile(env, id)
        val temp = File(file.parentFile, "${file.name}.part")
        try {
            file.parentFile?.mkdirs()
            temp.writeText(body)
            if (!temp.renameTo(file)) {
                temp.copyTo(file, overwrite = true)
                temp.delete()
            }
        } catch (_: Exception) {
            temp.delete()
        }
    }
}
