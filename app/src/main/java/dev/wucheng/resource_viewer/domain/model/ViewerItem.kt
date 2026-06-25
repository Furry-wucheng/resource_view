package dev.wucheng.resource_viewer.domain.model

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
    ) : ViewerItem()

    data class Video(
        override val title: String,
        val videoSource: VideoMediaSource,
    ) : ViewerItem()
}

sealed class VideoMediaSource {
    data class LocalFile(val path: String) : VideoMediaSource()
    data class SmbFile(
        val fileSource: FileSource,
        val relativePath: String,
        val fileSize: Long,
    ) : VideoMediaSource()
}
