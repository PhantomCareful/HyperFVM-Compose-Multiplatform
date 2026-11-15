package com.careful.hyperfvm.compose.data.image

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.careful.hyperfvm.compose.platform.AndroidAppEnv
import com.careful.hyperfvm.compose.platform.AppLaunchTarget

/**
 * 数据图下载前台服务。
 *
 * 下载本身由共享层的 [DataImageDownloadEngine] 完成，这里只做 Android 特有的事：
 * 前台服务保活、通知栏实时进度与暂停/继续/取消按钮、完成态通知。
 * 桌面端不需要这一层，直接把 [LocalDownloadRunner] 装上即可。
 *
 * 与 Java 版 `DownloadService` 的数据图分支对齐；刻意没有迁移 APK 升级包分支，
 * 本工程还没有升级检查功能，等它落地时再按同样结构补。
 */
class DataImageDownloadService : Service() {

    companion object {
        const val ActionStart = "com.careful.hyperfvm.compose.data.image.action.START"
        const val ActionPause = "com.careful.hyperfvm.compose.data.image.action.PAUSE"
        const val ActionResume = "com.careful.hyperfvm.compose.data.image.action.RESUME"
        const val ActionCancel = "com.careful.hyperfvm.compose.data.image.action.CANCEL"

        const val ExtraImageList = "extra_image_list"
        const val ExtraNewestVersion = "extra_newest_version"

        /** 进度与结果共用的通知 id；数据图页打开时按它撤下完成态通知。 */
        const val NotificationId = 0x1001

        /** 旧的单渠道 id：拆分后删掉，避免设置里残留一个永远收不到通知的条目。 */
        private const val LegacyChannelId = "data_image_download"

        /** 进度渠道：高重要性才有资格成为 Live Update，但要静音——进度刷新频率太高。 */
        private const val ProgressChannelId = "data_image_download_progress"

        /** 结果渠道：高重要性 + 默认提示音，完成/失败以悬浮通知弹出。 */
        private const val ResultChannelId = "data_image_download_result"

        /**
         * promoted ongoing（Live Update）请求的 extras 键。
         *
         * 同名字段 36.1 才加入，直接引用会触发 NewApi lint；键值是公开契约字符串，
         * 编译期内联、系统只读，Android 16.0 起即生效。
         */
        private const val ExtraRequestPromotedOngoing = "android.requestPromotedOngoing"

        /** 把下载请求转成一次前台服务启动。 */
        fun start(context: Context, list: List<DataImageRemoteInfo>, newestVersion: Long) {
            val intent = Intent(context, DataImageDownloadService::class.java).apply {
                action = ActionStart
                putExtra(ExtraNewestVersion, newestVersion)
                putExtra(ExtraImageList, ArrayList(list))
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun pause(context: Context) = sendCommand(context, ActionPause)

        fun resume(context: Context) = sendCommand(context, ActionResume)

        fun cancel(context: Context) = sendCommand(context, ActionCancel)

        private fun sendCommand(context: Context, action: String) {
            val intent = Intent(context, DataImageDownloadService::class.java).setAction(action)
            // 服务没在跑时 startService 会让它起来再立刻退出，属于无害空转；
            // 吞掉异常是为了避开 Android 12+ 后台启动限制导致的崩溃。
            try {
                context.startService(intent)
            } catch (_: IllegalStateException) {
            }
        }
    }

    private lateinit var appEnv: AndroidAppEnv

    @Volatile
    private var engine: DataImageDownloadEngine? = null

    override fun onCreate() {
        super.onCreate()
        appEnv = AndroidAppEnv(applicationContext)
        ensureChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ActionStart -> handleStart(intent)
            ActionPause -> engine?.pause()
            ActionResume -> engine?.resume()
            ActionCancel -> handleCancel()
            else -> if (engine?.isActive != true) stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        engine?.shutdown()
        super.onDestroy()
    }

    // ==================== 任务生命周期 ====================

    @Suppress("UNCHECKED_CAST")
    private fun handleStart(intent: Intent) {
        // 已有任务在跑时忽略重复启动（防止连点重复下载）。
        if (engine?.isActive == true) return

        val list = (intent.getSerializableExtra(ExtraImageList) as? ArrayList<DataImageRemoteInfo>)
            ?.toList()
            .orEmpty()
        val newestVersion = intent.getLongExtra(ExtraNewestVersion, 0L)

        if (list.isEmpty()) {
            // startForegroundService 的请求必须用 startForeground 兜底，否则系统会报错。
            ServiceCompat.startForeground(
                this,
                NotificationId,
                idleNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
            removeForegroundNotification()
            stopSelf()
            return
        }

        openFrontForeground(list.size)

        val engine = DataImageDownloadEngine(
            env = appEnv,
            onProgress = { state -> if (state.isActive) updateNotification(progressNotification(state)) },
            onFinished = ::handleFinished,
        )
        this.engine = engine
        engine.start(list, newestVersion)
    }

    /** 引擎跑完（含被取消）时收尾：完成态留在通知栏，取消态撤掉通知。 */
    private fun handleFinished(state: DataImageDownloadState) {
        if (state.phase == DataImageDownloadState.Phase.Idle) {
            removeForegroundNotification()
        } else {
            updateNotification(finalNotification(state))
            detachForeground()
        }
        engine = null
        stopSelf()
    }

    private fun handleCancel() {
        val engine = engine ?: return
        engine.cancel()
        // 取消后不该留下进度通知，必须用 REMOVE；DETACH 会把「已取消」的进度条留在通知栏。
        // 临时文件清理与状态收尾由引擎的批任务线程完成。
        removeForegroundNotification()
    }

    // ==================== 通知 ====================

    private fun idleNotification(): Notification =
        baseNotification(ProgressChannelId)
            .setContentTitle("准备下载数据图")
            .setContentText("正在启动下载任务")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    private fun progressNotification(state: DataImageDownloadState): Notification {
        val downloading = state.phase == DataImageDownloadState.Phase.Downloading
        // 百分比写进标题：Live Update 的状态栏胶囊直接展示标题里最显眼的那段文本。
        val title = if (downloading) {
            "正在下载数据图 ${state.percent}%"
        } else {
            "数据图下载已暂停 ${state.percent}%"
        }
        val text = buildString {
            append("已完成 ${state.completed} / ${state.total}")
            if (state.rangeRestarted) append("，图片源不支持续传，已从头下载")
        }
        val builder = baseNotification(ProgressChannelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                notificationAction(
                    if (downloading) "暂停" else "继续",
                    commandIntent(if (downloading) ActionPause else ActionResume),
                ),
            )
            .addAction(notificationAction("取消", commandIntent(ActionCancel)))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            // Android 16+ 的 Live Update：ProgressStyle 进度模板 + promoted ongoing，
            // 以状态栏胶囊 / 锁屏卡片的形式常驻展示进度。
            // progressMax 取所有 Segment 长度之和，这里用单段 100 表示百分比刻度。
            builder
                .setStyle(
                    Notification.ProgressStyle()
                        .setStyledByProgress(true)
                        .setProgressSegments(listOf(Notification.ProgressStyle.Segment(100)))
                        .setProgress(state.percent.coerceIn(0, 100)),
                )
                .addExtras(promotedOngoingExtras())
                .setShortCriticalText(if (downloading) "${state.percent}%" else "已暂停")
        } else {
            // 31-35 没有 Live Update，退回经典进度条。
            builder.setProgress(100, state.percent.coerceIn(0, 100), false)
        }
        return builder.build()
    }

    private fun finalNotification(state: DataImageDownloadState): Notification {
        val success = state.total - state.failedCount
        val title = if (state.failedCount == 0) "数据图更新完成" else "数据图更新完成（部分失败）"
        val text = if (state.failedCount == 0) {
            "已更新 $success 张数据图"
        } else {
            "$success 张更新成功，${state.failedCount} 张更新失败"
        }
        val builder = baseNotification(ResultChannelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setStyle(Notification.BigTextStyle().bigText(text))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            // 完成态同样走 Live Update：promoted 要求 ongoing、不可手动滑除，
            // 由数据图合集页打开时替用户撤下（见 DataImagesScreen）。
            builder
                .addExtras(promotedOngoingExtras())
                .setShortCriticalText("已完成")
                .setOngoing(true)
        } else {
            builder.setOngoing(false).setAutoCancel(true)
        }
        return builder.build()
    }

    /**
     * 构建基础 Builder。
     *
     * 用 framework 的 [Notification.Builder] 而不是 NotificationCompat：Live Update 需要的
     * `ProgressStyle` 与 `setShortCriticalText` 都只存在于 framework 侧。
     * 点按后一律回到数据图合集页，而不是主界面。
     */
    private fun baseNotification(channelId: String): Notification.Builder =
        Notification.Builder(this, channelId)
            .setCategory(Notification.CATEGORY_PROGRESS)
            // 启动目标由 :app 注入；尚未注入时与旧行为一致（不挂点击事件）。
            .also { builder -> openDataImagesIntent()?.let(builder::setContentIntent) }
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)

    /**
     * 数据图合集页的通知点击入口。
     *
     * 直接指向 Activity：Android 12+ 的 trampoline 政策禁止经 broadcast/service 中转 startActivity。
     * `CLEAR_TOP | SINGLE_TOP` 配合 Manifest 里的 singleTop 复用栈内已有的 MainActivity，
     * 由它在 onNewIntent 里翻译成一次导航。
     */
    private fun openDataImagesIntent(): PendingIntent? {
        val activityClass = AppLaunchTarget.activityClass ?: return null
        val intent = Intent(this, activityClass).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
            putExtra(AppLaunchTarget.ExtraOpenDataImages, true)
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /** 通知按钮。图标在 Android 7+ 已不再展示，用系统下载图标占位即可。 */
    private fun notificationAction(title: String, intent: PendingIntent): Notification.Action =
        Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.stat_sys_download),
            title,
            intent,
        ).build()

