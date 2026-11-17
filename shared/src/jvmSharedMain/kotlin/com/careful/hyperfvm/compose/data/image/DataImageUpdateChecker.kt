package com.careful.hyperfvm.compose.data.image

import com.careful.hyperfvm.compose.platform.AppEnv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 「数据图有没有新版本」的检查，供仪表盘那张更新提示卡使用。
 *
 * 只比版本号、不看文件缺失：云端清单里的最大版本比本地记录的版本新，
 * 就说明有一批图可以更新（从没下载过时本地版本是 0，同样会提示）。
 * 网络或解析失败原样抛出，由调用方决定要不要保留上一次的结果。
 */
internal object DataImageUpdateChecker {

    suspend fun hasNewVersion(env: AppEnv): Boolean = withContext(Dispatchers.IO) {
        val remote = DataImageStore.fetchRemoteList()
        DataImageStore.newestVersion(remote) > DataImageStore.localVersion(env)
    }
}
