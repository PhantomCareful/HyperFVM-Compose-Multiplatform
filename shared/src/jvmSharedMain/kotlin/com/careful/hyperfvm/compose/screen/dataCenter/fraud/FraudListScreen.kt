package com.careful.hyperfvm.compose.screen.dataCenter.fraud

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.careful.hyperfvm.compose.component.card.TipsCard
import com.careful.hyperfvm.compose.data.fraud.FraudAccountBrief
import com.careful.hyperfvm.compose.data.fraud.FraudQueryException
import com.careful.hyperfvm.compose.data.fraud.FraudRepository
import com.careful.hyperfvm.compose.navigation.LocalNavigator
import com.careful.hyperfvm.compose.navigation.Route
import com.careful.hyperfvm.compose.platform.LocalToaster
import com.careful.hyperfvm.compose.platform.isDesktopPlatform
import com.careful.hyperfvm.compose.screen.ScreenScaffold
import com.careful.hyperfvm.compose.screen.about.AboutContentCard
import com.careful.hyperfvm.compose.ui.AppSearchCancelAction
import kotlinx.coroutines.CancellationException
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.SearchBarDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 搜索框上下留白；与 InputField 最小高度一起构成顶栏 bottomContent 的高度。 */
private val SearchFieldVerticalPadding = 6.dp

/** 已有缓存时后台刷新失败的轻提示；刷新成功不打扰。 */
private const val RefreshFailedTips = "名单刷新失败，当前为上次缓存"

/**
 * 查黑系统的名单页：展示全部已标记的 QQ 号（QQ 号 / 行骗次数 / 最后行骗时间）。
 *
 * 布局照搬防御卡目录页：搜索框作为顶栏的 bottomContent，输入时在本地名单里过滤；
 * 刷新照搬数据图合集，重新拉一次 `getFraudAccountList`（Android 端是下拉，
 * 桌面端是顶栏右侧的按钮）。
 * 点任意一条压栈到骗子详情页，详情页按 QQ 号自行查询。
 */
