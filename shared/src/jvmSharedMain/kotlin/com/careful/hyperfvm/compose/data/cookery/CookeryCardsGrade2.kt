package com.careful.hyperfvm.compose.data.cookery

import com.careful.hyperfvm.compose.shared.*


/**
 * 秘制佳肴的食神谱卡片，对应 Java 版 `fragment_bottom_sheet_cookery_2_*` 布局与
 * `CookeryBottomSheetFragment_2_*` 的绑定结果（由 `tool/cookery_gen.ps1` 生成，勿手改）。
 */
internal val CookeryGrade2Cards: List<CookeryCard> = listOf(
    CookeryCard(
        name = "王府井包子",
        category = "秘制佳肴",
        reward = "奖励150食神点",
        image = Res.drawable.card_data_cookery_2_1,
        description = "王府井包子历史悠久，包子个儿大馅多，鲜美无比",
        sources = listOf(
            CookerySource(
                title = "铜食神宝箱",
                description = "前往火山遗迹-果仁瀑布/榛子瀑布，收集铜食神宝箱菜谱碎片，集齐10个碎片可前往假期特惠-长期兑换版块兑换铜食神宝箱",
                icons = listOf(Res.drawable.copper_cookery_box_part, Res.drawable.copper_cookery_box),
            ),
        ),
        recipes = listOf(
            CookeryRecipe(Res.drawable.x11130020, Res.drawable.star_image_6),
            CookeryRecipe(Res.drawable.x11130160, Res.drawable.star_image_6),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片冷却时间减少1秒",
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
        name = "猫屎咖啡",
        category = "秘制佳肴",
        reward = "奖励450食神点",
        image = Res.drawable.card_data_cookery_2_2,
        description = "别看名字俗气，实为世界上最贵的咖啡",
        sources = listOf(
            CookerySource(
                title = "银食神宝箱",
                description = "前往火山遗迹-黑提丛林/蓝莓丛林，收集银食神宝箱菜谱碎片，集齐25个碎片可前往假期特惠-长期兑换版块兑换银食神宝箱",
                icons = listOf(Res.drawable.silver_cookery_box_part, Res.drawable.silver_cookery_box),
            ),
        ),
        recipes = listOf(
            CookeryRecipe(Res.drawable.x11131034, Res.drawable.star_image_8),
            CookeryRecipe(Res.drawable.x11190190, Res.drawable.star_image_7),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升10%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("咖啡杯", Res.drawable.x11131034),
                    CookeryAuxiliaryCard("机枪咖啡杯", Res.drawable.x11700050),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "酒蒸鸡",
        category = "秘制佳肴",
        reward = "奖励250食神点",
        image = Res.drawable.card_data_cookery_2_3,
        description = "据闻是喝醉了酒的童子鸡掉进蒸锅里制成",
        sources = listOf(
            CookerySource(
                title = "银食神宝箱",
                description = "前往火山遗迹-黑提丛林/蓝莓丛林，收集银食神宝箱菜谱碎片，集齐25个碎片可前往假期特惠-长期兑换版块兑换银食神宝箱",
                icons = listOf(Res.drawable.silver_cookery_box_part, Res.drawable.silver_cookery_box),
            ),
        ),
        recipes = listOf(
            CookeryRecipe(Res.drawable.x11120020, Res.drawable.star_image_7),
            CookeryRecipe(Res.drawable.x11150014, Res.drawable.star_image_6),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片冷却时间减少1秒",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("酒瓶炸弹", Res.drawable.x11120020),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "劲爆鸡米花",
        category = "秘制佳肴",
        reward = "奖励300食神点",
        image = Res.drawable.card_data_cookery_2_4,
        description = "口感酥脆的鸡肉爆米花，快餐店常备食物",
        sources = listOf(
            CookerySource(
                title = "银食神宝箱",
                description = "前往火山遗迹-黑提丛林/蓝莓丛林，收集银食神宝箱菜谱碎片，集齐25个碎片可前往假期特惠-长期兑换版块兑换银食神宝箱",
                icons = listOf(Res.drawable.silver_cookery_box_part, Res.drawable.silver_cookery_box),
            ),
        ),
        recipes = listOf(
            CookeryRecipe(Res.drawable.x11150014, Res.drawable.star_image_6),
            CookeryRecipe(Res.drawable.x111300d0, Res.drawable.star_image_6),
            CookeryRecipe(Res.drawable.x11120034, Res.drawable.star_image_6),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片冷却时间减少1秒",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("面粉袋", Res.drawable.x111300d0),
                ),
            ),
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升10%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("烧鸡", Res.drawable.x11150014),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "葡萄美酒",
        category = "秘制佳肴",
        reward = "奖励900食神点",
        image = Res.drawable.card_data_cookery_2_5,
        description = "葡萄美酒夜光杯，欲饮琵琶马上催",
        sources = listOf(
            CookerySource(
                title = "金食神宝箱",
                description = "前往火山遗迹-奶昔基地/圣代基地，收集金食神宝箱菜谱碎片，集齐50个碎片可前往假期特惠-长期兑换版块兑换金食神宝箱",
                icons = listOf(Res.drawable.gold_cookery_box_part, Res.drawable.gold_cookery_box),
            ),
        ),
        recipes = listOf(
            CookeryRecipe(Res.drawable.x11111024, Res.drawable.star_image_9),
            CookeryRecipe(Res.drawable.x111300b0, Res.drawable.star_image_9),
            CookeryRecipe(Res.drawable.x11121020, Res.drawable.star_image_9),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升10%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("三线酒架", Res.drawable.x111300b0),
                    CookeryAuxiliaryCard("子母三线酒架", Res.drawable.x11700070),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "巧克力雪球",
        category = "秘制佳肴",
        reward = "奖励400食神点",
        image = Res.drawable.card_data_cookery_2_6,
        description = "外观甜美诱人，巧克力味浓郁",
        sources = listOf(
            CookerySource(
                title = "铜食神宝箱",
                description = "前往火山遗迹-果仁瀑布/榛子瀑布，收集铜食神宝箱菜谱碎片，集齐10个碎片可前往假期特惠-长期兑换版块兑换铜食神宝箱",
                icons = listOf(Res.drawable.copper_cookery_box_part, Res.drawable.copper_cookery_box),
            ),
        ),
        recipes = listOf(
            CookeryRecipe(Res.drawable.x113901d0, Res.drawable.star_image_7),
            CookeryRecipe(Res.drawable.x11130080, Res.drawable.star_image_6),
            CookeryRecipe(Res.drawable.x11930030, Res.drawable.star_image_7),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击间隔减少1秒，冷却时间减少2秒",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("巧克力大炮", Res.drawable.x11930030),
                    CookeryAuxiliaryCard("脆心死神大炮", Res.drawable.x11700110),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "冷烤火鸡",
        category = "秘制佳肴",
        reward = "奖励400食神点",
        image = Res.drawable.card_data_cookery_2_7,
        description = "历经冰火两重天的鸡肉，油而不腻飘香千里",
        sources = listOf(
            CookerySource(
                title = "银食神宝箱",
                description = "前往火山遗迹-黑提丛林/蓝莓丛林，收集银食神宝箱菜谱碎片，集齐25个碎片可前往假期特惠-长期兑换版块兑换银食神宝箱",
                icons = listOf(Res.drawable.silver_cookery_box_part, Res.drawable.silver_cookery_box),
            ),
        ),
        recipes = listOf(
            CookeryRecipe(Res.drawable.x11150014, Res.drawable.star_image_6),
            CookeryRecipe(Res.drawable.x11121020, Res.drawable.star_image_7),
            CookeryRecipe(Res.drawable.x11190141, Res.drawable.star_image_7),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片冷却时间减少1秒",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("冰桶炸弹", Res.drawable.x11121020),
                    CookeryAuxiliaryCard("火盆", Res.drawable.x11190141),
                    CookeryAuxiliaryCard("刺梨烧烤盘", Res.drawable.x11700080),
                ),
            ),
        ),
    ),
    CookeryCard(
        name = "麻婆豆腐",
        category = "秘制佳肴",
        reward = "奖励150食神点",
        image = Res.drawable.card_data_cookery_2_8,
        description = "特色在于麻辣烫香酥嫩鲜活，称之为八字箴言",
        sources = listOf(
            CookerySource(
                title = "深渊/海神宝箱",
                description = "每充值100点券可获得1积分，可在积分商城-神秘商人版块中，使用50积分进行兑换(部分时间打折，35积分)，需注意积分月底清零",
                icons = listOf(Res.drawable.shenyuan_box, Res.drawable.haishen_box),
            ),
        ),
        recipes = listOf(
            CookeryRecipe(Res.drawable.x11130130, Res.drawable.star_image_4),
            CookeryRecipe(Res.drawable.x11120040, Res.drawable.star_image_4),
        ),
        auxiliaryGroups = listOf(
            CookeryAuxiliaryGroup(
                title = "开启此食神谱后，以下卡片攻击力提升10%",
                tips = emptyList(),
                cards = listOf(
                    CookeryAuxiliaryCard("臭豆腐投手", Res.drawable.x11130130),
                ),
            ),
        ),
    ),
)
