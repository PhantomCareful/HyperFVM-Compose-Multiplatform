package com.careful.hyperfvm.compose.data.meishi

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.careful.hyperfvm.compose.platform.AppEnv
import com.careful.hyperfvm.compose.platform.KeyValueStore
import org.json.JSONArray
import org.json.JSONObject

/**
 * 温馨礼包的账号表。
 *
 * 列表顺序就是界面顺序：长按拖动排序后整表落盘，下次启动照旧。
 * 整表存成一段 JSON 字符串，字段名与 Java 版备份文件一致（openid / serverName / playerName），
 * 后续接设备份还原时可以直接复用这套编解码。
 *
 * openid 是账号的唯一标识：同一条链接不会存成两张卡，改名与删除也都按它定位。
 */
class MeishiStore(env: AppEnv) {

    private val prefs: KeyValueStore = env.prefs(PrefsName)

    /** 已保存的账号，顺序即展示顺序。 */
    var accounts: List<MeishiAccount> by mutableStateOf(read())
        private set

    /** 等定案的删除：界面已经看不到，落盘等 Snackbar 超时或页面退出。 */
    private var pendingRemoval: PendingRemoval? = null

    private class PendingRemoval(val index: Int, val account: MeishiAccount)

    fun contains(openId: String): Boolean = accounts.any { it.openId == openId }

    /** 追加一个账号；openid 已经存在时返回 false，不产生重复卡片。 */
    fun add(account: MeishiAccount): Boolean {
        if (contains(account.openId)) return false
        save(accounts + account)
        return true
    }

    /**
     * 只改角色名。
     *
     * 链接与区服由 openid 决定，界面上是只读的；角色名允许手动修正，
     * 因为玩家改名之后角色页返回的仍是旧名字。
     */
    fun rename(openId: String, playerName: String) {
        if (accounts.none { it.openId == openId }) return
        save(accounts.map { if (it.openId == openId) it.copy(playerName = playerName) else it })
    }

    /**
     * 入库：openid 已在表里就只刷新角色名，不生成第二张卡。
     *
     * 返回 true 表示这次是更新已有账号，调用方据此提示「已更新」而不是「添加成功」。
     */
    fun upsert(openId: String, serverName: String, playerName: String): Boolean {
        if (contains(openId)) {
            rename(openId, playerName)
            return true
        }
        add(MeishiAccount(openId = openId, serverName = serverName, playerName = playerName))
        return false
    }

    /**
     * 删除账号：界面立刻少一张卡，**落盘推迟**到 [commitRemoval]。
     *
     * 管理页的「删除」后面跟着一个可撤销的 Snackbar：撤销时 [undoRemoval] 把卡片放回原位，
     * Snackbar 自己消失（或用户退出管理页）时才 [commitRemoval] 真正写进存储。
     * 待定项只记「被删的那一条 + 它原来的位置」，期间别的改动（改名、排序）不会跟着回滚。
     */
    fun remove(openId: String): Boolean {
        val index = accounts.indexOfFirst { it.openId == openId }
        if (index < 0) return false
        // 上一条删除还在等定案就又删了一个：先把旧的落盘，一个待定项只能记一条。
        commitRemoval()
        pendingRemoval = PendingRemoval(index, accounts[index])
        accounts = accounts.filterNot { it.openId == openId }
        return true
    }

    /**
     * 敲定待定的删除：把当前列表写盘。没有待定项时什么都不做。
     *
     * [openId] 不为空时只让自己那条生效：连着删两个账号时，前一个 Snackbar 落回来
     * 不能把后一个的删除顺手定案。为 null 是页面退出的兜底，谁在等就定谁。
     */
    fun commitRemoval(openId: String? = null) {
        val pending = pendingRemoval ?: return
        if (openId != null && pending.account.openId != openId) return
        pendingRemoval = null
        prefs.putString(KeyAccounts, encode(accounts))
        prefs.commit()
    }

    /** 撤销待定的删除：卡片按原来的位置回到列表并落盘；[openId] 同 [commitRemoval]。 */
    fun undoRemoval(openId: String? = null) {
        val pending = pendingRemoval ?: return
        if (openId != null && pending.account.openId != openId) return
        pendingRemoval = null
        val restored = accounts.toMutableList().apply {
            add(pending.index.coerceIn(0, size), pending.account)
        }
        save(restored)
    }

    /** 长按拖动后的落点：把 [fromIndex] 挪到 [toIndex]，越界或原地不动都忽略。 */
    fun move(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        if (fromIndex !in accounts.indices || toIndex !in accounts.indices) return
        save(accounts.toMutableList().apply { add(toIndex, removeAt(fromIndex)) })
    }

    private fun save(next: List<MeishiAccount>) {
        accounts = next
        prefs.putString(KeyAccounts, encode(next))
        prefs.commit()
    }

    /** 读盘容错：单条记录缺 openid 就跳过，整段 JSON 坏了就当没有账号，不抛给界面。 */
    private fun read(): List<MeishiAccount> {
        val raw = prefs.getString(KeyAccounts, null)?.takeIf { it.isNotBlank() } ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val openId = item.optString("openid").takeIf { it.isNotEmpty() } ?: continue
                    add(
                        MeishiAccount(
                            openId = openId,
                            serverName = item.optString("serverName"),
                            playerName = item.optString("playerName"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encode(accounts: List<MeishiAccount>): String {
        val array = JSONArray()
        accounts.forEach { account ->
            array.put(
                JSONObject()
                    .put("openid", account.openId)
                    .put("serverName", account.serverName)
                    .put("playerName", account.playerName),
            )
        }
        return array.toString()
    }

    private companion object {
        const val PrefsName = "hyperfvm_meishi"
        const val KeyAccounts = "accounts"
    }
}

/**
 * 进程内唯一的账号表。
 *
 * 仪表盘与管理界面必须读同一份状态：管理界面里加完账号、删完账号，
 * 退回仪表盘时卡片要立刻反映出来，所以这里按 env 缓存一个实例，
 * 而不是像仪表盘首选项那样每次 `remember` 都新建一份。
 */
object MeishiAccounts {

    @Volatile
    private var instance: MeishiStore? = null

    fun of(env: AppEnv): MeishiStore = instance ?: synchronized(this) {
        instance ?: MeishiStore(env).also { instance = it }
    }
}
