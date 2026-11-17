package com.careful.hyperfvm.compose.desktop

import kotlinx.coroutines.channels.Channel
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException

/**
 * Windows 端单实例守卫：双击第二次启动时不再开新窗口，而是把已有窗口拉回前台。
 *
 * 机制是「文件锁 + 回环端口」：先启动的进程对 instance.lock 抢独占锁，同时监听一个
 * 随机回环端口并把端口号写进 instance.port；后启动的进程抢不到锁，就读端口文件发一句
 * 命令过去，然后自己退出。
 *
 * 不用 Win32 命名互斥量 + FindWindow 那套的原因：窗口藏进托盘时只是 AWT 层的
 * visible = false，Win32 的 ShowWindow 会绕开 Compose 的窗口状态、跟托盘逻辑打架。
 * 走进程间消息让主实例自己重新走一遍「显示窗口」流程（复位最小化、置可见、抢焦点），
 * 和托盘左键单击完全同一条路径。
 *
 * 锁由操作系统在进程结束时释放（正常退出、崩溃、任务管理器结束都算），
 * 不会像「写 PID 文件」那样留下僵尸锁。
 */
internal class SingleInstance private constructor(
    private val portFile: File,
    private val server: ServerSocket,
    private val lockHandle: RandomAccessFile,
    private val lock: FileLock,
) {

    /** 第二个进程发来的「显示窗口」请求，由窗口侧消费。容量 1：连点多次也只需显示一次。 */
    val activateRequests = Channel<Unit>(Channel.CONFLATED)

    fun start() {
        Thread({ acceptLoop() }, "hyperfvm-single-instance").apply {
            isDaemon = true
            start()
        }
    }

    fun close() {
        runCatching { portFile.delete() }
        runCatching { server.close() }
        runCatching { lock.release() }
        runCatching { lockHandle.close() }
    }

    private fun acceptLoop() {
        while (!server.isClosed) {
            val client = runCatching { server.accept() }.getOrNull() ?: break
            runCatching {
                client.use {
                    it.soTimeout = 1000
                    // 只认这一句命令：别的连接（比如误扫端口的）读不到就自然断开。
                    if (it.getInputStream().bufferedReader().readLine() == ActivateCommand) {
                        activateRequests.trySend(Unit)
                    }
                }
            }
        }
    }

    companion object {
        private const val ActivateCommand = "activate"

        /**
         * 抢单实例锁：拿到返回守卫（要 [start] 之后才会响应激活请求）；
         * 返回 null 说明已经有实例在跑，调用方应先用 [requestActivate] 递话再退出。
         */
        fun acquire(instanceDir: File): SingleInstance? {
            instanceDir.mkdirs()
            val handle = RandomAccessFile(File(instanceDir, "instance.lock"), "rw")
            val lock: FileLock? = try {
                handle.channel.tryLock()
            } catch (error: OverlappingFileLockException) {
                null
            } catch (error: IOException) {
                null
            }
            if (lock == null) {
                runCatching { handle.close() }
                return null
            }
            // 端口文件是上一轮留下的就删掉：它指向的端口早就没人监听了。
            val portFile = File(instanceDir, "instance.port").apply { delete() }
            // 只绑回环地址：不监听外部网卡，也不会触发 Windows 防火墙弹窗。
            val server = ServerSocket(0, 8, InetAddress.getLoopbackAddress())
            // 端口和 pid 一起写：后启动的进程要靠 pid 把前台权限让过来。
            portFile.writeText("${server.localPort} ${ProcessHandle.current().pid()}")
            return SingleInstance(portFile, server, handle, lock)
        }

        /** 把「显示窗口」请求发给已经在跑的实例，返回是否送达。 */
        fun requestActivate(instanceDir: File): Boolean {
            val portFile = File(instanceDir, "instance.port")
            // 主实例可能刚启动、还没写端口文件，重试一小会儿；端口关着也一样重试。
            repeat(20) { attempt ->
                if (attempt > 0) Thread.sleep(100)
                val parts = runCatching { portFile.readText().trim().split(' ') }.getOrNull()
                val port = parts?.getOrNull(0)?.toIntOrNull()
                if (port != null) {
                    // 把「可以抢前台」的权限让给主实例：本进程刚被用户双击拉起，手里握着这一次的
                    // 激活权限，主实例自己去要就要不到了（Windows 的前台锁）。失败无妨，顶多只亮任务栏。
                    parts.getOrNull(1)?.toIntOrNull()?.let { pid ->
                        runCatching { User32.INSTANCE.AllowSetForegroundWindow(pid) }
                    }
                    if (send(port)) return true
                }
            }
            return false
        }

        private fun send(port: Int): Boolean = runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(InetAddress.getLoopbackAddress(), port), 300)
                socket.getOutputStream().apply {
                    write("$ActivateCommand\n".toByteArray())
                    flush()
                }
            }
            true
        }.getOrDefault(false)
    }
}
