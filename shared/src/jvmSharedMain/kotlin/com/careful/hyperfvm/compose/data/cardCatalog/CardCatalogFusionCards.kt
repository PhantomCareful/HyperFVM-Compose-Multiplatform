package com.careful.hyperfvm.compose.data.cardCatalog

import com.careful.hyperfvm.compose.shared.*


/**
 * 融合卡（Java 版 `card_data_2` 表）的静态卡表，共 12 张。
 *
 * 每张卡按 base_name 归并列出全部形态：一行一个 [CardForm]，名字与卡图一一对应，
 * 顺序就是目录页里的显示顺序（也是 Java 版单卡布局里 ImageView 的顺序）。
 * 增删改卡直接改这一段：字母分节、拼音排序、搜索索引都由 [CardCatalog] 自动推导，
 * 不需要再新增任何布局文件或 strings.xml 常量。
 *
 * 额外的搜索别名写在 [DefenseCard.keywords]；ICU 读错多音字的卡用 [DefenseCard.pinyin] 纠正。
 */
internal val FusionCards: List<DefenseCard> = listOf(
    DefenseCard(
        CardForm("火炉菠萝面包", "x11700000"),
        CardForm("火龙果菠萝包", "x1170000e"),
        CardForm("机枪菠萝面包", "x1170000f"),
    ),
    DefenseCard(
        CardForm("雪芭煮蛋器", "x11700020"),
        CardForm("臭豆腐煮蛋器", "x1170002e"),
        CardForm("终结者煮蛋器", "x1170002f"),
    ),
    DefenseCard(
        CardForm("火影怪味鱿鱼", "x11700010"),
        CardForm("合金怪味鱿鱼", "x1170001e"),
        CardForm("松香怪味鱿鱼", "x1170001f"),
    ),
    DefenseCard(
        CardForm("酱香锅烤栗子", "x11700040"),
        CardForm("可乐香烤栗子", "x1170004e"),
        CardForm("如意香烤栗子", "x1170004f"),
    ),
    DefenseCard(
        CardForm("热狗耗油双菇", "x11700060"),
        CardForm("喷壶耗油双菇", "x1170006e"),
        CardForm("糖葫芦耗油双菇", "x1170006f"),
    ),
    DefenseCard(
        CardForm("子母三线酒架", "x11700070"),
        CardForm("仙人掌花酒架", "x1170007e"),
        CardForm("烤灯笼鱼酒架", "x1170007f"),
    ),
    DefenseCard(
        CardForm("刺梨烧烤盘", "x11700080"),
        CardForm("冰榴莲烤盘", "x1170008e"),
        CardForm("缤纷杂果冰盘", "x1170008f"),
    ),
    DefenseCard(
        CardForm("机枪咖啡杯", "x11700050"),
        CardForm("喷壶咖啡杯", "x1170005e"),
        CardForm("三叠咖啡杯", "x1170005f"),
    ),
    DefenseCard(
        CardForm("葡萄味软糖", "x11700090"),
        CardForm("彩虹棉花软糖", "x1170009e"),
        CardForm("无敌猫猫软糖", "x1170009f"),
    ),
    DefenseCard(
        CardForm("脆心死神大炮", "x11700110"),
        CardForm("霸王死神大炮", "x1170011e"),
        CardForm("乾坤死神大炮", "x1170011f"),
    ),
    DefenseCard(
        CardForm("仙人球海星刺身", "x11700120"),
        CardForm("奶油海星刺身", "x1170012e"),
        CardForm("王冠海星刺身", "x1170012f"),
    ),
    DefenseCard(
        CardForm("红温旋转咖啡壶", "x11700130"),
        CardForm("果味旋转咖啡壶", "x1170013e"),
        CardForm("觉醒旋转咖啡壶", "x1170013f"),
    ),
)
