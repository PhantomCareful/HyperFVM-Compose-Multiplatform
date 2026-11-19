package com.careful.hyperfvm.compose.data.cardCatalog

import com.careful.hyperfvm.compose.platform.Logs
import com.careful.hyperfvm.compose.platform.createHanTransliterator
import java.text.Normalizer
import java.util.Locale

/**
 * 防御卡的一个形态：一个名字 + 一张 40×50 的卡图。
 *
 * 每个形态的名字各自独立存储，不再像 Java 版那样把「主名以外的名字」拼成一条
 * strings.xml 常量（拿 "-" 拼接还会和名字里自带的 "·" 打架）。展示层要几行、
 * 用什么分隔符，当场决定。
 *
 * @param name 形态名，目录页第二行展示的就是它。
 * @param image 该形态的卡图 id（数据包 images/ 下的文件名），必须是 `x11*`。
 */
internal data class CardForm(
    val name: String,
    // 数据包内的图片 id：文件在运行时从本地数据包加载，随包安装包不再包含图片。
    val image: String,
)

/**
 * 一张防御卡，对应目录页里的一个入口（Java 版的一个 `card_card_data_index_*` 布局）。
 *
 * 数据表里只要按顺序列出全部 [CardForm]，排序用的拼音、字母分节、搜索索引全部由
 * [CardCatalog] 推导：增删改卡只改数据表，不需要新建布局或字符串资源。
 *
 * @param forms 全部形态，第一个是主名（目录排序与分节都按主名拼音）；至少一个。
 * @param keywords 额外的搜索词，原文与拼音都会被索引：别名、简称，或者直接写拼音。
 * @param pinyin 主名读音的纠正（无声调，音节可用空格分隔）。只在 ICU 读错多音字时用，
 *   例如「弹弹鸡」应写 `"tan tan ji"`。
 */
internal class DefenseCard(
    vararg forms: CardForm,
    val keywords: List<String> = emptyList(),
    val pinyin: String? = null,
) {
    /** 全部形态，顺序即目录页右侧卡图的排列顺序。 */
    val forms: List<CardForm> = forms.toList()

    init {
        require(this.forms.isNotEmpty()) { "DefenseCard 至少要有一个形态" }
    }

    /** 目录页第一行：主名。 */
    val primaryName: String get() = forms.first().name

    /** 目录页第二行：其余形态名用 " / " 连接；单一形态卡为空串。 */
    val aliasText: String get() = forms.drop(1).joinToString(" / ") { it.name }
}

/** 目录页的一个字母分节。[label] 是 "0"、单个大写字母，或 "#"。 */
internal data class CardSection(
    val label: String,
    val cards: List<DefenseCard>,
)

/**
 * 目录页顶部概览卡的数据口径（对应 Java 版 `CardDataLetterCatalogData.OverviewStats`）：
 * 总收录数是四张卡表之和，四个分表各取表长；
 * 生肖卡表里混着的星座卡（主名形如「白羊座精灵」）不算生肖卡，计数时剔除。
 */
internal data class CardCatalogOverview(
    val version: String,
    val totalCount: Int,
    val normalCount: Int,
    val goldenCount: Int,
    val fusionCount: Int,
    val zodiacCount: Int,
)

/**
 * 目录页的数据源：把 4 张静态卡表拼起来，按主名拼音排序、分节，并给搜索建索引。
 *
 * 拼音来自系统 ICU（`Han-Latin; Latin-ASCII`），不引入第三方拼音库；ICU 不可用时
 * 退化为原文排序（分节只剩 "#"），功能不崩。所有推导结果都在第一次访问时算好并缓存。
 */
internal object CardCatalog {

    /** 数字开头的分节标签。 */
    const val LabelDigit = "0"

    /** 无法归入 0-9 / A-Z 的分节标签。 */
    const val LabelOther = "#"

    /** 全部防御卡，顺序即各表内的登记顺序。 */
    val cards: List<DefenseCard> = NormalCards + FusionCards + GoldenCards + ZodiacCards

    /** 概览统计，第一次读取时算一次。 */
    val overview: CardCatalogOverview by lazy {
        // 星座卡的主名里带「座」，生肖卡表内只有它们含这个字，按主名现算即可；
        // Java 版写死的 13 已经和现表（12 张星座卡）对不上，这里不再沿用常量。
        val constellationCount = ZodiacCards.count { it.primaryName.contains("座") }
        CardCatalogOverview(
            version = CardCatalogDataVersion,
            totalCount = cards.size,
            normalCount = NormalCards.size,
            goldenCount = GoldenCards.size,
            fusionCount = FusionCards.size,
            zodiacCount = ZodiacCards.size - constellationCount,
        )
    }

    private val entries: List<IndexedCard> by lazy {
        cards.map(::IndexedCard).sortedBy { it.sortKey }
    }

    /**
     * 形态卡图 id → 所属防御卡。图片 id 在数据表内唯一，同一张图不会出现在两张卡里，
     * 所以可以拿任意形态的卡图反查出整张卡（含其余形态名与卡图）。
     */
    private val imageIndex: Map<String, DefenseCard> by lazy {
        buildMap {
            cards.forEach { card ->
                card.forms.forEach { form -> put(form.image, card) }
            }
        }
    }

