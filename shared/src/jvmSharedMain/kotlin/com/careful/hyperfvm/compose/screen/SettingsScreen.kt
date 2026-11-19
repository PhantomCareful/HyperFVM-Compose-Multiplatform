package com.careful.hyperfvm.compose.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import com.careful.hyperfvm.compose.data.settings.ThemeStyle
import com.careful.hyperfvm.compose.data.settings.isGlassStyle
import com.careful.hyperfvm.compose.data.settings.isLiquidGlassBar
import com.careful.hyperfvm.compose.navigation.LocalNavigator
import com.careful.hyperfvm.compose.navigation.Route
import com.careful.hyperfvm.compose.platform.isDesktopPlatform
import com.careful.hyperfvm.compose.platform.rememberBiometricGate
import com.careful.hyperfvm.compose.ui.isWideLayout
import com.careful.hyperfvm.compose.ui.theme.KeyColorNames
import com.careful.hyperfvm.compose.ui.AppCard
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import com.careful.hyperfvm.compose.ui.AppDialog
import com.careful.hyperfvm.compose.ui.AppDialogButton
import com.careful.hyperfvm.compose.ui.AppDialogButtonRole
import top.yukonga.miuix.kmp.preference.ArrowPreference
import com.careful.hyperfvm.compose.ui.AppDropdownPreference
import com.careful.hyperfvm.compose.ui.sheetCardColor
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
private val ThemeStyleOptions = listOf("轻透磨砂", "柔光玻璃")
private val NavigationBarModeOptions = listOf("图标与文字", "仅图标", "选中项显示文字")
/** 模糊关时两档：Q弹 也会退成滑块，所以不再单列。 */
private val FloatingBarStyleNamesOpaque = listOf("默认", "滑块")
private val FloatingBarStyleValuesOpaque = listOf(FloatingBarStyle.Default, FloatingBarStyle.LiquidGlass)

/** 模糊开时三档，下标与 [FloatingBarStyleValuesBlurred] 一一对应。 */
private val FloatingBarStyleNamesBlurred = listOf("高光描边", "液态玻璃（常规）", "液态玻璃（Q弹）")
private val FloatingBarStyleValuesBlurred =
    listOf(FloatingBarStyle.Default, FloatingBarStyle.LiquidGlass, FloatingBarStyle.BouncyGlass)
private val PagerGestureOptions = listOf("默认", "跨轴拦截")
/** 下标与 [com.careful.hyperfvm.compose.data.settings.CloseAction] 对应。 */
private val CloseActionOptions = listOf("最小化至托盘", "直接退出")

/** 转场样式名称，下标与 [com.careful.hyperfvm.compose.data.settings.AppSettings.navTransitionStyle] 对应。 */
internal val NavTransitionStyleNames = listOf("默认", "安卓原生")

/** 底栏设置行出现的时长：切回轻透磨砂时展开。 */
private const val ThemeRowRevealMillis = 240

/** 底栏设置行消失的时长：切到柔光玻璃时收起，略快一点，免得顶栏已经换完样式这行还在往下坠。 */
private const val ThemeRowHideMillis = 180

