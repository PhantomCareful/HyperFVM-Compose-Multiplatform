package com.careful.hyperfvm.compose

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.careful.hyperfvm.compose.data.settings.SettingsStore
import com.careful.hyperfvm.compose.platform.AndroidAppEnv
import com.careful.hyperfvm.compose.screen.meishi.MeishiLinkReceiverScreen

/**
 * 链接接入中转页（透明壳），对应 Java 版的 `MeishiLinkReceiverActivity`。
 *
 * 系统解析「打开受支持的链接」后启动的是本页而不是主界面：窗口全透明、不画任何背景，
 * 收到链接后直接在来源应用（如微信）的画面上弹出确认弹层，确认 / 取消后立刻 finish，
 * 全程不跳转任何业务界面。
 *
 * 不要在这里套用界面上那套主题管理：透明由 manifest 指定的主题决定，
 * 一旦给窗口铺上主题色，来源应用就会被整块盖住。
 *
 * singleTop 下弹层还开着时又来一条链接会走 onNewIntent，这里有意不作处理：
 * 与 Java 版一致，本页处理完一条就会 finish，先把手上这条处理完更不容易出错。
 */
class MeishiLinkReceiverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 系统因内存不足重建时弹层已经丢了，直接收尾，免得留下一个透明壳挡住来源应用。
        if (savedInstanceState != null) {
            finish()
            return
        }
        val link = intent?.dataString
        if (link.isNullOrEmpty()) {
            finish()
            return
        }

        // 透明窗口也走 edge-to-edge：底部安全区交给弹层自己吃，系统栏图标跟着深浅色走。
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        // 小米设备会强制给导航栏加对比度遮罩，透明窗口下会留下一条灰带。
        window.isNavigationBarContrastEnforced = false

        val env = AndroidAppEnv(applicationContext)
        val store = SettingsStore(env)
        setContent {
            MeishiLinkReceiverScreen(
                env = env,
                store = store,
                link = link,
                onFinished = { finish() },
            )
        }
    }
}