    /** 按任意形态的卡图反查防御卡；未登记过的图返回 null。 */
    fun cardOf(image: String): DefenseCard? = imageIndex[image]

    /**
     * 按形态名精确查卡：仪表盘弹层里的名单写的是主名或别名，
     * 与目录页的模糊搜索不同，这里要求完全相等。
     */
    fun cardNamed(name: String): DefenseCard? {
        val key = name.trim()
        if (key.isEmpty()) return null
        return cards.firstOrNull { card -> card.forms.any { it.name == key } }
    }

    /** 按标签排好的分节，顺序 0 → A-Z → #，空桶不会出现。 */
    val sections: List<CardSection> by lazy {
        val buckets = LinkedHashMap<String, MutableList<DefenseCard>>()
        entries.forEach { entry ->
            buckets.getOrPut(entry.label) { mutableListOf() }.add(entry.card)
        }
        buckets.entries
            .sortedBy { labelOrder(it.key) }
            .map { (label, sectionCards) -> CardSection(label, sectionCards) }
    }

    /**
     * 按关键词过滤：
     * - 原文：任意形态名或 [DefenseCard.keywords] 包含输入（忽略大小写）；
     * - 拼音：任意形态名或关键字的紧凑拼音包含输入的紧凑形式，
     *   所以「小笼」命中原文、「xiaolong」命中拼音。
     */
    fun search(query: String): List<DefenseCard> {
        val text = query.trim().lowercase(Locale.ROOT)
        if (text.isEmpty()) return emptyList()
        val compact = compactOf(text)
        return entries
            .filter { entry ->
                entry.nameKeys.any { it.contains(text) } ||
                    (compact.isNotEmpty() && entry.pinyinKeys.any { it.contains(compact) })
            }
            .map { it.card }
    }

    /** 分节排序：0 在最前，# 在最后，中间是 A-Z。 */
    private fun labelOrder(label: String): Int = when (label) {
        LabelDigit -> -1
        LabelOther -> 26
        else -> label[0] - 'A'
    }
}

/**
 * 一张卡的排序与搜索索引。
 *
 * 拼音在构造时一次算完（按 1062 个形态名 + 关键字），之后搜索与排序都是纯字符串比较，
 * 输入时不会再有 ICU 开销。
 */
private class IndexedCard(val card: DefenseCard) {

    /** 原文键：全部形态名 + 额外关键词，小写。 */
    val nameKeys: List<String> = (card.forms.map { it.name } + card.keywords)
        .map { it.lowercase(Locale.ROOT) }
        .distinct()

    /** 拼音键：主名用卡片自带的读音纠正，其余文本交给 ICU。 */
    val pinyinKeys: List<String> =
        card.forms.mapIndexed { index, form ->
            pinyinOf(form.name, if (index == 0) card.pinyin else null)
        } + card.keywords.map { pinyinOf(it, null) }

    /** 排序键：主名拼音的大写紧凑形式；主名没有字母数字时为空串。 */
    val sortKey: String = pinyinOf(card.primaryName, card.pinyin).uppercase(Locale.ROOT)

    /** 分节标签。 */
    val label: String = when {
        sortKey.isEmpty() -> CardCatalog.LabelOther
        sortKey[0] in '0'..'9' -> CardCatalog.LabelDigit
        sortKey[0] in 'A'..'Z' -> sortKey[0].toString()
        else -> CardCatalog.LabelOther
    }
}

private const val LogTag = "CardCatalog"

/**
 * 汉字 → 无声调拼音的 ICU 转写器。
 *
 * `Han-Latin` 负责逐字转写（带声调），`Latin-ASCII` 把声调符号折叠掉；
 * TinyPinyin 之类的第三方库因此不再需要。平台实现（[createHanTransliterator]）
 * 保证返回的转写函数是线程安全的。
 */
private val hanToLatin: ((String) -> String)? by lazy {
    createHanTransliterator().also { created ->
        if (created == null) {
            Logs.w(LogTag, "ICU 拼音转写不可用，字母分节将退化为原文排序")
        }
    }
}

/** 文本 → 小写紧凑拼音。主名可以带 [override] 直接返回，其余文本走 ICU。 */
private fun pinyinOf(text: String, override: String?): String {
    if (override != null) return compactOf(override)
    val transliterate = hanToLatin ?: return compactOf(text)
    return compactOf(transliterate(text))
}

/**
 * 归一化成「小写 + 只留字母数字」：空格、连字符、间隔号、声调符号全部去掉，
 * 这样「xiǎo lóng bāo」「xiao-long-bao」都会变成同一个键。
 */
private fun compactOf(text: String): String {
    val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
    return buildString(decomposed.length) {
        decomposed.forEach { char ->
            when {
                // Latin-ASCII 被系统裁掉时声调会留在文本里，这里再兜一道。
                Character.getType(char) == Character.NON_SPACING_MARK.toInt() -> Unit
                char.isLetterOrDigit() -> append(char.lowercaseChar())
            }
        }
    }
}