    /** promoted ongoing（Live Update）的请求 extras，36.0 设备上即生效。 */
    private fun promotedOngoingExtras(): Bundle =
        Bundle().apply { putBoolean(ExtraRequestPromotedOngoing, true) }

    private fun commandIntent(action: String): PendingIntent =
        PendingIntent.getService(
            this,
            action.hashCode(),
            Intent(this, DataImageDownloadService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun updateNotification(notification: Notification) {
        getSystemService(NotificationManager::class.java).notify(NotificationId, notification)
    }

    /**
     * 建好进度与结果两个渠道。
     *
     * 进度要静音（刷新频率高）、结果要出声并悬浮弹出，两者重要性诉求不同，只能拆成两个；
     * 渠道建好后重要性不可修改，旧版本留下的单渠道直接删掉。
     */
    private fun ensureChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.deleteNotificationChannel(LegacyChannelId)
        manager.createNotificationChannel(
            NotificationChannel(ProgressChannelId, "数据图下载进度", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "下载数据图资源时的实时进度"
                setSound(null, null)
                setShowBadge(false)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(ResultChannelId, "数据图下载完成", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "数据图更新完成与部分失败的提醒"
                setShowBadge(false)
            },
        )
    }

    private fun openFrontForeground(total: Int) {
        ServiceCompat.startForeground(
            this,
            NotificationId,
            progressNotification(DataImageDownloadState(phase = DataImageDownloadState.Phase.Downloading, total = total)),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    /** 摘掉前台关联但保留已发布的通知：完成态还要继续留在通知栏里。 */
    private fun detachForeground() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
    }

    /** 摘掉前台关联并移除通知。 */
    private fun removeForegroundNotification() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }
}

/** Android 侧的执行者：把请求转给 [DataImageDownloadService]。 */
class ServiceDownloadRunner(private val context: Context) : DataImageDownloadRunner {

    override fun start(env: com.careful.hyperfvm.compose.platform.AppEnv, list: List<DataImageRemoteInfo>, newestVersion: Long) {
        DataImageDownloadService.start(context, list, newestVersion)
    }

    override fun pause() = DataImageDownloadService.pause(context)

    override fun resume() = DataImageDownloadService.resume(context)

    override fun cancel() = DataImageDownloadService.cancel(context)
}
