package com.careful.hyperfvm.compose.data.announcement

import com.careful.hyperfvm.compose.platform.Logs
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 公告拿不到、或者在 App 内没法渲染时抛出；[message] 直接拿去当界面提示，
 * [url] 是这条公告的网页地址（已经知道是哪一条时才有，用来兜底跳浏览器）。
 */
class AnnouncementException(message: String, val url: String? = null) : Exception(message)

/**
 * 公告的两级来源：云端仓库的 m3u 只存一个 opus 链接，正文要靠 B 站接口解析。
 *
 * 为什么不用 `/x/polymer/web-dynamic/v1/detail`：文章型动态在那里只返回封面和
 * 一串 `[图片]` 占位，正文拿不到。PiliPlus 的图文详情页走的是
 * `/x/polymer/web-dynamic/v1/opus/detail`，无需登录、无需 Wbi 签名，
 * 只要求一个浏览器 UA（缺失时 B 站直接回 412），实测能拿到完整段落。
 *
 * 老式专栏（`data.fallback` 非空）在 opus 接口里只有跳转信息，正文另走两条路：
 * 带安卓 App 签名的 `/x/article/view`（正文在 `data.opus.content.paragraphs`），
 * 以及专栏网页的 SSR 数据（`__INITIAL_STATE__.detail` 的 modules）。两边的段落
 * 结构与 opus 基本一致，一份解析够用；区别只是正文接口用数字 `node_type` 标节点。
 *
 * 这两条路的风控都是随机的——接口随机回 -352 / -509，网页随机只回空壳，
 * 换全套浏览器 Cookie 也一样，所以只能交替重试。
 */
internal object AnnouncementSource {

    /** Java 版 `BilibiliFVMUtil.ANNOUNCEMENT_URL`，内容就是一条 opus 链接。 */
    private const val LinkUrl =
        "https://gitee.com/phantom-careful/hyper-fvm-updater/raw/main/LatestBilibiliFVMAnnouncement.m3u"

    private const val OpusDetailUrl = "https://api.bilibili.com/x/polymer/web-dynamic/v1/opus/detail"

    /** 「版本更新」图文合集（B 站 rl747016）：列表一次返回全部篇目，不分页。 */
    private const val ReadlistId = "747016"

    /** 合集里每一期的标题都以这类前缀开头，卡片上显示时统一削掉。 */
    private val TitlePrefixes = listOf("【版本更新】", "【更新公告】")

    private const val ReadlistUrl = "https://api.bilibili.com/x/article/list/web/articles"

    /**
     * 老式 cv 专栏的正文接口。
     *
     * 带上安卓 App 的 appkey 签名后依然会被随机风控（-352 / -509），只是概率比裸请求
     * 略好；成功时正文在 `data.opus.content.paragraphs`，与新版公告同一套段落结构。
     */
    private const val ArticleApiUrl = "https://api.bilibili.com/x/article/view"
    private const val ArticleAppKey = "1d8b6e7d45233436"
    private const val ArticleAppSecret = "560c52ccd288fed045859ed18bffd973"
    private const val ArticleAppBuild = "7780300"

    /**
     * 每次重试前等多久（第 0 次不等）。
     *
     * 连续快打会把两条路一起逼进风控：接口回 -509、网页只给空壳。同一台设备上实测，
     * 间隔 1 秒时网页 4 次里只有 1 次完整，间隔 5 秒时 4 次里有 3 次；正文只拉一次，
     * 慢一点无妨。
     */
    private val ArticleRetryDelays = longArrayOf(0L, 800L, 1_800L, 3_200L)

    private const val Tag = "AnnouncementSource"

    private const val UserAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    private const val Referer = "https://www.bilibili.com/"

    private const val ConnectTimeoutMs = 10_000
    private const val ReadTimeoutMs = 15_000

    /** 形如 `https://www.bilibili.com/opus/1269373302288154631`。 */
    private val OpusLinkRegex = Regex("""(?:opus|t)/(\d{6,})""")

    /** 云端仓库里当前那份公告的链接；失败返回 null，由界面提示重试。 */
    fun latestLink(): String? = try {
        // 同仪表盘的几个源：末尾拼随机数绕开 gitee 的 CDN 缓存，否则新公告要等缓存过期。
        getText("$LinkUrl?t=${System.currentTimeMillis()}")
            ?.trim()
            ?.takeIf { it.startsWith("http") }
    } catch (_: Exception) {
        null
    }

