package com.careful.hyperfvm.compose.component.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.careful.hyperfvm.compose.data.catalog.CatalogImages
import com.careful.hyperfvm.compose.data.catalog.CatalogRepository
import com.careful.hyperfvm.compose.platform.AppEnv
import com.careful.hyperfvm.compose.platform.LocalAppEnv
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Image
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 道具图标（远程）：按道具 id 从官方图床取图，落盘后与合集预览图共用同一份缓存。
 *
 * 概率公示的宝箱封面、抽奖封面与奖品图标都走这里：公示数据随时可能加入新道具，
 * 图片不随数据包分发，新增 id 不用重新打包也能显示。
 *
 * 旧数据里还有少量语义命名的图（如 `star_image_*` / `vip_*`），图床只认道具 id，
 * 这类交给 [PackImage] 继续读数据包；下载中 / 下载失败 / 无 id 时统一画
 * Miuix 的图片占位图标（与数据图合集「未下载」占位一致），卡片高度不会抽动。
 */
@Composable
fun RemoteItemImage(
    id: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
) {
    if (id == null || !ItemIdPattern.matches(id)) {
        PackImage(
            id = id,
            modifier = modifier,
            contentDescription = contentDescription,
            contentScale = contentScale,
            placeholder = { inner -> RemoteItemPlaceholder(inner) },
        )
        return
    }
    val env = LocalAppEnv.current
    // 图床地址与落盘文件名都用小写 id，源数据大小写不一致时也不用单独特判。
    val key = id.lowercase()
    // 缓存里已有就同步出图，避免滚动时闪一下空位。
    val bitmap by produceState<ImageBitmap?>(
        initialValue = CatalogImages.peek(CatalogRepository.imageFile(env, key)),
        key,
    ) {
        if (value != null) return@produceState
        val file = CatalogRepository.imageFile(env, key)
        CatalogRepository.ensureImage(env, key)
        value = CatalogImages.load(file)
    }
    val current = bitmap
    if (current != null) {
        Image(
            bitmap = current,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
    } else {
        RemoteItemPlaceholder(modifier)
    }
}

/**
 * 未加载 / 无图时的占位：Miuix 的图片图标，与数据图合集「未下载」卡片的占位一致。
 * 图标按容器取一半（40dp 格 → 20dp、44dp → 22dp、60dp → 30dp），各种尺寸的格子都不突兀。
 */
@Composable
private fun RemoteItemPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Icon(
            imageVector = MiuixIcons.Image,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.5f),
            tint = MiuixTheme.colorScheme.onBackgroundVariant,
        )
    }
}

/**
 * 预取一组道具图：详情页打开时先把整页要用的图标拉下来，滚动时不再逐行等下载。
 *
 * 单张失败只影响那一张（界面留空位），不阻断其余图片；已落盘的直接跳过。
 */
suspend fun prefetchItemImages(env: AppEnv, ids: Iterable<String>) = coroutineScope {
    ids.distinct().filter(ItemIdPattern::matches).forEach { id ->
        launch { CatalogRepository.ensureImage(env, id.lowercase()) }
    }
}

/** 道具 id 的命名规则：`x` + 十六进制，如 `x13b89730`；与图床地址的拼法一致。 */
private val ItemIdPattern = Regex("x[0-9a-fA-F]+")
