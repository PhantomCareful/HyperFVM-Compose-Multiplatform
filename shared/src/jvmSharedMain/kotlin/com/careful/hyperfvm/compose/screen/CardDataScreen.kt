package com.careful.hyperfvm.compose.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.careful.hyperfvm.compose.screen.cardCatalog.CardCatalogScreen

/**
 * 「防御卡」Tab：防御卡全能数据库的目录页。
 *
 * 目录页的实现（静态卡表、拼音分节、字母索引条、搜索）全部在
 * [CardCatalogScreen] 里，这里只是主界面 Pager 的接线点。
 */
@Composable
fun DefenseCardScreen(outerPadding: PaddingValues) {
    CardCatalogScreen(outerPadding = outerPadding)
}
