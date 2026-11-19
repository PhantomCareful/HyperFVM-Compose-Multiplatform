package com.careful.hyperfvm.compose.component.dialog

import org.jetbrains.compose.resources.DrawableResource
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.component.image.PackImage
import com.careful.hyperfvm.compose.platform.AppEnv
import com.careful.hyperfvm.compose.platform.LocalAppEnv
import com.careful.hyperfvm.compose.platform.LocalToaster
import com.careful.hyperfvm.compose.platform.Toaster
import org.jetbrains.compose.resources.painterResource
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 「即将前往」确认框的一次跳转目标。
 *
 * @param title 对话框正中的目标名（如「哔哩哔哩」）。
 * @param subtitle 目标名下方的说明（如账号名），为空则整行不显示。
 * @param url 确认后交给浏览器的真实地址。
 * @param iconRes 本地矢量图标（XML，随包分发）；为 null 则不显示。[imageId] 与它二选一。
 * @param imageId 数据包内的图片 id（WebP），彩色插图用它。
 * @param circleIcon 大图是否裁成正圆（[VisitUrlDialog] 用）；单色版 [UrlConfirmDialog] 不读取该字段。
 */
data class UrlTarget(
    val title: String,
    val subtitle: String?,
    val url: String,
    val iconRes: DrawableResource? = null,
    val imageId: String? = null,
    val circleIcon: Boolean = false,
)

/**
 * 外链跳转的确认框（单色版）：复刻 Java 版 `item_dialog_visit_url.xml`。
 *
 * 点击关于页 / 致谢页的外链行时先弹这里，用户确认后才调起浏览器；
 * 状态由调用方持有（[target] 为 null 即关闭），放在列表外防止列表项回收时弹窗一起销毁。
 * 大图会染成主题单色，适配 ic_github 这类单色 glyph；彩色插图请用 [VisitUrlDialog]。
 */
@Composable
fun UrlConfirmDialog(
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
                        Image(
                            modifier = Modifier.size(150.dp),
                            painter = painterResource(contentTarget.iconRes),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onBackground),
                        )
                    }
                } else if (contentTarget.imageId != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        PackImage(
                            id = contentTarget.imageId,
                            modifier = Modifier.size(150.dp),
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

/** 调起系统浏览器打开外链；没有可用浏览器时给出提示而不是崩溃。 */
internal fun openUrl(env: AppEnv, toaster: Toaster, url: String) {
    if (!env.openUrl(url)) toaster.show("无法打开浏览器")
}
