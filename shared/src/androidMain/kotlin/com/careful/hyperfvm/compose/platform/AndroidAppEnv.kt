package com.careful.hyperfvm.compose.platform

import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.Environment
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.careful.hyperfvm.compose.data.image.DataImageDownloadService
import java.io.File

/**
 * Android 侧的 [KeyValueStore]：直接包一层 SharedPreferences。
 *
 * 读取一律容错：SharedPreferences 的 getXxx 遇到类型不符会抛 ClassCastException，
 * 而升级安装时历史数据一定存在——旧版本用了 putStringSet 的键（例如仪表盘把卡片集合
 * 直接存成 StringSet），迁移后改成逗号拼接的字符串，不清数据就会在读取处崩。
 * 所以类型对不上时降级成默认值；[getString] 额外把历史 StringSet 就地拼成字符串，
 * 用户的排序与隐藏记录不会丢，下次写入就整体换成新类型。
 */
class AndroidKeyValueStore(private val prefs: SharedPreferences) : KeyValueStore {

    override fun getInt(key: String, defaultValue: Int): Int =
        runCatching { prefs.getInt(key, defaultValue) }.getOrDefault(defaultValue)

    override fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    override fun getLong(key: String, defaultValue: Long): Long =
        runCatching { prefs.getLong(key, defaultValue) }.getOrDefault(defaultValue)

    override fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    override fun getFloat(key: String, defaultValue: Float): Float =
        runCatching { prefs.getFloat(key, defaultValue) }.getOrDefault(defaultValue)

    override fun putFloat(key: String, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        runCatching { prefs.getBoolean(key, defaultValue) }.getOrDefault(defaultValue)

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun getString(key: String, defaultValue: String?): String? = try {
        prefs.getString(key, defaultValue)
    } catch (_: ClassCastException) {
        // 历史值是 StringSet：拼成新格式（逗号分隔）交给调用方过滤未知项。
        (prefs.all[key] as? Set<*>)?.filterIsInstance<String>()?.joinToString(",") ?: defaultValue
    }

    override fun putString(key: String, value: String?) {
        prefs.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    override fun contains(key: String): Boolean = prefs.contains(key)

    override fun commit() {
        prefs.edit().apply()
    }
}

/**
 * Android 平台环境。
 *
 * 注意：这里持有的是 applicationContext，避免泄漏 Activity。
 */
class AndroidAppEnv(context: Context) : AppEnv {

    private val appContext: Context = context.applicationContext

    override val filesDir get() = appContext.filesDir
    override val cacheDir get() = appContext.cacheDir

    // Android 侧拥有全部系统能力，提示仍走 Toast（与改造前一致）。
    override val capabilities = PlatformCapabilities()

    override val platformName: String =
        "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"

    // 取已安装包的元数据，不用在代码里再维护一份版本号。
    private val packageInfo = runCatching {
        @Suppress("DEPRECATION")
        appContext.packageManager.getPackageInfo(appContext.packageName, 0)
    }.getOrNull()

    override val appVersionName: String = packageInfo?.versionName ?: "—"
    override val appVersionCode: Long = packageInfo?.longVersionCode ?: 0L

    override fun prefs(name: String): KeyValueStore =
        AndroidKeyValueStore(appContext.getSharedPreferences(name, Context.MODE_PRIVATE))

    override fun openUrl(url: String): Boolean = runCatching {
        appContext.startActivity(
            Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.isSuccess

    override fun copyToClipboard(text: String): Boolean = runCatching {
        val manager = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText(null, text))
    }.isSuccess

    override fun showToast(message: String) {
        Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
    }

    override fun log(tag: String, message: String) {
        Log.i(tag, message)
    }

    override fun addToCalendar(title: String, description: String, beginMillis: Long, endMillis: Long): Boolean = runCatching {
        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, title)
            .putExtra(CalendarContract.Events.DESCRIPTION, description)
            // 主流日历（AOSP/Google）通过这两个 extra 读取起止时间，缺省时会落到当前时刻。
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginMillis)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            // DTSTART/DTEND 保留，兼容按数据列读取的其他日历。
            .putExtra(CalendarContract.Events.DTSTART, beginMillis)
            .putExtra(CalendarContract.Events.DTEND, endMillis)
            .putExtra(CalendarContract.Events.EVENT_TIMEZONE, "Asia/Shanghai")
            // 用的是 applicationContext，必须新建任务栈。
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }.isSuccess

    override fun saveImageToGallery(file: File, mimeType: String): Boolean {
        val resolver = appContext.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + File.separator + "HyperFVM",
            )
            // 两段式写入：复制完成前标记为 pending，避免相册里出现半截文件。
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        return try {
            resolver.openOutputStream(uri)?.use { output ->
                file.inputStream().use { input -> input.copyTo(output) }
            } ?: return false
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } catch (_: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            false
        }
    }

    override fun shareImage(file: File, mimeType: String): Boolean {
        val uri = try {
            FileProvider.getUriForFile(appContext, "${appContext.packageName}.dataimages", file)
        } catch (_: IllegalArgumentException) {
            return false
        }
        return runCatching {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            appContext.startActivity(
                Intent.createChooser(intent, "分享图片").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.isSuccess
    }

    override fun dismissBackgroundNotification() {
        appContext.getSystemService(NotificationManager::class.java)
            ?.cancel(DataImageDownloadService.NotificationId)
    }

    override fun openSupportedLinksSettings(): Boolean = runCatching {
        // 不限定 host：域名验证状态读不到，直接把用户丢到这一页自己确认。
        val intent = Intent(
            Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
            "package:${appContext.packageName}".toUri(),
        )
        val target = if (intent.resolveActivity(appContext.packageManager) != null) {
            intent
        } else {
            // 部分精简 ROM 没有这个页面，退到应用详情页让用户自己找。
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                "package:${appContext.packageName}".toUri(),
            )
        }
        appContext.startActivity(target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess

    override fun isSystemDark(): Boolean =
        (appContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
}
