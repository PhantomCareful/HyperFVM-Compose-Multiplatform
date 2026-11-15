package com.careful.hyperfvm.compose.data.announcement

import androidx.compose.runtime.Immutable

/** opus 图文动态的网页地址前缀：App 内解析失败时用它在浏览器里打开。 */
internal const val OpusWebBaseUrl = "https://www.bilibili.com/opus"

/** 老式 cv 专栏的网页地址前缀，老公告解析失败时用它兜底。 */
internal const val ArticleWebBaseUrl = "https://www.bilibili.com/read/cv"

/**
 * 一条更新公告，对应云端仓库里那个 opus 链接指向的图文动态，
 * 或者合集里某一期历史公告。
 *
 * 公告正文是「图片 + 文字 + 小标题」的序列：游戏官方的维护公告基本全是长图，
 * 早期的公告则文字非常多，靠小标题分节、靠加粗标项目名，所以块里带上了这两种信息。
 *
 * @param publishedAt 接口原样返回的中文时间文案（如「2026年11月11日 16:56」）。
 * @param url 原始网页地址：「在哔哩哔哩中打开」的兜底入口；新旧两种公告分别指向 opus 与 cv 页。
 */
@Immutable
data class Announcement(
    val id: String,
    val title: String,
    val author: String,
    val publishedAt: String,
    val blocks: List<AnnouncementBlock>,
    val url: String,
)

/** 公告正文的一段：图片、文字或小标题。 */
sealed interface AnnouncementBlock {

    /** [width] / [height] 是原图像素尺寸，用来按比例占位，图片还没下完时也不会跳版。 */
    data class Image(val url: String, val width: Int, val height: Int) : AnnouncementBlock

    /**
     * 一段文字。
     *
     * @param spans 其中需要加粗的区间：老公告把「活动时间」「活动内容」这类小标题
     *   也用加粗标出来，丢掉就只剩一大坨平文。
     */
    data class Text(val content: String, val spans: List<TextSpan> = emptyList()) : AnnouncementBlock

    /**
     * 小标题：老公告用 markdown 的 `#` 分级写小节名，接口单独给出 level（1 最大）。
     */
    data class Heading(val level: Int, val content: String) : AnnouncementBlock
}

/** [AnnouncementBlock.Text.content] 里的一段加粗区间，下标半开（含 [start]、不含 [end]）。 */
@Immutable
data class TextSpan(val start: Int, val end: Int)

/**
 * 合集里的一期公告：列表页用，不含正文。
 *
 * @param cvId 专栏号（cv 后面的数字），也是列表的唯一键。
 * @param opusId 对应的图文动态 id：新版公告能在 opus 详情接口里查到完整正文；
 *   老专栏这里只能查到跳转信息，要退回 cv 接口。
 * @param title 公告标题，已去掉每期都带的「【版本更新】」前缀。
 * @param publishedAt 发布时间（epoch 秒），列表页自己格式化成日期。
 * @param coverUrl 横版封面图（接口给的 `image_urls` 第一张）。
 */
@Immutable
data class AnnouncementEntry(
    val cvId: String,
    val opusId: String,
    val title: String,
    val publishedAt: Long,
    val summary: String,
    val coverUrl: String,
)

/**
 * 「版本更新」合集：各期公告，保持接口原序（旧 → 新）。
 *
 * 接口一次就返回全部篇目（当前 139 篇），不分页，列表页按需自己倒序。
 */
@Immutable
data class AnnouncementArchive(
    val id: String,
    val entries: List<AnnouncementEntry>,
)
