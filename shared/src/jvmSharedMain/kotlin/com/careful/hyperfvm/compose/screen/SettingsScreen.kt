package com.careful.hyperfvm.compose.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.data.settings.FloatingBarStyle
import com.careful.hyperfvm.compose.data.settings.LocalAppSettings
import com.careful.hyperfvm.compose.data.settings.LocalUpdateSettings
import com.careful.hyperfvm.compose.data.settings.isLiquidGlassBar
import com.careful.hyperfvm.compose.navigation.LocalNavigator
import com.careful.hyperfvm.compose.navigation.Route
import com.careful.hyperfvm.compose.ui.isWideLayout
import com.careful.hyperfvm.compose.ui.theme.KeyColorNames
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle
import kotlin.math.roundToInt

private val ColorModeOptions = listOf(
    "Miuix（跟随系统）", "Miuix（浅色）", "Miuix（深色）",
    "Monet（跟随系统）", "Monet（浅色）", "Monet（深色）",
)

private val PaletteStyleOptions = ThemePaletteStyle.entries.map { it.name }
private val ColorSpecOptions = ThemeColorSpec.entries.map { it.name }
private val BlurStyleOptions = listOf("高斯模糊", "渐进模糊")
private val NavigationBarModeOptions = listOf("图标与文字", "仅图标", "选中项显示文字")
/** 模糊关时两档：Q弹 也会退成滑块，所以不再单列。 */
private val FloatingBarStyleNamesOpaque = listOf("默认", "滑块")
private val FloatingBarStyleValuesOpaque = listOf(FloatingBarStyle.Default, FloatingBarStyle.LiquidGlass)

/** 模糊开时三档，下标与 [FloatingBarStyleValuesBlurred] 一一对应。 */
private val FloatingBarStyleNamesBlurred = listOf("高光描边", "液态玻璃（常规）", "液态玻璃（Q弹）")
private val FloatingBarStyleValuesBlurred =
    listOf(FloatingBarStyle.Default, FloatingBarStyle.LiquidGlass, FloatingBarStyle.BouncyGlass)
private val PagerGestureOptions = listOf("默认", "跨轴拦截", "iOS 风格")

/** 转场样式名称，下标与 [com.careful.hyperfvm.compose.data.settings.AppSettings.navTransitionStyle] 对应。 */
internal val NavTransitionStyleNames = listOf("默认", "安卓原生")

