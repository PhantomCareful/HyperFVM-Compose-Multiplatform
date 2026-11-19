package com.careful.hyperfvm.compose.component.itemQuery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.careful.hyperfvm.compose.component.card.TipsCard
import com.careful.hyperfvm.compose.component.image.PackImage
import com.careful.hyperfvm.compose.component.image.RemoteItemImage
import com.careful.hyperfvm.compose.data.datapack.PackImages
import com.careful.hyperfvm.compose.data.itemQuery.BagEntry
import com.careful.hyperfvm.compose.data.itemQuery.ItemBag
import com.careful.hyperfvm.compose.data.itemQuery.ItemEntry
import com.careful.hyperfvm.compose.data.itemQuery.ItemQueryRepository
import com.careful.hyperfvm.compose.platform.LocalAppEnv
import com.careful.hyperfvm.compose.ui.AppBottomSheet
import com.careful.hyperfvm.compose.ui.AppCard
import com.careful.hyperfvm.compose.ui.sheetCardColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 每次展示的结果条数；加载更多按这个步长追加。 */
private const val ResultPageSize = 10

/** 卡片入场动画时长：结果不能瞬间硬切出来。 */
private const val CardEnterDurationMillis = 220

/** 相邻两张卡片的入场动画错开间隔，同一页内依次淡入。 */
private const val CardEnterStaggerMillis = 25

/** 距列表底部还剩几条时开始追加，与 miuix Overscroll + Load More 示例同款。 */
private const val LoadMoreThreshold = 3

/** 追加前等回弹动画飞完：追加太快会让列表在回弹过程中「跳」一下。 */
private const val LoadMoreDelayMillis = 350L

/** 卡片右上角的道具图尺寸：图床图标本身不大，64dp 已是两倍密度下的清晰尺寸。 */
private val ItemIconSize = 64.dp

/** 礼包候选行里的道具图尺寸：一行一个，比卡片主图小得多。 */
private val BagIconSize = 24.dp

/** 礼包候选行之间的间距。 */
private val BagRowGap = 6.dp

/** 弹层内卡片之间的标准间距，与其它弹层一致。 */
private val SheetCardGap = 10.dp

/**
 * 查询按钮的内边距：上下跟 TextField 的默认 insideMargin 对齐（各 16dp）。
 *
 * 两边字体同为 17sp，内容高度一样，按钮就能和输入框严格等高，
 * 而且随字体缩放一起变高，不靠固定 dp 也不靠量尺寸。
 */
private val SearchButtonInsideMargin = PaddingValues(horizontal = 16.dp, vertical = 16.dp)

/**
 * 弹层内分节小标题的内边距，取 miuix 官方 BottomSheet 示例的写法
 * （比卡片左缘再缩进 16dp、上下各 8dp），而不是贴着卡片边缘。
 */
private val SheetSectionTitleMargin = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

/**
 * 道具查询弹层：顶部输入框 + 查询按钮固定不动，点击查询后才在 card_desc.xml 里检索，
 * 结果在下方滚动，每次最多 10 条，滑到接近底部再自动追加下一页。
 *
 * 检索与加载更多分开：输入过程中不触发任何检索，只有点「查询」或键盘搜索键才提交；
 * 加载更多沿用 miuix 官方 Overscroll + Load More 的做法——overScroll 走默认的
 * MiuixOverscrollEffect，由 [snapshotFlow] 监听「离底部不足几条」，等回弹飞完再追加，
 * 期间在列表尾部显示进度行。
 */
