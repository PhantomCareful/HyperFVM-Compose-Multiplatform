package com.careful.hyperfvm.compose.component.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.shared.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 许可证文本所在的资源目录（Compose Resources 的 files 区，双端同一路径）。 */
private const val LegalResourceDir = "files/legal"

/** 视口高度占弹层可用高度的比例，以及上下限：长文本不能把弹层撑出屏幕。 */
private const val LegalViewportRatio = 0.62f
private val LegalViewportMin = 180.dp
private val LegalViewportMax = 480.dp

/**
 * App 内置的许可证副本文件名。
 *
 * 这两份是仓库根 LICENSE / NOTICE 的逐字节副本：根目录那两份供 GitHub 识别与仓库阅读，
 * 资源目录这两份打进安装包。Apache-2.0 第 4 条要求分发时附带许可证副本，
 * 所以「关于」页要有入口能让使用者随时读到。
 */
private const val LicenseFileName = "LICENSE.txt"
private const val NoticeFileName = "NOTICE.txt"

/**
 * 「开源许可」弹层：展示 Apache License 2.0 全文与 NOTICE 声明。
 *
 * 文本在首次打开时从 Compose Resources 读一次并常驻（体积十几 KB，不值得每次重读）；
 * 内容区本身不滚动，这里给一个按可用高度收窄的视口，全文才能滚到底。
 */
@Composable
fun OpenSourceLicensesDialog(
    show: Boolean,
    onDismiss: () -> Unit,
) {
    var licenseText by remember { mutableStateOf<String?>(null) }
    var noticeText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(show) {
        if (!show || licenseText != null) return@LaunchedEffect
        val (license, notice) = withContext(Dispatchers.IO) {
            readLegalText(LicenseFileName) to readLegalText(NoticeFileName)
        }
        licenseText = license
        noticeText = notice
    }

    OverlayDialog(
        show = show,
        title = "开源许可",
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "HyperFVM 以 Apache License 2.0 授权，以下文本随应用一同分发，" +
                    "内容与项目仓库中的 LICENSE 和 NOTICE 文件一致。",
                modifier = Modifier.padding(bottom = 12.dp),
                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                fontSize = MiuixTheme.textStyles.body2.fontSize,
            )

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val viewportHeight = if (constraints.hasBoundedHeight) {
                    (maxHeight * LegalViewportRatio).coerceIn(LegalViewportMin, LegalViewportMax)
                } else {
                    LegalViewportMax
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(viewportHeight)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainerHigh),
                ) {
                    val license = licenseText
                    if (license == null) {
                        Text(
                            text = "正在读取许可证文本…",
                            modifier = Modifier.padding(12.dp),
                            color = MiuixTheme.colorScheme.onSurfaceSecondary,
                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                        ) {
                            item(key = "licenseTitle") {
                                LegalSectionTitle("Apache License, Version 2.0")
                            }
                            item(key = "licenseBody") { LegalBody(license) }
                            item(key = "noticeTitle") {
                                LegalSectionTitle(
                                    text = "NOTICE",
                                    modifier = Modifier.padding(top = 20.dp),
                                )
                            }
                            item(key = "noticeBody") { LegalBody(noticeText.orEmpty()) }
                        }
                    }
                }
            }

            TextButton(
                text = "关闭",
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}

/** 读取一份许可证文本；资源缺失时返回可读的提示，不让弹层在展示环节崩掉。 */
private suspend fun readLegalText(name: String): String =
    runCatching { Res.readBytes("$LegalResourceDir/$name").decodeToString() }
        .getOrElse { "读取 $name 失败：${it.message ?: "未知错误"}" }

@Composable
private fun LegalSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(bottom = 8.dp),
        color = MiuixTheme.colorScheme.onSurfaceContainer,
        fontSize = MiuixTheme.textStyles.title3.fontSize,
        fontWeight = FontWeight.Bold,
    )
}

/** 协议正文：沿用 body2 的字号，行高放大到 1.45 倍，长段落才不至于挤在一起。 */
@Composable
private fun LegalBody(text: String) {
    val style = MiuixTheme.textStyles.body2
    Text(
        text = text,
        color = MiuixTheme.colorScheme.onSurfaceContainer,
        fontSize = style.fontSize,
        lineHeight = style.fontSize * 1.45f,
    )
}
