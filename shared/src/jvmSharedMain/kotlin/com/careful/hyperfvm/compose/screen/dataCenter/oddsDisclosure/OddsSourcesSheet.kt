package com.careful.hyperfvm.compose.screen.dataCenter.oddsDisclosure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.component.card.TipsCard
import com.careful.hyperfvm.compose.component.dialog.openUrl
import com.careful.hyperfvm.compose.data.oddsDisclosure.OddsDataRepository
import com.careful.hyperfvm.compose.data.oddsDisclosure.OddsSource
import com.careful.hyperfvm.compose.platform.LocalAppEnv
import com.careful.hyperfvm.compose.platform.LocalToaster
import com.careful.hyperfvm.compose.ui.AppCard
import com.careful.hyperfvm.compose.ui.sheetCardColor
import top.yukonga.miuix.kmp.basic.CardDefaults
import com.careful.hyperfvm.compose.ui.AppBottomSheet
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 弹层标题：这一组来源服务于整个概率公示合集，不隶属于某一页。 */
private const val SourcesSheetTitle = "官方公示合集"

/** 来源弹层顶部的说明卡。 */
private const val SourcesTips =
    "本界面部分概率公示来源如下，您可以前往查证。若存在与本界面不相同的数据，请以这些来源的数据为准。"

/**
 * 官方公示合集弹层：顶栏的链接按钮打开，列出概率数据的官方来源。
 *
 * 列表随数据包分发（`data/odds_sources.json`），官方新增公示帖时改数据即可，
 * 不用发版；数据包没有该表时用 [OddsDataRepository] 里的内置兜底列表。
 * 说明卡在最上面，每条来源是一张可点击的卡，点了直接调起浏览器：
 * 来源列表本身已经说明了要去哪里，不再走「即将前往」确认框。
 */
@Composable
internal fun OddsSourcesSheet(show: Boolean, onDismissRequest: () -> Unit) {
    val env = LocalAppEnv.current
    val toaster = LocalToaster.current
    // 弹层内容会一直铺到手势条底下，最后一张卡下面补出导航栏高度。
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val sources = OddsDataRepository.sources

    AppBottomSheet(
        show = show,
        title = SourcesSheetTitle,
        onDismissRequest = onDismissRequest,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp + navigationBarBottom),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "tips") {
                TipsCard(tips = SourcesTips, outerPadding = PaddingValues(0.dp))
            }
            sources.forEachIndexed { index, source ->
                item(key = "source-$index") {
                    SourceCard(
                        source = source,
                        onClick = {
                            // 先收起弹层再跳浏览器：从浏览器回来时停在原来的页面上。
                            onDismissRequest()
                            openUrl(env, toaster, source.url)
                        },
                    )
                }
            }
        }
    }
}

/**
 * 一条来源：整张卡可点，行尾是 [ArrowPreference] 自带的箭头。
 *
 * 内层不传 onClick，否则它会再挂一层 clickable，把 Card 的按压反馈和点击都挡掉。
 */
@Composable
private fun SourceCard(source: OddsSource, onClick: () -> Unit) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        // 弹层背景与 Card 默认的 surface 同色，换成弹层内容卡底色才分得清卡片边界。
        colors = CardDefaults.defaultColors(color = sheetCardColor()),
        showIndication = true,
        onClick = onClick,
        cornerRadius = 20.dp,
    ) {
        ArrowPreference(
            title = source.title,
            summary = source.url,
        )
    }
}