    /** 从链接里抠出 opus id；不认识这个链接时返回 null。 */
    fun opusId(link: String): String? {
        val trimmed = link.trim()
        // 仓库里万一只写了裸 id。
        if (trimmed.length >= 6 && trimmed.all(Char::isDigit)) return trimmed
        return OpusLinkRegex.find(trimmed)?.groupValues?.get(1)
    }

    /** 拉原始 JSON：磁盘缓存直接存它，解析逻辑以后再改也不用让老缓存失效。 */
    fun fetchDetailJson(id: String): String = try {
        getText("$OpusDetailUrl?id=$id&timezone_offset=-480")
    } catch (e: Exception) {
        throw AnnouncementException("公告正文获取失败：${e.message ?: "网络异常"}", opusUrl(id))
    } ?: throw AnnouncementException("公告正文获取失败，请稍后重试。", opusUrl(id))

    /**
     * 把 opus 详情 JSON 解析成公告。
     *
     * `modules` 是数组，按 `module_type` 取标题、作者、正文三块；
     * 正文的 `paragraphs` 里，`pic.pics` 是图片段，`text.nodes` 是文字段，
     * 连续的文字段合并成一块，避免一句话被拆成十几个 Text。
     */
    fun parse(id: String, body: String): Announcement = try {
        parseOrThrow(id, body)
    } catch (e: AnnouncementException) {
        // 解析阶段的失败也补上网页地址，界面才有「在哔哩哔哩中打开」可选。
        throw AnnouncementException(e.message ?: "公告解析失败。", e.url ?: opusUrl(id))
    }

    private fun parseOrThrow(id: String, body: String): Announcement {
        val root = try {
            JSONObject(body)
        } catch (_: Exception) {
            throw AnnouncementException("公告正文解析失败。")
        }
        val code = root.optInt("code")
        throwIfRateLimited(root)
        if (code != 0) throw AnnouncementException("公告接口返回错误（$code）。")

        val data = root.optJSONObject("data") ?: throw AnnouncementException("公告正文为空。")
        // 老式 cv 专栏在这里只给跳转信息，正文改从专栏网页拿（见 fetchArticleJson）。
        if (data.optJSONObject("fallback") != null) {
            throw AnnouncementException("这条公告是旧版专栏，只能在哔哩哔哩中查看。")
        }
        val modules = data.optJSONObject("item")?.optJSONArray("modules")
            ?: throw AnnouncementException("公告正文为空。")
        return readModules(id, modules, opusUrl(id))
    }

    /**
     * 读 modules：标题、作者、正文各占一个模块，正文里的段落再分图片、文字、
     * 小标题、列表几种。opus 详情与专栏网页的正文都是这套结构，一份解析两处用。
     */
    private fun readModules(id: String, modules: JSONArray, url: String): Announcement {
        var title = ""
        var author = ""
        var publishedAt = ""
        val blocks = mutableListOf<AnnouncementBlock>()
        val pending = PendingText()

        for (index in 0 until modules.length()) {
            val module = modules.optJSONObject(index) ?: continue
            when (module.optString("module_type")) {
                "MODULE_TYPE_TITLE" ->
                    title = module.optJSONObject("module_title")?.optString("text").orEmpty()

                "MODULE_TYPE_AUTHOR" -> module.optJSONObject("module_author")?.let { moduleAuthor ->
                    author = moduleAuthor.optString("name")
                    publishedAt = cleanPublishTime(moduleAuthor.optString("pub_time"))
                }

                "MODULE_TYPE_CONTENT" -> {
                    val paragraphs = module.optJSONObject("module_content")?.optJSONArray("paragraphs")
                    if (paragraphs != null) appendParagraphs(paragraphs, blocks, pending)
                }
            }
        }
        pending.flushInto(blocks)

        if (blocks.isEmpty()) throw AnnouncementException("公告正文为空。")
        return Announcement(
            id = id,
            title = title,
            author = author,
            publishedAt = publishedAt,
            blocks = blocks,
            url = url,
        )
    }

    /** 专栏网页给的时间带「编辑于」前缀，与 opus 的 `pub_time` 对齐。 */
    private fun cleanPublishTime(text: String): String = text.trim().removePrefix("编辑于").trim()