@Composable
fun SettingsScreen(outerPadding: PaddingValues) {
    val settings = LocalAppSettings.current
    val update = LocalUpdateSettings.current
    val navigator = LocalNavigator.current
    val isWide = isWideLayout()
    val shaderSupported = isRuntimeShaderSupported()
    val monet = settings.colorMode in 3..5
    // 悬浮样式的名字跟着实际外观走：设备不支持着色器时模糊开关不显示、底栏也走非玻璃分支，
    // 名字一并落到「默认 / 滑块」，不会出现「写着液态玻璃、实际是滑块」的错位。
    val blurBottomBar = settings.enableBlur && shaderSupported
    val barStyleNames = if (blurBottomBar) FloatingBarStyleNamesBlurred else FloatingBarStyleNamesOpaque
    val barStyleValues = if (blurBottomBar) FloatingBarStyleValuesBlurred else FloatingBarStyleValuesOpaque
    // 历史编号（1 高光描边、3 滑块）不在表里时，按它所属的路线就近落下。
    val barStyleIndex = barStyleValues.indexOf(settings.floatingNavigationBarStyle)
        .takeIf { it >= 0 } ?: if (settings.isLiquidGlassBar) 1 else 0
    // 弹窗状态提到列表外面，才能保证列表项被回收时弹窗不被一起销毁。
    var showScaleDialog by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = "设置",
        outerPadding = outerPadding,
    ) { padding ->
        LazyColumn(
            // 铺满内容区：少数机型上列表不足一屏时，空白处的手势也要能进列表。
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
        ) {
            item(key = "permission") {
                Card(modifier = Modifier.padding(horizontal = 12.dp).padding(top = 12.dp, bottom = 12.dp)) {
                    ArrowPreference(
                        title = "授权检查",
                        summary = "授予必要的权限以使用App的完整功能",
                        onClick = { navigator.push(Route.Permission) },
                    )
                }
            }

            item(key = "appearance") {
                SmallTitle("主题")
                Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    OverlayDropdownPreference(
                        title = "主题",
                        summary = "选择应用的主题模式",
                        items = ColorModeOptions,
                        selectedIndex = settings.colorMode,
                        onSelectedIndexChange = { index -> update { it.copy(colorMode = index) } },
                    )
                    AnimatedVisibility(visible = monet) {
                        Column {
                            OverlayDropdownPreference(
                                title = "强调色",
                                summary = "在使用Monet时自定义种子色",
                                items = KeyColorNames,
                                selectedIndex = settings.keyColorIndex,
                                onSelectedIndexChange = { index -> update { it.copy(keyColorIndex = index) } },
                            )
                            AnimatedVisibility(visible = settings.keyColorIndex > 0) {
                                Column {
                                    OverlayDropdownPreference(
                                        title = "调色板风格",
                                        summary = "用于推导Material3调色板的色调算法",
                                        items = PaletteStyleOptions,
                                        selectedIndex = settings.paletteStyle,
                                        onSelectedIndexChange = { index -> update { it.copy(paletteStyle = index) } },
                                    )
                                    OverlayDropdownPreference(
                                        title = "色彩规范",
                                        summary = "Material3色彩规范版本",
                                        items = ColorSpecOptions,
                                        selectedIndex = settings.colorSpec,
                                        onSelectedIndexChange = { index -> update { it.copy(colorSpec = index) } },
                                    )
                                }
                            }
                        }
                    }
                    if (shaderSupported) {
                        SwitchPreference(
                            title = "模糊",
                            summary = "启用顶栏和底栏的模糊效果",
                            checked = settings.enableBlur,
                            onCheckedChange = { enabled -> update { it.copy(enableBlur = enabled) } },
                        )
                        AnimatedVisibility(visible = settings.enableBlur) {
                            OverlayDropdownPreference(
                                title = "顶栏模糊样式",
                                items = BlurStyleOptions,
                                selectedIndex = settings.blurStyle,
                                onSelectedIndexChange = { index -> update { it.copy(blurStyle = index) } },
                            )
                        }
                    }
                    // 宽屏用左侧 NavigationRail，没有可调项，整张卡不出现。
                    if(!isWide) {
                        AnimatedVisibility(visible = !settings.useFloatingNavigationBar) {
                            OverlayDropdownPreference(
                                title = "底栏样式",
                                items = NavigationBarModeOptions,
                                selectedIndex = settings.navigationBarMode,
                                onSelectedIndexChange = { index -> update { it.copy(navigationBarMode = index) } },
                            )
                        }
                        SwitchPreference(
                            title = "悬浮底栏",
                            checked = settings.useFloatingNavigationBar,
                            onCheckedChange = { enabled -> update { it.copy(useFloatingNavigationBar = enabled) } },
                        )
                        AnimatedVisibility(visible = settings.useFloatingNavigationBar) {
                            OverlayDropdownPreference(
                                title = "悬浮样式",
                                items = barStyleNames,
                                selectedIndex = barStyleIndex,
                                onSelectedIndexChange = { index ->
                                    update { it.copy(floatingNavigationBarStyle = barStyleValues[index]) }
                                },
                            )
                        }
                    }
                    OverlayDropdownPreference(
                        title = "转场样式",
                        summary = "页面切换时的动画",
                        items = NavTransitionStyleNames,
                        selectedIndex = settings.navTransitionStyle,
                        onSelectedIndexChange = { index -> update { it.copy(navTransitionStyle = index) } },
                    )
                    // 拖动期间只改本地状态，松手才落盘：密度一变整棵树都要重新解析 dp，
                    // 每移动一像素就重组一次会明显掉帧。
                    var scaleValue by remember(settings.uiScale) { mutableFloatStateOf(settings.uiScale) }
                    SliderPreference(
                        title = "界面缩放",
                        summary = "调整全局显示比例",
                        value = scaleValue,
                        onValueChange = { scaleValue = it },
                        onValueChangeFinished = { update { it.copy(uiScale = scaleValue) } },
                        valueText = "${(scaleValue * 100).roundToInt()}%",
                        valueRange = 0.8f..1.1f,
                        showKeyPoints = true,
                        keyPoints = listOf(0.8f, 0.9f, 1f, 1.1f),
                        magnetThreshold = 0.01f,
                        hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                        // 行末尾出现箭头，点击后弹出精确输入框。
                        onClick = { showScaleDialog = true },
                        holdDownState = showScaleDialog,
                    )
                }
            }

            item(key = "interaction") {
                SmallTitle("交互")
                Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    SwitchPreference(
                        title = "滑动到底振动",
                        summary = "列表滑到两端时给一次触感反馈",
                        checked = settings.scrollEndHaptic,
                        onCheckedChange = { enabled -> update { it.copy(scrollEndHaptic = enabled) } },
                    )
                    SwitchPreference(
                        title = "左右滑动切换页面",
                        summary = "允许在主界面左右滑动切换页面",
                        checked = settings.pageUserScroll,
                        onCheckedChange = { enabled -> update { it.copy(pageUserScroll = enabled) } },
                    )
                    AnimatedVisibility(visible = settings.pageUserScroll) {
                        OverlayDropdownPreference(
                            title = "切换手势模式",
                            summary = "左右滑动翻页的接管方式",
                            items = PagerGestureOptions,
                            selectedIndex = settings.pagerInterceptionMode,
                            onSelectedIndexChange = { index -> update { it.copy(pagerInterceptionMode = index) } },
                        )
                    }
                }
            }

            item(key = "navigation") {
                Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                    ArrowPreference(
                        title = "关于HyperFVM",
                        onClick = { navigator.push(Route.About) },
                    )
                }
            }

            item(key = "bottomSpacer") {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        ScaleDialog(
            show = showScaleDialog,
            currentScale = settings.uiScale,
            onScaleChange = { value -> update { it.copy(uiScale = value) } },
            onDismissRequest = { showScaleDialog = false },
        )
    }
}

