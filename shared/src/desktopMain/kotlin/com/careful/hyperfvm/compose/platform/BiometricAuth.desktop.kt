package com.careful.hyperfvm.compose.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sun.jna.Function
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.ptr.PointerByReference
import javax.swing.SwingUtilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 桌面端的验证入口：Windows Hello（指纹 / 人脸 / PIN）。
 *
 * 面板与验证手段都由系统决定——用户在「设置 → 账户 → 登录选项」里配了什么就按什么验，
 * 与 Android 的 `BiometricPrompt` 是同一层意思。没配 Hello、被组策略禁用，
 * 或根本不是在 Windows 上跑时 [available] 为 false，调用方据此隐藏入口，不会把界面锁死。
 */
@Composable
actual fun rememberBiometricGate(): BiometricGate {
    // 探测要碰 WinRT（Java 侧现解 COM，可能失败也可能慢），绝不能挂在组合期：
    // 这里只启动一次后台探测，界面先按「不可用」画，探到了再重组显示。
    LaunchedEffect(Unit) { WindowsHelloGate.probe() }
    return WindowsHelloGate
}

private object WindowsHelloGate : BiometricGate {

    /** 系统里这套东西统称 Windows Hello，界面文案直接用它。 */
    override val credentialName: String = "Windows Hello"

    /** 探测结果：默认不可用，后台探到后再发布快照，设置页随之出现或保持隐藏。 */
    override var available: Boolean by mutableStateOf(false)
        private set

    private var probing = false

    /**
     * 查一次系统能力：并发调用只会真正探一次，失败按不可用处理。
     *
     * 放在 IO 线程上是必须的：AWT 事件线程是 STA，在它上面等 WinRT 的异步完成会互相等死；
     * 后台线程自己以 MTA 初始化 COM，异步结果不需要消息泵。
     */
    suspend fun probe() {
        if (probing || available) return
        probing = true
        try {
            available = withContext(Dispatchers.IO) {
                runCatching { WinRt.isVerifierAvailable() }.getOrDefault(false)
            }
        } finally {
            probing = false
        }
    }

    override fun request(title: String, subtitle: String, onResult: (Boolean) -> Unit) {
        if (!available) {
            // 没有这一层时放行，与 Android 拿不到 Activity 时的处理一致。
            onResult(true)
            return
        }
        // Hello 面板会一直等用户操作，轮询完成状态的那个线程不能是界面线程：
        // 丢到后台线程里等，结果再回到 AWT 事件线程（Compose Desktop 的 Main）交给界面。
        val message = if (subtitle.isBlank()) title else "$title：$subtitle"
        Thread({
            // 探测说能用、真正调用时却跑不起来（激活失败等）：按「没有这一层」放行。
            val passed = runCatching { WinRt.requestVerification(message) }.getOrNull() ?: true
            SwingUtilities.invokeLater { onResult(passed) }
        }, "windows-hello").apply { isDaemon = true }.start()
    }
}

/**
 * `combase` 里用得着的几个导出函数：WinRT 字符串与激活工厂。
 *
 * 与 `desktop` 模块里那套 Win32 绑定一样自己声明签名，不依赖 jna-platform 的现成映射。
 */
internal interface ComBase : Library {
    fun RoInitialize(initType: Int): Int

    fun RoGetActivationFactory(classId: Pointer, iid: Pointer, factory: PointerByReference): Int

    fun WindowsCreateString(source: WString, length: Int, hstring: PointerByReference): Int

    fun WindowsDeleteString(hstring: Pointer): Int
}

/**
 * `UserConsentVerifier` 的最小互操作层。
 *
 * JVM 没有 WinRT 绑定，这里只做三件事：拿激活工厂（`RoGetActivationFactory`）、
 * 按 vtable 偏移直接调接口方法、轮询 `IAsyncOperation` 的完成状态再取结果。
 */
private object WinRt {

    /** WinRT 运行时类名。 */
    private const val VerifierClass = "Windows.Security.Credentials.UI.UserConsentVerifier"

    /** `RoInitialize` 的多线程套间。 */
    private const val RoInitMultithreaded = 1

    // vtable 槽位：IInspectable 占 0..5（QueryInterface / AddRef / Release / GetIids /
    // GetRuntimeClassName / GetTrustLevel），之后才是接口自己的方法。
    private const val SlotRelease = 2
    private const val SlotCheckAvailability = 6
    private const val SlotRequestVerification = 7

    // 拿到的异步对象只有 `IAsyncOperation<T>` 这一张表：IInspectable 之后就是
    // put_Completed(6) / get_Completed(7) / GetResults(8)。`IAsyncInfo.get_Status`
    // 属于另一个接口，不 QI 过去是调不到的（槽位越界会直接把进程写坏）。
    private const val SlotGetResults = 8

    /** `GetResults` 在异步操作还没完成时返回 `E_ILLEGAL_METHOD_CALL`。 */
    private const val EIllegalMethodCall = 0x8000000E.toInt()

    /** `UserConsentVerifierAvailability.Available`。 */
    private const val AvailabilityAvailable = 0

    /** `UserConsentVerificationResult.Verified`。 */
    private const val VerificationVerified = 0

    /** 查能力超时：正常是毫秒级，卡住就当没有这个能力，免得开界面时被拖住。 */
    private const val AvailabilityTimeoutMillis = 1500L

    /** 轮询间隔：Hello 面板是给人操作的，40ms 足够，也不会空转太凶。 */
    private const val PollIntervalMillis = 40L

