package com.careful.hyperfvm.compose.screen.about

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.ui.AppCard
import top.yukonga.miuix.kmp.basic.CardDefaults

/**
 * 二级页的内容卡：一行行成员 / 项目列表都装在里面，对应 Java 版的 General 卡。
 * 卡间距由本组件的 bottom 12dp 提供。
 *
 * [modifier] 加在卡片自身（内边距之内），数据中心用它挂长按拖动的指针手柄。
 */
@Composable
fun AboutContentCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp)
            .then(modifier),
        cornerRadius = 20.dp,
        colors = CardDefaults.defaultColors(),
    ) {
        content()
    }
}
