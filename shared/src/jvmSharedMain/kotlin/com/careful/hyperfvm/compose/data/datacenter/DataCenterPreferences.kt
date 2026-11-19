package com.careful.hyperfvm.compose.data.datacenter

import com.careful.hyperfvm.compose.platform.AppEnv

/**
 * 数据中心「特色功能」入口的展示顺序，存在 SharedPreferences 里。
 *
 * 只存入口 id 的逗号串：入口本身（标题、说明、目标页）是代码里的固定列表，
 * 版本升级新增入口时，没存过的 id 由界面按默认顺序接在末尾，不会丢排序。
 */
class DataCenterPreferences(env: AppEnv) {

    private val prefs = env.prefs(PrefsName)

    /** 存过的入口 id 顺序；没存过时返回空列表，由界面按默认顺序展示。 */
    fun readOrder(): List<String> =
        prefs.getString(KeyOrder, null)
            .orEmpty()
            .split(",")
            .filter { it.isNotEmpty() }
            .distinct()

    /** 拖动结束后落盘；顺序里只存入口 id，未知 id 读回时由界面过滤。 */
    fun saveOrder(ids: List<String>) {
        prefs.putString(KeyOrder, ids.joinToString(separator = ","))
        prefs.commit()
    }

    private companion object {
        const val PrefsName = "hyperfvm_data_center"
        const val KeyOrder = "featureOrder"
    }
}
