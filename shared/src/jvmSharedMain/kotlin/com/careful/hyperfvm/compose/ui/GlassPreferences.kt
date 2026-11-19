package com.careful.hyperfvm.compose.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.data.settings.LocalAppSettings
import com.careful.hyperfvm.compose.data.settings.isGlassStyle
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.DropdownArrowEndAction
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.glass.GlassDropdownPopup
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 全站统一的下拉偏好：标题 + 选中值 + 下拉箭头。
 *
 * 行本体固定是原生 [BasicComponent] 的磨砂观感；**弹出面板**按主题风格分支：
 * 轻透磨砂直接用 [OverlayDropdownPreference]（连同它的原生列表弹层），
 * 柔光玻璃换成 OS4 玻璃下拉（[GlassDropdownPopup] + [GlassPopupItem]）。
 *
 * miuix 没有玻璃版下拉偏好，玻璃分支按官方 OverlayDropdownPreference 的行结构复刻：
 * [BasicComponent] + 右端的选中值 + [DropdownArrowEndAction]；面板登记到页面根宿主
 * （[GlassMenuPanelHosted]），与顶栏图标菜单同一套几何，才能取样页面背板。
 *
 * 参数取两条分支共有的交集，调用方换名字即可。
 */
@Composable
fun AppDropdownPreference(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    showValue: Boolean = true,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    startAction: @Composable (() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    renderInRootScaffold: Boolean = true,
    onExpandedChange: ((Boolean) -> Unit)? = null,
) {
    if (!LocalAppSettings.current.isGlassStyle) {
        OverlayDropdownPreference(
            items = items,
            selectedIndex = selectedIndex,
            title = title,
            modifier = modifier,
            titleColor = titleColor,
            summary = summary,
            summaryColor = summaryColor,
            startAction = startAction,
            bottomAction = bottomAction,
            insideMargin = insideMargin,
            enabled = enabled,
            showValue = showValue,
            renderInRootScaffold = renderInRootScaffold,
            onExpandedChange = onExpandedChange,
            onSelectedIndexChange = onSelectedIndexChange,
        )
        return
    }
    GlassDropdownPreference(
        title = title,
        items = items,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = onSelectedIndexChange,
        modifier = modifier,
        summary = summary,
        enabled = enabled,
        showValue = showValue,
        titleColor = titleColor,
        summaryColor = summaryColor,
        startAction = startAction,
        bottomAction = bottomAction,
        insideMargin = insideMargin,
        onExpandedChange = onExpandedChange,
        renderInRootScaffold = renderInRootScaffold,
    )
}

/**
 * 柔光玻璃分支：磨砂的行 + 玻璃的弹层。
 *
 * 行结构与官方 OverlayDropdownPreference 逐项对齐（选中值、箭头、按住态、
 * 点击音效反馈），弹层换成玻璃面板；面板走 [GlassMenuPanelHosted] 挂到页面根，
 * 不在宿主里（如不在 ScreenScaffold 内）时退回原生磨砂实现。
 */
@Composable
private fun GlassDropdownPreference(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    summary: String?,
    enabled: Boolean,
    showValue: Boolean,
    titleColor: BasicComponentColors,
    summaryColor: BasicComponentColors,
    startAction: @Composable (() -> Unit)?,
    bottomAction: (@Composable () -> Unit)?,
    insideMargin: PaddingValues,
    onExpandedChange: ((Boolean) -> Unit)?,
    renderInRootScaffold: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    // 行锚点：面板按它定位（下拉从行的下方展开、空间不够时翻到上方）。
    var anchorBounds by remember { mutableStateOf(Rect.Zero) }
    val haptic = LocalHapticFeedback.current
    val currentHaptic by rememberUpdatedState(haptic)
    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)

    val itemsNotEmpty = items.isNotEmpty()
    val actualEnabled = enabled && itemsNotEmpty
    val actionColor = if (actualEnabled) {
        MiuixTheme.colorScheme.onSurfaceVariantActions
    } else {
        MiuixTheme.colorScheme.disabledOnSecondaryVariant
    }

    fun setExpanded(value: Boolean) {
        if (expanded != value) {
            expanded = value
            currentOnExpandedChange?.invoke(value)
        }
    }

    val backdrop = overlayBackdrop()
    val hosted = GlassMenuPanelHosted {
        GlassDropdownPopup(
            show = expanded,
            onDismissRequest = { setExpanded(false) },
            anchorBounds = anchorBounds,
            backdrop = backdrop,
        ) {
            items.forEachIndexed { index, label ->
                GlassPopupItem(
                    text = label,
                    selected = index == selectedIndex,
                    onClick = {
                        onSelectedIndexChange(index)
                        setExpanded(false)
                    },
                )
            }
        }
    }

    if (!hosted) {
        // 没有页面根宿主：面板会被取样层裁掉或录到自己，只能退回原生磨砂下拉。
        OverlayDropdownPreference(
            items = items,
            selectedIndex = selectedIndex,
            title = title,
            modifier = modifier,
            titleColor = titleColor,
            summary = summary,
            summaryColor = summaryColor,
            startAction = startAction,
            bottomAction = bottomAction,
            insideMargin = insideMargin,
            enabled = enabled,
            showValue = showValue,
            renderInRootScaffold = renderInRootScaffold,
            onExpandedChange = onExpandedChange,
            onSelectedIndexChange = onSelectedIndexChange,
        )
        return
    }

    BasicComponent(
        modifier = modifier.onGloballyPositioned { anchorBounds = it.boundsInWindow() },
        title = title,
        titleColor = titleColor,
        summary = summary,
        summaryColor = summaryColor,
        startAction = startAction,
        endActions = {
            if (showValue && itemsNotEmpty) {
                val text = items.getOrNull(selectedIndex)
                if (!text.isNullOrEmpty()) {
                    Text(
                        text = text,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .align(Alignment.CenterVertically)
                            .weight(1f, fill = false),
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                        color = actionColor,
                        textAlign = TextAlign.End,
                    )
                }
            }
            DropdownArrowEndAction(actionColor = actionColor)
        },
        bottomAction = bottomAction,
        insideMargin = insideMargin,
        onClick = {
            if (actualEnabled) {
                setExpanded(!expanded)
                if (expanded) {
                    currentHaptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                }
            }
        },
        role = Role.DropdownList,
        // 面板开着时行保持「按住」形态，与原生下拉的开合反馈一致。
        holdDownState = expanded,
        enabled = actualEnabled,
    )
}