@Composable
fun ItemQuerySheet(show: Boolean, onDismissRequest: () -> Unit) {
    val env = LocalAppEnv.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var input by remember { mutableStateOf("") }
    var keyword by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ItemEntry>>(emptyList()) }
    var visibleCount by remember { mutableIntStateOf(ResultPageSize) }
    var searched by remember { mutableStateOf(false) }
    var querying by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // 已经播过入场动画的卡片 key：滚出屏幕再滚回来不重播，新检索时清空。
    val enteredKeys = remember { mutableSetOf<String>() }

    fun submit() {
        val text = input.trim()
        if (text.isEmpty() || querying) return
        focusManager.clearFocus()
        scope.launch {
            querying = true
            error = null
            val all = runCatching { ItemQueryRepository.ensureLoaded(env) }
                .getOrElse { e ->
                    error = e.message ?: "读取道具表失败"
                    null
                }
            if (all != null) {
                val found = withContext(Dispatchers.Default) {
                    ItemQueryRepository.search(all, text)
                }
                // 先在旧列表上滚回顶部，再把内容换掉：新结果直接出现在顶部，
                // 不会先接在旧的滚动位置显示一帧再跳回去。
                listState.scrollToItem(0)
                results = found
                keyword = text
                searched = true
                enteredKeys.clear()
                visibleCount = ResultPageSize
            }
            querying = false
        }
    }

    // 接近底部时追加下一页：只在查询结果存在时监听，loadingMore 期间不重复触发。
    LaunchedEffect(listState, searched) {
        if (!searched) return@LaunchedEffect
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible >= 0 && lastVisible >= info.totalItemsCount - LoadMoreThreshold
        }
            .distinctUntilChanged()
            .collect { nearEnd ->
                if (nearEnd && !loadingMore && !querying && visibleCount < results.size) {
                    loadingMore = true
                    delay(LoadMoreDelayMillis)
                    visibleCount = (visibleCount + ResultPageSize).coerceAtMost(results.size)
                    loadingMore = false
                }
            }
    }

    AppBottomSheet(
        show = show,
        title = "道具查询",
        onDismissRequest = onDismissRequest,
        // 内容区滚到尽头后继续下滑不把弹层拖下去（长列表很容易误触）；
        // 拖顶部手柄和返回手势仍可关闭。
        enableNestedScroll = false,
        onDismissFinished = {
            // 弹层彻底收起后才清空：收起动画期间内容还得在。
            input = ""
            keyword = ""
            results = emptyList()
            searched = false
            querying = false
            loadingMore = false
            error = null
            enteredKeys.clear()
        },
    ) {
        val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        // 每次翻页只把当前窗口内的结果交给列表，避免一次组合一万多条卡片。
        val shown = remember(results, visibleCount) { results.take(visibleCount) }
        Column(modifier = Modifier.fillMaxWidth()) {
            // 输入行固定在弹层顶部，只有下面的结果列表滚动；
            // 列本身不加 weight，列表 weight(fill = false) 在内容不足一屏时贴合内容高度，
            // 弹层不会被撑满全屏。
            SearchRow(
                input = input,
                onInputChange = { input = it },
                onSubmit = { submit() },
            )
            Spacer(modifier = Modifier.size(SheetCardGap))
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentPadding = PaddingValues(bottom = 16.dp + navigationBarBottom),
                verticalArrangement = Arrangement.spacedBy(SheetCardGap),
            ) {
                when {
                    // 只有首次查询（还没出过任何结果）才把列表换成进度行。
                    // 已经出过结果时若也换成单行进度，弹层会从接近全屏瞬间塌成一行再弹回，
                    // 观感上像整个 BottomSheet 消失又出现（重复点查询时的闪屏）。
                    querying && !searched -> item(key = "querying") { QueryingRow() }

                    error != null -> item(key = "error") {
                        TipsCard(tips = error.orEmpty(), outerPadding = PaddingValues(0.dp))
                    }

                    !searched -> item(key = "hint") {
                        TipsCard(tips = QueryHint, outerPadding = PaddingValues(0.dp))
                    }

                    results.isEmpty() -> item(key = "empty") {
                        TipsCard(tips = "没有找到包含「$keyword」的道具。", outerPadding = PaddingValues(0.dp))
                    }

                    else -> {
                        item(key = "count") {
                            // 结果条数按 miuix 的分节小标题排（subtitle + 缩进），不贴着卡片边缘；
                            // 重复查询期间旧结果先留着，用这一行提示正在检索，列表高度不变。
                            SmallTitle(
                                text = if (querying) "正在检索…" else "共 ${results.size} 条结果",
                                insideMargin = SheetSectionTitleMargin,
                            )
                        }
                        itemsIndexed(
                            items = shown,
                            key = { index, entry -> "${entry.id}#$index" },
                        ) { index, entry ->
                            ItemQueryCard(entry = entry, index = index, enteredKeys = enteredKeys)
                        }
                        if (visibleCount < results.size) {
                            item(key = "loadMore") { LoadMoreRow(loading = loadingMore) }
                        } else if (results.size > ResultPageSize) {
                            item(key = "allLoaded") { AllLoadedRow(count = results.size) }
                        }
                    }
                }
            }
        }
    }
}