/**
 * 界面缩放的精确输入框。
 *
 * 滑块只能拖个大概，想调 93% 这种值就靠它；取值范围与滑块保持一致（80% - 110%），
 * 超界取边界值，输入框留空或乱填时回退到当前值。
 */
@Composable
private fun ScaleDialog(
    show: Boolean,
    currentScale: Float,
    onScaleChange: (Float) -> Unit,
    onDismissRequest: () -> Unit,
) {
    OverlayDialog(
        show = show,
        title = "界面缩放",
        summary = "80% - 110%",
        onDismissRequest = onDismissRequest,
    ) {
        // 按 show 重置：每次打开都从当前缩放值重新填，不会留下上一次没提交的半截输入。
        var text by remember(show) { mutableStateOf((currentScale * 100).roundToInt().toString()) }
        TextField(
            value = text,
            onValueChange = { input ->
                // 只接受纯数字；留空是合法中间态，用户得先把旧值删掉再输。
                if (input.isEmpty() || input.all { it.isDigit() }) {
                    text = input
                }
            },
            modifier = Modifier.padding(bottom = 16.dp),
            maxLines = 1,
            trailingIcon = {
                Text(
                    text = "%",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                )
            },
        )
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(
                text = "取消",
                onClick = onDismissRequest,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(20.dp))
            TextButton(
                text = "确定",
                onClick = {
                    val percent = text.toIntOrNull()?.coerceIn(80, 110)
                        ?: (currentScale * 100).roundToInt()
                    onScaleChange(percent / 100f)
                    onDismissRequest()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}
