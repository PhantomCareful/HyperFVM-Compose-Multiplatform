package com.careful.hyperfvm.compose.component.dashboard

import com.careful.hyperfvm.compose.shared.*

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.component.card.statusCardAccentColor
import com.careful.hyperfvm.compose.component.card.statusCardBackgroundColor
import com.careful.hyperfvm.compose.data.dashboard.DashboardCardId
import com.careful.hyperfvm.compose.data.dashboard.DashboardEntry
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/** 与项目里其它卡片保持一致的圆角。 */
private val CardCornerRadius = 20.dp

private val CardHorizontalPadding = 16.dp
private val CardVerticalPadding = 14.dp

/** 标题与状态之间的最小距离，状态文案长时靠它兜底。 */
private val TitleStatusGap = 12.dp

/** 温馨礼包卡的高度，够放下 22sp 标题、16sp 状态和右下角的大图标。 */
private val GiftCardHeight = 96.dp

/**
 * 右下角大图标的尺寸与溢出距离。半宽卡里图标再大就会盖住文字，
 * 所以这一套是「与更新公告并排」时的尺寸。
 */
private val GiftIconSize = 90.dp
private val GiftIconOffsetX = 20.dp
private val GiftIconOffsetY = 23.dp
private val PairCardHorizontalPadding = 14.dp
private val PairCardVerticalPadding = 12.dp

/** 温馨礼包与更新公告之间的缝，和竖向的 [CardCornerRadius] 同量级。 */
private val PairCardGap = 8.dp

/** 摘要文字的透明度：对“当前版本”那一档，取内容色的 70%。 */
private const val SummaryAlpha = 0.7f

/**
 * 仪表盘上「温馨礼包 + 更新公告」并排的一行：两张半宽卡，固定成对出现。
 *
 * 它们作为整体参与拖动排序与卡片管理，所以这里只负责把两张卡排好：
 * 左边是 KernelSU 同款的状态卡，右边是进公告页的入口。
 */