/** 顶部输入行：输入框占满剩余宽度，右侧是查询按钮。 */
@Composable
private fun SearchRow(
    input: String,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 标准 Placeholder & SingleLine 用法：label 作为占位符、单行输入，
        // 内边距与高度都交给 TextFieldDefaults（上下各 16dp），不另外压矮。
        TextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier.weight(1f),
            // 与其它弹层的输入框同款底色，避免和弹层面板糊成一片。
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = sheetCardColor(),
                labelColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            ),
            label = "输入道具名称",
            useLabelAsPlaceholder = true,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        )
        Spacer(modifier = Modifier.width(SheetCardGap))
        // 查询中不禁用：禁用的瞬间会刷成灰底灰字，出结果再刷回主色，看起来像在闪；
        // 重复点击由 submit 里的 querying 守卫拦下。
        Button(
            onClick = onSubmit,
            enabled = input.isNotBlank(),
            insideMargin = SearchButtonInsideMargin,
            colors = ButtonDefaults.buttonColorsPrimary(),
        ) {
            Text(text = "查询", style = MiuixTheme.textStyles.button)
        }
    }
}

/** 查询进行中：解析整表 + 检索都在后台线程，这里只给一行进度。 */
@Composable
private fun QueryingRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InfiniteProgressIndicator(size = 20.dp)
        Text(
            text = "正在检索…",
            modifier = Modifier.padding(start = 8.dp),
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        )
    }
}

/**
 * 结果卡片：首行是名称 / 类型标签 + 右上角道具图，描述与元信息从图片下方整宽铺开。
 *
 * 首次出现时淡入并轻微上移，同一页内按位置依次错开；[enteredKeys] 记录播过动画的卡片，
 * 滚动复用组合或重组时不再重播。
 */
@Composable
private fun ItemQueryCard(entry: ItemEntry, index: Int, enteredKeys: MutableSet<String>) {
    val itemKey = "${entry.id}#$index"
    val animateEnter = remember(itemKey) { itemKey !in enteredKeys }
    if (!animateEnter) {
        ItemCardBody(entry = entry)
        return
    }
    // 先登记再播动画：即使动画中途被滚出屏幕，回来也不再重播。
    LaunchedEffect(itemKey) { enteredKeys += itemKey }
    val enterState = remember(itemKey) {
        MutableTransitionState(initialState = false).apply { targetState = true }
    }
    val delayMillis = (index % ResultPageSize) * CardEnterStaggerMillis
    AnimatedVisibility(
        visibleState = enterState,
        enter = fadeIn(tween(durationMillis = CardEnterDurationMillis, delayMillis = delayMillis)) +
            slideInVertically(
                animationSpec = tween(durationMillis = CardEnterDurationMillis, delayMillis = delayMillis),
            ) { it / 8 },
    ) {
        ItemCardBody(entry = entry)
    }
}