    private fun appendParagraphs(
        paragraphs: JSONArray,
        blocks: MutableList<AnnouncementBlock>,
        pending: PendingText,
    ) {
        for (index in 0 until paragraphs.length()) {
            val paragraph = paragraphs.optJSONObject(index) ?: continue

            val pics = paragraph.optJSONObject("pic")?.optJSONArray("pics")
            if (pics != null && pics.length() > 0) {
                pending.flushInto(blocks)
                for (picIndex in 0 until pics.length()) {
                    val pic = pics.optJSONObject(picIndex) ?: continue
                    val url = pic.optString("url")
                    if (!url.startsWith("http")) continue
                    blocks += AnnouncementBlock.Image(
                        url = cleanImageUrl(url),
                        width = pic.optInt("width"),
                        height = pic.optInt("height"),
                    )
                }
                continue
            }

            // 老公告把「活动一」「维护内容」写成 markdown 的分级标题，单独成块才看得出层次。
            val heading = paragraph.optJSONObject("heading")
            if (heading != null) {
                val content = heading.optJSONArray("nodes").plainText()
                if (content.isNotEmpty()) {
                    pending.flushInto(blocks)
                    blocks += AnnouncementBlock.Heading(heading.optInt("level", 2), content)
                }
                continue
            }

            // 列表：每一项自带节点，换成一行一条、前面补项目符号。
            val list = paragraph.optJSONObject("list")
            if (list != null) {
                val items = list.optJSONArray("items")
                for (itemIndex in 0 until (items?.length() ?: 0)) {
                    val content = items?.optJSONObject(itemIndex)?.optJSONArray("nodes").plainText()
                    if (content.isNullOrEmpty()) continue
                    if (pending.isNotEmpty()) pending.append("\n")
                    pending.append("· $content")
                }
                pending.appendParagraphBreak()
                continue
            }

            // 代码块：偶尔用来贴兑换码 / 命令，按原样保留换行。
            val code = paragraph.optJSONObject("code")
            if (code != null) {
                val content = code.optString("content").trim()
                if (content.isNotEmpty()) {
                    pending.appendParagraphBreak()
                    pending.append(content)
                }
                pending.appendParagraphBreak()
                continue
            }

            // 引用段落：引号之外与普通文字一样，宁可把字显示出来。
            val quote = paragraph.optJSONObject("blockquote")
            if (quote != null) {
                pending.appendNodes(quote.optJSONArray("nodes"))
                pending.appendParagraphBreak()
                continue
            }

            pending.appendNodes(paragraph.optJSONObject("text")?.optJSONArray("nodes"))
            // 普通图文会有连续多段短文字，段落之间留个空行才不至于挤成一坨。
            pending.appendParagraphBreak()
        }
        pending.flushInto(blocks)
    }

    /** 取一段节点的纯文字：标题与列表项只需要字，不需要加粗区间。 */
    private fun JSONArray?.plainText(): String {
        if (this == null) return ""
        val pending = PendingText()
        pending.appendNodes(this)
        return pending.content()
    }

    /**
     * 攒文字用的手写缓冲区：一期内连续的文字段落合成一个 Text 块，
     * 同时记下哪些区间是加粗的，避免一句话被拆成十几个 Text。
     */
    private class PendingText {

        private val text = StringBuilder()
        private val spans = mutableListOf<TextSpan>()

        fun isNotEmpty(): Boolean = text.isNotEmpty()

        fun content(): String = text.toString().trim()

        fun append(content: String, bold: Boolean = false) {
            if (content.isEmpty()) return
            val start = text.length
            text.append(content)
            if (bold) spans += TextSpan(start, text.length)
        }

        /**
         * 文字由若干节点拼成：普通单词、富文本、@用户、公式各取自己那段可见文字。
         *
         * 节点类型在两套来源里标法不同（opus 是字符串 `type`，正文接口是数字
         * `node_type`），所以按「带哪个子对象」分派，两种都给认。
         */
        fun appendNodes(nodes: JSONArray?) {
            if (nodes == null) return
            for (index in 0 until nodes.length()) {
                val node = nodes.optJSONObject(index) ?: continue
                val word = node.optJSONObject("word")
                val rich = node.optJSONObject("rich")
                val user = node.optJSONObject("user")
                val formula = node.optJSONObject("formula")
                when {
                    word != null -> append(word.optString("words"), word.isBold())
                    rich != null -> append(rich.optString("text"), rich.isBold())
                    user != null -> append(user.optString("name"))
                    formula != null -> append(formula.optString("latex"))
                }
            }
        }

