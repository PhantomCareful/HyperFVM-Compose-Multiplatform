package com.careful.hyperfvm.compose.data.cardCatalog

import com.careful.hyperfvm.compose.shared.*


/**
 * 生肖卡与星座卡（Java 版 `card_data_4` 表）的静态卡表，共 157 张。
 *
 * 每张卡按 base_name 归并列出全部形态：一行一个 [CardForm]，名字与卡图一一对应，
 * 顺序就是目录页里的显示顺序（也是 Java 版单卡布局里 ImageView 的顺序）。
 * 增删改卡直接改这一段：字母分节、拼音排序、搜索索引都由 [CardCatalog] 自动推导，
 * 不需要再新增任何布局文件或 strings.xml 常量。
 *
 * 额外的搜索别名写在 [DefenseCard.keywords]；ICU 读错多音字的卡用 [DefenseCard.pinyin] 纠正。
 */
internal val ZodiacCards: List<DefenseCard> = listOf(
    DefenseCard(
        CardForm("天秤座精灵", Res.drawable.x11130074),
        CardForm("天秤座战将", Res.drawable.x1113007e),
        CardForm("天秤座星宿", Res.drawable.x1113000f),
    ),
    DefenseCard(
        CardForm("呆呆鸡", Res.drawable.x11130304),
        CardForm("水遁呆呆鸡", Res.drawable.x1113030e),
        CardForm("贤圣呆呆鸡", Res.drawable.x1113030f),
    ),
    DefenseCard(
        CardForm("二哈汪", Res.drawable.x11130104),
        CardForm("秘银二哈汪", Res.drawable.x1113010e),
        CardForm("金刚二哈汪", Res.drawable.x1113010f),
    ),
    DefenseCard(
        CardForm("双枪喵", Res.drawable.x11120260),
        CardForm("左轮双枪喵", Res.drawable.x1112026e),
        CardForm("等离子双枪喵", Res.drawable.x1112026f),
    ),
    DefenseCard(
        CardForm("散弹牛", Res.drawable.x11120490),
        CardForm("威武散弹牛", Res.drawable.x1112049e),
        CardForm("霸气散弹牛", Res.drawable.x1112049f),
    ),
    DefenseCard(
        CardForm("威风虎", Res.drawable.x11120720),
        CardForm("爆气威风虎", Res.drawable.x1112072e),
        CardForm("连弩威风虎", Res.drawable.x1112072f),
    ),
    DefenseCard(
        CardForm("射手座精灵", Res.drawable.x111300e4),
        CardForm("射手座战将", Res.drawable.x111300ee),
        CardForm("射手座星宿", Res.drawable.x111300ef),
    ),
    DefenseCard(
        CardForm("砰砰鸡", Res.drawable.x111303e4),
        CardForm("固岩砰砰鸡", Res.drawable.x111303ee),
        CardForm("暗殇砰砰鸡", Res.drawable.x111303ef),
    ),
    DefenseCard(
        CardForm("狩猎汪", Res.drawable.x11130114),
        CardForm("丛林狩猎汪", Res.drawable.x1113011e),
        CardForm("部落狩猎汪", Res.drawable.x1113011f),
    ),
    DefenseCard(
        CardForm("猪猪猎手", Res.drawable.x11120130),
        CardForm("青铜猪猪猎手", Res.drawable.x1112013e),
        CardForm("王者猪猪猎手", Res.drawable.x1112013f),
    ),
    DefenseCard(
        CardForm("包包龙", Res.drawable.x11122320),
        CardForm("懵懂包包龙", Res.drawable.x1112232e),
        CardForm("觉醒包包龙", Res.drawable.x1112232f),
    ),
    DefenseCard(
        CardForm("激光汪", Res.drawable.x11130164),
        CardForm("高频激光汪", Res.drawable.x1113016e),
        CardForm("超频激光汪", Res.drawable.x1113016f),
    ),
    DefenseCard(
        CardForm("枪塔喵", Res.drawable.x11120190),
        CardForm("榴弹枪塔喵", Res.drawable.x1112019e),
        CardForm("射线枪塔喵", Res.drawable.x1112019f),
    ),
    DefenseCard(
        CardForm("弩箭牛", Res.drawable.x11120300),
        CardForm("精英弩箭牛", Res.drawable.x1112030e),
        CardForm("暴力弩箭牛", Res.drawable.x1112030f),
    ),
    DefenseCard(
        CardForm("勺勺兔", Res.drawable.x11122050),
        CardForm("增强勺勺兔", Res.drawable.x1112205e),
        CardForm("盖世勺勺兔", Res.drawable.x1112205f),
    ),
    DefenseCard(
        CardForm("窃蛋龙", Res.drawable.x11122120),
        CardForm("蓝角窃蛋龙", Res.drawable.x1112212e),
        CardForm("钢爪窃蛋龙", Res.drawable.x1112212f),
    ),
    DefenseCard(
        CardForm("幻影蛇", Res.drawable.x11122450),
        CardForm("羽翼幻影蛇", Res.drawable.x1112245e),
        CardForm("金盔幻影蛇", Res.drawable.x1112245f),
    ),
    DefenseCard(
        CardForm("金乌马", Res.drawable.x11122620),
        CardForm("太阳金乌马", Res.drawable.x1112262e),
        CardForm("日冕金乌马", Res.drawable.x1112262f),
    ),
    DefenseCard(
        CardForm("双鱼座精灵", Res.drawable.x111300c4),
        CardForm("双鱼座战将", Res.drawable.x111300d4),
        CardForm("双鱼座星宿", Res.drawable.x111300cf),
    ),
    DefenseCard(
        CardForm("弹弹鸡", Res.drawable.x111303c4),
        CardForm("寒冰弹弹鸡", Res.drawable.x111303ce),
        CardForm("月光弹弹鸡", Res.drawable.x111303cf),
        // ICU 会把「弹弹」读成 dan dan，这里按词组读音纠正。
        pinyin = "tan tan ji",
    ),
    DefenseCard(
        CardForm("机械汪", Res.drawable.x11130124),
        CardForm("改装机械汪", Res.drawable.x1113012e),
        CardForm("自律机械汪", Res.drawable.x1113012f),
    ),
    DefenseCard(
        CardForm("投弹猪", Res.drawable.x11120090),
        CardForm("獠牙投弹猪", Res.drawable.x1112009e),
        CardForm("振金投弹猪", Res.drawable.x1112009f),
    ),
    DefenseCard(
        CardForm("飞鱼喵", Res.drawable.x11120200),
        CardForm("河滨飞鱼喵", Res.drawable.x1112020e),
        CardForm("深海飞鱼喵", Res.drawable.x1112020f),
    ),
    DefenseCard(
        CardForm("壮壮牛", Res.drawable.x11120380),
        CardForm("蛮力壮壮牛", Res.drawable.x1112038e),
        CardForm("乾坤壮壮牛", Res.drawable.x1112038f),
    ),
    DefenseCard(
        CardForm("投篮虎", Res.drawable.x11120540),
        CardForm("职业投篮虎", Res.drawable.x1112054e),
        CardForm("球星投篮虎", Res.drawable.x1112054f),
    ),
    DefenseCard(
        CardForm("铛铛虎", Res.drawable.x11120570),
        CardForm("速热铛铛虎", Res.drawable.x1112057e),
        CardForm("微波铛铛虎", Res.drawable.x1112057f),
    ),
    DefenseCard(
        CardForm("霜霜蛇", Res.drawable.x11122510),
        CardForm("雪花霜霜蛇", Res.drawable.x1112251e),
        CardForm("玄冰霜霜蛇", Res.drawable.x1112251f),
    ),
    DefenseCard(
        CardForm("猪猪料理机", Res.drawable.x11131200),
        CardForm("猪猪搅拌机", Res.drawable.x1113120e),
        CardForm("猪猪破壁机", Res.drawable.x1113120f),
    ),
    DefenseCard(
        CardForm("陀螺喵", Res.drawable.x11132200),
        CardForm("极光陀螺喵", Res.drawable.x1113220e),
        CardForm("金翼陀螺喵", Res.drawable.x1113220f),
    ),
    DefenseCard(
        CardForm("查克拉兔", Res.drawable.x11122020),
        CardForm("上忍查克拉兔", Res.drawable.x1112202e),
        CardForm("影级查克拉兔", Res.drawable.x1112202f),
    ),
    DefenseCard(
        CardForm("厨师虎", Res.drawable.x11120590),
        CardForm("银牌厨师虎", Res.drawable.x1112059e),
        CardForm("金牌厨师虎", Res.drawable.x1112059f),
    ),
    DefenseCard(
        CardForm("星星兔", Res.drawable.x11120820),
        CardForm("科技星星兔", Res.drawable.x1112082e),
        CardForm("宇宙星星兔", Res.drawable.x1112082f),
    ),
    DefenseCard(
        CardForm("烟花虎", Res.drawable.x11120780),
        CardForm("冷光烟花虎", Res.drawable.x1112078e),
        CardForm("礼炮烟花虎", Res.drawable.x1112078f),
    ),
    DefenseCard(
        CardForm("风车龙", Res.drawable.x11122290),
        CardForm("暴击风车龙", Res.drawable.x1112229e),
        CardForm("迅猛风车龙", Res.drawable.x1112229f),
    ),
    DefenseCard(
        CardForm("便便汪", Res.drawable.x11150054),
        CardForm("白银便便汪", Res.drawable.x1115005e),
        CardForm("黄金便便汪", Res.drawable.x1115005f),
    ),
    DefenseCard(
        CardForm("饼干汪", Res.drawable.x11150024),
        CardForm("冲锋饼干汪", Res.drawable.x1115002e),
        CardForm("加农饼干汪", Res.drawable.x1115002f),
    ),
    DefenseCard(
        CardForm("盾盾汪", Res.drawable.x11150044),
        CardForm("钢化盾盾汪", Res.drawable.x1115004e),
    ),
    DefenseCard(
        CardForm("金牛座精灵", Res.drawable.x11190154),
        CardForm("金牛座战将", Res.drawable.x1119015e),
        CardForm("金牛座星宿", Res.drawable.x1119015f),
    ),
    DefenseCard(
        CardForm("暖炉汪", Res.drawable.x11190164),
        CardForm("高温暖炉汪", Res.drawable.x1119016e),
        CardForm("火热暖炉汪", Res.drawable.x1119016f),
    ),
    DefenseCard(
        CardForm("能量喵", Res.drawable.x11190254),
        CardForm("蓝焰能量喵", Res.drawable.x1119025e),
        CardForm("樱红能量喵", Res.drawable.x1119025f),
    ),
    DefenseCard(
        CardForm("坩埚蛇", Res.drawable.x11122570),
        CardForm("灵力坩埚蛇", Res.drawable.x1112257e),
        CardForm("幻术坩埚蛇", Res.drawable.x1112257f),
    ),
    DefenseCard(
        CardForm("猪猪加强器", Res.drawable.x11120110),
        CardForm("猪猪信号塔", Res.drawable.x1112011e),
        CardForm("猪猪发射站", Res.drawable.x1112011f),
    ),
    DefenseCard(
        CardForm("香料虎", Res.drawable.x11120560),
        CardForm("海洋香料虎", Res.drawable.x1112056e),
        CardForm("魔力香料虎", Res.drawable.x1112056f),
    ),
    DefenseCard(
        CardForm("精灵龙", Res.drawable.x11122270),
        CardForm("蛋筒精灵龙", Res.drawable.x1112227e),
        CardForm("樱桃精灵龙", Res.drawable.x1112227f),
    ),
    DefenseCard(
        CardForm("五行蛇", Res.drawable.x11122380),
        CardForm("通灵五行蛇", Res.drawable.x1112238e),
        CardForm("泰斗五行蛇", Res.drawable.x1112238f),
    ),
    DefenseCard(
        CardForm("魔杖蛇", Res.drawable.x11122530),
        CardForm("青木魔杖蛇", Res.drawable.x1112253e),
        CardForm("凤羽魔杖蛇", Res.drawable.x1112253f),
    ),
    DefenseCard(
        CardForm("音盒马", Res.drawable.x11122830),
        CardForm("叮咚音盒马", Res.drawable.x1112283e),
        CardForm("玲珑音盒马", Res.drawable.x1112283f),
    ),
    DefenseCard(
        CardForm("炎焱兔", Res.drawable.x11120790),
        CardForm("火火炎焱兔", Res.drawable.x1112079e),
        CardForm("燚燚炎焱兔", Res.drawable.x1112079f),
    ),
    DefenseCard(
        CardForm("战旗马", Res.drawable.x11122610),
        CardForm("飞天战旗马", Res.drawable.x1139062e),
        CardForm("燎原战旗马", Res.drawable.x1139062f),
    ),
    DefenseCard(
        CardForm("双子座精灵", Res.drawable.x11110054),
        CardForm("双子座战将", Res.drawable.x11110064),
        CardForm("双子座星宿", Res.drawable.x1111005f),
    ),
    DefenseCard(
        CardForm("咕咕鸡", Res.drawable.x11111324),
        CardForm("萤火咕咕鸡", Res.drawable.x1111132e),
        CardForm("梦幻咕咕鸡", Res.drawable.x1111132f),
    ),
    DefenseCard(
        CardForm("暖暖鸡", Res.drawable.x11110354),
        CardForm("焰羽暖暖鸡", Res.drawable.x1111035e),
        CardForm("日耀暖暖鸡", Res.drawable.x1111035f),
    ),
    DefenseCard(
        CardForm("火焰牛", Res.drawable.x11120310),
        CardForm("幽蓝火焰牛", Res.drawable.x1112031e),
        CardForm("幻紫火焰牛", Res.drawable.x1112031f),
    ),
    DefenseCard(
        CardForm("花火龙", Res.drawable.x11122100),
        CardForm("灼灼花火龙", Res.drawable.x1112210e),
        CardForm("炽焰花火龙", Res.drawable.x1112210f),
    ),
    DefenseCard(
        CardForm("禅心马", Res.drawable.x11390660),
        CardForm("一念禅心马", Res.drawable.x1139066e),
        CardForm("明镜禅心马", Res.drawable.x1139066f),
    ),
    DefenseCard(
        CardForm("钱罐猪", Res.drawable.x11120160),
        CardForm("彩晶钱罐猪", Res.drawable.x1112016e),
        CardForm("招财钱罐猪", Res.drawable.x1112016f),
    ),
    DefenseCard(
        CardForm("罐罐牛", Res.drawable.x11120440),
        CardForm("五香罐罐牛", Res.drawable.x1112044e),
        CardForm("香辣罐罐牛", Res.drawable.x1112044f),
    ),
    DefenseCard(
        CardForm("烈火虎", Res.drawable.x11120740),
        CardForm("煤气烈火虎", Res.drawable.x1112074e),
        CardForm("燃油烈火虎", Res.drawable.x1112074f),
    ),
    DefenseCard(
        CardForm("布丁汪", Res.drawable.x111911d0),
        CardForm("双拼布丁汪", Res.drawable.x111911de),
        CardForm("什锦布丁汪", Res.drawable.x111911df),
    ),
    DefenseCard(
        CardForm("凉粉牛", Res.drawable.x11120320),
        CardForm("焦糖凉粉牛", Res.drawable.x1112032e),
        CardForm("山楂凉粉牛", Res.drawable.x1112032f),
    ),
    DefenseCard(
        CardForm("盘盘鸡", Res.drawable.x11370044),
        CardForm("王者盘盘鸡", Res.drawable.x1137004e),
        CardForm("超神盘盘鸡", Res.drawable.x1137004f),
    ),
    DefenseCard(
        CardForm("猫猫盘", Res.drawable.x11370050),
        CardForm("光能猫猫盘", Res.drawable.x1137005e),
        CardForm("无敌猫猫盘", Res.drawable.x1137005f),
    ),
    DefenseCard(
        CardForm("跳跳鸡", Res.drawable.x111302a4),
        CardForm("动感跳跳鸡", Res.drawable.x111302ae),
        CardForm("魔音跳跳鸡", Res.drawable.x111302af),
    ),
    DefenseCard(
        CardForm("防空喵", Res.drawable.x111302b0),
        CardForm("钢盔防空喵", Res.drawable.x111302be),
        CardForm("狂暴防空喵", Res.drawable.x111302bf),
    ),
    DefenseCard(
        CardForm("弹簧虎", Res.drawable.x11120660),
        CardForm("飞行弹簧虎", Res.drawable.x1112066e),
        CardForm("机器弹簧虎", Res.drawable.x1112066f),
        // 「弹簧」是 tan huang，不是 dan huang。
        pinyin = "tan huang hu",
    ),
    DefenseCard(
        CardForm("泡泡龙", Res.drawable.x11122180),
        CardForm("蓝鳍泡泡龙", Res.drawable.x1112218e),
        CardForm("彩色泡泡龙", Res.drawable.x1112218f),
    ),
    DefenseCard(
        CardForm("奶茶猪", Res.drawable.x11131140),
        CardForm("紫薯奶茶猪", Res.drawable.x1113114e),
        CardForm("爆弹奶茶猪", Res.drawable.x1113114f),
    ),
    DefenseCard(
        CardForm("科技喵", Res.drawable.x11132140),
        CardForm("坦克科技喵", Res.drawable.x1113214e),
        CardForm("重坦科技喵", Res.drawable.x1113214f),
    ),
    DefenseCard(
        CardForm("烈焰龙", Res.drawable.x11122110),
        CardForm("火山烈焰龙", Res.drawable.x1112211e),
        CardForm("岩浆烈焰龙", Res.drawable.x1112211f),
    ),
    DefenseCard(
        CardForm("狮子座精灵", Res.drawable.x11930074),
        CardForm("狮子座战将", Res.drawable.x1193007e),
        CardForm("狮子座星宿", Res.drawable.x1193007f),
    ),
    DefenseCard(
        CardForm("转转鸡", Res.drawable.x11930374),
        CardForm("五彩转转鸡", Res.drawable.x1193037e),
        CardForm("王室转转鸡", Res.drawable.x1193037f),
    ),
    DefenseCard(
        CardForm("可乐汪", Res.drawable.x11930084),
        CardForm("冰摇可乐汪", Res.drawable.x1193008e),
        CardForm("星杯可乐汪", Res.drawable.x1193008f),
    ),
    DefenseCard(
        CardForm("元气牛", Res.drawable.x11120500),
        CardForm("泡泡元气牛", Res.drawable.x1112050e),
        CardForm("酷酷元气牛", Res.drawable.x1112050f),
    ),
    DefenseCard(
        CardForm("巫蛊蛇", Res.drawable.x11122470),
        CardForm("暗黑巫蛊蛇", Res.drawable.x1112247e),
        CardForm("秘术巫蛊蛇", Res.drawable.x1112247f),
    ),
    DefenseCard(
        CardForm("巨蟹座精灵", Res.drawable.x11130064),
        CardForm("巨蟹座战将", Res.drawable.x111300de),
        CardForm("巨蟹座星宿", Res.drawable.x111300df),
    ),
    DefenseCard(
        CardForm("忍忍鸡", Res.drawable.x11130364),
        CardForm("疾风忍忍鸡", Res.drawable.x1113036e),
        CardForm("幻影忍忍鸡", Res.drawable.x1113036f),
    ),
    DefenseCard(
        CardForm("飞盘汪", Res.drawable.x11430034),
        CardForm("大厨飞盘汪", Res.drawable.x1143003e),
        CardForm("名厨飞盘汪", Res.drawable.x1143003f),
    ),
    DefenseCard(
        CardForm("铁甲飞镖猪", Res.drawable.x11131160),
        CardForm("银甲飞镖猪", Res.drawable.x1113116e),
        CardForm("金甲飞镖猪", Res.drawable.x1113116f),
    ),
    DefenseCard(
        CardForm("海盗兔", Res.drawable.x11120920),
        CardForm("首领海盗兔", Res.drawable.x1112092e),
        CardForm("洛克斯海贼兔", Res.drawable.x1112092f),
    ),
    DefenseCard(
        CardForm("雅典娜守护", Res.drawable.x11930090),
        CardForm("雅典娜圣衣", Res.drawable.x1193009e),
        CardForm("雅典娜光辉", Res.drawable.x1193009f),
    ),
    DefenseCard(
        CardForm("火箭猪", Res.drawable.x11120150),
        CardForm("运载火箭猪", Res.drawable.x1112015e),
        CardForm("反重力火箭猪", Res.drawable.x1112015f),
    ),
    DefenseCard(
        CardForm("魔法猪", Res.drawable.x11120120),
        CardForm("冰霜魔法猪", Res.drawable.x1112012e),
        CardForm("暴雪元素猪", Res.drawable.x1112012f),
    ),
    DefenseCard(
        CardForm("招财喵", Res.drawable.x11120210),
        CardForm("贵族招财喵", Res.drawable.x1112021e),
        CardForm("御守招财喵", Res.drawable.x1112021f),
    ),
    DefenseCard(
        CardForm("雪球兔", Res.drawable.x11120830),
        CardForm("见习雪球兔", Res.drawable.x1112083e),
        CardForm("导师雪球兔", Res.drawable.x1112083f),
    ),
    DefenseCard(
        CardForm("冰晶龙", Res.drawable.x11122190),
        CardForm("四棱冰晶龙", Res.drawable.x1112219e),
        CardForm("独角冰晶龙", Res.drawable.x1112219f),
    ),
    DefenseCard(
        CardForm("金刚马", Res.drawable.x11122660),
        CardForm("魔铃金刚马", Res.drawable.x1112266e),
        CardForm("梵音金刚马", Res.drawable.x1112266f),
    ),
    DefenseCard(
        CardForm("仙笛马", Res.drawable.x11122900),
        CardForm("悠扬仙笛马", Res.drawable.x1112290e),
        CardForm("天籁仙笛马", Res.drawable.x1112290f),
    ),
    DefenseCard(
        CardForm("萌海马", Res.drawable.x11122790),
        CardForm("嘟嘟萌海马", Res.drawable.x1112279e),
        CardForm("咕噜萌海马", Res.drawable.x1112279f),
    ),
    DefenseCard(
        CardForm("笼草马", Res.drawable.x11122960),
        CardForm("青芽笼草马", Res.drawable.x1112296e),
        CardForm("吞天笼草马", Res.drawable.x1112296f),
    ),
    DefenseCard(
        CardForm("风力空投猪", Res.drawable.x11120140),
        CardForm("导弹空投猪", Res.drawable.x1112014e),
        CardForm("涡轮空投猪", Res.drawable.x1112014f),
    ),
    DefenseCard(
        CardForm("电流虎", Res.drawable.x11120580),
        CardForm("磁铁电流虎", Res.drawable.x1112058e),
        CardForm("氢能电流虎", Res.drawable.x1112058f),
    ),
    DefenseCard(
        CardForm("霹雳马", Res.drawable.x11122640),
        CardForm("闪电霹雳马", Res.drawable.x1112264e),
        CardForm("雷霆霹雳马", Res.drawable.x1112264f),
    ),
    DefenseCard(
        CardForm("归元马", Res.drawable.x11122710),
        CardForm("时空归元马", Res.drawable.x1112271e),
        CardForm("幻境归元马", Res.drawable.x1112271f),
    ),
    DefenseCard(
        CardForm("吞噬龙", Res.drawable.x11122090),
        CardForm("幼年吞噬龙", Res.drawable.x1112209e),
        CardForm("成年吞噬龙", Res.drawable.x1112209f),
    ),
    DefenseCard(
        CardForm("三指兔", Res.drawable.x11120860),
        CardForm("开罗三指兔", Res.drawable.x1112086e),
        CardForm("胡夫三指兔", Res.drawable.x1112086f),
    ),
    DefenseCard(
        CardForm("结界马", Res.drawable.x11122680),
        CardForm("虚空结界马", Res.drawable.x1112268e),
        CardForm("镜反结界马", Res.drawable.x1139063f),
    ),
    DefenseCard(
        CardForm("功夫汪", Res.drawable.x11130370),
        CardForm("铁皮功夫汪", Res.drawable.x1113037e),
        CardForm("金甲功夫汪", Res.drawable.x1113037f),
    ),
    DefenseCard(
        CardForm("拳皇马", Res.drawable.x11122810),
        CardForm("格斗拳皇马", Res.drawable.x1112281e),
        CardForm("传奇拳皇马", Res.drawable.x1112281f),
    ),
    DefenseCard(
        CardForm("糖人马", Res.drawable.x11122720),
        CardForm("香芋糖人马", Res.drawable.x1112272e),
        CardForm("缤纷糖人马", Res.drawable.x1112272f),
    ),
    DefenseCard(
        CardForm("阴阳蛇", Res.drawable.x11122600),
        CardForm("伯仲阴阳蛇", Res.drawable.x1112260e),
        CardForm("龙虎阴阳蛇", Res.drawable.x1112260f),
    ),
    DefenseCard(
        CardForm("焚寂马", Res.drawable.x11122670),
        CardForm("星火焚寂马", Res.drawable.x1112267e),
        CardForm("烈日焚寂马", Res.drawable.x1112267f),
    ),
    DefenseCard(
        CardForm("天蝎座精灵", Res.drawable.x111300a4),
        CardForm("天蝎座战将", Res.drawable.x111300ce),
    ),
    DefenseCard(
        CardForm("工程猪", Res.drawable.x111310a0),
        CardForm("高级工程猪", Res.drawable.x111310ae),
        CardForm("科幻工程猪", Res.drawable.x111310af),
    ),
    DefenseCard(
        CardForm("双刃蛇", Res.drawable.x11122460),
        CardForm("武士双刃蛇", Res.drawable.x1112246e),
        CardForm("游侠双刃蛇", Res.drawable.x1112246f),
    ),
    DefenseCard(
        CardForm("元素蛇", Res.drawable.x11122550),
        CardForm("烈暴元素蛇", Res.drawable.x1112255e),
        CardForm("流星元素蛇", Res.drawable.x1112255f),
    ),
    DefenseCard(
        CardForm("御风马", Res.drawable.x11122650),
        CardForm("净化御风马", Res.drawable.x1112265e),
        CardForm("菩提御风马", Res.drawable.x1112265f),
    ),
    DefenseCard(
        CardForm("云霞马", Res.drawable.x11122840),
        CardForm("绵绵云霞马", Res.drawable.x1112284e),
        CardForm("绚丽云霞马", Res.drawable.x1112284f),
    ),
    DefenseCard(
        CardForm("回旋虎", Res.drawable.x11120670),
        CardForm("高能回旋虎", Res.drawable.x1112067e),
        CardForm("宗师回旋虎", Res.drawable.x1112067f),
    ),
    DefenseCard(
        CardForm("大师兔", Res.drawable.x11120810),
        CardForm("黑带大师兔", Res.drawable.x1112081e),
        CardForm("功夫大师兔", Res.drawable.x1112081f),
    ),
    DefenseCard(
        CardForm("赖皮蛇", Res.drawable.x11122400),
        CardForm("宝石赖皮蛇", Res.drawable.x1112240e),
        CardForm("大王赖皮蛇", Res.drawable.x1112240f),
    ),
    DefenseCard(
        CardForm("炎凰马", Res.drawable.x11122780),
        CardForm("白炽炎凰马", Res.drawable.x1112278e),
        CardForm("焚天炎凰马", Res.drawable.x1112278f),
    ),
    DefenseCard(
        CardForm("喷壶汪", Res.drawable.x11130190),
        CardForm("高压喷壶汪", Res.drawable.x1113019e),
        CardForm("连射喷壶汪", Res.drawable.x1113019f),
    ),
    DefenseCard(
        CardForm("派派鸡", Res.drawable.x111303a4),
        CardForm("少校派派鸡", Res.drawable.x111303ae),
        CardForm("将军派派鸡", Res.drawable.x111303af),
    ),
    DefenseCard(
        CardForm("小猪米花机", Res.drawable.x11120100),
        CardForm("巧克力米花机", Res.drawable.x1112010e),
        CardForm("金箔米花机", Res.drawable.x1112010f),
    ),
    DefenseCard(
        CardForm("喷气牛", Res.drawable.x11120400),
        CardForm("恒温喷气牛", Res.drawable.x1112040e),
        CardForm("无限喷气牛", Res.drawable.x1112040f),
    ),
    DefenseCard(
        CardForm("卖萌喵", Res.drawable.x111311a0),
        CardForm("核能卖萌喵", Res.drawable.x111311ae),
        CardForm("原子卖萌喵", Res.drawable.x111311af),
    ),
    DefenseCard(
        CardForm("法师蛇", Res.drawable.x11122390),
        CardForm("幽冥法师蛇", Res.drawable.x1112239e),
        CardForm("奥术法师蛇", Res.drawable.x1112239f),
    ),
    DefenseCard(
        CardForm("导弹蛇", Res.drawable.x11122410),
        CardForm("舰地导弹蛇", Res.drawable.x1112241e),
        CardForm("洲际导弹蛇", Res.drawable.x1112241f),
    ),
    DefenseCard(
        CardForm("镭射喵", Res.drawable.x11130330),
        CardForm("荧光镭射喵", Res.drawable.x1113033e),
        CardForm("电音镭射喵", Res.drawable.x1113033f),
    ),
    DefenseCard(CardForm("摩羯座精灵", Res.drawable.x11130234)),
    DefenseCard(
        CardForm("龙珠果", Res.drawable.x11122080),
        CardForm("烈火龙珠果", Res.drawable.x1112208e),
        CardForm("燃爆龙珠果", Res.drawable.x1112208f),
    ),
    DefenseCard(
        CardForm("奇门马", Res.drawable.x11122850),
        CardForm("玄妙奇门马", Res.drawable.x1112285e),
        CardForm("神术奇门马", Res.drawable.x1112285f),
    ),
    DefenseCard(
        CardForm("水瓶座精灵", Res.drawable.x11120074),
        CardForm("水瓶座战将", Res.drawable.x1112007e),
        CardForm("水瓶座星宿", Res.drawable.x1112007f),
    ),
    DefenseCard(
        CardForm("雷暴猪", Res.drawable.x11120170),
        CardForm("离子雷暴猪", Res.drawable.x1112017e),
        CardForm("电磁雷暴猪", Res.drawable.x1112017f),
    ),
    DefenseCard(
        CardForm("烛阴龙", Res.drawable.x11122350),
        CardForm("青莲烛阴龙", Res.drawable.x1112235e),
        CardForm("圣火烛阴龙", Res.drawable.x1112235f),
    ),
    DefenseCard(
        CardForm("爆裂蛇", Res.drawable.x11122370),
        CardForm("凤梨爆裂蛇", Res.drawable.x1112237e),
        CardForm("西瓜爆裂蛇", Res.drawable.x1112237f),
    ),
    DefenseCard(
        CardForm("芥末牛", Res.drawable.x11120360),
        CardForm("微辣芥末牛", Res.drawable.x1112036e),
        CardForm("辛辣芥末牛", Res.drawable.x1112036f),
    ),
    DefenseCard(
        CardForm("冰弹喵", Res.drawable.x11121040),
        CardForm("爆裂冰弹喵", Res.drawable.x1112104e),
        CardForm("旋风冰弹喵", Res.drawable.x1112104f),
    ),
    DefenseCard(CardForm("白羊座精灵", Res.drawable.x11120084)),
    DefenseCard(
        CardForm("酋长汪", Res.drawable.x11120080),
        CardForm("天使酋长汪", Res.drawable.x1112008e),
        CardForm("金翼酋长汪", Res.drawable.x1112008f),
    ),
    DefenseCard(
        CardForm("逗猫棒", Res.drawable.x11120290),
        CardForm("金鱼逗猫棒", Res.drawable.x1112029e),
        CardForm("鼠鼠逗猫棒", Res.drawable.x1112029f),
    ),
    DefenseCard(
        CardForm("贪吃兔", Res.drawable.x11120800),
        CardForm("肥肥贪吃兔", Res.drawable.x1112080e),
        CardForm("暴走贪吃兔", Res.drawable.x1112080f),
    ),
    DefenseCard(
        CardForm("娇娇虎", Res.drawable.x11120550),
        CardForm("贵族娇娇虎", Res.drawable.x1112055e),
        CardForm("皇室娇娇虎", Res.drawable.x1112055f),
    ),
    DefenseCard(
        CardForm("蛋蛋兔", Res.drawable.x11120870),
        CardForm("智能蛋蛋兔", Res.drawable.x1112087e),
        CardForm("外星蛋蛋兔", Res.drawable.x1112087f),
    ),
    DefenseCard(
        CardForm("醉翁马", Res.drawable.x11122970),
        CardForm("醇香醉翁马", Res.drawable.x1112297e),
        CardForm("御品醉翁马", Res.drawable.x1112297f),
    ),
    DefenseCard(
        CardForm("爆炸汪", Res.drawable.x11120234),
        CardForm("时尚爆炸汪", Res.drawable.x1112023e),
        CardForm("魔幻爆炸汪", Res.drawable.x1112023f),
    ),
    DefenseCard(
        CardForm("转龙壶", Res.drawable.x11390700),
        CardForm("充能转龙壶", Res.drawable.x1139070e),
        CardForm("巨星转龙壶", Res.drawable.x1139070f),
    ),
    DefenseCard(
        CardForm("幻幻鸡", Res.drawable.x113903d4),
        CardForm("学者幻幻鸡", Res.drawable.x113903de),
        CardForm("导师幻幻鸡", Res.drawable.x113903df),
    ),
    DefenseCard(
        CardForm("百变蛇", Res.drawable.x11390730),
        CardForm("孪生百变蛇", Res.drawable.x1139073e),
        CardForm("双子百变蛇", Res.drawable.x1139073f),
    ),
    DefenseCard(
        CardForm("顽皮龙", Res.drawable.x11390710),
        CardForm("捣蛋顽皮龙", Res.drawable.x1139071e),
        CardForm("神奇顽皮龙", Res.drawable.x1139071f),
    ),
    DefenseCard(
        CardForm("天使猪", Res.drawable.x11390500),
        CardForm("魔法天使猪", Res.drawable.x1139050e),
        CardForm("神力天使猪", Res.drawable.x1139050f),
    ),
    DefenseCard(
        CardForm("星穹马", Res.drawable.x11390610),
        CardForm("璀璨星穹马", Res.drawable.x1139061e),
        CardForm("浩瀚星穹马", Res.drawable.x1139061f),
    ),
    DefenseCard(
        CardForm("蛇蛇酒", Res.drawable.x11322050),
        CardForm("仙露蛇蛇酒", Res.drawable.x1132205e),
        CardForm("琼浆蛇蛇酒", Res.drawable.x1132205f),
    ),
    DefenseCard(
        CardForm("萤火蛇", Res.drawable.x11122420),
        CardForm("星辉萤火蛇", Res.drawable.x1112242e),
        CardForm("月芒萤火蛇", Res.drawable.x1112242f),
    ),
    DefenseCard(
        CardForm("爆爆鸡", Res.drawable.x11120334),
        CardForm("疯狂爆爆鸡", Res.drawable.x1112033e),
        CardForm("酷炫爆爆鸡", Res.drawable.x1112033f),
    ),
    DefenseCard(
        CardForm("清障猪", Res.drawable.x11120340),
        CardForm("拉环清障猪", Res.drawable.x1112034e),
        CardForm("核能清障猪", Res.drawable.x1112034f),
    ),
    DefenseCard(
        CardForm("旋风牛", Res.drawable.x11120370),
        CardForm("强压旋风牛", Res.drawable.x1112037e),
        CardForm("极速旋风牛", Res.drawable.x1132037f),
    ),
    DefenseCard(
        CardForm("碎冰喵", Res.drawable.x11120250),
        CardForm("闹钟碎冰喵", Res.drawable.x1112025e),
        CardForm("发条碎冰喵", Res.drawable.x1112025f),
    ),
    DefenseCard(
        CardForm("治愈喵", Res.drawable.x11120180),
        CardForm("武装治愈喵", Res.drawable.x1112018e),
        CardForm("全能治愈喵", Res.drawable.x1112018f),
    ),
    DefenseCard(
        CardForm("傀儡马", Res.drawable.x11122630),
        CardForm("机关傀儡马", Res.drawable.x1112263e),
        CardForm("灵枢傀儡马", Res.drawable.x1112263f),
    ),
    DefenseCard(
        CardForm("大福虎", Res.drawable.x11120610),
        CardForm("草莓大福虎", Res.drawable.x1112061e),
        CardForm("可可大福虎", Res.drawable.x1112061f),
    ),
    DefenseCard(
        CardForm("逆转牛", Res.drawable.x11120520),
        CardForm("匀速逆转牛", Res.drawable.x1112052e),
        CardForm("光速逆转牛", Res.drawable.x1112052f),
    ),
    DefenseCard(
        CardForm("处女座精灵", Res.drawable.x11240014),
        CardForm("处女座战将", Res.drawable.x1124002e),
        CardForm("处女座星宿", Res.drawable.x1124002f),
    ),
    DefenseCard(
        CardForm("祥龙环", Res.drawable.x11240030),
        CardForm("点睛祥龙环", Res.drawable.x1124003e),
        CardForm("紫鳞祥龙环", Res.drawable.x1124003f),
    ),
    DefenseCard(
        CardForm("守能汪", Res.drawable.x11210024),
        CardForm("蓝焰守能汪", Res.drawable.x1121002e),
        CardForm("耀金守能汪", Res.drawable.x1121002f),
    ),
    DefenseCard(
        CardForm("喵喵炉", Res.drawable.x11230024),
        CardForm("靓粉喵喵炉", Res.drawable.x1123002e),
        CardForm("炫紫喵喵炉", Res.drawable.x1123002f),
    ),
    DefenseCard(
        CardForm("彩虹蛇", Res.drawable.x11240040),
        CardForm("溏心彩虹蛇", Res.drawable.x1124004e),
        CardForm("缤纷彩虹蛇", Res.drawable.x1124004f),
    ),
)
