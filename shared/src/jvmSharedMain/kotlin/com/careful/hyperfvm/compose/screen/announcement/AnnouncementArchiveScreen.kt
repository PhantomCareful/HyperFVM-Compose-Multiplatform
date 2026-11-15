package com.careful.hyperfvm.compose.screen.announcement

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.component.card.TipsCard
import com.careful.hyperfvm.compose.data.announcement.AnnouncementArchive
import com.careful.hyperfvm.compose.data.announcement.AnnouncementCovers
import com.careful.hyperfvm.compose.data.announcement.AnnouncementEntry
import com.careful.hyperfvm.compose.data.announcement.AnnouncementRepository
import com.careful.hyperfvm.compose.navigation.LocalNavigator
import com.careful.hyperfvm.compose.navigation.Route
import com.careful.hyperfvm.compose.platform.LocalAppEnv
import com.careful.hyperfvm.compose.platform.LocalToaster
import com.careful.hyperfvm.compose.screen.ScreenScaffold
import kotlinx.coroutines.CancellationException
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.extended.Image as ImageIcon
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** 合集页的三种状态：加载中、已拿到、失败（可重试）。 */
private sealed interface ArchiveUiState {
    data object Loading : ArchiveUiState
    data class Ready(val archive: AnnouncementArchive) : ArchiveUiState
    data class Failed(val message: String) : ArchiveUiState
}

/** 列表里的日期只到天：同一期公告在一天内的具体时间对翻阅历史没有意义。 */
private val EntryDateFormat = SimpleDateFormat("yyyy年M月d日", Locale.CHINA).apply {
    timeZone = TimeZone.getTimeZone("Asia/Shanghai")
}

/** 年份筛选用的格式化器：和日期行同一个时区，跨年边界不会分错。 */
private val YearFormat = SimpleDateFormat("yyyy", Locale.CHINA).apply {
    timeZone = TimeZone.getTimeZone("Asia/Shanghai")
}

/**
 * 卡片几何：封面到卡片上、左、下三条边等距（[CardInset]），行高由封面决定。
 * 接口给的是约 3:1 的横版封面，按 2:1 裁切只切左右，中间的主视觉完整保留。
 */
private val CardInset = 12.dp
private val CoverWidth = 148.dp
private val CoverHeight = 74.dp
private val CoverCornerRadius = 10.dp

/** 封面与文字之间留一点呼吸，免得标题贴着图。 */
private val CoverTextGap = 12.dp

/**
 * 宽屏上的列表栏最大宽度。
 *
 * 卡片里只有一张小封面和一行标题，拉到 1280dp 宽会变成一条长带子；收窄居中后
 * 与正文页的「文档栏」同一个观感。手机上窗口本来就窄，不会触发。
 */
private val ArchiveMaxWidth = 720.dp

/** 排序菜单：索引 0 是倒序（最新在前，默认），索引 1 是顺序（最早在前）。 */
private val SortOptions = listOf("倒序查看（最新在前）", "顺序查看（最早在前）")

/**
 * 列表顶部的提示：合集与正文都是免登录接口拿的，连续快点容易被风控拦下；
 * 实在打不开还可以走详情页右上角的浏览器入口。
 */
private const val ArchiveTips =
    "当前为免登录获取内容，请勿频繁操作。如加载失败，请尝试重新加载或在公告详情页点击右上角的链接按钮通过浏览器查看。"

/**
 * 更新公告合集页：B 站「版本更新」文集里的全部历史公告，一期一张卡。
 *
 * 列表接口一次返回全部篇目（当前 139 篇），按发布时间排。顶栏一个菜单管两件事：
 * 排序（倒序最新在前，默认；或顺序最早在前）和年份筛选（可多选，年份从全部篇目
 * 里自动归出来）。下拉刷新重新拉列表，正文点开时才拉，并落在磁盘缓存里。
 *
 * 行样式沿用二级页内容卡：左侧大封面，右侧标题与日期。
 */
