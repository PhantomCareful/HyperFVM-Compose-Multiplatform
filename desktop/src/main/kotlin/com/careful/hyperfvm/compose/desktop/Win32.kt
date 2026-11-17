package com.careful.hyperfvm.compose.desktop

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.win32.StdCallLibrary

/*
 * 项目用到的最小 Win32 绑定：全局鼠标钩子（MenuDismissHook）+ 托盘图标（NativeTrayIcon）。
 *
 * 不用 jna-platform 的 User32/Shell32：那套签名随版本变，自己声明反而更稳。
 * 字符串一律走 [Pointer]（自己用 Memory.setWideString 造宽字符缓冲区）：JNA 默认把
 * Kotlin 的 String 当 ANSI 传，碰到 W 结尾的 API 参数会错位。
 */

internal interface User32 : StdCallLibrary {
    // 低层钩子
    fun SetWindowsHookExW(idHook: Int, lpfn: LowLevelMouseProc?, hMod: Pointer?, dwThreadId: Int): Pointer?

    fun UnhookWindowsHookEx(hhk: Pointer?): Boolean

    fun CallNextHookEx(hhk: Pointer?, nCode: Int, wParam: Pointer?, lParam: Pointer?): Pointer?

    // 消息泵
    fun GetMessageW(lpMsg: Pointer, hWnd: Pointer?, wMsgFilterMin: Int, wMsgFilterMax: Int): Int

    fun DispatchMessageW(lpMsg: Pointer): Pointer?

    fun PostThreadMessageW(idThread: Int, msg: Int, wParam: Pointer?, lParam: Pointer?): Boolean

    // 窗口（托盘图标要一个隐藏窗口来收 shell 的回调消息）
    fun RegisterClassExW(lpWndClass: Pointer): Int

    fun UnregisterClassW(lpClassName: Pointer, hInstance: Pointer?): Boolean

    fun CreateWindowExW(
        dwExStyle: Int,
        lpClassName: Pointer?,
        lpWindowName: Pointer?,
        dwStyle: Int,
        x: Int,
        y: Int,
        nWidth: Int,
        nHeight: Int,
        hWndParent: Pointer?,
        hMenu: Pointer?,
        hInstance: Pointer?,
        lpParam: Pointer?,
    ): Pointer?

    fun DestroyWindow(hWnd: Pointer?): Boolean

    fun DefWindowProcW(hWnd: Pointer?, msg: Int, wParam: Pointer?, lParam: Pointer?): Pointer?

    // 图标
    fun CreateIconIndirect(lpIconInfo: Pointer): Pointer?

    fun DestroyIcon(hIcon: Pointer?): Boolean

    fun GetSystemMetrics(nIndex: Int): Int

    // 单实例唤起：Windows 只允许「前台进程」把窗口顶到前面，跨进程得由对方把权限让过来。
    fun AllowSetForegroundWindow(dwProcessId: Int): Boolean

    companion object {
        val INSTANCE: User32 by lazy { Native.load("user32", User32::class.java) }
    }
}

/** 线程 id 在 kernel32，不在 user32（写错会直接在钩子线程抛 UnsatisfiedLinkError）。 */
internal interface Kernel32 : StdCallLibrary {
    fun GetCurrentThreadId(): Int

    fun GetModuleHandleW(lpModuleName: Pointer?): Pointer?

    companion object {
        val INSTANCE: Kernel32 by lazy { Native.load("kernel32", Kernel32::class.java) }
    }
}

internal interface Shell32 : StdCallLibrary {
    fun Shell_NotifyIconW(dwMessage: Int, lpData: Pointer): Boolean

    companion object {
        val INSTANCE: Shell32 by lazy { Native.load("shell32", Shell32::class.java) }
    }
}

internal interface Gdi32 : StdCallLibrary {
    /** [ppvBits] 传一块 8 字节内存，函数把像素缓冲区的地址写在里面。 */
    fun CreateDIBSection(
        hdc: Pointer?,
        pbmi: Pointer,
        usage: Int,
        ppvBits: Pointer,
        hSection: Pointer?,
        dwOffset: Int,
    ): Pointer?

    fun CreateBitmap(nWidth: Int, nHeight: Int, nPlanes: Int, nBitCount: Int, lpBits: Pointer?): Pointer?

    fun DeleteObject(ho: Pointer?): Boolean

    companion object {
        val INSTANCE: Gdi32 by lazy { Native.load("gdi32", Gdi32::class.java) }
    }
}

/** `LowLevelMouseProc`：系统对每次鼠标事件调用一次，返回值原样透传。 */
internal interface LowLevelMouseProc : StdCallLibrary.StdCallCallback {
    fun callback(nCode: Int, wParam: Pointer?, lParam: Pointer?): Pointer?
}

