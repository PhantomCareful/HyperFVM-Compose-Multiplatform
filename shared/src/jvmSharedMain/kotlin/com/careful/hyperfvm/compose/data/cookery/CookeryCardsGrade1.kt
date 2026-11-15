package com.careful.hyperfvm.compose.data.cookery

import com.careful.hyperfvm.compose.*


/**
 * 上等佳肴的食神谱卡片，对应 Java 版 `fragment_bottom_sheet_cookery_1_*` 布局与
 * `CookeryBottomSheetFragment_1_*` 的绑定结果（由 `tool/cookery_gen.ps1` 生成，勿手改）。
 */
internal val CookeryGrade1Cards: List<CookeryCard> = listOf(
    CookeryCard(
        name = "南翔小笼包",
        category = "上等佳肴",
        reward = "奖励40食神点",
        image = Res.drawable.card_data_cookery_1_1,
        description = "南翔小笼包，百年历史风味小吃，深受顾客欢迎",
        sources = listOf(
            CookerySource(
                title = "地图掉落",
                description = "玛奇朵港(击败BOSS)、吉拉朵港(击败BOSS)",
                icons = listOf(Res.drawable.gangkou_fan_a, Res.drawable.gangkou_fan_b, Res.drawable.qiyu_key),
            ),
        ),
        recipes = listOf(
            CookeryRecipe(Res.drawable.x11130014, Res.drawable.star_image_4),
            CookeryRecipe(Res.drawable.x11130050, Res.drawable.star_image_4),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片体力提升20%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("小笼包", Res.drawable.x11130014),
                    CookeryAuxiliaryCard("双层小笼包", Res.drawable.x11130020),
                    CookeryAuxiliaryCard("三向小笼包", Res.drawable.x11130100),
                    CookeryAuxiliaryCard("机枪小笼包", Res.drawable.x11930010),
                    CookeryAuxiliaryCard("冰冻小笼包", Res.drawable.x11130050),
                    CookeryAuxiliaryCard("双层冰冻小笼包", Res.drawable.x11130160),
                    CookeryAuxiliaryCard("三向冰冻小笼包", Res.drawable.x11130170),
                    CookeryAuxiliaryCard("机枪冰冻小笼包", Res.drawable.x11930070),
                    CookeryAuxiliaryCard("包包龙", Res.drawable.x11122320),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "咖啡奶茶",
        category = "上等佳肴",
        reward = "奖励80食神点",
        image = Res.drawable.card_data_cookery_1_2,
        description = "奶茶妹子与咖啡小哥的香浓结晶",
        sources = listOf(
            CookerySource(
                title = "铜食神宝箱",
                description = "前往火山遗迹-果仁瀑布/榛子瀑布，收集铜食神宝箱菜谱碎片，集齐10个碎片可前往假期特惠-长期兑换版块兑换铜食神宝箱",
                icons = listOf(Res.drawable.copper_cookery_box_part, Res.drawable.copper_cookery_box),
            ),
        ),
        recipes = listOf(
            CookeryRecipe(Res.drawable.x11131034, Res.drawable.star_image_5),
            CookeryRecipe(Res.drawable.x11431014, Res.drawable.star_image_5),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片冷却时间减少1秒",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("咖啡杯", Res.drawable.x11131034),
                    CookeryAuxiliaryCard("机枪咖啡杯", Res.drawable.x11700050),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "美食豪华套餐",
        category = "上等佳肴",
        reward = "奖励50食神点",
        image = Res.drawable.card_data_cookery_1_3,
        description = "必备快餐，挽救了不少早上要迟到的少年少女",
        sources = listOf(
            CookerySource(
                title = "地图掉落",
                description = "玛奇朵港(击败BOSS)、吉拉朵港(击败BOSS)",
                icons = listOf(Res.drawable.gangkou_fan_a, Res.drawable.gangkou_fan_b, Res.drawable.qiyu_key),
            ),
        ),
        recipes = listOf(
            CookeryRecipe(Res.drawable.x11120034, Res.drawable.star_image_4),
            CookeryRecipe(Res.drawable.x111300f0, Res.drawable.star_image_5),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片体力提升40%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("汉堡包", Res.drawable.x111300f0),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "冰镇西瓜",
        category = "上等佳肴",
        reward = "奖励100食神点",
        image = Res.drawable.card_data_cookery_1_4,
        description = "夏季常食水果，饱满多汁清爽可口",
        sources = listOf(
            CookerySource(
                title = "地图掉落",
                description = "雪顶火山(击败BOSS)",
                icons = listOf(Res.drawable.huoshan_fan_a, Res.drawable.huoshan_fan_b),
            ),
        ),
        recipes = listOf(
            CookeryRecipe(Res.drawable.x11121020, Res.drawable.star_image_4),
            CookeryRecipe(Res.drawable.x113901d0, Res.drawable.star_image_4),
            CookeryRecipe(Res.drawable.x11240010, Res.drawable.star_image_5),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片体力提升20%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("瓜皮护罩", Res.drawable.x11240010),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "麻辣烫",
        category = "上等佳肴",
        reward = "奖励50食神点",
        image = Res.drawable.card_data_cookery_1_5,
        description = "看上去娇嫩欲滴，越吃越辣，食后意犹未尽",
        sources = listOf(
            CookerySource(
                title = "地图掉落",
                description = "雪顶火山(击败BOSS)",
                icons = listOf(Res.drawable.huoshan_fan_a, Res.drawable.huoshan_fan_b),
            ),
        ),
        recipes = listOf(
            CookeryRecipe(Res.drawable.x11120040, Res.drawable.star_image_4),
            CookeryRecipe(Res.drawable.x11190141, Res.drawable.star_image_5),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片体力提升20%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("火盆", Res.drawable.x11190141),
                    CookeryAuxiliaryCard("刺梨烧烤盘", Res.drawable.x11700080),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "葡式蛋挞",
        category = "上等佳肴",
        reward = "奖励60食神点",
        image = Res.drawable.card_data_cookery_1_6,
        description = "精致圆润的挞皮，金黄的蛋液，香酥松软",
        sources = listOf(
            CookerySource(
                title = "铜食神宝箱",
                description = "前往火山遗迹-果仁瀑布/榛子瀑布，收集铜食神宝箱菜谱碎片，集齐10个碎片可前往假期特惠-长期兑换版块兑换铜食神宝箱",
                icons = listOf(Res.drawable.copper_cookery_box_part, Res.drawable.copper_cookery_box),
            ),
        ),
        recipes = listOf(
            CookeryRecipe(Res.drawable.x111300a0, Res.drawable.star_image_4),
            CookeryRecipe(Res.drawable.x111300d0, Res.drawable.star_image_4),
            CookeryRecipe(Res.drawable.x111300b0, Res.drawable.star_image_4),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片冷却时间减少1秒",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("三线酒架", Res.drawable.x111300b0),
                    CookeryAuxiliaryCard("子母三线酒架", Res.drawable.x11700070),
                    CookeryAuxiliaryCard("煮蛋器投手", Res.drawable.x111300a0),
                    CookeryAuxiliaryCard("雪芭煮蛋器", Res.drawable.x11700020),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "菠萝布丁",
        category = "上等佳肴",
        reward = "奖励50食神点",
        image = Res.drawable.card_data_cookery_1_7,
        description = "菠萝口味，口感甜滑，回味无穷",
        sources = listOf(
            CookerySource(
                title = "铜食神宝箱",
                description = "前往火山遗迹-果仁瀑布/榛子瀑布，收集铜食神宝箱菜谱碎片，集齐10个碎片可前往假期特惠-长期兑换版块兑换铜食神宝箱",
                icons = listOf(Res.drawable.copper_cookery_box_part, Res.drawable.copper_cookery_box),
            ),
        ),
        recipes = listOf(
            CookeryRecipe(Res.drawable.x11930080, Res.drawable.star_image_5),
            CookeryRecipe(Res.drawable.x111911c0, Res.drawable.star_image_5),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片体力提升40%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("樱桃反弹布丁", Res.drawable.x111911c0),
                ),
            ),
        ),
    ),
)
