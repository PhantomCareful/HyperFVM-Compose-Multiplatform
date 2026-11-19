package com.careful.hyperfvm.compose.component.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.careful.hyperfvm.compose.data.datapack.PackImages

/**
 * 数据包图片：按图片 id（文件名去扩展名）从本地数据包异步加载。
 *
 * 数据包未下载 / 图片缺失 / 解码失败时交给 [placeholder] 画占位（默认留空位），
 * 不抛异常也不显示破图——首次下载数据包之前所有图片界面的行为保持一致。
 */
@Composable
fun PackImage(
    id: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
    placeholder: @Composable (Modifier) -> Unit = {},
) {
    // 缓存里已有就同步出图，避免滚动时闪一下空位。
    val bitmap by produceState<ImageBitmap?>(initialValue = id?.let(PackImages::peek), id) {
        if (id != null && value == null) value = PackImages.load(id)
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
        Box(modifier) { placeholder(Modifier.fillMaxSize()) }
    }
}
