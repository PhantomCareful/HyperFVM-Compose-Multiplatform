package com.careful.hyperfvm.compose.data.cardCatalog

import com.careful.hyperfvm.compose.shared.*


/**
 * 金卡（Java 版 `card_data_3` 表）的静态卡表，共 37 张。
 *
 * 每张卡按 base_name 归并列出全部形态：一行一个 [CardForm]，名字与卡图一一对应，
 * 顺序就是目录页里的显示顺序（也是 Java 版单卡布局里 ImageView 的顺序）。
 * 增删改卡直接改这一段：字母分节、拼音排序、搜索索引都由 [CardCatalog] 自动推导，
 * 不需要再新增任何布局文件或 strings.xml 常量。
 *
 * 额外的搜索别名写在 [DefenseCard.keywords]；ICU 读错多音字的卡用 [DefenseCard.pinyin] 纠正。
 */
internal val GoldenCards: List<DefenseCard> = listOf(
    DefenseCard(
        CardForm("阿瑞斯神使", Res.drawable.x1113000a),
        CardForm("阿瑞斯圣神", Res.drawable.x1113000b),
        CardForm("战神·阿瑞斯", Res.drawable.x1113000c),
        CardForm("至尊战神", Res.drawable.x1113000d),
    ),
    DefenseCard(
        CardForm("丘比特神使", Res.drawable.x111300ea),
        CardForm("丘比特圣神", Res.drawable.x111300eb),
        CardForm("爱神·丘比特", Res.drawable.x111300ec),
        CardForm("至尊爱神", Res.drawable.x111300ed),
    ),
    DefenseCard(
        CardForm("尤弥尔神使", Res.drawable.x1112221a),
        CardForm("尤弥尔圣神", Res.drawable.x1112221b),
        CardForm("巨神·尤弥尔", Res.drawable.x1112221c),
        CardForm("至尊巨神", Res.drawable.x1112221d),
    ),
    DefenseCard(
        CardForm("索尔神使", Res.drawable.x111300ca),
        CardForm("索尔圣神", Res.drawable.x111300cb),
        CardForm("雷神·索尔", Res.drawable.x111300cc),
        CardForm("至尊雷神", Res.drawable.x111300cd),
    ),
    DefenseCard(
        CardForm("祝融神使", Res.drawable.x1112090a),
        CardForm("祝融圣神", Res.drawable.x1112090b),
        CardForm("赤帝·祝融", Res.drawable.x1112090c),
        CardForm("至尊赤帝", Res.drawable.x1112090d),
    ),
    DefenseCard(
        CardForm("哈迪斯神使", Res.drawable.x1113120a),
        CardForm("哈迪斯圣神", Res.drawable.x1113120b),
        CardForm("冥神·哈迪斯", Res.drawable.x1113120c),
        CardForm("至尊冥神", Res.drawable.x1113120d),
    ),
    DefenseCard(
        CardForm("里格神使", Res.drawable.x1112206a),
        CardForm("里格圣神", Res.drawable.x1112206b),
        CardForm("守护神·里格", Res.drawable.x1112206c),
        CardForm("至尊守护神", Res.drawable.x1112206d),
    ),
    DefenseCard(
        CardForm("洛基神使", Res.drawable.x1119015a),
        CardForm("洛基圣神", Res.drawable.x1119015b),
        CardForm("火神·洛基", Res.drawable.x1119015c),
        CardForm("至尊火神", Res.drawable.x1119015d),
    ),
    DefenseCard(
        CardForm("欧若拉神使", Res.drawable.x1112085a),
        CardForm("欧若拉圣神", Res.drawable.x1112085b),
        CardForm("曙光女神·欧若拉", Res.drawable.x1112085c),
        CardForm("至尊曙光女神", Res.drawable.x1112085d),
    ),
    DefenseCard(
        CardForm("塔利亚神使", Res.drawable.x1112215a),
        CardForm("塔利亚圣神", Res.drawable.x1112215b),
        CardForm("宴飨女神·塔利亚", Res.drawable.x1112215c),
        CardForm("至尊宴飨女神", Res.drawable.x1112215d),
    ),
    DefenseCard(
        CardForm("弗雷神使", Res.drawable.x1112258a),
        CardForm("弗雷圣神", Res.drawable.x1112258b),
        CardForm("丰饶神·弗雷", Res.drawable.x1112258c),
        CardForm("至尊丰饶神", Res.drawable.x1112258d),
    ),
    DefenseCard(
        CardForm("塔拉萨神使", Res.drawable.x1112277a),
        CardForm("塔拉萨圣神", Res.drawable.x1139065b),
        CardForm("海洋女神·塔拉萨", Res.drawable.x1139065c),
        CardForm("至尊海洋女神", Res.drawable.x1139065d),
    ),
    DefenseCard(
        CardForm("阿波罗神使", Res.drawable.x1111005a),
        CardForm("阿波罗圣神", Res.drawable.x1111005b),
        CardForm("太阳神·阿波罗", Res.drawable.x1111005c),
        CardForm("至尊太阳神", Res.drawable.x1111005d),
    ),
    DefenseCard(
        CardForm("忒提丝神使", Res.drawable.x1112207a),
        CardForm("忒提丝圣神", Res.drawable.x1112207b),
        CardForm("水神·忒提丝", Res.drawable.x1112207c),
        CardForm("至尊水神", Res.drawable.x1112207d),
    ),
    DefenseCard(
        CardForm("赫丘利神使", Res.drawable.x1112041a),
        CardForm("赫丘利圣神", Res.drawable.x1112041b),
        CardForm("大力神·赫丘利", Res.drawable.x1112041c),
        CardForm("至尊大力神", Res.drawable.x1112041d),
    ),
    DefenseCard(
        CardForm("埃罗斯神使", Res.drawable.x1112236a),
        CardForm("埃罗斯圣神", Res.drawable.x1112236b),
        CardForm("恶作剧神·埃罗斯", Res.drawable.x1112236c),
        CardForm("至尊恶作剧神", Res.drawable.x1112236d),
    ),
    DefenseCard(
        CardForm("赫斯提亚神使", Res.drawable.x1112248a),
        CardForm("赫斯提亚圣神", Res.drawable.x1112248b),
        CardForm("圣火女神·赫斯提亚", Res.drawable.x1112248c),
        CardForm("至尊圣火女神", Res.drawable.x1112248d),
    ),
    DefenseCard(
        CardForm("波塞冬神使", Res.drawable.x1193007a),
        CardForm("波塞冬圣神", Res.drawable.x1193007b),
        CardForm("海神·波塞冬", Res.drawable.x1193007c),
        CardForm("至尊海神", Res.drawable.x1193007d),
    ),
    DefenseCard(
        CardForm("狄安娜神使", Res.drawable.x111300da),
        CardForm("狄安娜圣神", Res.drawable.x111300db),
        CardForm("月神·狄安娜", Res.drawable.x111300dc),
        CardForm("至尊月神", Res.drawable.x111300dd),
    ),
    DefenseCard(
        CardForm("宙斯神使", Res.drawable.x1193009a),
        CardForm("宙斯圣神", Res.drawable.x1193009b),
        CardForm("天神·宙斯", Res.drawable.x1193009c),
        CardForm("至尊天神", Res.drawable.x1193009d),
    ),
    DefenseCard(
        CardForm("典伊神使", Res.drawable.x1112012a),
        CardForm("典伊圣神", Res.drawable.x1112012b),
        CardForm("冰神·典伊", Res.drawable.x1112012c),
        CardForm("至尊冰神", Res.drawable.x1112012d),
    ),
    DefenseCard(
        CardForm("俱吠罗神使", Res.drawable.x1112280a),
        CardForm("俱吠罗圣神", Res.drawable.x1112280b),
        CardForm("护法神·俱吠罗", Res.drawable.x1112280c),
        CardForm("至尊护法神", Res.drawable.x1112280d),
    ),
    DefenseCard(
        CardForm("混沌神使", Res.drawable.x1112230a),
        CardForm("混沌圣神", Res.drawable.x1112230b),
        CardForm("上古神·混沌", Res.drawable.x1112230c),
        CardForm("至尊上古神", Res.drawable.x1112230d),
    ),
    DefenseCard(
        CardForm("鲁班神使", Res.drawable.x1112270a),
        CardForm("鲁班圣神", Res.drawable.x1112270b),
        CardForm("工匠神·鲁班", Res.drawable.x1112270c),
        CardForm("至尊工匠神", Res.drawable.x1112270d),
    ),
    DefenseCard(
        CardForm("神农氏神使", Res.drawable.x1112298a),
        CardForm("神农氏圣神", Res.drawable.x1112298b),
        CardForm("炎帝·神农氏", Res.drawable.x1112298c),
        CardForm("至尊炎帝", Res.drawable.x1112298d),
    ),
    DefenseCard(
        CardForm("奥丁神使", Res.drawable.x1112064a),
        CardForm("奥丁圣神", Res.drawable.x1112064b),
        CardForm("主神·奥丁", Res.drawable.x1112064c),
        CardForm("至尊主神", Res.drawable.x1112064d),
    ),
    DefenseCard(
        CardForm("后羿神使", Res.drawable.x1112244a),
        CardForm("后羿圣神", Res.drawable.x1112244b),
        CardForm("宗布神·后羿", Res.drawable.x1112244c),
        CardForm("至尊宗布神", Res.drawable.x1112244d),
    ),
    DefenseCard(
        CardForm("盖亚神使", Res.drawable.x1112065a),
        CardForm("盖亚圣神", Res.drawable.x1112065b),
        CardForm("大地女神·盖亚", Res.drawable.x1112065c),
        CardForm("至尊大地女神", Res.drawable.x1112065d),
    ),
    DefenseCard(
        CardForm("黑暗神使", Res.drawable.x1112203a),
        CardForm("黑暗圣神", Res.drawable.x1112203b),
        CardForm("黑暗神·霍德尔", Res.drawable.x1112203c),
        CardForm("至尊黑暗神", Res.drawable.x1112203d),
    ),
    DefenseCard(
        CardForm("巴德尔神使", Res.drawable.x1112256a),
        CardForm("巴德尔圣神", Res.drawable.x1112256b),
        CardForm("光明神·巴德尔", Res.drawable.x1112256c),
        CardForm("至尊光明神", Res.drawable.x1112256d),
    ),
    DefenseCard(
        CardForm("潘多拉", Res.drawable.x1112009a),
        CardForm("潘多拉魔盒", Res.drawable.x1112009b),
        CardForm("疫神·潘多拉", Res.drawable.x1112009c),
        CardForm("至尊疫神", Res.drawable.x1112009d),
    ),
    DefenseCard(
        CardForm("灵鱼摩蹉神使", Res.drawable.x1112233a),
        CardForm("灵鱼摩蹉圣神", Res.drawable.x1139072b),
        CardForm("救世神·灵鱼摩蹉", Res.drawable.x1139072c),
        CardForm("至尊救世神", Res.drawable.x1139072d),
    ),
    DefenseCard(
        CardForm("柯罗诺斯神使", Res.drawable.x1139090a),
        CardForm("柯罗诺斯圣神", Res.drawable.x1139090b),
        CardForm("时间神·柯罗诺斯", Res.drawable.x1139090c),
        CardForm("至尊时间神", Res.drawable.x1139090d),
    ),
    DefenseCard(
        CardForm("梵天神使", Res.drawable.x1139070a),
        CardForm("梵天圣神", Res.drawable.x1139070b),
        CardForm("创造神·梵天", Res.drawable.x1139070c),
        CardForm("至尊创造神", Res.drawable.x1139070d),
    ),
    DefenseCard(
        CardForm("奥西里斯神使", Res.drawable.x1139067a),
        CardForm("奥西里斯圣神", Res.drawable.x1139067b),
        CardForm("重生神·奥西里斯", Res.drawable.x1139067c),
        CardForm("至尊重生神", Res.drawable.x1139067d),
    ),
    DefenseCard(
        CardForm("克洛托神使", Res.drawable.x1132206a),
        CardForm("克洛托圣神", Res.drawable.x1132206b),
        CardForm("命运女神·克洛托", Res.drawable.x1132206c),
        CardForm("至尊命运女神", Res.drawable.x1132206d),
    ),
    DefenseCard(
        CardForm("赫拉神使", Res.drawable.x1124002a),
        CardForm("赫拉圣神", Res.drawable.x1124002b),
        CardForm("天后·赫拉", Res.drawable.x1124002c),
        CardForm("至尊天后", Res.drawable.x1124002d),
    ),
)