@Composable
fun SettingsScreen(outerPadding: PaddingValues) {
    val settings = LocalAppSettings.current
    val update = LocalUpdateSettings.current
    val navigator = LocalNavigator.current
    val isWide = isWideLayout()
    val shaderSupported = isRuntimeShaderSupported()
    val monet = settings.colorMode in 3..5
    // 验证能力两端都有（Android 是指纹 / 锁屏密码，Windows 是 Hello）；
    // 桌面端探测是异步的，拿到能力之前安全分组先不画。
    val biometricGate = rememberBiometricGate()
    // 悬浮样式的名字跟着实际外观走：设备不支持着色器时「主题风格」「模糊」都不出现、底栏也走非玻璃分支，
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
            // 窗口行为是桌面端专属：Android 没有「点关闭按钮」这个动作。
            if (isDesktopPlatform) {
                item(key = "closeAction") {
                    AppCard(modifier = Modifier.padding(horizontal = 12.dp).padding(top = 12.dp, bottom = 12.dp)) {
                        AppDropdownPreference(
                            title = "关闭软件时",
                            summary = "点窗口右上角关闭按钮时的行为",
                            items = CloseActionOptions,
                            selectedIndex = settings.closeAction,
                            onSelectedIndexChange = { index -> update { it.copy(closeAction = index) } },
                        )
                    }
                }
            }

            // 授权检查（通知/安装/受支持链接/剪贴板）是 Android 专属，桌面端没有这些系统入口。
            if (!isDesktopPlatform) {
                item(key = "permission") {
                    AppCard(modifier = Modifier.padding(horizontal = 12.dp).padding(top = 12.dp, bottom = 12.dp)) {
                        ArrowPreference(
                            title = "授权检查",
                            summary = "授予必要的权限以使用App的完整功能",
                            onClick = { navigator.push(Route.Permission) },
                        )
                    }
                }
            }

            item(key = "appearance") {
                SmallTitle("主题")
                AppCard(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    AppDropdownPreference(
                        title = "主题",
                        summary = "选择应用的主题模式",
                        items = ColorModeOptions,
                        selectedIndex = settings.colorMode,
                        onSelectedIndexChange = { index -> update { it.copy(colorMode = index) } },
                    )
                    AnimatedVisibility(visible = monet) {
                        Column {
                            AppDropdownPreference(
                                title = "强调色",
                                summary = "在使用Monet时自定义种子色",
                                items = KeyColorNames,
                                selectedIndex = settings.keyColorIndex,
                                onSelectedIndexChange = { index -> update { it.copy(keyColorIndex = index) } },
                            )
                            AnimatedVisibility(visible = settings.keyColorIndex > 0) {
                                Column {
                                    AppDropdownPreference(
                                        title = "调色板风格",
                                        summary = "用于推导Material3调色板的色调算法",
                                        items = PaletteStyleOptions,
                                        selectedIndex = settings.paletteStyle,
                                        onSelectedIndexChange = { index -> update { it.copy(paletteStyle = index) } },
                                    )
                                    AppDropdownPreference(
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
                    // 主题风格决定顶栏模糊形式与底栏材质，不再单独提供顶栏模糊样式。
                    // 柔光玻璃依赖 RuntimeShader（Android 13 起），更早的系统上固定轻透磨砂，整行不出现。
                    if (shaderSupported) {
                        AppDropdownPreference(
                            title = "主题风格",
                            summary = "选择应用的主题风格",
                            items = ThemeStyleOptions,
                            selectedIndex = settings.themeStyle,
                            onSelectedIndexChange = { index ->
                                update {
                                    it.copy(
                                        themeStyle = index,
                                        // 柔光玻璃离不开模糊：切过去时一并打开，免得留下「玻璃但没模糊」的错位。
                                        enableBlur = if (index == ThemeStyle.Glass) true else it.enableBlur,
                                    )
                                }
                            },
                        )
                        // 柔光玻璃下模糊是硬性前提，开关失去意义，跟着隐藏。
                        AnimatedVisibility(visible = !settings.isGlassStyle) {
                            SwitchPreference(
                                title = "模糊",
                                summary = "启用顶栏和底栏的模糊效果",
                                checked = settings.enableBlur,
                                onCheckedChange = { enabled -> update { it.copy(enableBlur = enabled) } },
                            )
                        }
                    }
                    // 柔光玻璃统一使用 OS4 的悬浮底栏，底栏相关设置整体隐藏。
                    // 宽屏用左侧 NavigationRail，没有可调项，整张卡不出现。
                    // 包一层 AnimatedVisibility：切换主题风格时这几行跟着高度一起收起，而不是瞬间消失。
                    AnimatedVisibility(
                        visible = !isWide && !settings.isGlassStyle,
                        enter = expandVertically(animationSpec = tween(ThemeRowRevealMillis)) +
                            fadeIn(animationSpec = tween(ThemeRowRevealMillis)),
                        exit = shrinkVertically(animationSpec = tween(ThemeRowHideMillis)) +
                            fadeOut(animationSpec = tween(ThemeRowHideMillis)),
                    ) {
                        Column {
                            AnimatedVisibility(visible = !settings.useFloatingNavigationBar) {
                                AppDropdownPreference(
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
                                AppDropdownPreference(
                                    title = "悬浮样式",
                                    items = barStyleNames,
                                    selectedIndex = barStyleIndex,
                                    onSelectedIndexChange = { index ->
                                        update { it.copy(floatingNavigationBarStyle = barStyleValues[index]) }
                                    },
                                )
                            }
                        }
                    }
                    AppDropdownPreference(
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

            // 交互分组是触屏专属：桌面端没有触感反馈，翻页手势也用不到这几项，整块隐藏。
            if (!isDesktopPlatform) {
                item(key = "interaction") {
                    SmallTitle("交互")
                    AppCard(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                        SwitchPreference(
                            title = "滑动到底振动",
                            summary = "列表滑到两端时给一次触感反馈",
                            checked = settings.scrollEndHaptic,
                            onCheckedChange = { enabled -> update { it.copy(scrollEndHaptic = enabled) } },
                        )
                        SwitchPreference(
                            title = "左右滑动切换页面",
                            summary = "允许在主界面和带有分类标签的界面通过左右滑动来切换页面",
                            checked = settings.pageUserScroll,
                            onCheckedChange = { enabled -> update { it.copy(pageUserScroll = enabled) } },
                        )
                        AnimatedVisibility(visible = settings.pageUserScroll) {
                            AppDropdownPreference(
                                title = "切换手势模式",
                                summary = "左右滑动翻页的接管方式",
                                items = PagerGestureOptions,
                                selectedIndex = settings.pagerInterceptionMode,
                                onSelectedIndexChange = { index -> update { it.copy(pagerInterceptionMode = index) } },
                            )
                        }
                    }
                }
            }

            // 安全分组放在最底部：它只管温馨礼包这一处，不是全局行为。
            // Android 恒显示（不可用时就地说明原因）；桌面端只有 Hello 真的可用时才出现。
            if (!isDesktopPlatform || biometricGate.available) {
                item(key = "security") {
                    SmallTitle("安全")
                    AppCard(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                        SwitchPreference(
                            title = "保护锁",
                            // 文案参考 Java 版；两端的验证手段叫法不同（Android 是指纹或锁屏密码，
                            // Windows 是 Hello），让实现自己报名字，界面不按平台分支。
                            summary = if (biometricGate.available) {
                                "管理温馨礼包链接时将通过${biometricGate.credentialName}验证您的身份"
                            } else {
                                "当前设备不支持，请先在系统设置里配置${biometricGate.credentialName}"
                            },
                            checked = settings.contentBiometricAuth,
                            enabled = biometricGate.available,
                            // 开与关都要先验身份：关掉保护锁本身就是一次降低安全性的操作。
                            // 取消或验证失败时不动设置，开关也就停在原位。
                            onCheckedChange = { enabled ->
                                biometricGate.request(
                                    title = "保护锁",
                                    subtitle = if (enabled) {
                                        "验证您的身份以开启保护锁"
                                    } else {
                                        "验证您的身份以关闭保护锁"
                                    },
                                ) { passed ->
                                    if (passed) update { it.copy(contentBiometricAuth = enabled) }
                                }
                            },
                        )
                    }
                }
            }

            item(key = "about") {
                AppCard(modifier = Modifier.padding(horizontal = 12.dp)) {
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
    AppDialog(
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
            // 弹层里的输入框与内容卡同一种底色（柔光玻璃下换成不透明的 surfaceContainer）。
            colors = TextFieldDefaults.textFieldColors(backgroundColor = sheetCardColor()),
            trailingIcon = {
                Text(
                    text = "%",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                )
            },
        )
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            AppDialogButton(
                text = "取消",
                onClick = onDismissRequest,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(20.dp))
            AppDialogButton(
                text = "确定",
                onClick = {
                    val percent = text.toIntOrNull()?.coerceIn(80, 110)
                        ?: (currentScale * 100).roundToInt()
                    onScaleChange(percent / 100f)
                    onDismissRequest()
                },
                modifier = Modifier.weight(1f),
                role = AppDialogButtonRole.Primary,
            )
        }
    }
}
