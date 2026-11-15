package com.careful.hyperfvm.compose.screen

import androidx.compose.foundation.lazy.LazyColumn
import com.careful.hyperfvm.compose.shared.*

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.navigation.LocalNavigator
import com.careful.hyperfvm.compose.navigation.Route
import com.careful.hyperfvm.compose.screen.about.AboutContentCard
import top.yukonga.miuix.kmp.preference.ArrowPreference

/**
 * 实用工具一级页：「实用小工具」版块下的今日运势、资源储备计算器与强卡成功率计算器入口，
 * 对应 Java 版 `fragment_data_center.xml` 的同名分组；三个入口按 Java 版顺序排列。
 */
@Composable
fun ToolsScreen(outerPadding: PaddingValues) {
    val navigator = LocalNavigator.current

    ScreenScaffold(
        title = "实用工具",
        outerPadding = outerPadding,
    ) { padding ->
        LazyColumn(
            // 铺满内容区：只有一个入口卡时，空白处的手势也要能进列表，顶栏折叠才会响应。
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 12.dp,
                bottom = padding.calculateBottomPadding(),
            ),
        ) {
            item(key = "todayLucky") {
                AboutContentCard {
                    ArrowPreference(
                        title = "今日运势",
                        summary = "多个快速播放不同星级海星的动图",
                        startAction = {
                            Image(
                                modifier = Modifier.padding(end = 12.dp).size(50.dp),
                                painter = painterResource(Res.drawable.tools_image_today_lucky),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                            )
                        },
                        onClick = { navigator.push(Route.TodayLucky) },
                    )
                }
            }
            item(key = "resourceCalculator") {
                AboutContentCard {
                    ArrowPreference(
                        title = "资源储备计算器",
                        summary = "计算威望、金币、礼券、魅力币的储备总量",
                        startAction = {
                            Image(
                                modifier = Modifier.padding(end = 12.dp).size(50.dp),
                                painter = painterResource(Res.drawable.tools_image_prestige_calculator),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                            )
                        },
                        onClick = { navigator.push(Route.ResourceCalculator) },
                    )
                }
            }
            item(key = "cardOddsCalculator") {
                AboutContentCard {
                    ArrowPreference(
                        title = "强卡成功率计算器",
                        summary = "可以快捷计算不同星级组合的强卡成功率，不能模拟上卡哦",
                        startAction = {
                            Image(
                                modifier = Modifier.padding(end = 12.dp).size(50.dp),
                                painter = painterResource(Res.drawable.data_center_image_card_calculator),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                            )
                        },
                        onClick = { navigator.push(Route.CardOddsCalculator) },
                    )
                }
            }
        }
    }
}
