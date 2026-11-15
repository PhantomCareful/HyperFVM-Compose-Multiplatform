package com.careful.hyperfvm.compose.data.oddsDisclosure

import org.jetbrains.compose.resources.DrawableResource
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/** 概率文本（百分数形式）最多精确到的小数位数，与 Java 版 `OddsItem` 一致。 */
private const val MaxOddsPrecision = 3

/** 判断小数是否已精确到整数所使用的浮点误差容差。 */
private const val OddsPrecisionEpsilon = 1e-6

/**
 * 概率公示里的一条掉落物，对应 Java 版 `OddsItem`。
 *
 * [image] 是 drawable 资源对象只能在运行时读取，不再依赖 R 类的 int id。
 * 一旦用 const 固化进字节码，运行时 painterResource 会抛 Resources$NotFoundException。
 */
class OddsItem(
    val name: String,
    /** 一次开箱能获得的个数。 */
    val count: Int,
    val image: DrawableResource,
    /** 是否为重点关注项（转职凭证 / 武器等），详情页用 Important 卡突出显示。 */
    val expected: Boolean,
    /** 概率，取值 0~1。 */
    val odds: Double,
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
 * 同一次公示里统一取用的概率小数位数：取各项所需位数的最大值（下限 1、上限 3），
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
    /** 目录行左侧的 44dp 缩略图。 */
    val image: DrawableResource,
    /** 宝箱说明，展示在详情页第一张 Important 卡里。 */
    val description: String,
    val items: List<OddsItem>,
)

/** 目录里的一个类别（分节标题 + 该类别下的全部宝箱），对应 Java 版 `CategoryItem`。 */
class OddsCategory(
    val title: String,
    val boxes: List<OddsBox>,
)