/** `WNDPROC`：隐藏窗口的消息处理。回调必须一直被强引用，否则被 GC 后原生侧直接崩。 */
internal interface WndProc : StdCallLibrary.StdCallCallback {
    fun callback(hWnd: Pointer?, msg: Int, wParam: Pointer?, lParam: Pointer?): Pointer?
}

/** `WNDCLASSEXW`（x64 下 80 字节）。 */
internal class WNDCLASSEX : Structure() {
    @JvmField var cbSize: Int = 0
    @JvmField var style: Int = 0

    /** 窗口过程：直接放进结构体，JNA 写的时候会转成函数指针。 */
    @JvmField var lpfnWndProc: WndProc? = null
    @JvmField var cbClsExtra: Int = 0
    @JvmField var cbWndExtra: Int = 0
    @JvmField var hInstance: Pointer? = null
    @JvmField var hIcon: Pointer? = null
    @JvmField var hCursor: Pointer? = null
    @JvmField var hbrBackground: Pointer? = null
    @JvmField var lpszMenuName: Pointer? = null
    @JvmField var lpszClassName: Pointer? = null
    @JvmField var hIconSm: Pointer? = null

    /** 字段顺序不能靠反射猜，这里写死。 */
    override fun getFieldOrder(): List<String> = listOf(
        "cbSize", "style", "lpfnWndProc", "cbClsExtra", "cbWndExtra", "hInstance",
        "hIcon", "hCursor", "hbrBackground", "lpszMenuName", "lpszClassName", "hIconSm",
    )

    /** 必须在所有字段初始化后再算尺寸，否则 JNA 会报「array fields must be initialized」。 */
    init {
        cbSize = size()
    }
}

/**
 * `NOTIFYICONDATAW`（x64 下 976 字节）。
 *
 * WCHAR 数组用 [ShortArray] 而不是 CharArray：JNA 对 `char[]` 默认按 ANSI 一字节排布，
 * `short[]` 才是两字节，和 WCHAR 一致。
 */
internal class NOTIFYICONDATAW : Structure() {
    @JvmField var cbSize: Int = 0
    @JvmField var hWnd: Pointer? = null
    @JvmField var uID: Int = 0
    @JvmField var uFlags: Int = 0
    @JvmField var uCallbackMessage: Int = 0
    @JvmField var hIcon: Pointer? = null
    @JvmField var szTip: ShortArray = ShortArray(128)
    @JvmField var dwState: Int = 0
    @JvmField var dwStateMask: Int = 0
    @JvmField var szInfo: ShortArray = ShortArray(256)
    @JvmField var uVersion: Int = 0
    @JvmField var szInfoTitle: ShortArray = ShortArray(64)
    @JvmField var dwInfoFlags: Int = 0
    @JvmField var guidItem: ByteArray = ByteArray(16)
    @JvmField var hBalloonIcon: Pointer? = null

    /** 把 Kotlin 字符串写进 WCHAR 数组（截断到容量，保留结尾的 0）。 */
    fun setTip(text: String) {
        val limit = szTip.size - 1
        for (index in 0 until minOf(text.length, limit)) szTip[index] = text[index].code.toShort()
        szTip[minOf(text.length, limit)] = 0
    }

    override fun getFieldOrder(): List<String> = listOf(
        "cbSize", "hWnd", "uID", "uFlags", "uCallbackMessage", "hIcon", "szTip", "dwState",
        "dwStateMask", "szInfo", "uVersion", "szInfoTitle", "dwInfoFlags", "guidItem",
        "hBalloonIcon",
    )

    init {
        cbSize = size()
    }
}

/** `ICONINFO`（x64 下 32 字节）：拼 HICON 用。 */
internal class ICONINFO : Structure() {
    @JvmField var fIcon: Int = 1
    @JvmField var xHotspot: Int = 0
    @JvmField var yHotspot: Int = 0
    @JvmField var hbmMask: Pointer? = null
    @JvmField var hbmColor: Pointer? = null

    override fun getFieldOrder(): List<String> =
        listOf("fIcon", "xHotspot", "yHotspot", "hbmMask", "hbmColor")
}

internal const val WM_QUIT = 0x0012

/** 消息窗口的父句柄：`HWND_MESSAGE`，只收消息不显示。 */
internal val HWND_MESSAGE: Pointer = Pointer(-3L)

internal const val SM_CXSMICON = 49

/** MSG 在 64 位下是 48 字节，留足余量。 */
internal const val MESSAGE_SIZE = 96L
