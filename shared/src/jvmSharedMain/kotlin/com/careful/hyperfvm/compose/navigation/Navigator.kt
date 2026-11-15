package com.careful.hyperfvm.compose.navigation

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.staticCompositionLocalOf
import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.navBackStackOf

/**
 * 轻量的返回栈封装，支持 push / replace / pop / popUntil。
 *
 * 与 miuix 示例里的同名类一致：栈本身是 [NavBackStack]（一个 SnapshotStateList），
 * 所以增删会自动触发 NavDisplay 重组。
 */
class Navigator(
    val backStack: NavBackStack,
) {

    /** 入栈。已在栈里的 key 直接跳过，避免重复内容 key 被运行时拒绝。 */
    fun push(key: NavKey) {
        if (key !in backStack) {
            backStack.add(key)
        }
    }

    fun replace(key: NavKey) {
        if (backStack.isNotEmpty()) {
            backStack[backStack.lastIndex] = key
        } else {
            backStack.add(key)
        }
    }

    /** 出栈；栈只剩一个元素时不动，保证根页面始终存在。 */
    fun pop() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }

    fun popUntil(predicate: (NavKey) -> Boolean) {
        while (backStack.size > 1 && !predicate(backStack.last())) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    fun current(): NavKey? = backStack.lastOrNull()

    fun canGoBack(): Boolean = backStack.size > 1
}

/** 供各级页面取出导航器的 CompositionLocal。 */
val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("No Navigator provided!")
}

/**
 * 返回栈的 Bundle 存档。
 *
 * [NavKey] 不是 Parcelable，不能直接交给 `rememberSaveable`，所以只存 [Route.id]
 * 字符串，恢复时用 [Route.fromId] 反查回原来的 data object。
 * 存档为空时回到根页面，保证 `NavDisplay` 拿到的永远不是空栈。
 */
val NavBackStackSaver: Saver<NavBackStack, Any> = listSaver<NavBackStack, String>(
    save = { stack -> stack.mapNotNull { (it as? Route)?.id } },
    restore = { ids ->
        val keys: List<NavKey> = ids.mapNotNull(Route::fromId)
        navBackStackOf(*keys.ifEmpty { listOf(Route.Main) }.toTypedArray())
    },
)