        /** 普通图文有连续多段短文字，段落之间留个空行才不至于挤成一坨。 */
        fun appendParagraphBreak() {
            if (text.isEmpty() || text.endsWith("\n\n")) return
            text.append("\n\n")
        }

        /** 攒下的文字收成一块；只有换行的空段直接丢掉，加粗区间跟着去掉的首尾空白一起平移。 */
        fun flushInto(blocks: MutableList<AnnouncementBlock>) {
            val raw = text.toString()
            val content = raw.trim()
            val collected = spans.toList()
            text.clear()
            spans.clear()
            if (content.isEmpty()) return

            // trim 去掉了开头的若干空白，所有区间整体左移同样多的字符。
            val offset = raw.length - raw.trimStart().length
            val shifted = collected.mapNotNull { span ->
                val start = (span.start - offset).coerceIn(0, content.length)
                val end = (span.end - offset).coerceIn(0, content.length)
                if (end > start) TextSpan(start, end) else null
            }
            blocks += AnnouncementBlock.Text(content, shifted)
        }

        private fun JSONObject.isBold(): Boolean =
            optJSONObject("style")?.optBoolean("bold") == true
    }

    /**
     * 去掉 B 站图片地址上的规格后缀（`@300w.webp` 之类）并换成 https：
     * 具体下多大由 [AnnouncementImages] 统一决定，这里只保留原图地址。
     */
    private fun cleanImageUrl(url: String): String =
        url.substringBefore('@').replaceFirst("http://", "https://")

    private fun opusUrl(id: String): String = "$OpusWebBaseUrl/$id"

    private fun articleUrl(cvId: String): String = "$ArticleWebBaseUrl/$cvId"

    /** 拉合集列表：每篇给 cv 号与 `dyn_id_str`（就是 opus id），正文留到点开时再拉。 */
    fun fetchReadlistJson(): String = try {
        getText("$ReadlistUrl?id=$ReadlistId")
    } catch (e: Exception) {
        throw AnnouncementException("公告合集获取失败：${e.message ?: "网络异常"}")
    } ?: throw AnnouncementException("公告合集获取失败，请稍后重试。")

    fun parseReadlist(body: String): AnnouncementArchive {
        val root = try {
            JSONObject(body)
        } catch (_: Exception) {
            throw AnnouncementException("公告合集解析失败。")
        }
        throwIfRateLimited(root)
        val code = root.optInt("code")
        if (code != 0) throw AnnouncementException("公告合集接口返回错误（$code）。")

        val data = root.optJSONObject("data") ?: throw AnnouncementException("公告合集为空。")
        val articles = data.optJSONArray("articles") ?: throw AnnouncementException("公告合集为空。")
        val entries = buildList {
            for (index in 0 until articles.length()) {
                val item = articles.optJSONObject(index) ?: continue
                val cvId = item.optString("id").takeIf { it.isNotEmpty() } ?: continue
                add(
                    AnnouncementEntry(
                        cvId = cvId,
                        opusId = item.optString("dyn_id_str"),
                        title = stripTitlePrefix(item.optString("title")),
                        publishedAt = item.optLong("publish_time"),
                        summary = item.optString("summary"),
                        coverUrl = item.optJSONArray("image_urls")?.optString(0).orEmpty(),
                    ),
                )
            }
        }
        if (entries.isEmpty()) throw AnnouncementException("公告合集为空。")
        return AnnouncementArchive(id = ReadlistId, entries = entries)
    }

    /** 只削掉标题开头的「【版本更新】」/「【更新公告】」，剩下的日期与内容保持原样。 */
    private fun stripTitlePrefix(title: String): String {
        val trimmed = title.trimStart()
        val prefix = TitlePrefixes.firstOrNull(trimmed::startsWith) ?: return trimmed.trim()
        return trimmed.removePrefix(prefix).trim()
    }

