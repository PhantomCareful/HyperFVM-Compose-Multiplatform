package com.careful.hyperfvm.compose.data.image

/**
 * 一张数据图的静态元数据。
 *
 * [name] 是本地文件的基础名（不含目录，也不含 `_dark` / `_light` 深浅色变体后缀）。
 * Java 版每个分类是一个 Activity、每张图是一个 ArrowPreference；Compose 版把它们拍平成
 * 一个瀑布流，靠 [group] 在流里插分组标题。
 */
data class DataImageEntry(
    val name: String,
    val title: String,
    /** 卡片副标题：米鼠的图是 Q 群指令，其他数据图是作者署名。 */
    val summary: String? = null,
    /** 分类内的分组标题；同组连续的图片共享一个标题，null 表示不分组。 */
    val group: String? = null,
)

/** 数据图的一个分类，对应 Java 版的一个 `DataImage*Activity`。 */
data class DataImageCategory(
    /** 稳定标识，用作列表 key。 */
    val id: String,
    val title: String,
    /** 分类作者署名，显示在分类标题右侧。 */
    val author: String? = null,
    /** 分类说明，显示为分类下方的一张 tips 卡。 */
    val tips: String? = null,
    val images: List<DataImageEntry>,
)

/** 米鼠的图：Q 群机器人叫不出来时的兜底查看入口。 */
private const val TiramisuTip =
    "设置这个版块的目的是当米鼠有时候叫不出来时，您仍然可以在这里查看部分数据图。\n" +
        "图片更新可能不及时，当这里的图片和米鼠不一致时，请以米鼠提供的为准。"

private const val MouseHpTip = "数据图中的体力均为标准值，实际关卡内可能会修改百分比。"

private const val OthersTip =
    "这些数据图的作者仅代表开发者本人第一次在哪里看到这些数据。部分数据图经过开发者重新制作，" +
        "支持在App开启深色模式时展示深色版本的图片。"

/** 米鼠的图：一条 Q 群指令对应的副标题。 */
private fun tiramisuTip(command: String) = "Q群获取指令：@提拉米鼠 $command"

private val TiramisuImages = listOf(
    DataImageEntry("tiramisu_image_1_1", "周氪图", tiramisuTip("/周氪"), "氪金活动类"),
    DataImageEntry("tiramisu_image_1_2", "日氪图", tiramisuTip("/日氪"), "氪金活动类"),
    DataImageEntry("tiramisu_image_1_3_1", "消费图(0–100W)", tiramisuTip("/消费-1"), "氪金活动类"),
    DataImageEntry("tiramisu_image_1_3_2", "消费图(100–200W)", tiramisuTip("/消费-2"), "氪金活动类"),
    DataImageEntry("tiramisu_image_1_3_3", "消费图(200–300W)", tiramisuTip("/消费-3"), "氪金活动类"),
    DataImageEntry("tiramisu_image_1_4", "积分兑换图", tiramisuTip("/积分"), "氪金活动类"),
    DataImageEntry("tiramisu_image_1_5", "商城抢购图", tiramisuTip("/商城"), "氪金活动类"),
    DataImageEntry("tiramisu_image_1_6", "婚礼服装图", tiramisuTip("/婚礼"), "氪金活动类"),
    DataImageEntry("tiramisu_image_1_7", "假期兑换图", tiramisuTip("/假期"), "氪金活动类"),
    DataImageEntry("tiramisu_image_2_1", "三岛兑换图", tiramisuTip("/三岛"), "游戏资料类"),
    DataImageEntry("tiramisu_image_2_6", "施肥兑换图", tiramisuTip("/施肥"), "游戏资料类"),
    DataImageEntry("tiramisu_image_2_2", "悬赏声望图", tiramisuTip("/声望"), "游戏资料类"),
    DataImageEntry("tiramisu_image_2_3", "大赛奖励图", tiramisuTip("/大赛"), "游戏资料类"),
    // 云端大赛任务图还有 2–4 号，那三张只给 Java 版的旧入口用；Compose 版只挂 1 号，
    // 指令统一用 /大赛-X 指代整组。
    DataImageEntry("tiramisu_image_2_3_1", "大赛任务图", tiramisuTip("/大赛-X"), "游戏资料类"),
    DataImageEntry("tiramisu_image_2_5", "营地任务解谜图", tiramisuTip("/营地"), "游戏资料类"),
) + (1..16).map { level ->
    DataImageEntry(
        name = "tiramisu_image_2_4_$level",
        title = "VIP${level}权益图",
        summary = tiramisuTip("/VIP $level"),
        group = "游戏资料类",
    )
}

