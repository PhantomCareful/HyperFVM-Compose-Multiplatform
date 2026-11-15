package com.careful.hyperfvm.compose.screen.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.careful.hyperfvm.compose.screen.ScreenScaffold
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.pressable
import androidx.core.net.toUri

/** 剪贴板读/写操作对应的 AppOps 名称（即 OP_READ_CLIPBOARD / OP_WRITE_CLIPBOARD）。 */
private const val OP_READ_CLIPBOARD = "android:read_clipboard"
private const val OP_WRITE_CLIPBOARD = "android:write_clipboard"

/**
 * 三项有系统状态可查的授权的当前授予情况。
 *
 * 「打开受支持的链接」没有可靠的公开查询接口（域名验证状态不可读），像 Java 版一样
 * 只作为跳转入口，不参与状态判定，所以不在这里。
 */
private data class PermissionStates(
    val notification: Boolean,
    val install: Boolean,
    val clipboard: Boolean,
)

/**
 * 授权检查页：四项系统授权的跳转入口。
 *
 * 文案与跳转目标对齐 Java 版 SettingsActivity：
 * 通知走系统的应用通知页，安装走未知来源安装页，链接走「默认打开」页，
 * 剪贴板在米系设备上走安全中心的权限管理页、其余设备退到应用详情页。
 * 已授予的项在行尾显示对勾，未授予显示箭头。
 */
@Composable
actual fun PermissionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    // Activity 本身不是 LifecycleOwner，ComponentActivity 才是，所以统一取成 LifecycleOwner。
    val lifecycleOwner = remember(context) { context.findActivity() as? LifecycleOwner }
    var states by remember(context) { mutableStateOf(readPermissionStates(context)) }

    // 跳系统设置页授权后返回，Activity 只走 onResume，组合不会重建：
    // 必须监听生命周期事件，才能在回到前台时重新读一遍状态。
    DisposableEffect(lifecycleOwner, context) {
        if (lifecycleOwner == null) return@DisposableEffect onDispose {}
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                states = readPermissionStates(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ScreenScaffold(
        title = "授权检查",
        // 二级页没有外层 Pager 的底栏，底部安全区由内层 Scaffold 自己吃。
        outerPadding = PaddingValues(0.dp),
        onBack = onBack,
    ) { padding ->
        LazyColumn(
            // 铺满内容区：内容不满一屏时，空白处的手势也要能进列表，滚动与顶栏折叠才会响应。
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 12.dp,
                bottom = padding.calculateBottomPadding(),
            ),
        ) {
            item(key = "notification") {
                PermissionCard(
                    title = "通知推送权限",
                    summary = "授予通知权限后，您可在设备的控制中心添加HyperFVM磁贴，点击磁贴即可领取温馨礼包，" +
                            "并向您推送部分重要的仪表盘数据",
                    granted = states.notification,
                    // Android 12 及以下没有通知运行时权限，状态恒为已授予，也没有可跳转的通知页（对齐 Java 版）。
                    onClick = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        {
                            openSystemSettings(
                                context,
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                            )
                        }
                    } else {
                        null
                    },
                )
            }

            item(key = "install") {
                PermissionCard(
                    title = "软件包安装权限",
                    summary = "授予安装权限后，当您在App内收到更新推送并下载好新版本安装包后，" +
                            "才能够顺利调起系统的软件包安装程序进行升级",
                    granted = states.install,
                    onClick = {
                        openSystemSettings(
                            context,
                            Intent(
                                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                "package:${context.packageName}".toUri(),
                            ),
                        )
                    },
                )
            }

            item(key = "links") {
                // 域名验证状态没有公开接口，这张卡固定显示箭头，与 Java 版一致。
                PermissionCard(
                    title = "允许打开受支持的链接",
                    summary = "允许App打开受支持的链接后，您在微信公众号-美食中心内选择“在浏览器中打开”，" +
                            "即可自动跳转到App，一步添加链接；您也可以在设备任意位置（如笔记、便签）长按选中链接，" +
                            "系统也会弹出使用App打开的选项（一个链接符号或者直接是App图标）",
                    granted = null,
                    onClick = {
                        openSystemSettings(
                            context,
                            Intent(
                                Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                                "package:${context.packageName}".toUri(),
                            ),
                        )
                    },
                )
            }

            item(key = "clipboard") {
                PermissionCard(
                    title = "读取/写入剪贴板权限",
                    summary = "授予剪贴板相关权限后，您可以更方便地备份/还原温馨礼包链接数据",
                    granted = states.clipboard,
                    onClick = {
                        // 米系设备的权限管理页里才有剪贴板开关，其他设备只能进应用详情页手动找。
                        val intent = if (isMIUIOrHyperOS()) {
                            Intent("miui.intent.action.APP_PERM_EDITOR")
                                .putExtra("extra_pkgname", context.packageName)
                        } else {
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                "package:${context.packageName}".toUri(),
                            )
                        }
                        openSystemSettings(context, intent)
                    },
                )
            }
        }
    }
}

