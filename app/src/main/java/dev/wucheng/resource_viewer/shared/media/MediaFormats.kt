package dev.wucheng.resource_viewer.shared.media

object MediaFormats {
    val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "bmp", "gif", "avif")
    val videoExtensions = setOf("mp4", "mkv", "avi", "mov", "webm", "wmv")

    fun isImage(extension: String): Boolean = extension.lowercase() in imageExtensions
    fun isVideo(extension: String): Boolean = extension.lowercase() in videoExtensions
    fun isPreviewable(extension: String): Boolean = isImage(extension) || isVideo(extension)
}
