package com.careful.hyperfvm.compose.data.itemQuery

/**
 * 道具查询的一条结果，对应 card_desc.xml 的一条 `<item>`。
 *
 * 只保留卡片要展示的字段：整张表一万五千余条，把完整属性表都留着会多占十几 MB，
 * 这里按展示需要挑最少的几项。
 *
 * [id] 是图片 id（`x` + 十六进制，如 `x1112254e`），与图床地址、数据包文件名同一套写法；
 * [description] 已解 XML 实体并把源数据里的 `<br>` 与 `|` 换成换行，空描述为 null；
 * [bag] 是从描述里拆出的礼包结构，非礼包道具为 null。
 */
data class ItemEntry(
    val id: String,
    val name: String,
    val description: String?,
    /** 道具类型（攻击型 / 辅助型 / 功能道具…），个别条目带 `|等级` 后缀。 */
    val type: String,
    /** 能量消耗，源数据里可能是 `100`、`250+` 这类写法；为空表示不适用。 */
    val energy: String?,
    /** 体力加成；为空表示该道具没有这项属性。 */
    val hp: String?,
    /** 礼包的候选列表；普通道具为 null，界面照常展示 [description]。 */
    val bag: ItemBag? = null,
)

/**
 * 礼包（随机 / 概率 / 开出类）：描述首行提到「获得 / 开出」并以冒号结尾，
 * 如「打开可随机获得以下道具之一：」「随机开出以下物品之一:」，其下每行是一个候选道具，
 * 末尾通常还有一句绑定说明。
 *
 * [header] 保留首行原文（含结尾冒号）；[note] 是末尾那句说明（多行已拼成一句）。
 * 卡片上不再单独占一行，而是并到 [header] 后面的括号里。
 */
data class ItemBag(
    val header: String,
    val note: String?,
    val entries: List<BagEntry>,
)

/**
 * 礼包里的一个候选道具。
 *
 * [name] 是展示用的名字：星数前缀（如 `0星`）、漏写右括号的半截说明、末尾方括号备注
 * 都已去掉，名字自带的 `(30天)` 这类后缀保留；
 * [quantity] 是 `xN` 里的 N，货币行（如 `10点券`）与无数量的行都是 null；
 * [itemId] 能在道具表里按名字找到时有值，用来出图；[isWeapon] 决定取 0x24 高清图。
 */
data class BagEntry(
    val name: String,
    val quantity: String?,
    val itemId: String?,
    val isWeapon: Boolean,
)