/**
 * 单张授权卡：标题 + 说明 + 行尾状态图标。
 *
 * [granted] 为 null 表示这项没有可查的状态（受支持的链接），固定画箭头；
 * [onClick] 为 null 时不挂 clickable（Android 12 及以下的通知行），
 * 用 [pressable] 补上 [LocalIndication] 的点按阴影。
 */
@Composable
private fun PermissionCard(
    title: String,
    summary: String,
    granted: Boolean?,
    onClick: (() -> Unit)?,
) {
    Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
        BasicComponent(
            // 无点击的行不挂 clickable，只用 pressable 补点按阴影。
            modifier = if (onClick == null) {
                Modifier.pressable(
                    interactionSource = null,
                    indication = LocalIndication.current,
                )
            } else {
                Modifier
            },
            title = title,
            summary = summary,
            endActions = {
                val tint = MiuixTheme.colorScheme.onSurfaceVariantActions
                if (granted == true) {
                    Icon(
                        imageVector = MiuixIcons.Demibold.Ok,
                        contentDescription = "已授予",
                        modifier = Modifier.size(20.dp),
                        tint = tint,
                    )
                } else {
                    Icon(
                        imageVector = MiuixIcons.Basic.ArrowRight,
                        contentDescription = if (granted == false) "未授予" else null,
                        tint = tint,
                    )
                }
            },
            onClick = onClick,
        )
    }
}

/**
 * 跳转系统设置页；目标页解析不到时退到本应用详情页（对齐 Java 版对安装/链接两项的处理）。
 */
@SuppressLint("QueryPermissionsNeeded")
private fun openSystemSettings(context: Context, intent: Intent) {
    val target = if (intent.resolveActivity(context.packageManager) != null) {
        intent
    } else {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            "package:${context.packageName}".toUri(),
        )
    }
    context.startActivity(target)
}

private fun readPermissionStates(context: Context): PermissionStates = PermissionStates(
    notification = hasNotificationPermission(context),
    install = context.packageManager.canRequestPackageInstalls(),
    clipboard = hasClipboardPermission(context),
)

/** Android 12 及以下默认拥有通知权限。 */
private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

/**
 * 剪贴板读写权限：系统没有对应的运行时权限，经 AppOps 读/写操作码判定；
 * 厂商（MIUI/HyperOS 等）在「权限管理」里撤销时会把对应 AppOps 置为忽略/拒绝。
 */
private fun hasClipboardPermission(context: Context): Boolean {
    val appOps = context.getSystemService(AppOpsManager::class.java) ?: return true
    return isAppOpGranted(appOps, OP_READ_CLIPBOARD, context.packageName) &&
        isAppOpGranted(appOps, OP_WRITE_CLIPBOARD, context.packageName)
}

/** MODE_DEFAULT 表示系统未做限制，视为已授予。 */
private fun isAppOpGranted(appOps: AppOpsManager, op: String, packageName: String): Boolean {
    val mode = appOps.checkOpNoThrow(op, Process.myUid(), packageName)
    return mode == AppOpsManager.MODE_ALLOWED || mode == AppOpsManager.MODE_DEFAULT
}

/**
 * 是否 MIUI/HyperOS：先看品牌，再看特征系统属性（覆盖移植 ROM —— 设备不是小米系，
 * 但系统确实是 MIUI/HyperOS 时属性仍在）。
 */
private fun isMIUIOrHyperOS(): Boolean {
    val manufacturer = Build.MANUFACTURER.lowercase()
    if (manufacturer.contains("xiaomi") || manufacturer.contains("redmi")) {
        return true
    }
    return readSystemProperty("ro.miui.ui.version.name").isNotEmpty() ||
        readSystemProperty("ro.mi.os.version.name").isNotEmpty() ||
        readSystemProperty("ro.mi.os.version.code").isNotEmpty()
}

/**
 * 从可能被包装的 Context 里取出 Activity：监听 onResume 需要 Activity 的 Lifecycle。
 */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** android.os.SystemProperties 是隐藏 API，只能反射读取，失败时返回空串。 */
@SuppressLint("PrivateApi")
private fun readSystemProperty(key: String): String = try {
    val clazz = Class.forName("android.os.SystemProperties")
    clazz.getMethod("get", String::class.java).invoke(null, key) as? String ?: ""
} catch (_: Exception) {
    ""
}