@Composable
fun AnnouncementArchiveScreen(onBack: () -> Unit) {
    val env = LocalAppEnv.current
    val toaster = LocalToaster.current
    val navigator = LocalNavigator.current
    val scrollBehavior = MiuixScrollBehavior()
    val pullToRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()

    var reloadToken by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<ArchiveUiState>(ArchiveUiState.Loading) }
    var refreshing by remember { mutableStateOf(false) }
    // 同一份回调喂两处：Android 端管触摸下拉，桌面端管顶栏上的刷新按钮。
    val requestRefresh: () -> Unit = {
        refreshing = true
        reloadToken += 1
    }
    // 默认最新在前：这是个「查看器」，多数人点进来只看最近一期。
    var newestFirst by rememberSaveable { mutableStateOf(true) }
    // 年份筛选是空集时表示「全部年份」；勾了某几年就只看那几年。
    var selectedYears by rememberSaveable { mutableStateOf(setOf<Int>()) }

    LaunchedEffect(reloadToken) {
        val refresh = reloadToken > 0
        if (!refresh) {
            // 内存里有列表就先上屏，后台再确认有没有新一期，避免每次进页面都白屏。
            AnnouncementRepository.cachedArchive()?.let { state = ArchiveUiState.Ready(it) }
            if (state !is ArchiveUiState.Ready) state = ArchiveUiState.Loading
        }
        try {
            state = ArchiveUiState.Ready(AnnouncementRepository.loadArchive(env, refresh))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (state is ArchiveUiState.Ready) {
                // 已有旧内容时刷新失败只提示一下，不把列表清空。
                toaster.show(e.message ?: "刷新失败")
            } else {
                state = ArchiveUiState.Failed(e.message ?: "公告合集加载失败。")
            }
        } finally {
            refreshing = false
        }
    }

    val archive = (state as? ArchiveUiState.Ready)?.archive

    // 年份从「拿到手的全部篇目」里归出来，最新的一年排最前，每项顺带带上当年的期数。
    val yearCounts = remember(archive) {
        archive?.entries?.groupingBy { yearOf(it) }?.eachCount().orEmpty()
    }
    val years = remember(yearCounts) { yearCounts.keys.sortedDescending() }
    // 刷新后如果某一年已经没有公告了，把它从筛选里摘掉，免得列表卡在空白。
    LaunchedEffect(years) {
        if (selectedYears.any { it !in years }) selectedYears = selectedYears.intersect(years.toSet())
    }

    val entries = remember(archive, newestFirst, selectedYears) {
        archive?.entries.orEmpty()
            .filter { selectedYears.isEmpty() || yearOf(it) in selectedYears }
            .let { if (newestFirst) it.asReversed() else it }
    }

    // 换了排序或筛选后回到顶部，否则会停在上一份列表的滚动位置上。
    LaunchedEffect(newestFirst, selectedYears) {
        listState.scrollToItem(0)
    }

    // 菜单分两组：上面排序互斥，下面年份可多选（「全部年份」就是清空筛选）。
    val menuEntries = remember(archive, years, yearCounts, newestFirst, selectedYears) {
        val orderItems = listOf(
            DropdownItem(
                text = SortOptions[0],
                selected = newestFirst,
                onClick = { newestFirst = true },
            ),
            DropdownItem(
                text = SortOptions[1],
                selected = !newestFirst,
                onClick = { newestFirst = false },
            ),
        )
        val yearItems = buildList {
            add(
                DropdownItem(
                    text = "全部年份",
                    summary = "共 ${archive?.entries?.size ?: 0} 期",
                    selected = selectedYears.isEmpty(),
                    onClick = { selectedYears = emptySet() },
                ),
            )
            years.forEach { year ->
                add(
                    DropdownItem(
                        text = "${year}年",
                        summary = "${yearCounts[year] ?: 0} 期",
                        selected = year in selectedYears,
                        onClick = {
                            selectedYears = if (year in selectedYears) {
                                selectedYears - year
                            } else {
                                selectedYears + year
                            }
                        },
                    ),
                )
            }
        }
        listOf(DropdownEntry(items = orderItems), DropdownEntry(items = yearItems))
    }

    ScreenScaffold(
        title = "更新公告",
        subtitle = when {
            archive == null -> ""
            selectedYears.isEmpty() -> "共 ${entries.size} 期"
            else -> "${selectedYears.sorted().joinToString("、") { "${it}年" }} · 共 ${entries.size} 期"
        },
        outerPadding = PaddingValues(0.dp),
        onBack = onBack,
        scrollBehavior = scrollBehavior,
        // 顶栏的滚动连接交给 PullToRefresh 统一编排，两边都挂会让折叠速度翻倍。
        attachScrollBehavior = false,
        // 桌面端没有下拉手势，刷新入口挂在顶栏右侧的按钮上。
        onRefresh = requestRefresh,
        refreshing = refreshing,
        actions = {
            // 排序与年份合成一个菜单：点某项不会收起（年份要多选），点空白处或再点图标
            // 才收起；菜单右缘对齐与点击震动都由组件内部处理。
            OverlayIconDropdownMenu(
                entries = menuEntries,
                collapseOnSelection = false,
            ) {
                Icon(imageVector = MiuixIcons.Sort, contentDescription = "排序与年份筛选")
            }
        },
    ) { padding ->
        when (val current = state) {
            ArchiveUiState.Loading -> ArchiveLoading(padding)

            is ArchiveUiState.Failed -> ArchiveFailed(
                padding = padding,
                message = current.message,
                onRetry = { reloadToken += 1 },
            )

            is ArchiveUiState.Ready -> PullToRefresh(
                isRefreshing = refreshing,
                onRefresh = requestRefresh,
                modifier = Modifier.fillMaxSize(),
                pullToRefreshState = pullToRefreshState,
                topAppBarScrollBehavior = scrollBehavior,
                contentPadding = PaddingValues(top = padding.calculateTopPadding()),
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                refreshTexts = listOf(
                    "下拉刷新公告",
                    "松开立即刷新",
                    "正在刷新",
                    "刷新完成",
                ),
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    // 宽屏（平板）把列表收窄居中；手机上两侧不会缩。
                    val sideGutter = if (maxWidth > ArchiveMaxWidth) (maxWidth - ArchiveMaxWidth) / 2 else 0.dp
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        // 列表仍铺满屏幕，两侧空白处的手势也能滚，只是卡片收进中间那一栏。
                        contentPadding = PaddingValues(
                            start = sideGutter,
                            end = sideGutter,
                            top = padding.calculateTopPadding() + 12.dp,
                            bottom = padding.calculateBottomPadding() + 24.dp,
                        ),
                    ) {
                        // 提示卡不参与筛选：不管看全部还是只看某几年，都先读一遍这段提醒。
                        item(key = "tips") {
                            TipsCard(tips = ArchiveTips)
                        }
                        items(items = entries, key = { it.cvId }) { entry ->
                            ArchiveEntryCard(
                                entry = entry,
                                onClick = { navigator.push(Route.AnnouncementArticle(entry.cvId)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveLoading(padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        InfiniteProgressIndicator()
        Text(
            text = "公告加载中",
            modifier = Modifier.padding(top = 12.dp),
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = MiuixTheme.textStyles.body2.fontSize,
        )
    }
}

@Composable
private fun ArchiveFailed(
    padding: PaddingValues,
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = padding.calculateTopPadding())
            .padding(horizontal = 12.dp),
    ) {
        Spacer(Modifier.height(24.dp))
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
 * 一期公告的卡片：左侧大封面、右侧标题与日期、末尾箭头，整卡可点。
 *
 * 不用 [ArrowPreference] 是因为它的标题不限行数：标题一旦比封面高，封面就会被居中，
 * 上下留白大于左侧留白，「三边等距」就守不住了。这里把标题压到两行，行高始终由封面决定。
 */
@Composable
private fun ArchiveEntryCard(
    entry: AnnouncementEntry,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            // 与 AboutContentCard 同款外距和圆角，只是换成整卡可点。
            .padding(bottom = 12.dp),
        cornerRadius = 20.dp,
        colors = CardDefaults.defaultColors(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(CardInset),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArchiveCover(entry.coverUrl)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = CoverTextGap),
            ) {
                Text(
                    text = entry.title,
                    fontSize = MiuixTheme.textStyles.headline1.fontSize,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = entrySummary(entry),
                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Icon(
                imageVector = MiuixIcons.Basic.ArrowRight,
                contentDescription = null,
                modifier = Modifier.size(width = 10.dp, height = 16.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        }
    }
}

/**
 * 封面缩略图：横版封面按 2:1 裁进圆角块，图片到位前显示占位图标。
 *
 * 进列表就按需下载（只下可见项），落盘后滚动回来直接读内存，不重复请求。
 */
@Composable
private fun ArchiveCover(url: String) {
    val env = LocalAppEnv.current
    var bitmap by remember(url) { mutableStateOf(AnnouncementCovers.cached(url)) }
    LaunchedEffect(url) {
        if (bitmap == null) bitmap = AnnouncementCovers.load(env, url)
    }

    Box(
        modifier = Modifier
            .size(width = CoverWidth, height = CoverHeight)
            .clip(RoundedCornerShape(CoverCornerRadius))
            .background(MiuixTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        val image: ImageBitmap? = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = MiuixIcons.ImageIcon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MiuixTheme.colorScheme.onBackgroundVariant,
            )
        }
    }
}

/** 摘要行：日期只到天，前面补「发表于」——一期公告在一天内的具体时间对翻阅历史没有意义。 */
private fun entrySummary(entry: AnnouncementEntry): String {
    if (entry.publishedAt <= 0L) return ""
    return "${EntryDateFormat.format(Date(entry.publishedAt * 1000L))}"
}

/** 一期公告属于哪一年（北京时间），年份筛选项就是这样归出来的。 */
private fun yearOf(entry: AnnouncementEntry): Int =
    YearFormat.format(Date(entry.publishedAt * 1000L)).toInt()
