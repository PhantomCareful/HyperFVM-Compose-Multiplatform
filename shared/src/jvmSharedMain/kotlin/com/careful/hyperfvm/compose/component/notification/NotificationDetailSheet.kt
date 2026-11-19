package com.careful.hyperfvm.compose.component.notification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.component.dialog.openUrl
import com.careful.hyperfvm.compose.data.notification.StoredNotification
import com.careful.hyperfvm.compose.platform.LocalAppEnv
import com.careful.hyperfvm.compose.platform.LocalToaster
import com.careful.hyperfvm.compose.ui.AppCard
import com.careful.hyperfvm.compose.ui.AppDialogButton
import com.careful.hyperfvm.compose.ui.AppDialogButtonRole
import com.careful.hyperfvm.compose.ui.sheetCardColor
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import com.careful.hyperfvm.compose.ui.AppBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/** 弹层内容卡的圆角，与项目里其它弹层一致。 */
private val SheetCardCornerRadius = 20.dp

/** 正文卡与下方按钮之间、相邻两个链接按钮之间的间距。 */
private val SheetButtonGap = 12.dp
private val LinkButtonGap = 8.dp

/**
 * 通知详情弹层：标题是通知标题，正文放在一张卡里，链接按钮排在卡下方。
 * 一条通知可以带多个链接，每个链接一个按钮，文案取它的 button + title（见
 * [com.careful.hyperfvm.compose.data.notification.NotificationLink.label]）。
 *
 * 正文卡按需求「无按压动画、有触摸反馈」：`pressFeedbackType = None` 关掉缩放，
 * `showIndication = true` 保留点击时的高光。miuix 的 Card 只有同时拿到 onClick 才会
 * 画这层高光，所以传一个空回调——卡片本身不承载任何动作，点它什么都不会发生。
 *
 * [show] 与 [notification] 分开传：收起动画期间内容不能清空，
 * 否则弹层会在半空变成空白再滑走。
 */
@Composable
fun NotificationDetailSheet(
    notification: StoredNotification?,
    show: Boolean,
    onDismissRequest: () -> Unit,
) {
    // 退出动画期间调用方可能已经清空内容，记住最后一次的通知继续渲染。
    var lastNotification by remember { mutableStateOf<StoredNotification?>(null) }
    if (notification != null) lastNotification = notification
    val content = notification ?: lastNotification ?: return

    val env = LocalAppEnv.current
    val toaster = LocalToaster.current
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    AppBottomSheet(
        show = show,
        title = content.title,
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp + navigationBarBottom),
        ) {
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = SheetCardCornerRadius,
                // 弹层里换成弹层内容卡底色，卡片边界才分辨得出来。
                colors = CardDefaults.defaultColors(color = sheetCardColor()),
                pressFeedbackType = PressFeedbackType.None,
                showIndication = true,
                onClick = {},
            ) {
                Text(
                    text = content.content,
                    modifier = Modifier.padding(16.dp),
                    // 字号、字重、颜色都与列表标题一致，弹层里只有这一块正文，不另起一套样式。
                    color = MiuixTheme.colorScheme.onBackground,
                    fontSize = MiuixTheme.textStyles.headline1.fontSize,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (content.links.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = SheetButtonGap),
                    verticalArrangement = Arrangement.spacedBy(LinkButtonGap),
                ) {
                    content.links.forEach { link ->
                        AppDialogButton(
                            text = link.label,
                            onClick = { openUrl(env, toaster, link.url) },
                            modifier = Modifier.fillMaxWidth(),
                            role = AppDialogButtonRole.Primary,
                        )
                    }
                }
            }
        }
    }
}