@Composable
internal fun DashboardGiftPairRow(
    giftStatus: String,
    giftFailed: Boolean,
    onGiftClick: () -> Unit,
    noticeStatus: String,
    onNoticeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        DashboardGiftCard(
            title = DashboardCardId.SweetGift.title,
            status = giftStatus,
            failed = giftFailed,
            onClick = onGiftClick,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(PairCardGap))
        DashboardNoticeCard(
            status = noticeStatus,
            onClick = onNoticeClick,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 仪表盘的一行状态卡：左侧标题（可带一行副标题），右侧状态。
 *
 * 状态是「12/21」这种进度时，卡片背景按比例铺一层底色；其余状态是纯色卡片。
 * 所有颜色都来自 [dashboardCardPalette]：这里只管排版，不判断场景。
 * 卡片带按压反馈与水波纹，点击行为由 [onClick] 决定（弹详情，或抢红包的提示）；
 * 长按拖动排序仍由外层手柄接管。
 *
 * @param title 卡面标题，由调用方传入（双爆与二转打折会覆盖默认标题）。
 * @param progressReveal 进度条的展开系数（0→1 的曲线）：每次刷新后从 0 长到 1，
 * 背景条就会从左侧喷到目标宽度。它由屏幕层持有，卡片被列表回收再滚回来时不会重播。
 */
@Composable
internal fun DashboardStatusCard(
    title: String,
    entry: DashboardEntry,
    progressReveal: Animatable<Float, AnimationVector1D>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val progress = entry.progress
    val palette = dashboardCardPalette(style = entry.style, hasProgress = progress != null)
    Card(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = CardCornerRadius,
        colors = palette.card,
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = onClick,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (progress != null) {
                // 卡片本身会把子内容裁成超椭圆，这里的矩形不用自己处理圆角。
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBehind {
                            val width = size.width * progress * progressReveal.value.coerceIn(0f, 1f)
                            if (width > 0f) {
                                drawRect(color = palette.progress, size = Size(width, size.height))
                            }
                        },
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CardHorizontalPadding, vertical = CardVerticalPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MiuixTheme.textStyles.headline1,
                        fontWeight = FontWeight.Medium,
                        color = palette.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (entry.detail.isNotEmpty()) {
                        Text(
                            text = entry.detail,
                            style = MiuixTheme.textStyles.body2,
                            color = palette.detail,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(TitleStatusGap))
                // 不给状态层权重：它按自身宽度测量后贴在行尾，标题列吃剩下的全部宽度，
                // 状态文案因此真正右对齐，而不是浮在中线右侧。
                Text(
                    text = entry.status,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Medium,
                    color = palette.status,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

/**
 * 温馨礼包卡：照搬 KernelSU 的 miuix 运行状态卡。
 *
 * 左上标题、左下状态、右下角一张压出边界的大图标（Material 的 Rounded CheckCircleOutline，
 * 与 KernelSU 同款）。这里用固定高度的 Box 代替 KernelSU 的 `fillMaxSize()`：miuix 的
 * Card 高度由内容决定，`fillMaxSize()` 在无界约束下会退化成 0，显式给高度更稳。
 *
 * 文字配色对齐防御卡目录的版本卡：标题取卡片内容色（同版本号），
 * 左下那句状态取它的 70% 透明度（同「当前版本」）。
 *
 * [PressFeedbackType.Tilt] 也是 KernelSU 原样的按压倾斜反馈：`pressable` 自己解析按压手势，
 * 不依赖 `onClick`，点击与长按拖动的分工互不干扰。
 *
 * [failed] 为 true 时（这一轮领取出错）右下角换成错误态图标，与状态文案保持一致。
 */
@Composable
internal fun DashboardGiftCard(
    title: String,
    status: String,
    failed: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val contentColor = CardDefaults.defaultColors(color = statusCardBackgroundColor()).contentColor
    Card(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = CardCornerRadius,
        colors = CardDefaults.defaultColors(color = statusCardBackgroundColor()),
        pressFeedbackType = PressFeedbackType.Tilt,
        showIndication = true,
        // 点击进温馨礼包链接管理页（进页前已做过保护锁验证）。
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(GiftCardHeight)
                // 右下角图标故意溢出卡片边界，靠卡片自己裁。Android 上靠的是图层的矩形边界，
                // 桌面（Skiko）的离屏图层不裁边界，按压倾斜动画时就会看到溢出的那半截，
                // 所以这里显式裁一次，两端表现才一致。
                .clip(RoundedCornerShape(CardCornerRadius)),
        ) {
            Icon(
                painter = painterResource(
                    if (failed) Res.drawable.ic_error_circle_outline else Res.drawable.ic_check_circle_outline,
                ),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = GiftIconOffsetX, y = GiftIconOffsetY)
                    .size(GiftIconSize),
                tint = if (failed) MiuixTheme.colorScheme.error else statusCardAccentColor(),
            )
            Text(
                text = title,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(horizontal = PairCardHorizontalPadding, vertical = CardVerticalPadding),
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
            )
            Text(
                text = status,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = PairCardHorizontalPadding, vertical = 10.dp),
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.Medium,
                color = contentColor.copy(alpha = SummaryAlpha),
            )
        }
    }
}

/**
 * 更新公告卡：点一下进 App 内的公告页。
 *
 * 与温馨礼包同一套几何（同高、同圆角、右下角淡色大图标），
 * 但用普通白底，和左边那张绿卡区分开。
 */
@Composable
internal fun DashboardNoticeCard(
    status: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = CardCornerRadius,
        colors = CardDefaults.defaultColors(),
        pressFeedbackType = PressFeedbackType.Tilt,
        showIndication = true,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(GiftCardHeight)
                // 同上：图标压出卡片，显式裁剪才能跨端一致。
                .clip(RoundedCornerShape(CardCornerRadius)),
        ) {
            Icon(
                imageVector = MiuixIcons.Notes,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = GiftIconOffsetX, y = GiftIconOffsetY)
                    .size(GiftIconSize),
                tint = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.16f),
            )
            Text(
                text = DashboardCardId.UpdateNotice.title,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(horizontal = PairCardHorizontalPadding, vertical = PairCardVerticalPadding),
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onBackground,
            )
            Text(
                text = status,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = PairCardHorizontalPadding, vertical = 10.dp),
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