@Composable
fun FraudListScreen(onBack: () -> Unit) {
    val navigator = LocalNavigator.current
    val focusManager = LocalFocusManager.current
    val toaster = LocalToaster.current
    val scrollBehavior = MiuixScrollBehavior()

    var query by rememberSaveable { mutableStateOf("") }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    val keyword = query.trim()

    // 有缓存时先渲染旧名单，后台再拉一次；首次进入才显示加载态。
    var entries by remember { mutableStateOf(FraudRepository.cachedList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadToken by remember { mutableIntStateOf(0) }
    var refreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    // 同一份回调喂两处：Android 端管触摸下拉，桌面端管顶栏上的刷新按钮。
    val requestRefresh: () -> Unit = {
        refreshing = true
        reloadToken += 1
    }

    LaunchedEffect(reloadToken) {
        error = null
        // 进页时可能已有上次的缓存：刷新失败就保留旧名单，只轻提示一次，不让页面变成失败态。
        val hasCache = entries != null
        try {
            entries = FraudRepository.queryList()
        } catch (e: CancellationException) {
            throw e
        } catch (e: FraudQueryException) {
            if (hasCache) {
                toaster.show(RefreshFailedTips)
            } else {
                error = e.message
            }
        } catch (_: Exception) {
            if (hasCache) {
                toaster.show(RefreshFailedTips)
            } else {
                error = "名单加载失败，请稍后重试。"
            }
        } finally {
            refreshing = false
        }
    }

    val filtered = remember(entries, keyword) {
        val list = entries.orEmpty()
        if (keyword.isEmpty()) list else list.filter { it.qq.contains(keyword) }
    }

    fun closeSearch() {
        searchExpanded = false
        query = ""
        focusManager.clearFocus()
    }

    val navigationEventState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = searchExpanded,
        onBackCompleted = { closeSearch() },
    )

    // 进入搜索态时把大标题平滑收起，给名单腾出高度（与防御卡目录页一致）。
    LaunchedEffect(searchExpanded) {
        if (!searchExpanded) return@LaunchedEffect
        val limit = scrollBehavior.state.heightOffsetLimit
        if (!limit.isFinite() || limit >= 0f) return@LaunchedEffect
        animate(scrollBehavior.state.heightOffset, limit) { value, _ ->
            scrollBehavior.state.heightOffset = value
        }
    }

    val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    ScreenScaffold(
        title = "查黑系统",
        outerPadding = PaddingValues(0.dp),
        onBack = onBack,
        scrollBehavior = scrollBehavior,
        // 顶栏的滚动连接交给 PullToRefresh 统一编排（同数据图合集）。
        attachScrollBehavior = false,
        // 桌面端没有下拉手势，刷新入口挂在顶栏右侧的按钮上。
        onRefresh = requestRefresh,
        refreshing = refreshing,
        bottomContent = {
            FraudSearchBar(
                query = query,
                expanded = searchExpanded,
                onQueryChange = { query = it },
                onSearch = { focusManager.clearFocus() },
                onExpandedChange = { searchExpanded = it },
                onCancel = { closeSearch() },
            )
        },
    ) { padding ->
        PullToRefresh(
            isRefreshing = refreshing,
            onRefresh = requestRefresh,
            modifier = Modifier.fillMaxSize(),
            pullToRefreshState = pullToRefreshState,
            topAppBarScrollBehavior = scrollBehavior,
            // 指示器避开折叠顶栏：内容顶部内边距正好就是顶栏（含搜索框）高度。
            contentPadding = PaddingValues(top = padding.calculateTopPadding()),
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            refreshTexts = listOf(
                "下拉刷新名单",
                "松开立即刷新",
                "正在刷新",
                "刷新完成",
            ),
        ) {
            when (entries) {
                null if error == null -> FraudListLoading(padding)
                null -> FraudListFailed(
                    padding = padding,
                    message = error.orEmpty(),
                    onRetry = { reloadToken += 1 },
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding() + 8.dp,
                        bottom = maxOf(padding.calculateBottomPadding(), imeBottomPadding) + 12.dp,
                    ),
                ) {
                    if (filtered.isEmpty()) {
                        item(key = "empty") {
                            Text(
                                text = if (keyword.isEmpty()) {
                                    // 桌面端没有下拉手势，提示词跟着换动作。
                                    if (isDesktopPlatform) {
                                        "暂无已标记的 QQ 号，点右上角按钮刷新试试"
                                    } else {
                                        "暂无已标记的 QQ 号，下拉刷新试试"
                                    }
                                } else {
                                    "没有找到匹配的 QQ 号"
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 48.dp),
                                color = MiuixTheme.colorScheme.onBackgroundVariant,
                                fontSize = MiuixTheme.textStyles.body2.fontSize,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    items(items = filtered, key = { "fraud-${it.qq}" }) { entry ->
                        FraudListCard(
                            entry = entry,
                            onClick = {
                                // 先收起键盘，详情页转场时不会带着输入法一起滑。
                                focusManager.clearFocus()
                                navigator.push(Route.IcuFraud(entry.qq))
                            },
                        )
                    }
                }
            }
        }
    }
}

/** 一条名单：QQ 号做标题，行骗次数与最后行骗时间做摘要。 */
@Composable
private fun FraudListCard(entry: FraudAccountBrief, onClick: () -> Unit) {
    AboutContentCard {
        ArrowPreference(
            title = entry.qq,
            summary = if (entry.lastFraudTime.isEmpty()) {
                "行骗 ${entry.fraudCount} 次"
            } else {
                "行骗 ${entry.fraudCount} 次 · 最后行骗 ${entry.lastFraudTime}"
            },
            onClick = onClick,
        )
    }
}

@Composable
private fun FraudListLoading(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = padding.calculateTopPadding()),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 与骗子详情页的加载指示器同款：miuix 默认无限指示器（20dp 细环 + 轨道点）。
            InfiniteProgressIndicator()
            Text(
                text = "正在加载名单",
                modifier = Modifier.padding(top = 12.dp),
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = MiuixTheme.textStyles.body2.fontSize,
            )
        }
    }
}

@Composable
private fun FraudListFailed(padding: PaddingValues, message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = padding.calculateTopPadding() + 24.dp)
            .padding(horizontal = 12.dp),
    ) {
        TipsCard(tips = message, outerPadding = PaddingValues(0.dp))
        TextButton(
            text = "重新加载",
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            colors = ButtonDefaults.textButtonColorsPrimary(),
        )
    }
}

/**
 * 顶栏里的搜索框：输入框 + 展开时从右侧滑出的取消动作，与防御卡目录页同款。
 * 轻透磨砂下取消是文字，柔光玻璃下是玻璃圆钮上的 ×。
 */
@Composable
private fun FraudSearchBar(
    query: String,
    expanded: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(
                    horizontal = SearchBarDefaults.InsideMargin.width,
                    vertical = SearchFieldVerticalPadding,
                ),
        ) {
            InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = { onSearch() },
                expanded = expanded,
                // 点击输入框获得焦点时由 InputField 回调 true，搜索态随之展开。
                onExpandedChange = onExpandedChange,
                label = "搜索QQ号",
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandHorizontally() + slideInHorizontally(initialOffsetX = { it }),
            exit = shrinkHorizontally() + slideOutHorizontally(targetOffsetX = { it }),
        ) {
            AppSearchCancelAction(
                onCancel = onCancel,
                modifier = Modifier.padding(end = 16.dp),
            )
        }
    }
}
