package com.careful.hyperfvm.compose.screen.about

import androidx.compose.foundation.lazy.LazyColumn
import com.careful.hyperfvm.compose.shared.*
import org.jetbrains.compose.resources.DrawableResource

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.component.card.TipsCard
import com.careful.hyperfvm.compose.component.dialog.UrlConfirmDialog
import com.careful.hyperfvm.compose.component.dialog.UrlTarget
import com.careful.hyperfvm.compose.screen.ScreenScaffold
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.utils.pressable

/** 一行致谢的展示与跳转信息，对应 Java 版 thanks_list_fvm 的一条。 */
private data class ThanksGameEntry(
    val title: String,
    val description: String,
    /** 左侧 50dp 数据图。 */
    val image: DrawableResource,
    /** 外链确认框的目标，null 表示这行没有点击行为。 */
    val target: UrlTarget?,
)

private val ThanksGameEntries = listOf(
    ThanksGameEntry(
        title = "So陌路の",
        description = "综合数据表作者\n" +
            "防御卡全能数据库中的大部分内容都来自此数据表，为了更方便地查询防御卡数据，HyperFVM应运而生",
        image = Res.drawable.thanks_list_image_fvm_1,
        target = UrlTarget(
            title = "哔哩哔哩",
            subtitle = "So陌路の",
            url = "https://space.bilibili.com/383124923",
            iconRes = Res.drawable.ic_bilibili,
        ),
    ),
    ThanksGameEntry(
        title = "夏夜浅酌",
        description = "提拉米鼠、美食数据站作者\n" +
            "HyperFVM收录并定期更新部分重要的米鼠的图作为备份\n" +
            "数据中心里面的美食数据站和夏夜老师的网站内容保持一致",
        image = Res.drawable.thanks_list_image_fvm_2,
        target = UrlTarget(
            title = "哔哩哔哩",
            subtitle = "夏夜浅酌2332",
            url = "https://space.bilibili.com/34549193",
            iconRes = Res.drawable.ic_bilibili,
        ),
    ),
    ThanksGameEntry(
        title = "右撇三叶",
        description = "部分高清图素材作者\n" +
            "启动遮罩中的冰激凌海星就是出自这位老师之手",
        image = Res.drawable.thanks_list_image_fvm_3,
        target = UrlTarget(
            title = "美食大战老鼠吧",
            subtitle = "高清图图楼",
            url = "https://tieba.baidu.com/p/6759444219",
            iconRes = Res.drawable.ic_tieba,
        ),
    ),
    // 查黑系统的查询窗口走的是 Java 版的 QQ 输入框对话框，这里先不做，保持纯展示。
    ThanksGameEntry(
        title = "就喝百事",
        description = "查黑系统作者\n" +
            "数据中心内集成了本地的查询窗口，便于快捷查询",
        image = Res.drawable.thanks_list_image_fvm_4,
        target = null,
    ),
    ThanksGameEntry(
        title = "哪片花火",
        description = "部分数据图作者\n" +
            "从这位老师那里看到了一些不错的数据图，重制之后署名并收录在数据图合集里",
        image = Res.drawable.thanks_list_image_fvm_5,
        target = null,
    ),
)

/**
 * 致谢页（游戏相关）：五位老师排在一张大卡里，与 Java 版 `activity_thanks_game.xml` 一致。
 *
 * 行尾统一走 [ArrowPreference] 自带的箭头；
 * fvm_1 ~ fvm_3 点击先弹外链确认框，后两行纯展示、不挂 clickable，
 * 但用 [pressable] 补上 [LocalIndication] 的点按阴影。
 */
@Composable
fun ThanksGameScreen(onBack: () -> Unit) {
    // 弹窗状态提到列表外面，才能保证列表项被回收时弹窗不被一起销毁。
    var pendingUrl by remember { mutableStateOf<UrlTarget?>(null) }

    ScreenScaffold(
        title = "致谢",
        subtitle = "游戏相关",
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
                    "HyperFVM 在开发过程中使用了这些老师们的作品，在此表示衷心感谢。" +
                        "（排名顺序不分先后）",
                )
            }

            item(key = "cards") {
                AboutContentCard {
                    ThanksGameEntries.forEach { entry ->
                        if (entry.target != null) {
                            ArrowPreference(
                                title = entry.title,
                                summary = entry.description,
                                startAction = {
                                    Image(
                                        modifier = Modifier.padding(end = 12.dp).size(50.dp).clip(CircleShape),
                                        painter = painterResource(entry.image),
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
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
                                    Image(
                                        modifier = Modifier.padding(end = 12.dp).size(50.dp).clip(CircleShape),
                                        painter = painterResource(entry.image),
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                    )
                                },
                            )
                        }
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
