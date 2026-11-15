package com.careful.hyperfvm.compose.screen.about

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.component.card.TipsCard
import com.careful.hyperfvm.compose.screen.ScreenScaffold
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.utils.pressable

/**
 * 共建团页：为 HyperFVM 提供过帮助的同学名单，按区服分四张卡。
 *
 * 文案与分组对齐 Java 版 `activity_co_contributor_team.xml`；
 * 统一用 [ContributorRow]（[BasicComponent] + [pressable] 补点按阴影）。
 */
@Composable
fun CoContributorTeamScreen(onBack: () -> Unit) {
    ScreenScaffold(
        title = "共建团",
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
                    "感谢以下同学为HyperFVM的开发提供了不同程度的帮助。" +
                        "（按照区服进行划分，区服内的排名顺序为时间先后顺序）",
                )
            }

            item(key = "card4399") {
                AboutContentCard {
                    ContributorRow(
                        title = "4399 2–6服·早睡日氪一元",
                        summary = "感谢提出防御卡数据整合的建议\n" +
                            "感谢提出相关防御卡跳转查询的建议\n" +
                            "感谢帮助检查防御卡全能数据库中的错误",
                    )
                }
            }

            item(key = "cardQq18") {
                AboutContentCard {
                    ContributorRow(
                        title = "QQ空间1–8服·飛飛",
                        summary = "感谢提出在主界面添加防御卡数据查询按钮的建议",
                    )
                    ContributorRow(
                        title = "QQ空间1–8服·HS灬缄默",
                        summary = "感谢提出在图鉴防御卡数据中展示超清大图的建议",
                    )
                    ContributorRow(
                        title = "QQ空间1–8服·darg1",
                        summary = "感谢帮助修复部分低质大图",
                    )
                    ContributorRow(
                        title = "QQ空间1–8服·司康饼的午后",
                        summary = "感谢帮助修复部分低质大图",
                    )
                    ContributorRow(
                        title = "QQ空间1–8服·是小甘诶",
                        summary = "感谢帮助完善资源储备计算器",
                    )
                }
            }

            item(key = "cardQq1326") {
                AboutContentCard {
                    ContributorRow(
                        title = "QQ空间13–26服·一只小小明~",
                        summary = "感谢提出增加宝箱概率公示合集的建议\n" +
                            "感谢提出在仪表盘增加走马观花场次信息的建议",
                    )
                }
            }

            item(key = "cardBilibili") {
                AboutContentCard {
                    ContributorRow(
                        title = "哔哩哔哩·失眠小羊-ovo",
                        summary = "感谢提出图片导出功能的建议",
                    )
                }
            }
        }
    }
}

/**
 * 共建团的纯展示行：没有点击事件，不挂 clickable（TalkBack 也不会播报可点）；
 * 点按阴影由 [pressable] 补上 [LocalIndication] 提供的主题效果。
 */
@Composable
private fun ContributorRow(title: String, summary: String) {
    BasicComponent(
        modifier = Modifier.pressable(
            interactionSource = null,
            indication = LocalIndication.current,
        ),
        title = title,
        summary = summary,
    )
}
