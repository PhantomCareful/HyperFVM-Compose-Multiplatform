package com.careful.hyperfvm.compose.data.catalog

/** 合集条目共用的性别：套装、翅膀都只有男、女两档。 */
enum class CatalogSex(val label: String) {
    Male("男"),
    Female("女"),
}

/**
 * 一档续费：[duration] 是档位名（7天 / 30天 / 永久），[price] 是 market_item.xml 里
 * 这一档的售价（货币单位已拼进字符串，如「1000点券」）。
 */
data class CatalogRenewal(
    val duration: String,
    val price: String,
)

/**
 * 合集里的一条内容：套装合集与翅膀合集共用同一套列表 / 详情布局。
 *
 * [id] 是道具 id 对应的图片文件名（如 x14d00110）：全局唯一，兼做列表 key——
 * Android 上 Lazy 列表的 key 必须能存进 Bundle，不能拿图片对象当 key。
 * 预览图不在包里，运行时按 [id] 从道具 CDN 下载到本地缓存，见
 * [com.careful.hyperfvm.compose.data.catalog.CatalogRepository]。
 *
 * [name] 取自 card_desc 的 name（同名变体已按末尾 1/2/3 区分）；[hp] 是它的体力加成；
 * [description] 是 card_desc 的 desc，个别条目在源数据里没有描述，为 null；
 * [renewals] 按 7天 / 30天 / 永久 排列，market_item.xml 查不到的条目为空列表。
 */
data class CatalogItem(
    val id: String,
    val name: String,
    val sex: CatalogSex,
    val hp: Int,
    val description: String?,
    val renewals: List<CatalogRenewal>,
)

/**
 * 合集种类：套装与翅膀共用同一套「下载 → 解析 → 缓存 → 展示」管线，
 * 区别只在道具 id 前缀、筛选规则与界面文案。
 *
 * [idPrefix] 是 card_desc 里的道具 id 前缀（不含 `0x`）；[noun] / [unit] 用于标题与
 * 计数文案；[maleLabel] / [femaleLabel] 用于更新完成弹窗的「男装新增X套」。
 */
enum class CatalogKind(
    val noun: String,
    val unit: String,
    val maleLabel: String,
    val femaleLabel: String,
    val idPrefix: String,
) {
    Clothing("套装", "套", "男装", "女装", "14d"),
    Wing("翅膀", "件", "男款", "女款", "14b"),
}
