package com.careful.hyperfvm.compose.data.cardCatalog

import com.careful.hyperfvm.compose.shared.*


/**
 * 普通卡（Java 版 `card_data_1` 表）的静态卡表，共 167 张。
 *
 * 每张卡按 base_name 归并列出全部形态：一行一个 [CardForm]，名字与卡图一一对应，
 * 顺序就是目录页里的显示顺序（也是 Java 版单卡布局里 ImageView 的顺序）。
 * 增删改卡直接改这一段：字母分节、拼音排序、搜索索引都由 [CardCatalog] 自动推导，
 * 不需要再新增任何布局文件或 strings.xml 常量。
 *
 * 额外的搜索别名写在 [DefenseCard.keywords]；ICU 读错多音字的卡用 [DefenseCard.pinyin] 纠正。
 */
internal val NormalCards: List<DefenseCard> = listOf(
    DefenseCard(
        CardForm("双向水管", "x11130060"),
        CardForm("控温双向水管", "x1113006e"),
        CardForm("合金水管", "x1113006f"),
    ),
    DefenseCard(
        CardForm("三线酒架", "x111300b0"),
        CardForm("强力三线酒架", "x111300be"),
        CardForm("终结者酒架", "x111300bf"),
    ),
    DefenseCard(
        CardForm("炙烤灯笼鱼", "x11120460"),
        CardForm("炙烤辣灯笼鱼", "x1112046e"),
        CardForm("炙烤五香灯笼鱼", "x1112046f"),
    ),
    DefenseCard(CardForm("小笼包", "x11130014")),
    DefenseCard(CardForm("双层小笼包", "x11130020")),
    DefenseCard(CardForm("三向小笼包", "x11130100")),
    DefenseCard(
        CardForm("机枪小笼包", "x11930010"),
        CardForm("竹筒机枪小笼包", "x1193001e"),
        CardForm("格林机枪笼包", "x1193001f"),
    ),
    DefenseCard(CardForm("冰冻小笼包", "x11130050")),
    DefenseCard(CardForm("双层冰冻小笼包", "x11130160")),
    DefenseCard(CardForm("三向冰冻小笼包", "x11130170")),
    DefenseCard(CardForm("机枪冰冻小笼包", "x11930070")),
    DefenseCard(
        CardForm("国王小笼包", "x11630010"),
        CardForm("红心国王小笼包", "x1163001e"),
        CardForm("黑心国王小笼包", "x1163001f"),
    ),
    DefenseCard(
        CardForm("三向国王小笼包", "x11630100"),
        CardForm("红心三向国王小笼包", "x1163010e"),
        CardForm("黑心三向国王小笼包", "x1163010f"),
    ),
    DefenseCard(CardForm("贵族小笼包", "x11130030")),
    DefenseCard(
        CardForm("玉蜀黍", "x11930060"),
        CardForm("奶油玉米机枪", "x1193006e"),
        CardForm("加农玉米机枪", "x1193006f"),
    ),
    DefenseCard(
        CardForm("咖啡杯", "x11131034"),
        CardForm("花纹咖啡杯", "x1113103e"),
        CardForm("骨瓷咖啡杯", "x1113003f"),
    ),
    DefenseCard(CardForm("水上茶杯", "x11431014")),
    DefenseCard(
        CardForm("仙人掌刺身", "x111304e0"),
        CardForm("仙人球刺身", "x111304ee"),
        CardForm("仙人掌花刺身", "x111304ef"),
    ),
    DefenseCard(
        CardForm("全能糖球投手", "x11122520"),
        CardForm("水果糖全能投手", "x1112252e"),
        CardForm("可可糖全能投手", "x1112252f"),
    ),
    DefenseCard(
        CardForm("煮蛋器投手", "x111300a0"),
        CardForm("威力煮蛋器", "x111300ae"),
        CardForm("强袭煮蛋器", "x111300af"),
    ),
    DefenseCard(
        CardForm("冰煮蛋器", "x11930020"),
        CardForm("节能冰煮蛋器", "x1193002e"),
        CardForm("冰河煮蛋器", "x1193002f"),
    ),
    DefenseCard(
        CardForm("雪糕投手", "x11130180"),
        CardForm("麦旋风投手", "x1113018e"),
        CardForm("水果雪芭投手", "x1113018f"),
    ),
    DefenseCard(
        CardForm("烤蜥蜴投手", "x111304c0"),
        CardForm("坚果蜥蜴投手", "x111304ce"),
        CardForm("花椒蜥蜴投手", "x111304cf"),
    ),
    DefenseCard(
        CardForm("钵钵鸡", "x11122160"),
        CardForm("飘香钵钵鸡", "x1112216e"),
        CardForm("川香钵钵鸡", "x1112216f"),
    ),
    DefenseCard(
        CardForm("色拉投手", "x11130094"),
        CardForm("果蔬色拉投手", "x1113002e"),
        CardForm("凯撒色拉投手", "x1113002f"),
    ),
    DefenseCard(
        CardForm("巧克力投手", "x11130080"),
        CardForm("浓情巧克力投手", "x1113001e"),
        CardForm("脆心巧克力投手", "x1113001f"),
    ),
    DefenseCard(
        CardForm("臭豆腐投手", "x11130130"),
        CardForm("什锦臭豆腐投手", "x1113013e"),
        CardForm("铁板臭豆腐投手", "x1113013f"),
    ),
    DefenseCard(
        CardForm("8周年蛋糕", "x11131090"),
        CardForm("8周年慕斯", "x1113109e"),
        CardForm("8周年红丝绒", "x1113109f"),
    ),
    DefenseCard(
        CardForm("生煎锅", "x11130260"),
        CardForm("水煎包锅", "x1113026e"),
        CardForm("驴肉火烧锅", "x1113026f"),
    ),
    DefenseCard(
        CardForm("糖炒栗子", "x11122280"),
        CardForm("开口笑栗子", "x1112228e"),
        CardForm("焦香烤栗子", "x1112228f"),
    ),
    DefenseCard(
        CardForm("马卡龙烤箱", "x11122690"),
        CardForm("抹茶马卡龙烤箱", "x1112269e"),
        CardForm("幻彩马卡龙烤箱", "x1112269f"),
    ),
    DefenseCard(
        CardForm("炭烧海星", "x11130200"),
        CardForm("芝士焗海星", "x1113020e"),
        CardForm("芥末海星刺身", "x1113020f"),
    ),
    DefenseCard(
        CardForm("坚果爆炒机", "x11120950"),
        CardForm("橡子搅拌机", "x1112095e"),
        CardForm("松塔爆破机", "x1112095f"),
    ),
    DefenseCard(
        CardForm("邪恶牛油果", "x11122910"),
        CardForm("沙沙牛油果", "x1112291e"),
        CardForm("桑巴牛油果", "x1112291f"),
    ),
    DefenseCard(
        CardForm("怪味鱿鱼", "x11120760"),
        CardForm("爆汁怪味鱿鱼", "x1112076e"),
        CardForm("天椒怪味鱿鱼", "x1112076f"),
    ),
    DefenseCard(
        CardForm("元宝饺子鼎", "x11122930"),
        CardForm("四喜饺子鼎", "x1112293e"),
        CardForm("八珍饺子鼎", "x1112293f"),
    ),
    DefenseCard(
        CardForm("鲈鱼", "x11150034"),
        CardForm("美味鲈鱼", "x1115003e"),
        CardForm("龙门鲈鱼", "x1115003f"),
    ),
    DefenseCard(
        CardForm("烧鸡", "x11150014"),
        CardForm("美味烤鸡", "x1115001e"),
        CardForm("奥尔良烤鸡", "x1115001f"),
    ),
    DefenseCard(CardForm("牛角面包", "x11150040")),
    DefenseCard(
        CardForm("火盆", "x11190141"),
        CardForm("电子烤盘", "x1119014e"),
        CardForm("岩烧烤盘", "x1119014f"),
    ),
    DefenseCard(CardForm("蓝莓信号塔塔", "x11120420")),
    DefenseCard(
        CardForm("美味水果塔", "x11120700"),
        CardForm("风车水果塔", "x1112070e"),
        CardForm("巧克力风车塔", "x1112070f"),
    ),
    DefenseCard(
        CardForm("莓果点心", "x11120470"),
        CardForm("薄荷莓果点心", "x1112047e"),
        CardForm("流心莓果点心", "x1112047f"),
    ),
    DefenseCard(
        CardForm("龙须面", "x11122130"),
        CardForm("三鲜龙须面", "x1112213e"),
        CardForm("佛跳墙龙须面", "x1112213f"),
    ),
    DefenseCard(
        CardForm("五谷丰登", "x11122310"),
        CardForm("五谷营养餐", "x1112231e"),
        CardForm("杂粮大丰收", "x1112231f"),
    ),
    DefenseCard(
        CardForm("加速榨汁机", "x11122500"),
        CardForm("苹果榨汁机", "x1112250e"),
        CardForm("大菠萝榨汁机", "x1112250f"),
    ),
    DefenseCard(
        CardForm("浮生茶", "x11122860"),
        CardForm("清心浮生茶", "x1112286e"),
        CardForm("忘忧浮生茶", "x1112286f"),
    ),
    DefenseCard(CardForm("11周年美食盒子", "x11120530")),
    DefenseCard(
        CardForm("小火炉", "x11110014"),
        CardForm("日光炉", "x1111001e"),
        CardForm("太阳能高效炉", "x1111001f"),
    ),
    DefenseCard(
        CardForm("大火炉", "x11930040"),
        CardForm("高能火炉", "x1193004e"),
        CardForm("超能燃气炉", "x1193004f"),
    ),
    DefenseCard(
        CardForm("酒杯灯", "x11111024"),
        CardForm("节能灯", "x1111102e"),
        CardForm("高效节能灯", "x1111102f"),
    ),
    DefenseCard(CardForm("7周年蜡烛", "x11111034")),
    DefenseCard(
        CardForm("蛇羹煲", "x11390740"),
        CardForm("厨圣蛇羹煲", "x1139074e"),
        CardForm("帝王蛇羹煲", "x1139074f"),
    ),
    DefenseCard(
        CardForm("樱桃反弹布丁", "x111911c0"),
        CardForm("节能反弹布丁", "x111911ce"),
        CardForm("热量反弹布丁", "x111911cf"),
        // ICU 会把「反弹」读成 fan dan，这里按词组读音纠正。
        pinyin = "ying tao fan tan bu ding",
    ),
    DefenseCard(CardForm("艾草粑粑", "x111911e0")),
    DefenseCard(
        CardForm("木盘子", "x11370015"),
        CardForm("友情木盘子", "x1137001e"),
        CardForm("坚韧木盘子", "x1137001f"),
    ),
    DefenseCard(
        CardForm("魔法软糖", "x11370070"),
        CardForm("魔法师软糖", "x1137007e"),
        CardForm("魔导士软糖", "x1137007f"),
    ),
    DefenseCard(
        CardForm("棉花糖", "x11370025"),
        CardForm("草莓棉花糖", "x1137002e"),
        CardForm("彩虹棉花糖", "x1137002f"),
    ),
    DefenseCard(
        CardForm("苏打气泡", "x11370034"),
        CardForm("五彩香皂泡泡", "x1137003e"),
    ),
    DefenseCard(
        CardForm("麦芽糖", "x11370060"),
        CardForm("莓莓麦芽糖", "x1137006e"),
    ),
    DefenseCard(
        CardForm("糖葫芦炮弹", "x11130280"),
        CardForm("水果糖葫芦弹", "x1113028e"),
        CardForm("七彩糖葫芦弹", "x1113028f"),
    ),
    DefenseCard(CardForm("香肠", "x11130110")),
    DefenseCard(
        CardForm("热狗大炮", "x11130140"),
        CardForm("热狗高射炮", "x1113014e"),
        CardForm("热狗榴弹炮", "x1113014f"),
    ),
    DefenseCard(
        CardForm("爱心便当", "x11122200"),
        CardForm("烤肠爱心便当", "x1112220e"),
        CardForm("八爪鱼爱心便当", "x1112220f"),
    ),
    DefenseCard(
        CardForm("梦幻多拿滋", "x11122250"),
        CardForm("仙女多拿滋", "x1112225e"),
        CardForm("女王多拿滋", "x1112225f"),
    ),
    DefenseCard(
        CardForm("耗油双菇", "x11120690"),
        CardForm("伯爵耗油双菇", "x1112069e"),
        CardForm("皇家耗油双菇", "x1112069f"),
    ),
    DefenseCard(
        CardForm("咖啡喷壶", "x11131070"),
        CardForm("香醇咖啡喷壶", "x1113107e"),
        CardForm("红温咖啡喷壶", "x1113107f"),
    ),
    DefenseCard(
        CardForm("关东煮喷锅", "x111310c0"),
        CardForm("福袋关东煮喷锅", "x111310ce"),
        CardForm("海鲜关东煮喷锅", "x111310cf"),
    ),
    DefenseCard(
        CardForm("肥牛火锅", "x11130240"),
        CardForm("酸汤肥牛锅", "x1113024e"),
        CardForm("海鲜肥牛锅", "x1113024f"),
    ),
    DefenseCard(
        CardForm("麻辣香锅", "x11130250"),
        CardForm("孜然羊肉锅", "x1113025e"),
        CardForm("酱香鱿鱼锅", "x1113025f"),
    ),
    DefenseCard(
        CardForm("旋转咖啡喷壶", "x11931060"),
        CardForm("节能旋转咖啡壶", "x1193106e"),
        CardForm("原子咖啡壶", "x1193106f"),
    ),
    DefenseCard(
        CardForm("章鱼烧", "x11430030"),
        CardForm("两栖章鱼烧", "x1113004e"),
        CardForm("火影章鱼烧", "x1113005f"),
    ),
    DefenseCard(
        CardForm("咖喱龙虾炮", "x111301a0"),
        CardForm("麻辣龙虾炮", "x111301ae"),
        CardForm("加农龙虾炮", "x111301af"),
    ),
    DefenseCard(
        CardForm("冰块冷萃机", "x11122260"),
        CardForm("低温冷萃机", "x1112226e"),
        CardForm("迅捷冷萃机", "x1112226f"),
    ),
    DefenseCard(
        CardForm("红柳烤串机", "x11122890"),
        CardForm("茴香红柳烤串机", "x1112289e"),
        CardForm("星火红柳烤串机", "x1112289f"),
    ),
    DefenseCard(
        CardForm("蜜糖陷阱", "x11122750"),
        CardForm("香浓蜜糖陷阱", "x1112275e"),
        CardForm("王冠蜜糖陷阱", "x1112275f"),
    ),
    DefenseCard(
        CardForm("鼠鼠蛋糕空投器", "x11120280"),
        CardForm("喵博士蛋糕空投器", "x1112028e"),
    ),
    DefenseCard(
        CardForm("椒麻靓仔鸡", "x11122940"),
        CardForm("酥麻靓仔鸡", "x1112294e"),
        CardForm("变态辣仔鸡", "x1112294f"),
    ),
    DefenseCard(
        CardForm("汉堡包", "x111300f0"),
        CardForm("天椒双层堡", "x1112254e"),
        CardForm("牛肉双黑汉堡", "x1112254f"),
    ),
    DefenseCard(CardForm("贪食蛙", "x111300f1")),
    DefenseCard(
        CardForm("香辣年糕蟹", "x11122220"),
        CardForm("中辣年糕蟹", "x1112222e"),
        CardForm("厨神年糕蟹", "x1112222f"),
    ),
    DefenseCard(
        CardForm("雷电长棍面包", "x11130150"),
        CardForm("节能面包", "x1113015e"),
        CardForm("负离子面包", "x1113015f"),
        // 「长棍」是 chang gun，不是 zhang gun。
        pinyin = "lei dian chang gun mian bao",
    ),
    DefenseCard(
        CardForm("新疆拉面", "x11130270"),
        CardForm("刀削拉面", "x1113027e"),
        CardForm("真刀削拉面", "x1113027f"),
    ),
    DefenseCard(
        CardForm("丸子厨师", "x11130300"),
        CardForm("盛宴丸子厨师", "x1112282e"),
        CardForm("传奇丸子厨师", "x1112282f"),
    ),
    DefenseCard(CardForm("鱼刺", "x11130120")),
    DefenseCard(
        CardForm("钢鱼刺", "x11930050"),
        CardForm("无座钢鱼刺", "x1113005e"),
        CardForm("海贼王鱼刺", "x1113007f"),
    ),
    DefenseCard(
        CardForm("糖渍刺梨", "x11120940"),
        CardForm("烟熏刺梨", "x1112094e"),
        CardForm("生腌酸刺梨", "x1112094f"),
    ),
    DefenseCard(
        CardForm("蜂蜜史莱姆", "x11120880"),
        CardForm("蜂糖史莱姆", "x1112088e"),
        CardForm("蜂王浆史莱姆", "x1112088f"),
    ),
    DefenseCard(
        CardForm("弹珠汽水", "x11122740"),
        CardForm("香橙弹珠汽水", "x1112274e"),
        CardForm("果味弹珠汽水", "x1112274f"),
    ),
    DefenseCard(
        CardForm("15周年猴赛雷", "x11122590"),
        CardForm("捕风手猴赛雷", "x1112259e"),
        CardForm("艺术家猴赛雷", "x1112259f"),
    ),
    DefenseCard(
        CardForm("迷你披萨炉", "x11122490"),
        CardForm("香脆披萨炉", "x1112249e"),
        CardForm("拉丝披萨炉", "x1112249f"),
    ),
    DefenseCard(
        CardForm("灯影花糕", "x11122880"),
        CardForm("初见灯影花糕", "x1112288e"),
        CardForm("梦境灯影花糕", "x1112288f"),
    ),
    DefenseCard(
        CardForm("焦油喷壶", "x11130090"),
        CardForm("强力焦油喷壶", "x1113009e"),
        CardForm("祝融喷壶", "x1113009f"),
    ),
    DefenseCard(
        CardForm("街头烤肉大师", "x11122430"),
        CardForm("户外烤肉大师", "x1112243e"),
        CardForm("BBQ烤肉大师", "x1112243f"),
    ),
    DefenseCard(
        CardForm("巧克力大炮", "x11930030"),
        CardForm("节能巧克力大炮", "x1193003e"),
        CardForm("死神大炮", "x1193003f"),
    ),
    DefenseCard(
        CardForm("榴莲", "x11130210"),
        CardForm("雪山榴莲", "x1113021e"),
        CardForm("冰河世纪榴莲", "x1113021f"),
    ),
    DefenseCard(
        CardForm("美味电鳗", "x11130230"),
        CardForm("变异美味电鳗", "x1113023e"),
        CardForm("霸王美味电鳗", "x1113023f"),
    ),
    DefenseCard(
        CardForm("火龙果", "x11130220"),
        CardForm("炎阳火龙果", "x1113022e"),
        CardForm("九天皓日火龙果", "x1113022f"),
    ),
    DefenseCard(
        CardForm("桥头米线", "x11122870"),
        CardForm("灵泉桥头米线", "x1112287e"),
        CardForm("金汤桥头米线", "x1112287f"),
    ),
    DefenseCard(
        CardForm("泡椒春笋", "x11122920"),
        CardForm("火辣泡椒笋", "x1112292e"),
        CardForm("望乡泡椒笋", "x1112292f"),
    ),
    DefenseCard(
        CardForm("啵啵跳跳糖", "x11122950"),
        CardForm("香梨跳跳糖", "x1112295e"),
        CardForm("星辰跳跳糖", "x1112295f"),
    ),
    DefenseCard(
        CardForm("可乐炸弹", "x11120034"),
        CardForm("云爆可乐弹", "x1112003e"),
        CardForm("燃烧可乐弹", "x1112003f"),
    ),
    DefenseCard(
        CardForm("酒瓶炸弹", "x11120020"),
        CardForm("高爆酒瓶弹", "x1112002e"),
        CardForm("子母酒瓶弹", "x1112002f"),
    ),
    DefenseCard(
        CardForm("开水壶炸弹", "x11121010"),
        CardForm("汽油壶炸弹", "x1112101e"),
        CardForm("汽油干冰弹", "x1112101f"),
    ),
    DefenseCard(
        CardForm("威士忌炸弹", "x11120070"),
        CardForm("水晶威士忌炸弹", "x1112039e"),
        CardForm("钻石威士忌炸弹", "x1112039f"),
    ),
    DefenseCard(
        CardForm("玉兔灯笼", "x11121070"),
        CardForm("广寒玉兔灯笼", "x1112107e"),
        CardForm("莲花玉兔灯笼", "x1112107f"),
    ),
    DefenseCard(CardForm("深水炸弹", "x11120510")),
    DefenseCard(
        CardForm("微波炉爆弹", "x11122230"),
        CardForm("星际微波爆弹", "x1112223e"),
        CardForm("宇宙微波核弹", "x1112223f"),
    ),
    DefenseCard(CardForm("爆竹", "x11120044")),
    DefenseCard(CardForm("美食烟花普通版", "x11120054")),
    DefenseCard(CardForm("美食烟花华丽版", "x11120064")),
    DefenseCard(
        CardForm("爆辣河豚", "x11120620"),
        CardForm("朝天椒河豚", "x1112062e"),
        CardForm("青芥末河豚", "x1112062f"),
    ),
    DefenseCard(
        CardForm("糖果罐子", "x11120960"),
        CardForm("瞬息糖果罐子", "x1112096e"),
        CardForm("核爆糖果罐子", "x1112096f"),
    ),
    DefenseCard(
        CardForm("10周年烟花", "x11120270"),
        CardForm("10周年爆竹", "x1112027e"),
        CardForm("10周年礼花", "x1112027f"),
    ),
    DefenseCard(
        CardForm("冰桶炸弹", "x11121020"),
        CardForm("酸橙冰桶炸弹", "x1112102e"),
        CardForm("杂果冰桶炸弹", "x1112102f"),
    ),
    DefenseCard(
        CardForm("冰兔菓子", "x11120840"),
        CardForm("流心冰兔菓子", "x1112084e"),
        CardForm("糯叽叽冰兔菓子", "x1112084f"),
    ),
    DefenseCard(
        CardForm("石榴爆爆冰", "x11122990"),
        CardForm("霜冻石榴爆冰", "x1112299e"),
        CardForm("酷寒石榴爆冰", "x1112299f"),
    ),
    DefenseCard(CardForm("炸地鼠爆竹", "x11120350")),
    DefenseCard(CardForm("萌虎高压锅", "x11120600")),
    DefenseCard(
        CardForm("金牛烟花", "x11140220"),
        CardForm("迷幻金牛烟花", "x1114022e"),
        CardForm("璀璨金牛烟花", "x1132207f"),
    ),
    DefenseCard(
        CardForm("面粉袋", "x111300d0"),
        CardForm("影分身袋", "x111300fe"),
        CardForm("乾坤分身袋", "x111300ff"),
    ),
    DefenseCard(
        CardForm("椰子果", "x11140240"),
        CardForm("浓香椰子果", "x1114024e"),
        CardForm("金丝椰子果", "x1114024f"),
    ),
    DefenseCard(
        CardForm("青涩柿柿", "x11122340"),
        CardForm("成熟柿柿", "x1112234e"),
        CardForm("柿柿如意", "x1112234f"),
    ),
    DefenseCard(CardForm("钢丝球", "x11430020")),
    DefenseCard(
        CardForm("老鼠夹子", "x11120010"),
        CardForm("多用老鼠夹子", "x1112001e"),
        CardForm("黑猫鼠夹", "x1112001f"),
    ),
    DefenseCard(CardForm("麻辣串炸弹", "x11120040")),
    DefenseCard(CardForm("竹筒粽子", "x11121050")),
    DefenseCard(
        CardForm("泡泡鸡尾酒", "x11122760"),
        CardForm("蜜语鸡尾酒", "x1112276e"),
        CardForm("粉红佳人鸡尾酒", "x1112276f"),
    ),
    DefenseCard(
        CardForm("辣椒粉", "x11120220"),
        CardForm("火爆辣椒粉", "x1112022e"),
        CardForm("魔鬼辣椒粉", "x1112022f"),
    ),
    DefenseCard(CardForm("月蟾兔", "x11120230")),
    DefenseCard(CardForm("肉松清明粿", "x11120630")),
    DefenseCard(
        CardForm("冰激凌", "x113901d0"),
        CardForm("果蔬冰淇淋", "x113901de"),
        CardForm("极寒冰沙", "x113901df"),
    ),
    DefenseCard(
        CardForm("美味计时器", "x11390800"),
        CardForm("佳肴计时器", "x1139080e"),
        CardForm("珍馐计时器", "x1139080f"),
    ),
    DefenseCard(
        CardForm("圣诞包裹", "x11390400"),
        CardForm("奢华圣诞包裹", "x1139040e"),
        CardForm("至尊圣诞包裹", "x1139040f"),
    ),
    DefenseCard(
        CardForm("黯然销魂饭", "x11390600"),
        CardForm("培根香肠饭", "x1139060e"),
        CardForm("天妇罗盖饭", "x1139060f"),
    ),
    DefenseCard(
        CardForm("13周年时光机", "x11322040"),
        CardForm("超载时光机", "x1132204e"),
        CardForm("未来时光机", "x1132204f"),
    ),
    DefenseCard(
        CardForm("油灯", "x11191160"),
        CardForm("高亮油灯", "x1119116e"),
    ),
    DefenseCard(CardForm("南瓜灯", "x11190180")),
    DefenseCard(
        CardForm("防萤草灯笼", "x11120730"),
        CardForm("灵光防萤草灯", "x1112073e"),
        CardForm("圣辉防萤草灯", "x1112073f"),
    ),
    DefenseCard(CardForm("换气扇", "x11190150")),
    DefenseCard(
        CardForm("9周年幸运草扇", "x11190160"),
        CardForm("9周年超能扇", "x1119021e"),
        CardForm("9周年SSR草扇", "x1119021f"),
    ),
    DefenseCard(
        CardForm("棕榈吹风机", "x11140210"),
        CardForm("金棕榈吹风机", "x1114021e"),
    ),
    DefenseCard(
        CardForm("酸柠檬爆弹", "x11120710"),
        CardForm("电子柠檬爆弹", "x1112071e"),
        CardForm("质子柠檬爆弹", "x1112071f"),
    ),
    DefenseCard(
        CardForm("炸炸菇", "x11120680"),
        CardForm("魔王炸炸菇", "x1112068e"),
    ),
    DefenseCard(
        CardForm("海盐粉", "x11121030"),
        CardForm("芥末海盐粉", "x1112103e"),
        CardForm("十三香海盐粉", "x1112103f"),
    ),
    DefenseCard(
        CardForm("防风草沙拉", "x11121060"),
        CardForm("蛋黄酱防风草", "x1112106e"),
    ),
    DefenseCard(
        CardForm("金箔甜筒", "x11120450"),
        CardForm("金箔可可甜筒", "x1112045e"),
    ),
    DefenseCard(
        CardForm("12周年能量饮料", "x11120770"),
        CardForm("12周年活力饮料", "x1112077e"),
        CardForm("12周年狂暴饮料", "x1112077f"),
    ),
    DefenseCard(CardForm("木塞子", "x11190171")),
    DefenseCard(
        CardForm("咖啡粉", "x113e0025"),
        CardForm("名贵咖啡粉", "x113e002e"),
        CardForm("皇族咖啡粉", "x113e002f"),
    ),
    DefenseCard(
        CardForm("尖叫马咖", "x113e0030"),
        CardForm("魔法马咖", "x113e003e"),
        CardForm("觉醒曼德拉", "x113e003f"),
    ),
    DefenseCard(CardForm("猫猫盒", "x11190190")),
    DefenseCard(CardForm("猫猫箱", "x111901f0")),
    DefenseCard(CardForm("小丑盒子", "x11190200")),
    DefenseCard(
        CardForm("泡泡糖", "x11120430"),
        CardForm("大大泡泡糖", "x1112043e"),
        CardForm("萄萄泡泡糖", "x1112043f"),
    ),
    DefenseCard(
        CardForm("鼠乐宝味觉糖", "x11120480"),
        CardForm("黑加仑味觉糖", "x1112048e"),
        CardForm("车厘子味觉糖", "x1112048f"),
    ),
    DefenseCard(CardForm("土司面包", "x11140014")),
    DefenseCard(CardForm("月饼", "x11140030")),
    DefenseCard(CardForm("冰皮月饼", "x111400f4")),
    DefenseCard(
        CardForm("巧克力面包", "x11140020"),
        CardForm("德芙面包", "x1114002e"),
    ),
    DefenseCard(
        CardForm("菠萝爆炸面包", "x11930080"),
        CardForm("独角菠萝面包", "x1113008e"),
        CardForm("皇冠菠萝面包", "x1113008f"),
    ),
    DefenseCard(
        CardForm("老虎蟹面包", "x11140034"),
        CardForm("帝王蟹面包", "x1114003e"),
    ),
    DefenseCard(
        CardForm("桂花酒", "x11120240"),
        CardForm("陈年桂花酒", "x1112024e"),
        CardForm("仙酿桂花酒", "x1112024f"),
    ),
    DefenseCard(CardForm("榴莲千层饼", "x11140230")),
    DefenseCard(
        CardForm("瓜皮护罩", "x11240010"),
        CardForm("尖刺瓜皮护罩", "x1124001e"),
        CardForm("锋芒瓜皮护罩", "x1124001f"),
    ),
    DefenseCard(CardForm("生日帽", "x11220024")),
    DefenseCard(
        CardForm("扑克牌护罩", "x11250010"),
        CardForm("精致黑桃护罩", "x1125001e"),
        CardForm("豪华梅花护罩", "x1125001f"),
    ),
)