private val CardImages = listOf(
    DataImageEntry("data_image_card_0_1", "新卡速递", "看看最近出了哪些卡片"),
    DataImageEntry("data_image_card_0_2_1", "防御卡名称(Part1)", "包含：普通卡、融合卡、星座卡、纪念卡"),
    DataImageEntry("data_image_card_0_2_2", "防御卡名称(Part2)", "包含：生肖卡、至尊金卡"),
    DataImageEntry("data_image_card_0_3", "神级进化", "包含：合成金卡需要哪些材料卡"),
    DataImageEntry("data_image_card_1", "1.管线类"),
    DataImageEntry("data_image_card_2", "2.投掷类"),
    DataImageEntry("data_image_card_3", "3.(类)海星类&竞技类"),
    DataImageEntry("data_image_card_4", "4.增幅类"),
    DataImageEntry("data_image_card_5", "5.产能类"),
    DataImageEntry("data_image_card_6", "6.反弹类&承载类"),
    DataImageEntry("data_image_card_7", "7.专业防空"),
    DataImageEntry("data_image_card_8", "8.喷壶类"),
    DataImageEntry("data_image_card_9", "9.追踪类"),
    DataImageEntry("data_image_card_10", "10.杂项类"),
    DataImageEntry("data_image_card_11", "11.直线射击类"),
    DataImageEntry("data_image_card_12", "12.炸弹I类"),
    // 注意这张的名字带尾部下划线，与上面那张是两张不同的图，匹配变体时不能按前缀判断。
    DataImageEntry("data_image_card_12_", "12'.炸弹II类"),
    DataImageEntry("data_image_card_13", "13.“强力”辅助类"),
    DataImageEntry("data_image_card_14", "14.防御类"),
    DataImageEntry("data_image_card_15", "15.融合卡", "全新玩法"),
    DataImageEntry("data_image_card_16", "16.真爱结晶", "包含：结晶合成、强化、分解"),
    DataImageEntry("data_image_card_17", "17.食神谱", "包含：开启食神谱需要的卡片、加成、同时可开启食谱数"),
    DataImageEntry("data_image_card_18", "18.特别说明", "针对一些卡片的各种补充说明"),
)

private val WeaponAndGemImages = listOf(
    DataImageEntry("data_image_weapon_and_gem_0_1", "神级进化", "旧三神→新三神→超三神→补强宝石"),
    DataImageEntry("data_image_weapon_and_gem_1", "1.主武器"),
    DataImageEntry("data_image_weapon_and_gem_2", "2.副武器"),
    DataImageEntry("data_image_weapon_and_gem_3", "3.超级武器"),
    DataImageEntry("data_image_weapon_and_gem_4", "4.外观模型&子弹样式"),
    DataImageEntry("data_image_weapon_and_gem_5", "5.宝石强化&分解&进化返还"),
)

private val DecomposeAndGetTitles = listOf(
    "1.至尊金卡", "2.星座卡", "3.鸡年生肖卡", "4.狗年生肖卡", "5.猪年生肖卡", "6.鼠年生肖卡",
    "7.牛年生肖卡", "8.虎年生肖卡", "9.兔年生肖卡", "10.龙年生肖卡", "11.蛇年生肖卡", "12.马年生肖卡",
)

private val DecomposeAndGetImages = DecomposeAndGetTitles.mapIndexed { index, title ->
    DataImageEntry("data_image_decompose_and_get_${index + 1}", title)
}

private val MouseHpTitles = listOf(
    "1.美味岛&火山岛&浮空岛",
    "2.星际穿越&海底&转职&竞技岛",
    "3.暑假岛",
    "4.跨服远征–小兵",
    "5.魔塔蛋糕–小兵",
    "6.巅峰对决",
    "7.跨服远征–BOSS",
    "8.魔塔蛋糕–BOSS",
    "9.萌宠神殿",
    "10.特殊召唤物",
)

private val MouseHpImages = MouseHpTitles.mapIndexed { index, title ->
    DataImageEntry("data_image_mouse_hp_${index + 1}", title)
}

private val OthersImages = listOf(
    DataImageEntry("data_image_others_1", "1.跨服徽章掉落数据", "By Phantom_Careful"),
    DataImageEntry("data_image_others_2", "2.转运数据", "By 夏夜浅酌"),
    DataImageEntry("data_image_others_3", "3.大富翁", "By So陌路の"),
    DataImageEntry("data_image_others_4", "4.垫卡概率对照表", "By Phantom_Careful"),
    DataImageEntry("data_image_others_5", "5.宠物数据", "By So陌路の"),
    DataImageEntry("data_image_others_6", "6.世界BOSS段位数据", "By Phantom_Careful"),
    DataImageEntry("data_image_others_7", "7.角色等级数据", "By Phantom_Careful"),
    DataImageEntry("data_image_others_8", "8.游戏功能解锁条件对照表", "By 哪片花火"),
)

/**
 * 数据图全目录，顺序与 Java 版 `activity_data_image_index.xml` 的卡片顺序一致。
 *
 * 云端清单（`DataImagesUrl.m3u`）与 Java 版共用，里面的每一张图都能在这里找到基础名：
 * 89 张基础图，加上「其他数据图」6 张图的深浅色变体。只给 Java 版旧入口的几张
 * （如大赛任务图 2–4 号）不在目录里，拉取清单时会被过滤掉。
 */
val DataImageCategories: List<DataImageCategory> = listOf(
    DataImageCategory(
        id = "tiramisu",
        title = "米鼠的图",
        tips = TiramisuTip,
        images = TiramisuImages,
    ),
    DataImageCategory(
        id = "card",
        title = "防御卡数据图",
        author = "By So陌路の",
        images = CardImages,
    ),
    DataImageCategory(
        id = "weapon_and_gem",
        title = "武器宝石数据图",
        author = "By So陌路の",
        images = WeaponAndGemImages,
    ),
    DataImageCategory(
        id = "decompose_and_get",
        title = "分解兑换数据图",
        author = "By So陌路の",
        images = DecomposeAndGetImages,
    ),
    DataImageCategory(
        id = "mouse_hp",
        title = "老鼠血量数据图",
        author = "By So陌路の",
        tips = MouseHpTip,
        images = MouseHpImages,
    ),
    DataImageCategory(
        id = "others",
        title = "其他数据图",
        tips = OthersTip,
        images = OthersImages,
    ),
)

/** 全部基础图，按目录顺序展开。 */
val AllDataImages: List<DataImageEntry> = DataImageCategories.flatMap { it.images }
