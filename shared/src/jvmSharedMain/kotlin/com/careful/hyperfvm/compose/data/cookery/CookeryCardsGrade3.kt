package com.careful.hyperfvm.compose.data.cookery

import com.careful.hyperfvm.compose.shared.*


/**
 * 极品佳肴的食神谱卡片，对应 Java 版 `fragment_bottom_sheet_cookery_3_*` 布局与
 * `CookeryBottomSheetFragment_3_*` 的绑定结果（由 `tool/cookery_gen.ps1` 生成，勿手改）。
 */
internal val CookeryGrade3Cards: List<CookeryCard> = listOf(
    CookeryCard(
        name = "狗不理包子",
        category = "极品佳肴",
        reward = "奖励600食神点",
        image = "card_data_cookery_3_1",
        description = "狗不理是因为一旦理了就吃不下别的东西了",
        sources = listOf(
            CookerySource(
                title = "金食神宝箱",
                description = "前往火山遗迹-奶昔基地/圣代基地，收集金食神宝箱菜谱碎片，集齐50个碎片可前往假期特惠-长期兑换版块兑换金食神宝箱",
                icons = listOf("x12602820", "x13b12630"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11130014", "star_image_7"),
            CookeryRecipe("x11130020", "star_image_7"),
            CookeryRecipe("x11930010", "star_image_7"),
            CookeryRecipe("x11130050", "star_image_7"),
            CookeryRecipe("x11130160", "star_image_7"),
            CookeryRecipe("x11930070", "star_image_7"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升10%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("小笼包", "x11130014"),
                    CookeryAuxiliaryCard("双层小笼包", "x11130020"),
                    CookeryAuxiliaryCard("三向小笼包", "x11130100"),
                    CookeryAuxiliaryCard("机枪小笼包", "x11930010"),
                    CookeryAuxiliaryCard("冰冻小笼包", "x11130050"),
                    CookeryAuxiliaryCard("双层冰冻小笼包", "x11130160"),
                    CookeryAuxiliaryCard("三向冰冻小笼包", "x11130170"),
                    CookeryAuxiliaryCard("机枪冰冻小笼包", "x11930070"),
                    CookeryAuxiliaryCard("包包龙", "x11122320"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "鸡丝银鱼汤",
        category = "极品佳肴",
        reward = "奖励850食神点",
        image = "card_data_cookery_3_2",
        description = "地上跑的水里游的都有，汤汁清澈味道鲜美",
        sources = listOf(
            CookerySource(
                title = "金食神宝箱",
                description = "前往火山遗迹-奶昔基地/圣代基地，收集金食神宝箱菜谱碎片，集齐50个碎片可前往假期特惠-长期兑换版块兑换金食神宝箱",
                icons = listOf("x12602820", "x13b12630"),
            ),
            CookerySource(
                title = "星能碎片",
                description = "通过每日充值(日氪)收集星能碎片，使用400个星能碎片进行兑换",
                icons = listOf("x12603750"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11150014", "star_image_9"),
            CookeryRecipe("x11150034", "star_image_9"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片冷却时间减少1秒",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("烧鸡", "x11150014"),
                    CookeryAuxiliaryCard("鲈鱼", "x11150034"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "毛血旺",
        category = "极品佳肴",
        reward = "奖励750食神点",
        image = "card_data_cookery_3_3",
        description = "如果没有与辣椒花椒比拼的勇气，谨吃慎食",
        sources = listOf(
            CookerySource(
                title = "金食神宝箱",
                description = "前往火山遗迹-奶昔基地/圣代基地，收集金食神宝箱菜谱碎片，集齐50个碎片可前往假期特惠-长期兑换版块兑换金食神宝箱",
                icons = listOf("x12602820", "x13b12630"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11930040", "star_image_8"),
            CookeryRecipe("x11190141", "star_image_9"),
            CookeryRecipe("x11110014", "star_image_8"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片增幅倍率提升10%",
                tips = listOf(
                    CookeryTip("实际数据为增幅倍率提升0.1", isWarning = false),
                ),
                cards = listOf(
                    CookeryAuxiliaryCard("火盆", "x11190141"),
                    CookeryAuxiliaryCard("刺梨烧烤盘", "x11700080"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "番茄炒蛋",
        category = "极品佳肴",
        reward = "奖励800食神点",
        image = "card_data_cookery_3_4",
        description = "家常小炒菜，虽说简单却色香味俱全",
        sources = listOf(
            CookerySource(
                title = "金食神宝箱",
                description = "前往火山遗迹-奶昔基地/圣代基地，收集金食神宝箱菜谱碎片，集齐50个碎片可前往假期特惠-长期兑换版块兑换金食神宝箱",
                icons = listOf("x12602820", "x13b12630"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x111300a0", "star_image_9"),
            CookeryRecipe("x11130094", "star_image_9"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升10%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("煮蛋器投手", "x111300a0"),
                    CookeryAuxiliaryCard("雪芭煮蛋器", "x11700020"),
                    CookeryAuxiliaryCard("色拉投手", "x11130094"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "面包家族",
        category = "极品佳肴",
        reward = "奖励480食神点",
        image = "card_data_cookery_3_5",
        description = "面包是世界上食用群体最广的食物，种类繁多",
        sources = listOf(
            CookerySource(
                title = "银食神宝箱",
                description = "前往火山遗迹-黑提丛林/蓝莓丛林，收集银食神宝箱菜谱碎片，集齐25个碎片可前往假期特惠-长期兑换版块兑换银食神宝箱",
                icons = listOf("x12602810", "x13b12530"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11140014", "star_image_7"),
            CookeryRecipe("x11140020", "star_image_7"),
            CookeryRecipe("x11930080", "star_image_7"),
            CookeryRecipe("x11130150", "star_image_7"),
            CookeryRecipe("x11150040", "star_image_7"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片体力提升20%，攻击间隔减少2秒",
                tips = listOf(
                    CookeryTip("实际数据为攻击间隔减少1.5秒", isWarning = false),
                    CookeryTip("15、16星的负离子面包吃不到攻击间隔加成", isWarning = true),
                ),
                cards = listOf(
                    CookeryAuxiliaryCard("雷电长棍面包", "x11130150"),
                ),
            ),
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片体力提升20%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("牛角面包", "x11150040"),
                    CookeryAuxiliaryCard("菠萝爆炸面包", "x11930080"),
                    CookeryAuxiliaryCard("火炉菠萝面包", "x11700000"),
                    CookeryAuxiliaryCard("土司面包", "x11140014"),
                    CookeryAuxiliaryCard("巧克力面包", "x11140020"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "关东煮",
        category = "极品佳肴",
        reward = "奖励950食神点",
        image = "card_data_cookery_3_6",
        description = "食过后口齿生香，回味绵长，品尝一次欲罢不能",
        sources = listOf(
            CookerySource(
                title = "银食神宝箱",
                description = "前往火山遗迹-黑提丛林/蓝莓丛林，收集银食神宝箱菜谱碎片，集齐25个碎片可前往假期特惠-长期兑换版块兑换银食神宝箱",
                icons = listOf("x12602810", "x13b12530"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x111310c0", "star_image_9"),
            CookeryRecipe("x11130110", "star_image_8"),
            CookeryRecipe("x11121010", "star_image_8"),
            CookeryRecipe("x11430030", "star_image_9"),
            CookeryRecipe("x111300d0", "star_image_8"),
            CookeryRecipe("x11130130", "star_image_8"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升10%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("关东煮喷锅", "x111310c0"),
                    CookeryAuxiliaryCard("章鱼烧", "x11430030"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "茶叶蛋",
        category = "极品佳肴",
        reward = "奖励880食神点",
        image = "card_data_cookery_3_7",
        description = "做法简单，携带方便，物美价廉",
        sources = listOf(
            CookerySource(
                title = "金食神宝箱",
                description = "前往火山遗迹-奶昔基地/圣代基地，收集金食神宝箱菜谱碎片，集齐50个碎片可前往假期特惠-长期兑换版块兑换金食神宝箱",
                icons = listOf("x12602820", "x13b12630"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11121010", "star_image_9"),
            CookeryRecipe("x111300a0", "star_image_9"),
            CookeryRecipe("x11431014", "star_image_8"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻速提升10%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("煮蛋器投手", "x111300a0"),
                    CookeryAuxiliaryCard("雪芭煮蛋器", "x11700020"),
                ),
            ),
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片冷却时间减少1秒",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("开水壶炸弹", "x11121010"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "拉菲限量版",
        category = "极品佳肴",
        reward = "奖励450食神点",
        image = "card_data_cookery_3_8",
        description = "法国拉菲庄园的限量版葡萄酒，堪称酒王",
        sources = listOf(
            CookerySource(
                title = "星能碎片",
                description = "通过每日充值(日氪)收集星能碎片，使用400个星能碎片进行兑换",
                icons = listOf("x12603750"),
            ),
            CookerySource(
                title = "深渊/海神宝箱",
                description = "每充值100点券可获得1积分，可在积分商城-神秘商人版块中，使用50积分进行兑换(部分时间打折，35积分)，需注意积分月底清零",
                icons = listOf("x13b17330", "x13b25030"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x111300b0", "star_image_7"),
            CookeryRecipe("x11190171", "star_image_7"),
            CookeryRecipe("x11120020", "star_image_7"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻速提升5%",
                tips = listOf(
                    CookeryTip("实际数据为攻速提升3%", isWarning = false),
                    CookeryTip("该食神谱无实际效果。攻速提升3%时，实际缩减量为0.6帧＜1帧(你游20帧游戏这一块🙄)", isWarning = true),
                ),
                cards = listOf(
                    CookeryAuxiliaryCard("三线酒架", "x111300b0"),
                    CookeryAuxiliaryCard("子母三线酒架", "x11700070"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "圣诞姜饼",
        category = "极品佳肴",
        reward = "奖励450食神点",
        image = "card_data_cookery_3_9",
        description = "美味镇圣诞节专用小吃，谁吃谁知道！",
        sources = listOf(
            CookerySource(
                title = "星能碎片",
                description = "通过每日充值(日氪)收集星能碎片，使用300个星能碎片进行兑换",
                icons = listOf("x12603750"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x111300a0", "star_image_7"),
            CookeryRecipe("x111300d0", "star_image_7"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻速提升5%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("煮蛋器投手", "x111300a0"),
                    CookeryAuxiliaryCard("雪芭煮蛋器", "x11700020"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "水煮鱼",
        category = "极品佳肴",
        reward = "奖励990食神点",
        image = "card_data_cookery_3_10",
        description = "满目的辣椒红亮养眼，辣而不燥，麻而不苦，可谓“麻上头，辣过瘾”",
        sources = listOf(
            CookerySource(
                title = "星能碎片",
                description = "通过每日充值(日氪)收集星能碎片，使用400个星能碎片进行兑换",
                icons = listOf("x12603750"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11130060", "star_image_9"),
            CookeryRecipe("x11150034", "star_image_9"),
            CookeryRecipe("x11930040", "star_image_9"),
            CookeryRecipe("x11190141", "star_image_8"),
            CookeryRecipe("x11121010", "star_image_8"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升10%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("双向水管", "x11130060"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "钻石风味香槟",
        category = "极品佳肴",
        reward = "奖励1020食神点",
        image = "card_data_cookery_3_11",
        description = "世上最棒（贵）的香槟，佐餐时能无限激发食物的美味",
        sources = listOf(
            CookerySource(
                title = "星座碎片",
                description = "分解星座相关卡片和道具获得星座碎片，使用95个星座碎片进行兑换",
                icons = listOf("yellow_crystal"),
            ),
            CookerySource(
                title = "星能碎片",
                description = "通过每日充值(日氪)收集星能碎片，使用600个星能碎片进行兑换",
                icons = listOf("x12603750"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x111300e4", "star_image_9"),
            CookeryRecipe("x111300b0", "star_image_9"),
            CookeryRecipe("x11120020", "star_image_9"),
            CookeryRecipe("x11190171", "star_image_8"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升10%，攻速提升5%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("射手座精灵", "x111300e4"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "川香冒菜",
        category = "极品佳肴",
        reward = "奖励850食神点",
        image = "card_data_cookery_3_12",
        description = "火爆的川味小吃，平衡了酸甜苦辣咸，各种味道都刚刚好",
        sources = listOf(
            CookerySource(
                title = "星座碎片",
                description = "分解星座相关卡片和道具获得星座碎片，使用95个星座碎片进行兑换",
                icons = listOf("yellow_crystal"),
            ),
            CookerySource(
                title = "星能碎片",
                description = "通过每日充值(日氪)收集星能碎片，使用600个星能碎片进行兑换",
                icons = listOf("x12603750"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11130074", "star_image_9"),
            CookeryRecipe("x11130060", "star_image_9"),
            CookeryRecipe("x111310c0", "star_image_8"),
            CookeryRecipe("x11121010", "star_image_8"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升10%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("天秤座精灵", "x11130074"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "咖喱鱼蛋",
        category = "极品佳肴",
        reward = "奖励1050食神点",
        image = "card_data_cookery_3_13",
        description = "口感Q弹的鱼蛋，吃下去之后让人元气满满",
        sources = listOf(
            CookerySource(
                title = "星座碎片",
                description = "分解星座相关卡片和道具获得星座碎片，使用95个星座碎片进行兑换",
                icons = listOf("yellow_crystal"),
            ),
            CookerySource(
                title = "星能碎片",
                description = "通过每日充值(日氪)收集星能碎片，使用600个星能碎片进行兑换",
                icons = listOf("x12603750"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x111300c4", "star_image_9"),
            CookeryRecipe("x11930020", "star_image_9"),
            CookeryRecipe("x111300a0", "star_image_9"),
            CookeryRecipe("x11150034", "star_image_8"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升10%，攻速提升5%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("双鱼座精灵", "x111300c4"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "海鲜烧烤",
        category = "极品佳肴",
        reward = "奖励950食神点",
        image = "card_data_cookery_3_14",
        description = "烧烤的方式最能激发海鲜的香味，再简单辅以食盐和辛料而佐之，就是顶级的人间美味了",
        sources = listOf(
            CookerySource(
                title = "星座碎片",
                description = "分解星座相关卡片和道具获得星座碎片，使用95个星座碎片进行兑换",
                icons = listOf("yellow_crystal"),
            ),
            CookerySource(
                title = "星能碎片",
                description = "通过每日充值(日氪)收集星能碎片，使用600个星能碎片进行兑换",
                icons = listOf("x12603750"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11130064", "star_image_9"),
            CookeryRecipe("x111300a4", "star_image_9"),
            CookeryRecipe("x11430030", "star_image_9"),
            CookeryRecipe("x11190141", "star_image_8"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升10%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("巨蟹座精灵", "x11130064"),
                    CookeryAuxiliaryCard("天蝎座精灵", "x111300a4"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "海星拉面",
        category = "极品佳肴",
        reward = "奖励950食神点",
        image = "card_data_cookery_3_15",
        description = "刺客忍者最爱的食物。加入海星调味后，更是鲜美无比，让食客们发挥无限潜能",
        sources = listOf(
            CookerySource(
                title = "冥神辅助礼包",
                description = "礼包内包含本食神谱，可通过周氪、贵宾返利等途径获得",
                icons = listOf("x13affc40"),
            ),
            CookerySource(
                title = "星能碎片",
                description = "通过每日充值(日氪)收集星能碎片，使用2600个星能碎片进行兑换",
                icons = listOf("x12603750"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11130200", "star_image_11"),
            CookeryRecipe("x111300b0", "star_image_11"),
            CookeryRecipe("x11130060", "star_image_10"),
            CookeryRecipe("x11430030", "star_image_10"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升12%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("哈迪斯神使", "x1113120a"),
                ),
            ),
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升10%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("猪猪料理机", "x11131200"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "老北京涮锅",
        category = "极品佳肴",
        reward = "奖励830食神点",
        image = "card_data_cookery_3_16",
        description = "高端的食材往往只需要采用最朴素的烹饪方式，清水涮羊肉便是其中之一",
        sources = listOf(
            CookerySource(
                title = "点券兑换",
                description = "可前往假期特惠-节日兑换版块，使用15000点券+50幸运金币进行兑换",
                icons = listOf("x12608b30", "x1260a430"),
            ),
            CookerySource(
                title = "星能碎片",
                description = "通过每日充值(日氪)收集星能碎片，使用600个星能碎片进行兑换",
                icons = listOf("x12603750"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11130240", "star_image_9"),
            CookeryRecipe("x11130250", "star_image_9"),
            CookeryRecipe("x11130260", "star_image_9"),
            CookeryRecipe("x111310c0", "star_image_8"),
            CookeryRecipe("x11120040", "star_image_8"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升10%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("肥牛火锅", "x11130240"),
                    CookeryAuxiliaryCard("麻辣香锅", "x11130250"),
                    CookeryAuxiliaryCard("生煎锅", "x11130260"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "菲丽水",
        category = "极品佳肴",
        reward = "奖励1240食神点",
        image = "card_data_cookery_3_17",
        description = "使用世界上最昂贵的水，可以喷出世界上威力最大的水子弹",
        sources = listOf(
            CookerySource(
                title = "食神谱碎片",
                description = "可在限时送礼(百万消费)活动中，消费指定数量的点券后可获得食神谱碎片，使用10个碎片进行兑换",
                icons = listOf("x12614460"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11130060", "star_image_11"),
            CookeryRecipe("x11130014", "star_image_11"),
            CookeryRecipe("x11131034", "star_image_10"),
            CookeryRecipe("x11130140", "star_image_9"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升25%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("阿瑞斯神使", "x1113000a"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "汉帝茅台",
        category = "极品佳肴",
        reward = "奖励1240食神点",
        image = "card_data_cookery_3_18",
        description = "美食国的珍贵国酒，可以让食物发挥无限潜能",
        sources = listOf(
            CookerySource(
                title = "食神谱碎片",
                description = "可在限时送礼(百万消费)活动中，消费指定数量的点券后可获得食神谱碎片，使用10个碎片进行兑换",
                icons = listOf("x12614460"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x111300b0", "star_image_11"),
            CookeryRecipe("x111304e0", "star_image_11"),
            CookeryRecipe("x11130020", "star_image_10"),
            CookeryRecipe("x11120460", "star_image_9"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升20%，攻速提升5%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("丘比特神使", "x111300ea"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "怀石料理",
        category = "极品佳肴",
        reward = "奖励1240食神点",
        image = "card_data_cookery_3_19",
        description = "能够治愈人心的顶级盛宴",
        sources = listOf(
            CookerySource(
                title = "食神谱碎片",
                description = "可在限时送礼(百万消费)活动中，消费指定数量的点券后可获得食神谱碎片，使用10个碎片进行兑换",
                icons = listOf("x12614460"),
            ),
            CookerySource(
                title = "星能碎片",
                description = "通过每日充值(日氪)收集星能碎片，使用2600个星能碎片进行兑换",
                icons = listOf("x12603750"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11930030", "star_image_11"),
            CookeryRecipe("x11130080", "star_image_11"),
            CookeryRecipe("x111300a0", "star_image_10"),
            CookeryRecipe("x11130094", "star_image_9"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击间隔减少2秒，冷却时间减少2秒，攻击力提升10%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("盖亚神使", "x1112065a"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "复活节彩蛋",
        category = "极品佳肴",
        reward = "奖励1240食神点",
        image = "card_data_cookery_3_20",
        description = "复活节当日制作的彩绘鸡蛋，食用后可以投掷出威力更大的子弹",
        sources = listOf(
            CookerySource(
                title = "食神谱碎片",
                description = "可在限时送礼(百万消费)活动中，消费指定数量的点券后可获得食神谱碎片，使用10个碎片进行兑换",
                icons = listOf("x12614460"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x111300a0", "star_image_11"),
            CookeryRecipe("x11930020", "star_image_11"),
            CookeryRecipe("x11130094", "star_image_10"),
            CookeryRecipe("x11121020", "star_image_9"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升15%，攻速提升5%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("索尔神使", "x111300ca"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "铁板鱿鱼",
        category = "极品佳肴",
        reward = "奖励1240食神点",
        image = "card_data_cookery_3_21",
        description = "充满烟火气的街边小吃，最能抚慰凡人的心灵",
        sources = listOf(
            CookerySource(
                title = "食神谱碎片",
                description = "可在限时送礼(百万消费)活动中，消费指定数量的点券后可获得食神谱碎片，使用10个碎片进行兑换",
                icons = listOf("x12614460"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11130064", "star_image_11"),
            CookeryRecipe("x11430030", "star_image_11"),
            CookeryRecipe("x11190141", "star_image_10"),
            CookeryRecipe("x11130280", "star_image_9"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片冷却时间减少20秒，攻速提升5%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("狄安娜神使", "x111300da"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "法式焗海螺",
        category = "极品佳肴",
        reward = "奖励1240食神点",
        image = "card_data_cookery_3_22",
        description = "美味的蔬菜焗海螺在浪漫的法式餐具装点下，更加香气四溢了",
        sources = listOf(
            CookerySource(
                title = "食神谱碎片",
                description = "可在限时送礼(百万消费)活动中，消费指定数量的点券后可获得食神谱碎片，使用10个碎片进行兑换",
                icons = listOf("x12614460"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11120950", "star_image_11"),
            CookeryRecipe("x11120460", "star_image_11"),
            CookeryRecipe("x111300b0", "star_image_10"),
            CookeryRecipe("x11130060", "star_image_9"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升15%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("里格神使", "x1112206a"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "普罗旺斯鱼汤",
        category = "极品佳肴",
        reward = "奖励1240食神点",
        image = "card_data_cookery_3_23",
        description = "将八分熟的灯笼鱼片淋上撒着辣椒粉的热汤，口感层次丰富，回味无穷",
        sources = listOf(
            CookerySource(
                title = "食神谱碎片",
                description = "可在限时送礼(百万消费)活动中，消费指定数量的点券后可获得食神谱碎片，使用10个碎片进行兑换",
                icons = listOf("x12614460"),
            ),
            CookerySource(
                title = "星能碎片",
                description = "通过每日充值(日氪)收集星能碎片，使用2600个星能碎片进行兑换",
                icons = listOf("x12603750"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11120460", "star_image_11"),
            CookeryRecipe("x111300b0", "star_image_11"),
            CookeryRecipe("x11130094", "star_image_10"),
            CookeryRecipe("x11130090", "star_image_9"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升15%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("奥丁神使", "x1112064a"),
                ),
            ),
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升10%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("卖萌喵", "x111311a0"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "锤子肉片",
        category = "极品佳肴",
        reward = "奖励1240食神点",
        image = "card_data_cookery_3_24",
        description = "锤薄的肉片烫熟后，加上调味品并淋上热油，最后放一勺灵魂辣椒酱，鲜嫩爽口",
        sources = listOf(
            CookerySource(
                title = "食神谱碎片",
                description = "可在限时送礼(百万消费)活动中，消费指定数量的点券后可获得食神谱碎片，使用10个碎片进行兑换",
                icons = listOf("x12614460"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x111304c0", "star_image_11"),
            CookeryRecipe("x111300b0", "star_image_11"),
            CookeryRecipe("x111300a0", "star_image_10"),
            CookeryRecipe("x111301a0", "star_image_9"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升15%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("尤弥尔神使", "x1112221a"),
                ),
            ),
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升10%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("窃蛋龙", "x11122120"),
                    CookeryAuxiliaryCard("勺勺兔", "x11122050"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "幻露流光",
        category = "极品佳肴",
        reward = "奖励1240食神点",
        image = "card_data_cookery_3_25",
        description = "源自神仙秘境的至臻饮品，蕴含无尽魔力与祝福。同时象征着纯净、不朽的力量",
        sources = listOf(
            CookerySource(
                title = "食神谱碎片",
                description = "可在限时送礼(百万消费)活动中，消费指定数量的点券后可获得食神谱碎片，使用20个碎片进行兑换",
                icons = listOf("x12614460"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11930090", "star_image_11"),
            CookeryRecipe("x11122260", "star_image_11"),
            CookeryRecipe("x11930030", "star_image_10"),
            CookeryRecipe("x11430030", "star_image_9"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升80%，冷却时间减少13秒",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("宙斯神使", "x1193009a"),
                ),
            ),
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升50%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("雅典娜守护", "x11930090"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "椰子西米露",
        category = "极品佳肴",
        reward = "奖励1240食神点",
        image = "card_data_cookery_3_26",
        description = "巧妙地将Q弹的西米和浓郁的椰汁完美结合，除了清凉解暑之外，还能为食用者补充能量",
        sources = listOf(
            CookerySource(
                title = "食神谱碎片",
                description = "可在限时送礼(百万消费)活动中，消费指定数量的点券后可获得食神谱碎片，使用10个碎片进行兑换",
                icons = listOf("x12614460"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11240010", "star_image_11"),
            CookeryRecipe("x11250010", "star_image_11"),
            CookeryRecipe("x11140034", "star_image_10"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片体力提升22%，冷却时间减少3秒",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("赫拉神使", "x1124002a"),
                ),
            ),
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片体力提升18%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("祥龙环", "x11240030"),
                ),
            ),
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片体力提升10%，冷却时间减少5秒",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("彩虹蛇", "x11240040"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "明炉炭烧",
        category = "极品佳肴",
        reward = "奖励1240食神点",
        image = "card_data_cookery_3_27",
        description = "肉片在烤盘上滋滋作响，油脂与炭火交织出美妙的旋律，香气四溢，令人垂涎欲滴",
        sources = listOf(
            CookerySource(
                title = "食神谱碎片",
                description = "可在限时送礼(百万消费)活动中，消费指定数量的点券后可获得食神谱碎片，使用10个碎片进行兑换",
                icons = listOf("x12614460"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11130260", "star_image_11"),
            CookeryRecipe("x11130250", "star_image_11"),
            CookeryRecipe("x11130240", "star_image_10"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升15%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("祝融神使", "x1112090a"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "冰糖葫芦",
        category = "极品佳肴",
        reward = "奖励1180食神点",
        image = "card_data_cookery_3_28",
        description = "颗颗饱满的红山楂，裹上一层晶莹剔透的冰糖外壳，是冬日里的一抹甜蜜记忆！",
        sources = listOf(
            CookerySource(
                title = "食神谱碎片",
                description = "可在限时送礼(百万消费)活动中，消费指定数量的点券后可获得食神谱碎片，使用10个碎片进行兑换",
                icons = listOf("x12614460"),
            ),
            CookerySource(
                title = "星能碎片",
                description = "通过每日充值(日氪)收集星能碎片，使用2450个星能碎片进行兑换",
                icons = listOf("x12603750"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11130280", "star_image_11"),
            CookeryRecipe("x11120690", "star_image_11"),
            CookeryRecipe("x11122220", "star_image_10"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升15%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("赫丘利神使", "x1112041a"),
                    CookeryAuxiliaryCard("糖葫芦炮弹", "x11130280"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "海鲜大咖",
        category = "极品佳肴",
        reward = "奖励950食神点",
        image = "card_data_cookery_3_29",
        description = "海鲜的鲜美与特制酱料的醇香完美融合，经过精心烹饪后，散发出令人难以抗拒的香气",
        sources = listOf(
            CookerySource(
                title = "金食神宝箱",
                description = "前往火山遗迹-奶昔基地/圣代基地，收集金食神宝箱菜谱碎片，集齐50个碎片可前往假期特惠-长期兑换版块兑换金食神宝箱",
                icons = listOf("x12602820", "x13b12630"),
            ),
            CookerySource(
                title = "星能碎片",
                description = "通过每日充值(日氪)收集星能碎片，使用900个星能碎片进行兑换",
                icons = listOf("x12603750"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11130200", "star_image_9"),
            CookeryRecipe("x11140034", "star_image_9"),
            CookeryRecipe("x111301a0", "star_image_9"),
            CookeryRecipe("x11370034", "star_image_8"),
            CookeryRecipe("x11430030", "star_image_8"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升10%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("炭烧海星", "x11130200"),
                    CookeryAuxiliaryCard("仙人球海星刺身", "x11700120"),
                ),
            ),
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升20%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("咖喱龙虾炮", "x111301a0"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "沙坑羊排",
        category = "极品佳肴",
        reward = "奖励950食神点",
        image = "card_data_cookery_3_30",
        description = "经过酱料腌制的上等羊排，在沙坑中慢烤至外皮金黄酥脆，咬上一口，外焦里嫩，香气四溢",
        sources = listOf(
            CookerySource(
                title = "金食神宝箱",
                description = "前往火山遗迹-奶昔基地/圣代基地，收集金食神宝箱菜谱碎片，集齐50个碎片可前往假期特惠-长期兑换版块兑换金食神宝箱",
                icons = listOf("x12602820", "x13b12630"),
            ),
            CookerySource(
                title = "点券兑换",
                description = "可前往假期特惠-节日兑换版块，使用15000点券+50幸运金币进行兑换",
                icons = listOf("x12608b30", "x1260a430"),
            ),
            CookerySource(
                title = "星能碎片",
                description = "通过每日充值(日氪)收集星能碎片，使用650个星能碎片进行兑换",
                icons = listOf("x12603750"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x111304c0", "star_image_9"),
            CookeryRecipe("x111304e0", "star_image_9"),
            CookeryRecipe("x11121060", "star_image_9"),
            CookeryRecipe("x11140210", "star_image_8"),
            CookeryRecipe("x11130270", "star_image_8"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升10%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("烤蜥蜴投手", "x111304c0"),
                ),
            ),
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升15%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("仙人掌刺身", "x111304e0"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "雷城全家桶",
        category = "极品佳肴",
        reward = "奖励950食神点",
        image = "card_data_cookery_3_31",
        description = "一份满足全家口味的欢乐盛宴！无论是炸灯笼鱼条还是莓果味小甜品，每一口都是对味蕾的极致诱惑",
        sources = listOf(
            CookerySource(
                title = "铜食神宝箱",
                description = "前往火山遗迹-果仁瀑布/榛子瀑布，收集铜食神宝箱菜谱碎片，集齐10个碎片可前往假期特惠-长期兑换版块兑换铜食神宝箱",
                icons = listOf("x12602800", "x13b12430"),
            ),
            CookerySource(
                title = "点券兑换",
                description = "可前往假期特惠-节日兑换版块，使用15000点券+50幸运金币进行兑换",
                icons = listOf("x12608b30", "x1260a430"),
            ),
            CookerySource(
                title = "星能碎片",
                description = "通过每日充值(日氪)收集星能碎片，使用680个星能碎片进行兑换",
                icons = listOf("x12603750"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11120460", "star_image_9"),
            CookeryRecipe("x11120470", "star_image_9"),
            CookeryRecipe("x11370060", "star_image_9"),
            CookeryRecipe("x11120450", "star_image_8"),
            CookeryRecipe("x11120034", "star_image_8"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升15%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("炙烤灯笼鱼", "x11120460"),
                ),
            ),
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片冷却时间减少3秒",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("莓果点心", "x11120470"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "奇境下午茶",
        category = "极品佳肴",
        reward = "奖励950食神点",
        image = "card_data_cookery_3_32",
        description = "精致的餐盘飘散出缕缕茶香，酥脆的炸蘑菇再配上精致的三明治或是口感丰富的怪味鱿鱼，让人在忙碌的生活中找到一丝安宁与惬意",
        sources = listOf(
            CookerySource(
                title = "银食神宝箱",
                description = "前往火山遗迹-黑提丛林/蓝莓丛林，收集银食神宝箱菜谱碎片，集齐25个碎片可前往假期特惠-长期兑换版块兑换银食神宝箱",
                icons = listOf("x12602810", "x13b12530"),
            ),
            CookerySource(
                title = "点券兑换",
                description = "可前往假期特惠-节日兑换版块，使用15000点券+50幸运金币进行兑换",
                icons = listOf("x12608b30", "x1260a430"),
            ),
            CookerySource(
                title = "星能碎片",
                description = "通过每日充值(日氪)收集星能碎片，使用950个星能碎片进行兑换",
                icons = listOf("x12603750"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11120690", "star_image_9"),
            CookeryRecipe("x11120760", "star_image_9"),
            CookeryRecipe("x11250010", "star_image_9"),
            CookeryRecipe("x11120680", "star_image_8"),
            CookeryRecipe("x111911c0", "star_image_8"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升15%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("耗油双菇", "x11120690"),
                    CookeryAuxiliaryCard("热狗耗油双菇", "x11700060"),
                    CookeryAuxiliaryCard("怪味鱿鱼", "x11120760"),
                    CookeryAuxiliaryCard("火影怪味鱿鱼", "x11700010"),
                ),
            ),
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片体力提升20%，冷却时间减少1秒",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("扑克牌护罩", "x11250010"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "M双层堡",
        category = "极品佳肴",
        reward = "奖励1240食神点",
        image = "card_data_cookery_3_33",
        description = "焦香的洋葱裹挟着炙烤安格斯牛肉，一口咬下之后，黑松露酱的浓郁香味在味蕾上绽放",
        sources = listOf(
            CookerySource(
                title = "食神谱碎片",
                description = "可在限时送礼(百万消费)活动中，消费指定数量的点券后可获得食神谱碎片，使用10个碎片进行兑换",
                icons = listOf("x12614460"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11120660", "star_image_11"),
            CookeryRecipe("x11130140", "star_image_10"),
            CookeryRecipe("x11120690", "star_image_9"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升65%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("埃罗斯神使", "x1112236a"),
                ),
            ),
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升25%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("弹簧虎", "x11120660"),
                ),
            ),
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升15%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("泡泡龙", "x11122180"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "太阳饼",
        category = "极品佳肴",
        reward = "奖励1240食神点",
        image = "card_data_cookery_3_34",
        description = "一口咬下，金黄酥皮层层掉落，内里麦芽糖心甜而不腻，是阳光下最甜蜜的酥脆滋味",
        sources = listOf(
            CookerySource(
                title = "食神谱碎片",
                description = "可在限时送礼(百万消费)活动中，消费指定数量的点券后可获得食神谱碎片，使用10个碎片进行兑换",
                icons = listOf("x12614460"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11110014", "star_image_11"),
            CookeryRecipe("x11930040", "star_image_10"),
            CookeryRecipe("x11111024", "star_image_10"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片生产时间减少2秒，火苗产量提升25%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("阿波罗神使", "x1111005a"),
                ),
            ),
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片生产时间减少2秒，火苗产量提升35%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("咕咕鸡", "x11111324"),
                ),
            ),
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片火苗产量提升30%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("小火炉", "x11110014"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "杏仁芝麻茶",
        category = "极品佳肴",
        reward = "奖励1240食神点",
        image = "card_data_cookery_3_35",
        description = "白色杏仁茶中加入香浓黑芝麻糊，在节庆或宴席中，此类阴阳调和的搭配也寓意“和谐圆满”",
        sources = listOf(
            CookerySource(
                title = "食神谱碎片",
                description = "可在限时送礼(百万消费)活动中，消费指定数量的点券后可获得食神谱碎片，使用10个碎片进行兑换",
                icons = listOf("x12614460"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11120450", "star_image_11"),
            CookeryRecipe("x11370025", "star_image_10"),
            CookeryRecipe("x11930030", "star_image_9"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升65%，冷却时间减少14秒",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("黑暗神使", "x1112203a"),
                ),
            ),
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升65%，冷却时间减少30秒",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("巴德尔神使", "x1112256a"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "冰摇桃桃",
        category = "极品佳肴",
        reward = "奖励1240食神点",
        image = "card_data_cookery_3_36",
        description = "把阳光下的蜜桃和碎冰一起摇匀倒入杯中，每一口都能品尝到微风穿过果园的清凉",
        sources = listOf(
            CookerySource(
                title = "食神谱碎片",
                description = "可在限时送礼(百万消费)活动中，消费指定数量的点券后可获得食神谱碎片，使用10个碎片进行兑换",
                icons = listOf("x12614460"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11931060", "star_image_11"),
            CookeryRecipe("x11122260", "star_image_10"),
            CookeryRecipe("x11370060", "star_image_9"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升55%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("波塞冬神使", "x1193007a"),
                ),
            ),
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升35%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("巫蛊蛇", "x11122470"),
                    CookeryAuxiliaryCard("元气牛", "x11120500"),
                ),
            ),
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升25%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("可乐汪", "x11930084"),
                    CookeryAuxiliaryCard("转转鸡", "x11930374"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "烈焰熔岩蛋糕",
        category = "极品佳肴",
        reward = "奖励1240食神点",
        image = "card_data_cookery_3_37",
        description = "巧克力蛋糕切开后，香浓的棕色流心如岩浆般涌出，入口先苦后甜",
        sources = listOf(
            CookerySource(
                title = "食神谱碎片",
                description = "可在限时送礼(百万消费)活动中，消费指定数量的点券后可获得食神谱碎片，使用10个碎片进行兑换",
                icons = listOf("x12614460"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x111304e0", "star_image_11"),
            CookeryRecipe("x111300b0", "star_image_11"),
            CookeryRecipe("x11120460", "star_image_10"),
            CookeryRecipe("x11120940", "star_image_9"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升40%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("后羿神使", "x1112244a"),
                ),
            ),
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升30%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("法师蛇", "x11122390"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "火焰冰淇淋",
        category = "极品佳肴",
        reward = "奖励1240食神点",
        image = "card_data_cookery_3_38",
        description = "绵密的牛奶冰淇淋，浇上朗姆酒并点燃，呈现冰火两重天的口感",
        sources = listOf(
            CookerySource(
                title = "食神谱碎片",
                description = "可在限时送礼(百万消费)活动中，消费指定数量的点券后可获得食神谱碎片，使用10个碎片进行兑换",
                icons = listOf("x12614460"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11130240", "star_image_11"),
            CookeryRecipe("x11130090", "star_image_11"),
            CookeryRecipe("x11130260", "star_image_10"),
            CookeryRecipe("x11120450", "star_image_9"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升30%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("赫斯提亚神使", "x1112248a"),
                ),
            ),
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升35%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("烈焰龙", "x11122110"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "营地炖肉",
        category = "极品佳肴",
        reward = "奖励950食神点",
        image = "card_data_cookery_3_39",
        description = "香浓的肉味、柴火的烟熏味和香料交织在一起，顺着晚风钻进鼻腔",
        sources = listOf(
            CookerySource(
                title = "金食神宝箱",
                description = "前往火山遗迹-奶昔基地/圣代基地，收集金食神宝箱菜谱碎片，集齐50个碎片可前往假期特惠-长期兑换版块兑换金食神宝箱",
                icons = listOf("x12602820", "x13b12630"),
            ),
            CookerySource(
                title = "点券兑换",
                description = "可前往假期特惠-节日兑换版块，使用15000点券+50幸运金币进行兑换",
                icons = listOf("x12608b30", "x1260a430"),
            ),
            CookerySource(
                title = "星能碎片",
                description = "通过每日充值(日氪)收集星能碎片，使用599个星能碎片进行兑换",
                icons = listOf("x12603750"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11120950", "star_image_9"),
            CookeryRecipe("x11120940", "star_image_9"),
            CookeryRecipe("x11120760", "star_image_9"),
            CookeryRecipe("x11140210", "star_image_8"),
            CookeryRecipe("x11121060", "star_image_8"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升15%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("坚果爆炒机", "x11120950"),
                ),
            ),
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片冷却时间减少3秒",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("糖渍刺梨", "x11120940"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "星云奶茶",
        category = "极品佳肴",
        reward = "奖励950食神点",
        image = "card_data_cookery_3_40",
        description = "茶叶来自汉堡王星的零重力茶园，带着淡淡的月桂与蜂蜜的香气",
        sources = listOf(
            CookerySource(
                title = "金食神宝箱",
                description = "前往火山遗迹-奶昔基地/圣代基地，收集金食神宝箱菜谱碎片，集齐50个碎片可前往假期特惠-长期兑换版块兑换金食神宝箱",
                icons = listOf("x12602820", "x13b12630"),
            ),
            CookerySource(
                title = "点券兑换",
                description = "可前往假期特惠-节日兑换版块，使用15000点券+50幸运金币进行兑换",
                icons = listOf("x12608b30", "x1260a430"),
            ),
            CookerySource(
                title = "星能碎片",
                description = "通过每日充值(日氪)收集星能碎片，使用700个星能碎片进行兑换",
                icons = listOf("x12603750"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11122250", "star_image_9"),
            CookeryRecipe("x11122260", "star_image_9"),
            CookeryRecipe("x11122220", "star_image_9"),
            CookeryRecipe("x11122230", "star_image_8"),
            CookeryRecipe("x111300f0", "star_image_8"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升15%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("梦幻多拿滋", "x11122250"),
                ),
            ),
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升20%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("冰块冷萃机", "x11122260"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "多元料理",
        category = "极品佳肴",
        reward = "奖励950食神点",
        image = "card_data_cookery_3_41",
        description = "如果有让你不知如何下筷的美食，那一定是这份五花八门的料理",
        sources = listOf(
            CookerySource(
                title = "金食神宝箱",
                description = "前往火山遗迹-奶昔基地/圣代基地，收集金食神宝箱菜谱碎片，集齐50个碎片可前往假期特惠-长期兑换版块兑换金食神宝箱",
                icons = listOf("x12602820", "x13b12630"),
            ),
            CookerySource(
                title = "点券兑换",
                description = "可前往假期特惠-节日兑换版块，使用15000点券+50幸运金币进行兑换",
                icons = listOf("x12608b30", "x1260a430"),
            ),
            CookerySource(
                title = "星能碎片",
                description = "通过每日充值(日氪)收集星能碎片，使用650个星能碎片进行兑换",
                icons = listOf("x12603750"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11122490", "star_image_10"),
            CookeryRecipe("x11122520", "star_image_9"),
            CookeryRecipe("x11122500", "star_image_9"),
            CookeryRecipe("x111300a0", "star_image_8"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升20%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("迷你披萨炉", "x11122490"),
                    CookeryAuxiliaryCard("全能糖球投手", "x11122520"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "印度飞饼",
        category = "极品佳肴",
        reward = "奖励1240食神点",
        image = "card_data_cookery_3_42",
        description = "薄如蝉翼的面皮在厨师指尖飞舞，成就了香脆交织的独特口感",
        sources = listOf(
            CookerySource(
                title = "食神谱碎片",
                description = "可在限时送礼(百万消费)活动中，消费指定数量的点券后可获得食神谱碎片，使用10个碎片进行兑换",
                icons = listOf("x12614460"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11122490", "star_image_11"),
            CookeryRecipe("x11122880", "star_image_11"),
            CookeryRecipe("x111300b0", "star_image_10"),
            CookeryRecipe("x11130200", "star_image_9"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升30%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("赖皮蛇", "x11122400"),
                ),
            ),
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升35%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("鲁班神使", "x1112270a"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "暴打柠檬茶",
        category = "极品佳肴",
        reward = "奖励1240食神点",
        image = "card_data_cookery_3_43",
        description = "在杯中大力捶打搅拌，使柠檬的酸味和香气充分释放",
        sources = listOf(
            CookerySource(
                title = "食神谱碎片",
                description = "可在限时送礼(百万消费)活动中，消费指定数量的点券后可获得食神谱碎片，使用20个碎片进行兑换",
                icons = listOf("x12614460"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11122500", "star_image_11"),
            CookeryRecipe("x11122860", "star_image_11"),
            CookeryRecipe("x11190141", "star_image_10"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片增幅倍率提升1.2",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("魔杖蛇", "x11122530"),
                ),
            ),
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片增幅倍率提升1.5",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("塔拉萨神使", "x1112277a"),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "米其林雀烧",
        category = "极品佳肴",
        reward = "奖励1240食神点",
        image = "card_data_cookery_3_44",
        description = "浓浓的照烧酱被烤成琥珀色，油脂滴落激起一缕细烟，焦香四溢",
        sources = listOf(
            CookerySource(
                title = "食神谱碎片",
                description = "可在限时送礼(百万消费)活动中，消费指定数量的点券后可获得食神谱碎片，使用10个碎片进行兑换",
                icons = listOf("x12614460"),
            ),
        ),
        recipes = listOf(
            CookeryRecipe("x11122880", "star_image_11"),
            CookeryRecipe("x11122490", "star_image_11"),
            CookeryRecipe("x11122860", "star_image_10"),
            CookeryRecipe("x11122230", "star_image_9"),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升40%，攻速降低0.6秒",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("神农氏神使", "x1112298a"),
                ),
            ),
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升30%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("炎凰马", "x11122780"),
                ),
            ),
        ),
    ),
)
