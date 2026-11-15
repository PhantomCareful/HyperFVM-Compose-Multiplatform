package com.careful.hyperfvm.compose

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import com.careful.hyperfvm.compose.data.fraud.FraudAuthKey
import com.careful.hyperfvm.compose.data.image.DataImageDownloadController
import com.careful.hyperfvm.compose.data.image.ServiceDownloadRunner
import com.careful.hyperfvm.compose.data.settings.SettingsStore
import com.careful.hyperfvm.compose.navigation.Route
import com.careful.hyperfvm.compose.platform.AndroidAppEnv
import com.careful.hyperfvm.compose.platform.AppLaunchTarget
import com.careful.hyperfvm.compose.ui.theme.COLOR_MODE_DARK
import com.careful.hyperfvm.compose.ui.theme.COLOR_MODE_LIGHT
import com.careful.hyperfvm.compose.ui.theme.COLOR_MODE_MONET_DARK
import com.careful.hyperfvm.compose.ui.theme.COLOR_MODE_MONET_LIGHT
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    /**
     * 外部入口（目前是下载通知）指定的二级页面，交给 Compose 侧的导航器消费。
     * 导航完成后由 [HyperFVMApp] 置回 null，同一个请求不会被重复执行。
     */
    private val pendingRoute = MutableStateFlow<Route?>(null)

    /** 平台环境：onNewIntent 里也要用它撤完成态通知，所以提升到字段。 */
    private lateinit var env: AndroidAppEnv

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 查黑接口的 KEY 由 BuildConfig 注入，共享层通过 FraudAuthKey 读取。
        FraudAuthKey.value = BuildConfig.FRAUD_AUTH_KEY
        // 下载通知要跳回当前 Activity：共享层拿不到这个类，启动时注入一次。
        AppLaunchTarget.activityClass = MainActivity::class.java

        // 平台环境与设置仓库在 setContent 之前建好，保证首帧就是用户上次选择的配色。
        env = AndroidAppEnv(applicationContext)
        val store = SettingsStore(env)

        // 下载任务的执行者换成前台服务：需要通知栏进度与暂停/取消按钮。
        DataImageDownloadController.runner = ServiceDownloadRunner(applicationContext)

        // 只在首次创建时读启动参数：配置变更重建时返回栈已从 Bundle 恢复，
        // 再消费一次会把用户从当时所在的页面重新拽到数据图页。
        // （代价是进程被回收后再点通知会回到主界面——这种场景下 savedInstanceState
        // 同样非空，无法与配置变更区分，宁可不误跳。）
        if (savedInstanceState == null) applyRouteRequest(intent)

        setContent {
            // 系统栏图标的明暗必须跟着配色模式走，否则切到深色后状态栏会「黑底黑字」。
            val darkMode = when (store.settings.colorMode) {
                COLOR_MODE_LIGHT, COLOR_MODE_MONET_LIGHT -> false
                COLOR_MODE_DARK, COLOR_MODE_MONET_DARK -> true
                else -> isSystemInDarkTheme()
            }

            DisposableEffect(darkMode) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkMode },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkMode },
                )
                // 小米设备会强制给导航栏加对比度遮罩，导致透明导航栏发灰。
                window.isNavigationBarContrastEnforced = false
                onDispose {}
            }

            HyperFVMApp(env = env, store = store, pendingRoute = pendingRoute)
        }
    }

    // Activity 是 singleTop + CLEAR_TOP 复用的，新 Intent 要显式替换旧值，
    // 否则 getIntent() 会一直返回启动时那一个。
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyRouteRequest(intent)
    }

    /** 把外部入口的 Intent 翻译成一次导航请求。 */
    private fun applyRouteRequest(intent: Intent?) {
        if (intent?.getBooleanExtra(AppLaunchTarget.ExtraOpenDataImages, false) != true) return
        // 点通知即视为已读：撤下完成态通知。
        // 任务还在跑时不动手——那条是前台服务的进度通知，撤了服务就没法继续更新。
        if (!DataImageDownloadController.state.value.isActive) {
            env.dismissBackgroundNotification()
        }
        pendingRoute.value = Route.DataImages
    }
}
