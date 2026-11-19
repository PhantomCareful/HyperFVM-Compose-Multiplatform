package com.careful.hyperfvm.compose.data.oddsDisclosure

import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * 概率文本（百分数形式）最多精确到的小数位数。
 *
 * 官方公示已出现 0.0001%~0.0006% 级别的条目（如诸神宝殿的番外卡片包、神谕之石宝箱），
 * 只保留 3 位会被四舍五入成 0.000%，因此取 4 位。
 */
private const val MaxOddsPrecision = 4

/** 判断小数是否已精确到整数所使用的浮点误差容差。 */
private const val OddsPrecisionEpsilon = 1e-6

/**
 * 概率公示里的一条掉落物，对应 Java 版 `OddsItem`。
 *
 * [image] 是道具 id（如 `x13b89730`），界面通过
 * [com.careful.hyperfvm.compose.component.image.RemoteItemImage] 按 id 从道具图床取图，
 * 不再随数据包分发；公示新增道具时不用重新打包也能显示。
 * 允许为 null：幸运转转的公示页只有名称与概率，详情页的卡片会自动省掉左侧图标位。
 *
 * 服装 / 翅膀的预览图用 [remoteId] 指定道具 id，与 [image] 走同一套远程加载；
 * 两个字段互斥，[remoteId] 非空时优先。
 */
class OddsItem(
    val name: String,
    /** 一次开箱能获得的个数。 */
    val count: Int,
    /** 图标道具 id；为 null 表示这项没有图标。 */
    val image: String?,
    /** 是否为重点关注项（转职凭证 / 武器等；幸运转转取「送幸运星」列），详情页用 Important 卡突出显示。 */
    val expected: Boolean,
    /** 概率，取值 0~1。 */
    val odds: Double,
    /** 远程图标的道具 id（如 x14d05220），图片从道具 CDN 按需下载；为 null 表示用 [image] 或没有图标。 */
    val remoteId: String? = null,
) {
    /** 展示用名称：「名称×数量」。 */
    val nameAndCount: String get() = "$name×$count"

    /**
     * 该概率在百分数形式下无损显示所需的小数位数（1~[MaxOddsPrecision]）。
     * 原理：概率乘以 100 后逐次乘 10 放大，能变为整数的最小放大次数即所需位数。
     */
    fun requiredPrecision(): Int {
        var scaled = odds * 100
        for (precision in 1..MaxOddsPrecision) {
            scaled *= 10
            if (abs(scaled - round(scaled)) < OddsPrecisionEpsilon) return precision
        }
        return MaxOddsPrecision
    }

    /** 按统一小数位数输出百分数文本，如 precision = 3 时 0.030 → "3.000%"。 */
    fun oddsText(precision: Int): String =
        String.format(Locale.CHINA, "%.${precision}f%%", odds * 100)
}

/**
 * 同一次公示里统一取用的概率小数位数：取各项所需位数的最大值（下限 1、上限 [MaxOddsPrecision]），
 * 避免同一个宝箱里出现 "3%" 与 "0.003%" 混排。
 */
val List<OddsItem>.oddsTextPrecision: Int
    get() = min(MaxOddsPrecision, fold(1) { precision, item -> max(precision, item.requiredPrecision()) })

/**
 * 目录里的一个宝箱，对应 Java 版 `IndexItem`。
 * Java 版另有 topBarName 字段，数据里与 [name] 完全一致，这里合并成一个。
 */
class OddsBox(
    val name: String,
    /** 目录行左侧的 44dp 缩略图的道具 id，按 id 从图床加载。 */
    val image: String?,
    /** 宝箱说明，展示在详情页第一张 Important 卡里；null 表示不显示这张说明卡。 */
    val description: String? = null,
    val items: List<OddsItem>,
    /** 详情页勘误卡的文案：null 不显示卡片；空串用默认文案 [OddsErrataTips]；非空则自定义。 */
    val errata: String? = "",
)

/** 目录里的一个类别（分节标题 + 该类别下的全部宝箱），对应 Java 版 `CategoryItem`。 */
class OddsCategory(
    val title: String,
    val boxes: List<OddsBox>,
)

/**
 * 抽奖概率目录里的一场抽奖（「抽奖概率」页的一行），结构与 [OddsBox] 同构。
 */
class LotteryDraw(
    val name: String,
    /** 目录行左侧的 44dp 缩略图的道具 id，按 id 从图床加载；为 null 时目录行只有文字（如幸运转转）。 */
    val image: String?,
    /** 抽奖说明，展示在详情页第一张 TipsCard 里；null 表示不显示这张说明卡。 */
    val description: String? = null,
    /** 奖池条目，复用宝箱详情页的 [OddsItem]。 */
    val prizes: List<OddsItem>,
    /** 详情页勘误卡的文案：null 不显示卡片；空串用默认文案 [OddsErrataTips]；非空则自定义。 */
    val errata: String? = "",
)

/** 抽奖概率目录里的一个类别（分节标题 + 该类别下的全部抽奖）。 */
class LotteryCategory(
    val title: String,
    val draws: List<LotteryDraw>,
)

/**
 * 勘误卡最终要展示的文案：
 * null 表示这一项没有勘误卡，界面直接不渲染；空串回退到默认文案 [OddsErrataTips]；非空原样展示。
 */
val String?.resolvedErrata: String?
    get() = this?.ifEmpty { OddsErrataTips }

/**
 * 「官方公示合集」弹层里的一条来源：行标题 + 外部链接。
 *
 * 列表随数据包分发（`data/odds_sources.json`），官方新增公示帖时改数据即可，
 * 不用发版；数据包没有该文件时用 [OddsDataRepository] 里的内置兜底列表。
 */
class OddsSource(val title: String, val url: String)
