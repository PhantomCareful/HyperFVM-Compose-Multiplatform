package com.careful.hyperfvm.compose.data.resourceCalculator

import com.careful.hyperfvm.compose.shared.*


/**
 * 一个道具输入行的展示信息：label 直接作为输入框标题（TextField 的 With title 样式），
 * [coinValue] 为该道具折算的储备币数，对应 Java 版 `denominations` 里的同下标项。
 * Java 版每行左右各有一个 60dp 图标，两图可以不同（月卡 / 豪华礼包行两侧就是不同图案）。
 * 图标字段有两种取值：游戏道具 id（`x` 开头）由 RemoteItemImage 从图床加载；
 * 自制的数据包图片 id（如 `data_center_image_prestige_calculator`）不是道具 id，
 * RemoteItemImage 会自动回退读数据包。为 null 时界面画 Miuix 的图片占位图标。
 */
data class ResourceField(
    val label: String,
    val coinValue: Long,
    /** 行左侧 60dp 道具图标 id；null 表示用占位图标。 */
    val leadingIconRes: String?,
    /** 行右侧 60dp 道具图标 id；null 表示用占位图标。 */
    val trailingIconRes: String?,
)

/** 一个子页：TabRow 上的标题与该页的输入行表。 */
data class ResourceSection(
    val title: String,
    val fields: List<ResourceField>,
)

/**
 * 资源储备计算器的全部静态数据，按 TabRow 顺序分成威望 / 金币 / 礼券 / 魅力币四页。
 *
 * 每页的输入行表格式完全相同（见 [ResourceField]），只有标题与各行币值不同：
 * 金币、礼券、魅力币三页的 [ResourceSection.fields] 暂留空，补齐数据即可，
 * 界面与计算逻辑不用改（空表时该页只剩说明卡、顶栏副标题算出 0）。
 */
internal object ResourceCalculatorData {

    /**
     * 威望页的输入行表，顺序与 Java 版 `activity_prestige_calculator.xml` 一致。
     * 威望币没有独立的道具图标，行两侧统一用自制的储备图标（随数据包分发，不走图床）。
     */
    private val PrestigeFields = listOf(
        ResourceField(
            "余额",
            1,
            "data_center_image_prestige_calculator",
            "data_center_image_prestige_calculator",
        ),
        ResourceField(
            "1威望币的数量",
            1,
            "data_center_image_prestige_calculator",
            "data_center_image_prestige_calculator",
        ),
        ResourceField(
            "5威望币的数量",
            5,
            "data_center_image_prestige_calculator",
            "data_center_image_prestige_calculator",
        ),
        ResourceField(
            "10威望币的数量",
            10,
            "data_center_image_prestige_calculator",
            "data_center_image_prestige_calculator",
        ),
        ResourceField(
            "50威望币的数量",
            50,
            "data_center_image_prestige_calculator",
            "data_center_image_prestige_calculator",
        ),
        ResourceField(
            "100威望币的数量",
            100,
            "data_center_image_prestige_calculator",
            "data_center_image_prestige_calculator",
        ),
        ResourceField(
            "200威望币的数量",
            200,
            "data_center_image_prestige_calculator",
            "data_center_image_prestige_calculator",
        ),
        ResourceField(
            "500威望币的数量",
            500,
            "data_center_image_prestige_calculator",
            "data_center_image_prestige_calculator",
        ),
        ResourceField(
            "1000威望币的数量",
            1000,
            "data_center_image_prestige_calculator",
            "data_center_image_prestige_calculator",
        ),
        ResourceField(
            "2000威望币的数量",
            2000,
            "data_center_image_prestige_calculator",
            "data_center_image_prestige_calculator",
        ),
        ResourceField(
            "3000威望币的数量",
            3000,
            "data_center_image_prestige_calculator",
            "data_center_image_prestige_calculator",
        ),
        ResourceField(
            "4000威望币的数量",
            4000,
            "data_center_image_prestige_calculator",
            "data_center_image_prestige_calculator",
        ),
        ResourceField(
            "5000威望币的数量",
            5000,
            "data_center_image_prestige_calculator",
            "data_center_image_prestige_calculator",
        ),
        ResourceField(
            "8000威望币的数量",
            8000,
            "data_center_image_prestige_calculator",
            "data_center_image_prestige_calculator",
        ),
        ResourceField(
            "10000威望币的数量",
            10000,
            "data_center_image_prestige_calculator",
            "data_center_image_prestige_calculator",
        ),
        ResourceField(
            "月卡礼包的数量",
            600,
            "x131201d0",
            "x13af8330"
        ),
        ResourceField(
            "豪华福利礼包的数量",
            500,
            "x12609030",
            "x13af4130"
        ),
        ResourceField(
            "营养套餐A的数量",
            100,
            "x13a22730",
            "x13a22730"
        ),
        ResourceField(
            "营养套餐B的数量",
            200,
            "x13a22730",
            "x13a22730"
        ),
        ResourceField(
            "营养套餐C的数量",
            200,
            "x13a22730",
            "x13a22730"
        ),
        ResourceField(
            "营养套餐D的数量",
            400,
            "x13a22730",
            "x13a22730"
        ),
        ResourceField(
            "营养套餐E的数量",
            400,
            "x13a22730",
            "x13a22730"
        ),
        ResourceField(
            "营养套餐F的数量",
            500,
            "x13a22730",
            "x13a22730"
        ),
        ResourceField(
            "营养套餐G的数量",
            500,
            "x13a22730",
            "x13a22730"
        ),
    )

