package com.careful.hyperfvm.compose.desktop

import com.sun.jna.Memory
import com.sun.jna.Platform
import com.sun.jna.Pointer

/**
 * 菜单打开期间的全局鼠标钩子（Win32 `WH_MOUSE_LL`）。
 *
 * 为什么需要它：Swing 的弹出菜单只能感知「本进程」里的点击，点在任务栏、桌面、别的程序上
 * 时一个事件都收不到，菜单就一直挂着。Windows 也不把前台让给后台进程（托盘点击的前台还在
 * explorer 那边），靠「本进程失去活动窗口」判断同样行不通。
 *
 * 低层鼠标钩子是系统级回调，屏幕上任何一次按下都能收到。这里只**观察**、不拦截：回调用
 * 原样 `CallNextHookEx` 交还事件，所以那一下点击照常递给下面的窗口——点任务栏抽屉箭头时，
 * 菜单和抽屉会一起收起，和系统原生菜单手感一致，也不需要模拟点击。
 *
 * 回调跑在自建线程上，该线程必须自己泵消息（`GetMessage`），否则系统不会派发；收起菜单时
 * 投递 `WM_QUIT` 让循环退出并卸钩子——钩子不卸会一直拖慢全系统的鼠标输入。
 */
internal class MenuDismissHook(
    /** 屏幕坐标是否落在菜单上：落在菜单上的点击交给 Swing 自己处理。 */
    private val isInsideMenu: (x: Int, y: Int) -> Boolean,
    /** 点到菜单外面了。在钩子线程上调用，实现方自己切回界面线程。 */
    private val onOutsideClick: () -> Unit,
) {
    private var worker: Thread? = null

    @Volatile
    private var threadId = 0

    @Volatile
    private var stopping = false

    @Volatile
    private var hook: Pointer? = null

    fun start() {
        if (!Platform.isWindows() || worker != null) return
        stopping = false
        worker = Thread({ runLoop() }, "tray-menu-hook").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        worker = null
        stopping = true
        // 线程还没来得及记下 id 时不用管：它一进循环就看见 stopping 直接退出。
        val id = threadId
        if (id != 0) runCatching { User32.INSTANCE.PostThreadMessageW(id, WM_QUIT, null, null) }
    }

    private fun runLoop() {
        val user32 = runCatching { User32.INSTANCE }.getOrNull() ?: return
        val kernel32 = runCatching { Kernel32.INSTANCE }.getOrNull() ?: return
        threadId = kernel32.GetCurrentThreadId()
        if (stopping) {
            threadId = 0
            return
        }

        val proc = object : LowLevelMouseProc {
            override fun callback(nCode: Int, wParam: Pointer?, lParam: Pointer?): Pointer? {
                if (nCode >= 0 && wParam != null && lParam != null &&
                    isButtonDown(Pointer.nativeValue(wParam).toInt())
                ) {
                    // MSLLHOOKSTRUCT 前两个字段就是 POINT{x, y}。
                    if (!isInsideMenu(lParam.getInt(0), lParam.getInt(4))) onOutsideClick()
                }
                return user32.CallNextHookEx(hook, nCode, wParam, lParam)
            }
        }

        hook = user32.SetWindowsHookExW(WH_MOUSE_LL, proc, null, 0)
        if (hook == null) {
            println("[tray] SetWindowsHookEx failed, outside clicks won't close the menu")
            threadId = 0
            return
        }
        println("[tray] mouse hook installed")

        // 低层钩子的回调由系统直接调用，这里只要把消息泵住，线程别退出就行。
        val message = Memory(MESSAGE_SIZE)
        while (user32.GetMessageW(message, null, 0, 0) > 0) {
            // 不需要 TranslateMessage / DispatchMessage：这里没有窗口要处理消息。
        }
        hook?.let { user32.UnhookWindowsHookEx(it) }
        hook = null
        threadId = 0
    }

    private fun isButtonDown(message: Int): Boolean =
        message == WM_LBUTTONDOWN || message == WM_RBUTTONDOWN ||
            message == WM_MBUTTONDOWN || message == WM_XBUTTONDOWN
}

private const val WH_MOUSE_LL = 14
private const val WM_LBUTTONDOWN = 0x0201
private const val WM_RBUTTONDOWN = 0x0204
private const val WM_MBUTTONDOWN = 0x0207
private const val WM_XBUTTONDOWN = 0x020B

