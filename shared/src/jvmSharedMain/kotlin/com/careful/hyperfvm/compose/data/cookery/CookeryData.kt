package com.careful.hyperfvm.compose.data.cookery

/**
 * 食神谱合集的数据表：三个分组共 59 张卡，逐项对应 Java 版
 * `com.careful.HyperFVM.Activities.DataCenter.DetailCardData.Cookery` 下的
 * `fragment_bottom_sheet_cookery_*` 布局与 `CookeryBottomSheetFragment_*` 的绑定。
 *
 * 卡片内容由 `tool/cookery_gen.ps1` 从 Java 工程的布局与 strings.xml 抽取到
 * CookeryCardsGrade1/2/3.kt；静态数据只求值一次，详情弹层直接引用卡对象，
 * 不需要像 Java 版那样按卡名回查布局与 binder。
 */
internal object CookeryData {

    /** 目录页的三个分组，标题取自 Java 版 `category_card_data_cookery_1/2/3`。 */
    val sections: List<CookerySection> = listOf(
        CookerySection("上等佳肴", CookeryGrade1Cards),
        CookerySection("秘制佳肴", CookeryGrade2Cards),
        CookerySection("极品佳肴", CookeryGrade3Cards),
    )
}