/** 卡片的实际内容，与入场动画解耦。 */
@Composable
private fun ItemCardBody(entry: ItemEntry) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        insideMargin = BasicComponentDefaults.InsideMargin,
        colors = CardDefaults.defaultColors(color = sheetCardColor()),
        // 只保留按下时的高亮回应（showIndication），pressFeedbackType 仍用默认的 None：
        // Sink 会让整块卡片下沉，长卡片拖动时观感很差。
        showIndication = true,
        onClick = {},
    ) {
        // 名字 / 类型与图片先排成一行，描述就能从图片下方整宽铺开，
        // 不会在图片左侧被挤成窄条；左侧文字比图片矮时以图片中线为准居中。
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MiuixTheme.textStyles.headline1,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entry.type.isNotEmpty()) {
                    Spacer(modifier = Modifier.size(4.dp))
                    TypeTag(text = entry.type)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            ItemQueryIcon(
                id = entry.id,
                // 武器类（主武器 / 副武器 / 超级武器）取 0x24 高清图，其余按默认策略。
                preferHighRes = entry.type.contains("武器"),
                modifier = Modifier.size(ItemIconSize),
            )
        }
        val bag = entry.bag
        val description = entry.description
        if (bag != null) {
            Spacer(modifier = Modifier.size(6.dp))
            BagContent(bag = bag)
        } else if (description != null) {
            Spacer(modifier = Modifier.size(6.dp))
            // 描述不限行数：部分道具的说明很长，按实际内容完整展示。
            Text(
                text = description,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
        }
        val meta = metaLine(entry)
        if (meta != null) {
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = meta,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 礼包（随机 / 几率掉落）的内容：首行说明（末尾的绑定说明并进括号里）+ 候选道具列表。
 *
 * 不再把「名称x数量」堆成一大段描述，每行拆成图标、名称与数量，长礼包也能一眼扫完。
 */
@Composable
private fun BagContent(bag: ItemBag) {
    val note = bag.note
    Text(
        text = if (note != null) "${bag.header}（$note）" else bag.header,
        style = MiuixTheme.textStyles.body2,
        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
    )
    Spacer(modifier = Modifier.size(BagRowGap))
    Column(verticalArrangement = Arrangement.spacedBy(BagRowGap)) {
        bag.entries.forEach { BagRow(entry = it) }
    }
}

/** 礼包的一行：道具图、名称、数量；道具表里查不到名字的行（如点券）只留名字。 */
@Composable
private fun BagRow(entry: BagEntry) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val itemId = entry.itemId
        if (itemId != null) {
            ItemQueryIcon(
                id = itemId,
                preferHighRes = entry.isWeapon,
                modifier = Modifier.size(BagIconSize),
            )
        } else {
            // 空出图标位，后面的名字仍然对齐。
            Spacer(modifier = Modifier.size(BagIconSize))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = entry.name,
            modifier = Modifier.weight(1f),
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceContainer,
        )
        val quantity = entry.quantity
        if (quantity != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "×$quantity",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                maxLines = 1,
            )
        }
    }
}

/**
 * 类型标签：小圆角底色，长类型名（如「爱情徽章|5级」）单行截断。
 *
 * 配色跟着重要提示卡（TipsCard）走：miuix 静态色板下 TipsCard 用的就是 tertiaryContainer，
 * Monet 动态取色也统一落到 tertiary 系，两套主题都是「低饱和底色 + 同系深色文字」。
 */
@Composable
private fun TypeTag(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MiuixTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        style = MiuixTheme.textStyles.footnote1,
        color = MiuixTheme.colorScheme.onTertiaryContainer,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** 元信息行：能量 / 体力有值才显示，两项都没有时整行不渲染。 */
private fun metaLine(entry: ItemEntry): String? = buildList {
    entry.energy?.let { add("能量 $it") }
    entry.hp?.let { add("体力 $it") }
}.joinToString(" · ").takeIf { it.isNotEmpty() }

/**
 * 道具图：数据包内有图的核心道具直接读包（离线可用、出图更快），
 * 其余按道具 id 从图床取图；数据包与图床都没有时画统一的占位图标。
 *
 * [preferHighRes] 为 true（武器类）时先取 0x24 高清版；普通道具则是 0x14 缺图
 * （404）时自动退到 0x24。
 */
@Composable
private fun ItemQueryIcon(id: String, preferHighRes: Boolean, modifier: Modifier) {
    val inPack = remember(id) { PackImages.file(id) != null }
    if (inPack) {
        PackImage(
            id = id,
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    } else {
        RemoteItemImage(
            id = id,
            modifier = modifier,
            contentScale = ContentScale.Fit,
            preferHighRes = preferHighRes,
        )
    }
}

/** 列表尾部的加载行：还有下一页时显示，追加完成后由外层换成「已显示全部」。 */
@Composable
private fun LoadMoreRow(loading: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            InfiniteProgressIndicator(size = 20.dp)
            Text(
                text = "正在加载更多…",
                modifier = Modifier.padding(start = 8.dp),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
        } else {
            Text(
                text = "上滑加载更多",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
        }
    }
}

/** 全部结果都已展示时的收尾行。 */
@Composable
private fun AllLoadedRow(count: Int) {
    Text(
        text = "已显示全部 $count 条结果",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        textAlign = TextAlign.Center,
        style = MiuixTheme.textStyles.body2,
        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
    )
}

/** 未查询时的说明：明确检索范围与「点了才查」的交互。 */
private const val QueryHint =
    "输入关键词后点击「查询」，将按道具名称检索；结果每次展示 10 条，滑到底部会自动加载更多。"
