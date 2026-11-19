package com.careful.hyperfvm.compose.component.meishi

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.data.meishi.MeishiAccount
import com.careful.hyperfvm.compose.component.image.RemoteItemImage
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/** 温馨礼包相关卡片的圆角，与项目里其它卡片一致。 */
internal val MeishiCardCornerRadius = 20.dp

/** 卡片之间的竖向间距，与仪表盘弹层里的卡间距一致。 */
internal val MeishiCardGap = 10.dp

/** 页面上卡片的左右边距，与设置页、仪表盘一致。 */
internal val MeishiSidePadding = 12.dp

/** 礼包图标的边长：固定 44dp，不裁切，图按原比例缩放后完整地落在方框里。 */
private val GiftIconSize = 44.dp

/** 账号卡里角色名缺失时的兜底，同 Java 版。 */
internal const val UnknownPlayerName = "未知角色"

/** 账号卡里区服缺失时的兜底，同 Java 版。 */
internal const val UnknownServerName = "未知区服"

/**
 * 弹层里一个字段的展示卡：body2 标签 + title3 加粗数值。
 *
 * 与仪表盘详情弹层里的「开始时间 / 结束时间」卡同规格（同圆角、同底色、同内边距），
 * 添加链接时解析出的角色 ID 与所在区服就用它展示。
 */
@Composable
internal fun MeishiFieldCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = MeishiCardCornerRadius,
        insideMargin = BasicComponentDefaults.InsideMargin,
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        // 展示卡只有按压反馈，没有点击动作；空回调只为了把水波纹挂上。
        onClick = {},
    ) {
        Column {
            Text(
                text = label,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurfaceContainer,
            )
        }
    }
}

/**
 * 只读文本卡：账号管理里的链接与所在区服用它展示。
 *
 * 链接很长，所以允许换行，也不给固定行数——按内容自然撑高。
 * [onCopy] 不为空时在卡片最右边加一个复制按钮：链接没法手打，
 * 想把它挪到别处（比如发给别人）只能靠复制。
 */
@Composable
internal fun MeishiTextCard(
    text: String,
    modifier: Modifier = Modifier,
    onCopy: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = MeishiCardCornerRadius,
        insideMargin = BasicComponentDefaults.InsideMargin,
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = {},
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceContainer,
            )
            if (onCopy != null) {
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Icon(
                        imageVector = MiuixIcons.Copy,
                        contentDescription = "复制链接",
                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    )
                }
            }
        }
    }
}

/**
 * 账号列表里的一张卡：title 是角色 ID，summary 是所在区服。
 *
 * 字号与颜色对齐 Miuix 的 BasicComponent：标题 17sp Medium、摘要 14sp 常规。
 * 点击进专属管理弹层，长按拖动排序由调用方的 `longPressDraggableHandle` 接管
 * （与仪表盘卡片一样，整张卡都是拖动手柄）。
 */
@Composable
internal fun MeishiAccountCard(
    account: MeishiAccount,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = MeishiCardCornerRadius,
        insideMargin = BasicComponentDefaults.InsideMargin,
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = onClick,
    ) {
        Column {
            Text(
                text = account.playerName.ifBlank { UnknownPlayerName },
                fontSize = MiuixTheme.textStyles.headline1.fontSize,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurfaceContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = account.serverName.ifBlank { UnknownServerName },
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 礼包清单里的一项：左侧标题 + 说明，右侧道具图标，对应 Java 版的整行卡。
 *
 * 字号同 [MeishiAccountCard]，对齐 BasicComponent；图标固定 44dp，不裁切。
 */
@Composable
internal fun MeishiGiftRow(
    title: String,
    description: String,
    icon: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = MeishiCardCornerRadius,
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = {},
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = MiuixTheme.textStyles.headline1.fontSize,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            RemoteItemImage(
                id = icon,
                contentDescription = null,
                modifier = Modifier.size(GiftIconSize),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