    /**
     * 拉某期老式专栏的正文：正文接口与专栏网页交替尝试。
     *
     * 两边的失败都是随机的，单靠一条路经常连续几次都打不回来；交替之后任意一次
     * 成功就算拿到。重试用尽后抛出带网页地址的异常，页面照旧给「在哔哩哔哩中打开」。
     */
    fun fetchArticleJson(cvId: String): String {
        var lastError: Exception? = null
        var blocked = false
        for (attempt in ArticleRetryDelays.indices) {
            val delay = ArticleRetryDelays[attempt]
            if (delay > 0L) Thread.sleep(delay)
            // 先走接口：专栏网页在部分网络下几乎每次都只给空壳。
            val outcome = if (attempt % 2 == 0) {
                fetchArticleFromApi(cvId)
            } else {
                fetchArticleFromPage(cvId)
            }
            when (outcome) {
                is FetchOutcome.Success -> return outcome.body
                FetchOutcome.Blocked -> blocked = true
                is FetchOutcome.Failed -> lastError = outcome.error ?: lastError
            }
        }
        throw AnnouncementException(
            when {
                // 断网 / 超时之类的具体原因比「返回错误码」更有用。
                lastError != null -> "公告正文获取失败：${lastError.message ?: "网络异常"}"
                blocked -> "请求过于频繁，请稍后再试。"
                else -> "公告正文获取失败，请稍后重试。"
            },
            articleUrl(cvId),
        )
    }

    /** 一次取正文的结局：拿到、被风控拦下，或者网络 / 页面结构不对。 */
    private sealed interface FetchOutcome {
        class Success(val body: String) : FetchOutcome
        object Blocked : FetchOutcome
        class Failed(val error: Exception? = null) : FetchOutcome
    }

    /** 走正文接口：老专栏在这里也返回 `data.opus.content.paragraphs`。 */
    private fun fetchArticleFromApi(cvId: String): FetchOutcome {
        // 参数按字母序拼好再对 appkey 的 secret 取 md5，就是 App 端的签名方式。
        val query = "appkey=$ArticleAppKey&build=$ArticleAppBuild&id=$cvId" +
            "&mobi_app=android&platform=android&ts=${System.currentTimeMillis() / 1000}"
        val body = try {
            getText("$ArticleApiUrl?$query&sign=${md5(query + ArticleAppSecret)}")
        } catch (e: Exception) {
            return FetchOutcome.Failed(e)
        } ?: return FetchOutcome.Failed()

        val root = try {
            JSONObject(body)
        } catch (_: Exception) {
            return FetchOutcome.Failed()
        }
        val code = root.optInt("code")
        if (code != 0) {
            Logs.w(Tag, "article api cv$cvId -> $code")
            // -509 是「请求过于频繁」，-352 同为风控拦截，下一轮换条路再试。
            return if (code == -509 || code == -352) FetchOutcome.Blocked else FetchOutcome.Failed()
        }
        val paragraphs = root.optJSONObject("data")
            ?.optJSONObject("opus")
            ?.optJSONObject("content")
            ?.optJSONArray("paragraphs")
        if (paragraphs == null || paragraphs.length() == 0) {
            Logs.w(Tag, "article api cv$cvId -> no paragraphs")
            return FetchOutcome.Failed()
        }
        return FetchOutcome.Success(body)
    }

    /** 抓专栏网页，从 SSR 数据里抠出 `detail` 对象；空壳页算这一轮没拿到。 */
    private fun fetchArticleFromPage(cvId: String): FetchOutcome {
        val html = try {
            getText("$ArticleWebBaseUrl/$cvId")
        } catch (e: Exception) {
            return FetchOutcome.Failed(e)
        } ?: return FetchOutcome.Failed()
        val detail = extractArticleDetail(html)
        if (detail == null) {
            Logs.w(Tag, "article page cv$cvId -> shell ${html.length}")
            return FetchOutcome.Failed()
        }
        return FetchOutcome.Success(detail)
    }

    /** 签名用的小写 md5 十六进制串。 */
    private fun md5(text: String): String =
        MessageDigest.getInstance("MD5").digest(text.toByteArray())
            .joinToString("") { "%02x".format(it) }

