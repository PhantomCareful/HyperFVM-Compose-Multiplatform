package com.careful.hyperfvm.compose.screen.about

import androidx.compose.foundation.lazy.LazyColumn
import org.jetbrains.compose.resources.DrawableResource
import com.careful.hyperfvm.compose.*

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.component.card.TipsCard
import com.careful.hyperfvm.compose.component.dialog.UrlConfirmDialog
import com.careful.hyperfvm.compose.component.dialog.UrlTarget
import com.careful.hyperfvm.compose.screen.ScreenScaffold
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.pressable

/** 一行开源项目致谢的展示与跳转信息，对应 Java 版 thanks_list_app 的一条。 */
private data class ThanksAppEntry(
    val title: String,
    val description: String,
    /** 左侧 24dp 项目图标。 */
    val iconRes: DrawableResource,
    /** 外链确认框的目标，null 表示这行没有点击行为（AI 辅助工具两行）。 */
    val target: UrlTarget?,
)

/** 可跳转的四行：都指向各自的 GitHub 仓库，弹窗标题统一是 Github。 */
private val ThanksAppEntries = listOf(
    ThanksAppEntry(
        title = "YuKongA/miuix",
        description = "一个适用于Compose的UI组件库，提供了一系列精仿Xiaomi HyperOS风格的组件",
        iconRes = Res.drawable.ic_github,
        target = UrlTarget(
            title = "Github",
            subtitle = "miuix",
            url = "https://github.com/compose-miuix-ui/miuix",
            iconRes = Res.drawable.ic_github,
        ),
    ),
    ThanksAppEntry(
        title = "Kyant0/AndroidLiquidGlass",
        description = "一个适用于Compose的液态玻璃效果库",
        iconRes = Res.drawable.ic_github,
        target = UrlTarget(
            title = "Github",
            subtitle = "AndroidLiquidGlass",
            url = "https://github.com/Kyant0/AndroidLiquidGlass",
            iconRes = Res.drawable.ic_github,
        ),
    ),
    ThanksAppEntry(
        title = "saket/telephoto",
        description = "高分辨率图片显示依赖库，支持转场动画、双指缩放、多图切换浏览等功能",
        iconRes = Res.drawable.ic_github,
        target = UrlTarget(
            title = "Github",
            subtitle = "telephoto",
            url = "https://github.com/saket/telephoto",
            iconRes = Res.drawable.ic_github,
        ),
    ),
    ThanksAppEntry(
        title = "Calvin-LL/Reorderable",
        description = "一个适用于Compose项目的效果库，可实现通过拖放重新排序列表和网格中的组件",
        iconRes = Res.drawable.ic_github,
        target = UrlTarget(
            title = "Github",
            subtitle = "Reorderable",
            url = "https://github.com/Calvin-LL/Reorderable",
            iconRes = Res.drawable.ic_github,
        ),
    ),
)

/** 辅助编码的两行，Java 版只挂了 clickable 没有 listener，这里保持纯展示。 */
private val ThanksAppToolEntries = listOf(
    ThanksAppEntry(
        title = "DeepSeek",
        description = "辅助编写代码",
        iconRes = Res.drawable.ic_deepseek,
        target = null,
    ),
    ThanksAppEntry(
        title = "Xiaomi MiMo",
        description = "辅助编写代码",
        iconRes = Res.drawable.ic_xiaomi,
        target = null,
    ),
)

/**
 * 致谢页（App相关）：四条开源项目 + 两条 AI 辅助工具，与 Java 版 `activity_thanks_app.xml` 一致。
 *
 * 卡1 的四行点击先弹外链确认框；卡2 的两行（Xiaomi MiMo 在上、DeepSeek 在下）
 * 没有点击行为，不挂 clickable，但用 [pressable] 补上 [LocalIndication] 的点按阴影。
 * 行尾统一走 [ArrowPreference] 自带的箭头。
 */
@Composable
fun ThanksAppScreen(onBack: () -> Unit) {
    // 弹窗状态提到列表外面，才能保证列表项被回收时弹窗不被一起销毁。
    var pendingUrl by remember { mutableStateOf<UrlTarget?>(null) }

    ScreenScaffold(
        title = "致谢",
        subtitle = "App相关",
        // 二级页没有外层 Pager 的底栏，底部安全区由内层 Scaffold 自己吃。
        outerPadding = PaddingValues(0.dp),
        onBack = onBack,
    ) { padding ->
        LazyColumn(
            // 铺满内容区：内容不满一屏时，空白处的手势也要能进列表，滚动与顶栏折叠才会响应。
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 12.dp,
                bottom = padding.calculateBottomPadding(),
            ),
        ) {
            item(key = "tips") {
                TipsCard(
                    "HyperFVM 在开发过程中使用了这些优秀的开源项目或辅助工具，在此表示衷心感谢。" +
                        "（排名顺序不分先后）",
                )
            }

            item(key = "cardProjects") {
                AboutContentCard {
                    ThanksAppEntries.forEach { entry ->
                        if (entry.target != null) {
                            ArrowPreference(
                                title = entry.title,
                                summary = entry.description,
                                startAction = {
                                    Icon(
                                        modifier = Modifier.padding(end = 12.dp),
                                        painter = painterResource(entry.iconRes),
                                        contentDescription = null,
                                        tint = MiuixTheme.colorScheme.onBackground,
                                    )
                                },
                                onClick = entry.target.let { target -> { pendingUrl = target } },
                            )
                        } else {
                            BasicComponent(
                                modifier = Modifier.pressable(
                                    interactionSource = null,
                                    indication = LocalIndication.current,
                                ),
                                title = entry.title,
                                summary = entry.description,
                                startAction = {
                                    Icon(
                                        modifier = Modifier.padding(end = 12.dp),
                                        painter = painterResource(entry.iconRes),
                                        contentDescription = null,
                                        tint = MiuixTheme.colorScheme.onBackground,
                                    )
                                },
                            )
                        }
                    }
                }
            }

            item(key = "cardTools") {
                AboutContentCard {
                    ThanksAppToolEntries.forEach { entry ->
                        BasicComponent(
                            modifier = Modifier.pressable(
                                interactionSource = null,
                                indication = LocalIndication.current,
                            ),
                            title = entry.title,
                            summary = entry.description,
                            startAction = {
                                Icon(
                                    modifier = Modifier.padding(end = 12.dp),
                                    painter = painterResource(entry.iconRes),
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onBackground,
                                )
                            },
                        )
                    }
                }
            }
        }

        UrlConfirmDialog(
            target = pendingUrl,
            onDismiss = { pendingUrl = null },
        )
    }
}