    /**
     * `IUserConsentVerifierStatics` 的 IID：{AF4F3F91-564C-4DDC-B8B5-973447627C65}。
     *
     * 别和 `IUserConsentVerifierInterop` 的 {39E050C3-…} 搞混：那个是给桌面应用带窗口
     * 弹面板用的，方法签名不一样，当 statics 调会直接栈溢出。
     * GUID 在内存里是 Data1/2/3 小端、Data4 原样排列，所以按这个顺序写字节。
     */
    private fun staticsIid(): Memory = Memory(16).apply {
        setInt(0, 0xAF4F3F91.toInt())
        setShort(4, 0x564C.toShort())
        setShort(6, 0x4DDC.toShort())
        setByte(8, 0xB8.toByte())
        setByte(9, 0xB5.toByte())
        setByte(10, 0x97.toByte())
        setByte(11, 0x34.toByte())
        setByte(12, 0x47.toByte())
        setByte(13, 0x62.toByte())
        setByte(14, 0x7C.toByte())
        setByte(15, 0x65.toByte())
    }

    /** 不是 Windows 就没有 combase，直接当作没有这个能力。 */
    private val combase: ComBase? = runCatching { Native.load("combase", ComBase::class.java) }.getOrNull()

    /** 激活一次就够，之后所有调用都走这个 statics 接口。 */
    private val staticsInterface: Pointer? by lazy { activateStatics() }

    fun isVerifierAvailable(): Boolean {
        // 每个碰 WinRT 的线程都要先入 MTA：重复调用只返回 S_FALSE，不影响。
        combase?.RoInitialize(RoInitMultithreaded)
        val statics = staticsInterface ?: return false
        val operation = PointerByReference()
        if (slot(statics, SlotCheckAvailability).invokeInt(arrayOf(statics, operation)) < 0) return false
        return awaitInt(operation.value, AvailabilityTimeoutMillis) == AvailabilityAvailable
    }

    /**
     * 弹 Hello 面板验证一次。
     *
     * 返回 null 表示这套能力没跑起来（不是 Windows、激活失败等），由调用方按「没有这一层」处理；
     * 返回 false 只代表用户取消、重试次数用完或被策略拦下。
     */
    fun requestVerification(message: String): Boolean? {
        combase?.RoInitialize(RoInitMultithreaded)
        val statics = staticsInterface ?: return null
        val text = createString(message) ?: return null
        return try {
            val operation = PointerByReference()
            if (slot(statics, SlotRequestVerification).invokeInt(arrayOf(statics, text, operation)) < 0) {
                null
            } else {
                awaitInt(operation.value, timeoutMillis = 0) == VerificationVerified
            }
        } finally {
            combase?.WindowsDeleteString(text)
        }
    }

    /**
     * 激活 `IUserConsentVerifierStatics`。
     *
     * `RoGetActivationFactory` 的 iid 参数就是「想要的接口」本身，直接传 statics 的 IID 即可，
     * 不需要先拿 `IActivationFactory` 再 QueryInterface。
     */
    private fun activateStatics(): Pointer? {
        val api = combase ?: return null
        // 本线程已经以 STA 初始化过时这里会返回 RPC_E_CHANGED_MODE，不影响激活，忽略即可。
        runCatching { api.RoInitialize(RoInitMultithreaded) }
        val classId = createString(VerifierClass) ?: return null
        return try {
            val statics = PointerByReference()
            val hr = api.RoGetActivationFactory(classId, staticsIid(), statics)
            if (hr < 0) null else statics.value
        } finally {
            api.WindowsDeleteString(classId)
        }
    }

    private fun createString(text: String): Pointer? {
        val api = combase ?: return null
        val hstring = PointerByReference()
        // 长度按 UTF-16 码元算，正好是 Kotlin String 的 length。
        val hr = api.WindowsCreateString(WString(text), text.length, hstring)
        return if (hr < 0) null else hstring.value
    }

    /** 取 [obj] 第 [index] 个 vtable 槽位上的方法。 */
    private fun slot(obj: Pointer, index: Int): Function = Function.getFunction(
        obj.getPointer(0).getPointer(index * Native.POINTER_SIZE.toLong()),
        Function.ALT_CONVENTION,
    )

    /**
     * 等一个 `IAsyncOperation<T>` 完成并取结果（这里的 T 都是枚举，按 Int 取）。
     *
     * 这类对象只暴露 `IAsyncOperation<T>` 一张表，没有状态可查，只能反复调 `GetResults`：
     * 没完成时它返回 `E_ILLEGAL_METHOD_CALL`，完成了就把结果写进 out 参数。
     * 注意 out 参数一个都不能少——少传会让被调方往未初始化的地址写结果，整个进程当场崩掉。
     * [timeoutMillis] 为 0 表示不设上限——Hello 面板会一直等用户操作。
     */
    private fun awaitInt(operation: Pointer?, timeoutMillis: Long): Int? {
        if (operation == null) return null
        val getResults = slot(operation, SlotGetResults)
        val result = Memory(8)
        val startedAt = System.currentTimeMillis()
        try {
            while (true) {
                result.clear()
                val hr = getResults.invokeInt(arrayOf(operation, result))
                when {
                    hr == 0 -> return result.getInt(0)
                    hr == EIllegalMethodCall -> Unit
                    else -> return null
                }
                if (timeoutMillis > 0 && System.currentTimeMillis() - startedAt > timeoutMillis) return null
                try {
                    Thread.sleep(PollIntervalMillis)
                } catch (_: InterruptedException) {
                    return null
                }
            }
        } finally {
            // 结果已经取到，没人再用这个异步对象，顺手放掉引用。
            slot(operation, SlotRelease).invokeInt(arrayOf(operation))
        }
    }
}