    /**
     * 从专栏网页里抠出 `__INITIAL_STATE__.detail` 这一段 JSON。
     *
     * 整个页面是 HTML 包着一大段内嵌脚本，只能按花括号配平截取：字符串里的括号与
     * 转义要跳过，否则正文里出现的 `{` 会让截取提前结束。拿不到正文模块（空壳页）
     * 时返回 null，交给上层重试。
     */
    private fun extractArticleDetail(html: String): String? {
        val stateStart = html.indexOf("__INITIAL_STATE__")
        if (stateStart < 0) return null
        val detailKey = "\"detail\":"
        var start = html.indexOf(detailKey, stateStart)
        if (start < 0) return null
        start += detailKey.length
        while (start < html.length && html[start].isWhitespace()) start++
        // `detail` 为 null（文章被删 / 风控页）时没必要再去猜下一个花括号是谁的。
        if (start >= html.length || html[start] != '{') return null

        var depth = 0
        var inString = false
        var escaped = false
        for (index in start until html.length) {
            when {
                inString -> when {
                    escaped -> escaped = false
                    html[index] == '\\' -> escaped = true
                    html[index] == '"' -> inString = false
                }

                html[index] == '"' -> inString = true
                html[index] == '{' -> depth++
                html[index] == '}' -> {
                    depth--
                    if (depth == 0) {
                        val detail = html.substring(start, index + 1)
                        return detail.takeIf { it.contains("\"modules\"") }
                    }
                }
            }
        }
        return null
    }

    fun parseArticle(cvId: String, body: String): Announcement = try {
        parseArticleOrThrow(cvId, body)
    } catch (e: AnnouncementException) {
        throw AnnouncementException(e.message ?: "公告解析失败。", e.url ?: articleUrl(cvId))
    }

    /**
     * 专栏正文有三种结构：网页 SSR 里的 `detail`（自带 modules）、正文接口的整包响应
     * （正文在 `data.opus.content.paragraphs`）、以及更早缓存下来的同类整包。三者都认，
     * 已经落盘的历史公告不用为了换接口重新联网。
     */
    private fun parseArticleOrThrow(cvId: String, body: String): Announcement {
        val root = try {
            JSONObject(body)
        } catch (_: Exception) {
            throw AnnouncementException("公告正文解析失败。")
        }
        if (!root.has("data")) {
            val modules = root.optJSONArray("modules")
                ?: throw AnnouncementException("这条公告的正文格式不支持。")
            return readModules(cvId, modules, articleUrl(cvId))
        }

        throwIfRateLimited(root)
        val code = root.optInt("code")
        if (code != 0) throw AnnouncementException("公告接口返回错误（$code）。")

        val data = root.optJSONObject("data") ?: throw AnnouncementException("公告正文为空。")
        val paragraphs = data.optJSONObject("opus")
            ?.optJSONObject("content")
            ?.optJSONArray("paragraphs")
            ?: throw AnnouncementException("这条公告的正文格式不支持。")

        val blocks = mutableListOf<AnnouncementBlock>()
        val pending = PendingText()
        appendParagraphs(paragraphs, blocks, pending)
        pending.flushInto(blocks)
        if (blocks.isEmpty()) throw AnnouncementException("公告正文为空。")

        return Announcement(
            id = cvId,
            // 正文接口的标题带「【版本更新】」之类的前缀，与合集卡片保持一致地削掉。
            title = stripTitlePrefix(data.optString("title")),
            author = data.optJSONObject("author")?.optString("name").orEmpty(),
            publishedAt = formatPublishTime(data.optLong("publish_time")),
            blocks = blocks,
            url = articleUrl(cvId),
        )
    }

    /** B 站对未登录请求限得很紧：-509 是「请求过于频繁」，给能看懂的提示。 */
    private fun throwIfRateLimited(root: JSONObject) {
        if (root.optInt("code") == -509) {
            throw AnnouncementException("请求过于频繁，请稍后再试。")
        }
    }

    /** epoch 秒 → 与 opus 接口一致的中文时间文案。 */
    private fun formatPublishTime(epochSeconds: Long): String {
        if (epochSeconds <= 0L) return ""
        val format = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA)
        format.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        return format.format(Date(epochSeconds * 1000L))
    }

    private fun getText(url: String): String? {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = ConnectTimeoutMs
            connection.readTimeout = ReadTimeoutMs
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", UserAgent)
            connection.setRequestProperty("Referer", Referer)
            val code = connection.responseCode
            if (code !in 200..299) {
                Logs.w(Tag, "http $code <- $url")
                null
            } else {
                connection.inputStream.bufferedReader().use { it.readText() }
            }
        } finally {
            connection.disconnect()
        }
    }
}
