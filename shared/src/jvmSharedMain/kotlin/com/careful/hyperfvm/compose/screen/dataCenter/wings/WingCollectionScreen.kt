package com.careful.hyperfvm.compose.screen.dataCenter.wings

import androidx.compose.runtime.Composable
import com.careful.hyperfvm.compose.component.catalog.CatalogCollectionScreen
import com.careful.hyperfvm.compose.data.catalog.CatalogKind

/**
 * 翅膀合集页：布局与服装合集完全一致（筛选、搜索、瀑布流、详情弹层共用
 * 通用组件），这里只指定合集种类；条目与预览图由
 * [com.careful.hyperfvm.compose.data.catalog.CatalogRepository] 在线加载。
 */
@Composable
fun WingCollectionScreen(onBack: () -> Unit) {
    CatalogCollectionScreen(kind = CatalogKind.Wing, onBack = onBack)
}
