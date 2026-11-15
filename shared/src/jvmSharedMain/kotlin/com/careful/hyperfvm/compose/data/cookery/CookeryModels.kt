package com.careful.hyperfvm.compose.data.cookery

import org.jetbrains.compose.resources.DrawableResource
/**
 * 食神谱的一张卡片，对应 Java 版一个 `fragment_bottom_sheet_cookery_X_Y` 布局与
 * `CookeryBottomSheetFragment_X_Y` 的绑定结果。
 *
 * [image] 是 drawable 资源对象只能在运行时读取，不再依赖 R 类的 int id。
 * 一旦用 const 固化进字节码，运行时 painterResource 会抛 Resources$NotFoundException。
 */
internal class CookeryCard(
    val name: String,
    /** 分组名（上等佳肴 / 秘制佳肴 / 极品佳肴），即 Java 版 `category_card_data_cookery_*`。 */
    val category: String,
    /** 奖励文案，如「奖励40食神点」，即 Java 版 `reward_card_data_cookery_*`。 */
    val reward: String,
    val image: DrawableResource,
    /** 一句话描述，显示在目录行与详情页的描述卡里。 */
    val description: String,
    /** 获取途径（不含各种返利活动）。 */
    val sources: List<CookerySource>,
    /** 合成材料，每项是「一张防御卡 + 星级图」。 */
    val recipes: List<CookeryRecipe>,
    /** 开启后的辅助卡组：可能多组，每组带标题、若干提示卡与一个卡表。 */
    val auxiliaryGroups: List<CookeryAuxiliaryGroup>,
) {
    /** 基础信息卡第二行的「分类·奖励」，Java 版 binder 里拼出的同款文本。 */
    val subtitle: String get() = "$category·$reward"
}

/** 获取途径里的一条来源，例如「地图掉落」「铜食神宝箱」。 */
internal class CookerySource(
    val title: String,
    val description: String,
    /** 右侧一排 44dp 小图标（地图饼干、宝箱、钥匙等），数量不定。 */
    val icons: List<DrawableResource>,
)

/** 合成材料里的一格：防御卡图标 + 星级图标。 */
internal class CookeryRecipe(
    val cardRes: DrawableResource,
    /** `star_image_N` 星级图。 */
    val starRes: DrawableResource,
)

/** 一组辅助卡：标题 + 若干提示卡 + 一个卡表。 */
internal class CookeryAuxiliaryGroup(
    val title: String,
    val tips: List<CookeryTip>,
    val cards: List<CookeryAuxiliaryCard>,
)

/**
 * 辅助组里的提示卡。
 *
 * [isWarning] 为 true 时用勘误的红色样式（Java 版 `colorErrorContainer`），
 * 否则用 Important 样式（`importantCardColors`）。
 */
internal class CookeryTip(
    val text: String,
    val isWarning: Boolean,
)

/** 辅助卡表里的一行：卡名 + 卡图。 */
internal class CookeryAuxiliaryCard(
    val name: String,
    val image: DrawableResource,
)

/** 目录页的一个分组（上等佳肴 / 秘制佳肴 / 极品佳肴），对应 Java 版索引页的三个分节。 */
internal class CookerySection(
    val title: String,
    val cards: List<CookeryCard>,
)
