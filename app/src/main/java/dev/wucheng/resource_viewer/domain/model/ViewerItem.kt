package dev.wucheng.resource_viewer.domain.model

import androidx.media3.datasource.DataSource
import dev.wucheng.resource_viewer.shared.filesource.FileSource

sealed class ViewerItem {
    abstract val title: String

    /**
     * 图片/PDF 页。
     * 不存储 lambda，ViewModel 通过 contentProvider.loadPage(pageIndex, w, h) 加载。
     */
    data class ImagePage(
        override val title: String,
        val pageIndex: Int,
        /** ContentProvider 实例标识，ViewModel 用于查找对应 Provider 加载 */
        val providerKey: String = "",
        val pixelWidth: Int? = null,
        val pixelHeight: Int? = null,
        /** 文件扩展名（如 "gif", "jpg"），用于区分动画图片和静态图片 */
        val extension: String = "",
    ) : ViewerItem() {
        /** 是否为动画图片（GIF/animated WebP） */
        val isAnimated: Boolean
            get() = extension.lowercase() in ANIMATED_EXTENSIONS

        companion object {
            private val ANIMATED_EXTENSIONS = setOf("gif", "webp")
        }
    }

    data class Video(
        override val title: String,
        val videoSource: VideoMediaSource,
    ) : ViewerItem()
}

sealed class VideoMediaSource {
    data class LocalFile(val path: String) : VideoMediaSource()

    /**
     * SMB 视频源。
     * @param dataSourceFactory ExoPlayer DataSource 工厂（由调用方创建，传入 Source + password）
     * @param relativePath 视频文件相对路径
     * @param fileSize 文件大小 (bytes)
     */
    data class SmbFile(
        val dataSourceFactory: DataSource.Factory,
        val relativePath: String,
        val fileSize: Long,
    ) : VideoMediaSource()
}
