package com.careful.hyperfvm.compose.desktop

import com.sun.jna.Memory
import com.sun.jna.Pointer
import java.awt.image.BufferedImage
import javax.swing.SwingUtilities

/**
 * 自己做托盘图标：`Shell_NotifyIcon` + 自建 32bpp HICON。
 *
 * 为什么不用 `java.awt.SystemTray` 的 `TrayIcon`：AWT 在 Windows 上固定按
 * `getTrayIconSize()`（本机 16）生成 HICON，超出部分按像素裁掉、不足的部分交给系统拉伸。
 * 200% 缩放下任务栏要的是 32px，于是同一个图标在托盘里永远比任务栏糊。任务栏窗口图标
 * 清楚是因为 Compose 一次给 AWT 多个尺寸、系统挑对了。
 *
 * 这里按设备像素画图，自己拼 HICON 交给 shell，尺寸对上了就一样清晰；顺带把左/右键回调
 * 也从隐藏窗口的消息里直接拿到。
 *
 * 结构体布局按 x64 写死（见 [NOTIFYICONDATAW]/[WNDCLASSEX]），并且在自己的线程上装/卸：
 * shell 的回调消息只会派发给创建窗口的那个线程。
 */
internal class NativeTrayIcon(
    private val tooltip: String,
    private val onLeftClick: () -> Unit,
    private val onRightClick: () -> Unit,
) {
    private var worker: Thread? = null

    @Volatile
    private var threadId = 0

    @Volatile
    private var stopping = false

    // 原生侧还持有引用的东西：回调、类名缓冲区、图标句柄。被 GC 掉会崩或者花屏。
    private var wndProc: WndProc? = null
    private var className: Memory? = null
    private var messageWindow: Pointer? = null
    private var iconHandle: Pointer? = null
    private var notifyData: NOTIFYICONDATAW? = null

    /** [image] 必须是**设备像素**尺寸（200% 缩放下 32×32）。 */
    fun install(image: BufferedImage) {
        if (worker != null) return
        stopping = false
        worker = Thread({ runLoop(image) }, "tray-icon").apply {
            isDaemon = true
            start()
        }
    }

    fun remove() {
        worker = null
        stopping = true
        // 让消息循环返回 0，收尾（删图标、销毁窗口）都在那个线程上做。
        val id = threadId
        if (id != 0) runCatching { User32.INSTANCE.PostThreadMessageW(id, WM_QUIT, null, null) }
    }

    private fun runLoop(image: BufferedImage) {
        val user32 = runCatching { User32.INSTANCE }.getOrNull() ?: return
        val kernel32 = runCatching { Kernel32.INSTANCE }.getOrNull() ?: return
        val shell32 = runCatching { Shell32.INSTANCE }.getOrNull() ?: return

        threadId = kernel32.GetCurrentThreadId()
        if (stopping) {
            threadId = 0
            return
        }

        val instance = kernel32.GetModuleHandleW(null)
        val classBuffer = wideString(TRAY_CLASS_NAME)
        className = classBuffer

        val proc = object : WndProc {
            override fun callback(hWnd: Pointer?, msg: Int, wParam: Pointer?, lParam: Pointer?): Pointer? {
                if (msg == TRAY_CALLBACK_MESSAGE) {
                    // shell 把鼠标消息塞在 lParam 的低位。
                    when (Pointer.nativeValue(lParam ?: Pointer.NULL).toInt() and 0xFFFF) {
                        WM_LBUTTONUP -> SwingUtilities.invokeLater(onLeftClick)
                        WM_RBUTTONUP -> SwingUtilities.invokeLater(onRightClick)
                    }
                    return Pointer.NULL
                }
                return user32.DefWindowProcW(hWnd, msg, wParam, lParam)
            }
        }
        wndProc = proc

        val windowClass = WNDCLASSEX().apply {
            hInstance = instance
            lpfnWndProc = proc
            lpszClassName = classBuffer
        }
        windowClass.write()
        // 返回 0 可能是「类已注册」（重复 install），下面继续建窗口即可。
        user32.RegisterClassExW(windowClass.pointer)

        val window = user32.CreateWindowExW(
            dwExStyle = 0,
            lpClassName = classBuffer,
            lpWindowName = classBuffer,
            dwStyle = 0,
            x = 0,
            y = 0,
            nWidth = 0,
            nHeight = 0,
            hWndParent = HWND_MESSAGE,
            hMenu = null,
            hInstance = instance,
            lpParam = null,
        )
        if (window == null) {
            println("[tray] CreateWindowEx failed, native tray icon disabled")
            threadId = 0
            return
        }
        messageWindow = window

        val icon = createHIcon(image)
        if (icon == null) {
            println("[tray] CreateIconIndirect failed, using tooltip-only icon")
        }
        iconHandle = icon

        val data = NOTIFYICONDATAW().apply {
            hWnd = window
            uID = TRAY_ICON_ID
            uFlags = NIF_MESSAGE or NIF_ICON or NIF_TIP
            uCallbackMessage = TRAY_CALLBACK_MESSAGE
            hIcon = icon
            setTip(tooltip)
        }
        notifyData = data
        data.write()
        val added = runCatching { shell32.Shell_NotifyIconW(NIM_ADD, data.pointer) }.getOrDefault(false)
        println("[tray] native icon added=$added size=${image.width}x${image.height}")

        val message = Memory(MESSAGE_SIZE)
        while (user32.GetMessageW(message, null, 0, 0) > 0) {
            user32.DispatchMessageW(message)
        }

        // 收尾：图标、HICON、窗口、窗口类都在装它的这个线程上卸掉。
        notifyData?.let { runCatching { shell32.Shell_NotifyIconW(NIM_DELETE, it.pointer) } }
        iconHandle?.let { runCatching { user32.DestroyIcon(it) } }
        messageWindow?.let { runCatching { user32.DestroyWindow(it) } }
        runCatching { user32.UnregisterClassW(classBuffer, instance) }

        notifyData = null
        iconHandle = null
        messageWindow = null
        wndProc = null
        className = null
        threadId = 0
    }

    /**
     * 位图 → HICON。
     *
     * 颜色位图是 32bpp 自上而下的 DIB（BGRA、预乘 alpha）；掩码位图必须给，否则
     * `CreateIconIndirect` 会失败，内容全 0（透明度完全交给 alpha 通道）。
     */
    private fun createHIcon(image: BufferedImage): Pointer? {
        val user32 = runCatching { User32.INSTANCE }.getOrNull() ?: return null
        val gdi32 = runCatching { Gdi32.INSTANCE }.getOrNull() ?: return null
        val width = image.width
        val height = image.height

        val header = Memory(BITMAPINFOHEADER_SIZE)
        header.clear()
        header.setInt(0, BITMAPINFOHEADER_SIZE.toInt()) // biSize
        header.setInt(4, width) // biWidth
        header.setInt(8, -height) // biHeight：负数表示自上而下
        header.setShort(12, 1) // biPlanes
        header.setShort(14, 32) // biBitCount
        header.setInt(16, BI_RGB) // biCompression

        val bitsPointer = Memory(8)
        val colorBitmap = gdi32.CreateDIBSection(null, header, DIB_RGB_COLORS, bitsPointer, null, 0)
            ?: return null
        val pixels = toBgra(image)
        bitsPointer.getPointer(0).write(0, pixels, 0, pixels.size)

        // 1bpp 位图每行按 16 位对齐：((w + 15) / 16) * 2 字节。
        val maskRowBytes = ((width + 15) / 16) * 2
        val maskBits = Memory((maskRowBytes * height).toLong())
        maskBits.clear()
        val maskBitmap = gdi32.CreateBitmap(width, height, 1, 1, maskBits)

        val info = ICONINFO().apply {
            hbmMask = maskBitmap
            hbmColor = colorBitmap
        }
        info.write()
        val handle = user32.CreateIconIndirect(info.pointer)

        runCatching {
            gdi32.DeleteObject(maskBitmap)
            gdi32.DeleteObject(colorBitmap)
        }
        return handle
    }

    /** ARGB → BGRA，并按 HICON 的要求预乘 alpha（不预乘的话圆角边缘会发白）。 */
    private fun toBgra(image: BufferedImage): ByteArray {
        val width = image.width
        val height = image.height
        val argb = image.getRGB(0, 0, width, height, null, 0, width)
        val bytes = ByteArray(argb.size * 4)
        for (index in argb.indices) {
            val color = argb[index]
            val alpha = (color ushr 24) and 0xFF
            bytes[index * 4] = ((color and 0xFF) * alpha / 255).toByte()
            bytes[index * 4 + 1] = (((color shr 8) and 0xFF) * alpha / 255).toByte()
            bytes[index * 4 + 2] = (((color shr 16) and 0xFF) * alpha / 255).toByte()
            bytes[index * 4 + 3] = alpha.toByte()
        }
        return bytes
    }

    private fun wideString(text: String): Memory {
        val memory = Memory((text.length + 1) * 2L)
        memory.clear()
        memory.setWideString(0, text)
        return memory
    }
}

private const val TRAY_CLASS_NAME = "HyperFVMTrayWindow"
private const val TRAY_ICON_ID = 1
private const val TRAY_CALLBACK_MESSAGE = 0x8000 + 1 // WM_APP + 1
private const val NIM_ADD = 0
private const val NIM_DELETE = 2
private const val NIF_MESSAGE = 0x01
private const val NIF_ICON = 0x02
private const val NIF_TIP = 0x04
private const val WM_LBUTTONUP = 0x0202
private const val WM_RBUTTONUP = 0x0205
private const val BI_RGB = 0
private const val DIB_RGB_COLORS = 0
private const val BITMAPINFOHEADER_SIZE = 40L