    private val GoldCoinFields = listOf(
        ResourceField(
            "余额",
            1,
            "x12608a30",
            "x12608a30"
        ),
        ResourceField(
            "100金币的数量",
            100,
            "x12608a30",
            "x12608a30"
        ),
        ResourceField(
            "200金币的数量",
            200,
            "x12608a30",
            "x12608a30"
        ),
        ResourceField(
            "500金币的数量",
            500,
            "x12608a30",
            "x12608a30"
        ),
        ResourceField(
            "1000金币的数量",
            1000,
            "x12608a30",
            "x12608a30"
        ),
        ResourceField(
            "2000金币的数量",
            2000,
            "x12608a30",
            "x12608a30"
        ),
        ResourceField(
            "5000金币的数量",
            5000,
            "x12608a30",
            "x12608a30"
        ),
        ResourceField(
            "月卡礼包的数量",
            30000,
            "x131201d0",
            "x13af8330"
        ),
        ResourceField(
            "小金币袋的数量",
            10000,
            "x13310010",
            "x13310010"
        ),
        ResourceField(
            "大金币袋的数量",
            100000,
            "x13310020",
            "x13310020"
        ),
    )

    private val VoucherFields = listOf(
        ResourceField(
            "余额",
            1,
            "x12608c30",
            "x12608c30"
        ),
        ResourceField(
            "1礼券的数量",
            1,
            "x12608c30",
            "x12608c30"
        ),
        ResourceField(
            "10礼券的数量",
            10,
            "x12608c30",
            "x12608c30"
        ),
        ResourceField(
            "20礼券的数量",
            20,
            "x12608c30",
            "x12608c30"
        ),
        ResourceField(
            "30礼券的数量",
            30,
            "x12608c30",
            "x12608c30"
        ),
        ResourceField(
            "50礼券的数量",
            50,
            "x12608c30",
            "x12608c30"
        ),
        ResourceField(
            "80礼券的数量",
            80,
            "x12608c30",
            "x12608c30"
        ),
        ResourceField(
            "100礼券的数量",
            100,
            "x12608c30",
            "x12608c30"
        ),
        ResourceField(
            "120礼券的数量",
            120,
            "x12608c30",
            "x12608c30"
        ),
        ResourceField(
            "160礼券的数量",
            160,
            "x12608c30",
            "x12608c30"
        ),
        ResourceField(
            "200礼券的数量",
            200,
            "x12608c30",
            "x12608c30"
        ),
        ResourceField(
            "250礼券的数量",
            250,
            "x12608c30",
            "x12608c30"
        ),
        ResourceField(
            "300礼券的数量",
            300,
            "x12608c30",
            "x12608c30"
        ),
        ResourceField(
            "350礼券的数量",
            350,
            "x12608c30",
            "x12608c30"
        ),
        ResourceField(
            "400礼券的数量",
            400,
            "x12608c30",
            "x12608c30"
        ),
        ResourceField(
            "450礼券的数量",
            450,
            "x12608c30",
            "x12608c30"
        ),
        ResourceField(
            "500礼券的数量",
            500,
            "x12608c30",
            "x12608c30"
        ),
        ResourceField(
            "遗迹古券(10礼券)的数量",
            10,
            "x13332900",
            "x13332900"
        ),
        ResourceField(
            "遗迹古券(20礼券)的数量",
            20,
            "x13332900",
            "x13332900"
        ),
        ResourceField(
            "遗迹古券(30礼券)的数量",
            30,
            "x13332900",
            "x13332900"
        ),
        ResourceField(
            "月卡礼包的数量",
            50,
            "x131201d0",
            "x13af8330"
        ),
        ResourceField(
            "每日工资VIP13的数量",
            550,
            "x13a22730",
            "x13a22730"
        ),
        ResourceField(
            "每日工资VIP14的数量",
            600,
            "x13a22730",
            "x13a22730"
        ),
        ResourceField(
            "每日工资VIP15的数量",
            1000,
            "x13af5500",
            "x13af5500"
        ),
        ResourceField(
            "每日工资VIP16的数量",
            800,
            "x13af5500",
            "x13af5500"
        ),
        ResourceField(
            "100礼券许愿彩礼的数量",
            100,
            "x12608c30",
            "x12608c30"
        ),
        ResourceField(
            "1000礼券许愿彩礼的数量",
            1000,
            "x12608c30",
            "x12608c30"
        ),
        ResourceField(
            "5000礼券许愿彩礼的数量",
            5000,
            "x12608c30",
            "x12608c30"
        ),
    )

    private val CharmFields = listOf(
        ResourceField(
            "余额",
            1,
            "x13340020",
            "x13340020"
        ),
        ResourceField(
            "10魅力币的数量",
            10,
            "x13340020",
            "x13340020"
        ),
        ResourceField(
            "52魅力币的数量",
            52,
            "x13340020",
            "x13340020"
        ),
        ResourceField(
            "100魅力币的数量",
            100,
            "x13340020",
            "x13340020"
        ),
        ResourceField(
            "豪华福利礼包的数量",
            10,
            "x12609030",
            "x13af4130"
        ),
    )

    /** TabRow 的四个子页，顺序即 Tab 顺序。 */
    val sections: List<ResourceSection> = listOf(
        ResourceSection("威望", PrestigeFields),
        ResourceSection("金币", GoldCoinFields),
        ResourceSection("礼券", VoucherFields),
        ResourceSection("魅力币", CharmFields),
    )
}
