package com.careful.hyperfvm.compose.data.oddsDisclosure

import com.careful.hyperfvm.compose.data.datapack.DataPackRepository
import com.careful.hyperfvm.compose.platform.AppEnv
import org.json.JSONArray
import org.json.JSONObject

/**
 * 概率公示仓储：两个版块的数据与官方来源都来自数据包内的 JSON
 * （`data/odds_boxes.json`、`data/odds_draws.json`、`data/odds_sources.json`），
 * 官方公示更新时只需替换数据包，不必再发版。表里只存道具 id，图标由界面按 id 从图床取，
 * 不随数据包分发。
 *
 * 数据包安装 / 检测完成后由 [DataPackRepository] 调用 [load] 解析一次；
 * 界面的进入路径都在数据包就绪之后，所以直接读属性即可。文件缺失或解析失败
 * 时退化为空表（来源表退化为 [DefaultSources]）——目录页只显示勘误提醒，不会崩溃。
 */
object OddsDataRepository {

    /** 数据包里没有来源表时的兜底列表，内容与打包脚本里的初始数据一致。 */
    private val DefaultSources = listOf(
        OddsSource("美食大战老鼠 概率公示说明！", "https://my.4399.com/mforums/thread-63198880"),
        OddsSource("美食大战老鼠许愿池概率公示", "https://my.4399.com/yxmsdzls/ziliao/10578/"),
        OddsSource("美食大战老鼠幸运转转公示", "https://td-qqgame-ms.123u.com/ms_truntableRate.html"),
        OddsSource("全新活动 - 欢乐扭蛋概率公示~", "https://my.4399.com/forums/thread-64627590"),
    )

    /** 宝箱概率公示的类别列表，下标与详情页的导航参数一致。 */
    var boxCategories: List<OddsCategory> = emptyList()
        private set

    /** 手工维护的抽奖概率类别（幸运转转由 [TurntableOddsRepository] 动态解析，不在这里）。 */
    var lotteryCategories: List<LotteryCategory> = emptyList()
        private set

    /** 「官方公示合集」弹层里的来源列表，顺序即展示顺序；数据包没有该表时用 [DefaultSources]。 */
    var sources: List<OddsSource> = DefaultSources
        private set

    /** 从数据包读取并解析三张表；每次安装新数据包后重新调用。 */
    fun load(env: AppEnv) {
        boxCategories = runCatching {
            parseBoxes(DataPackRepository.dataFile(env, "odds_boxes.json")?.readText())
        }.getOrDefault(emptyList())
        lotteryCategories = runCatching {
            parseDraws(DataPackRepository.dataFile(env, "odds_draws.json")?.readText())
        }.getOrDefault(emptyList())
        sources = runCatching {
            parseSources(DataPackRepository.dataFile(env, "odds_sources.json")?.readText())
        }.getOrNull()?.takeIf { it.isNotEmpty() } ?: DefaultSources
    }

    /** 数据包被清理 / 移除时调用，避免沿用旧数据。 */
    fun clear() {
        boxCategories = emptyList()
        lotteryCategories = emptyList()
        sources = DefaultSources
    }

    private fun parseBoxes(text: String?): List<OddsCategory> {
        val array = categoriesOf(text) ?: return emptyList()
        return List(array.length()) { categoryIndex ->
            val category = array.getJSONObject(categoryIndex)
            val boxes = category.optJSONArray("boxes") ?: JSONArray()
            OddsCategory(
                title = category.optString("title"),
                boxes = List(boxes.length()) { boxIndex ->
                    val box = boxes.getJSONObject(boxIndex)
                    OddsBox(
                        name = box.optString("name"),
                        image = box.stringOrNull("image"),
                        description = box.stringOrNull("description"),
                        items = parseItems(box.optJSONArray("items")),
                        errata = box.stringOrNull("errata"),
                    )
                },
            )
        }
    }

    private fun parseDraws(text: String?): List<LotteryCategory> {
        val array = categoriesOf(text) ?: return emptyList()
        return List(array.length()) { categoryIndex ->
            val category = array.getJSONObject(categoryIndex)
            val draws = category.optJSONArray("draws") ?: JSONArray()
            LotteryCategory(
                title = category.optString("title"),
                draws = List(draws.length()) { drawIndex ->
                    val draw = draws.getJSONObject(drawIndex)
                    LotteryDraw(
                        name = draw.optString("name"),
                        image = draw.stringOrNull("image"),
                        description = draw.stringOrNull("description"),
                        prizes = parseItems(draw.optJSONArray("prizes")),
                        errata = draw.stringOrNull("errata"),
                    )
                },
            )
        }
    }

    private fun categoriesOf(text: String?): JSONArray? {
        if (text.isNullOrBlank()) return null
        return JSONObject(text).optJSONArray("categories")
    }

    /** 解析「官方公示合集」的来源表；缺标题 / 链接的条目直接丢掉，避免出现点不动的行。 */
    private fun parseSources(text: String?): List<OddsSource> {
        if (text.isNullOrBlank()) return emptyList()
        val array = JSONObject(text).optJSONArray("sources") ?: return emptyList()
        return List(array.length()) { index ->
            val source = array.getJSONObject(index)
            OddsSource(
                title = source.optString("title"),
                url = source.optString("url"),
            )
        }.filter { it.title.isNotBlank() && it.url.isNotBlank() }
    }

    private fun parseItems(array: JSONArray?): List<OddsItem> {
        if (array == null) return emptyList()
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            OddsItem(
                name = item.optString("name"),
                count = item.optInt("count", 1),
                image = item.stringOrNull("image"),
                expected = item.optBoolean("expected"),
                odds = item.optDouble("odds"),
                remoteId = item.stringOrNull("remoteId"),
            )
        }
    }

    /** JSON 里显式 null 与字段缺失都归为 null，交给界面走各自的回退分支。 */
    private fun JSONObject.stringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key)
}
