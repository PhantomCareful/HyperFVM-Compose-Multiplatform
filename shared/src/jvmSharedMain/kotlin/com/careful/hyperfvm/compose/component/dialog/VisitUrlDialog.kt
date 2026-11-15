package com.careful.hyperfvm.compose.component.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.platform.LocalAppEnv
import com.careful.hyperfvm.compose.platform.LocalToaster
import org.jetbrains.compose.resources.painterResource
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 外链跳转的确认框（彩色版）：复刻 Java 版 `DialogBuilderManager.showDialogAndVisitUrl`。
 *
 * 与单色版 [UrlConfirmDialog] 的差异：大图原彩显示、不加 tint，
 * 并按 [UrlTarget.circleIcon] 决定是否裁成正圆（对应 Java 版的 imageRadius 参数）。
 * 用于数据中心等展示彩色插图的外链行；
 * 状态由调用方持有（[target] 为 null 即关闭），放在列表外防止列表项回收时弹窗一起销毁。
 */
@Composable
fun VisitUrlDialog(
    target: UrlTarget?,
    onDismiss: () -> Unit,
) {
    val env = LocalAppEnv.current
    val toaster = LocalToaster.current
    // 退出动画期间调用方已把 target 置空，但弹窗容器还会滑出约 260ms：
    // 记住最后一次的目标继续渲染，图标与文字才会随容器一起退出，而不是动画开头就消失。
    var lastTarget by remember { mutableStateOf<UrlTarget?>(null) }
    if (target != null) lastTarget = target
    val contentTarget = target ?: lastTarget

    OverlayDialog(
        show = target != null,
        title = "即将前往",
        onDismissRequest = onDismiss,
    ) {
        if (contentTarget != null) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (contentTarget.iconRes != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val iconModifier = if (contentTarget.circleIcon) {
                            Modifier.size(150.dp).clip(CircleShape)
                        } else {
                            Modifier.size(150.dp)
                        }
                        Image(
                            modifier = iconModifier,
                            painter = painterResource(contentTarget.iconRes),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
                Text(
                    text = contentTarget.title,
                    modifier = Modifier.fillMaxWidth(),
                    color = MiuixTheme.colorScheme.onBackground,
                    fontSize = MiuixTheme.textStyles.title2.fontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                val subtitle = contentTarget.subtitle
                if (!subtitle.isNullOrEmpty()) {
                    Text(
                        text = subtitle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        fontSize = MiuixTheme.textStyles.body1.fontSize,
                        textAlign = TextAlign.Center,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(
                        text = "咱手滑了",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(20.dp))
                    TextButton(
                        text = "立即前往",
                        onClick = {
                            openUrl(env, toaster, contentTarget.url)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }
        }
    }
}
