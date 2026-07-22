package dev.wucheng.resource_viewer.shared.media

object MediaFormats {
    val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "bmp", "gif", "avif")
    val videoExtensions = setOf("mp4", "mkv", "avi", "mov", "webm", "wmv")
    val archiveExtensions = setOf("zip", "cbz", "7z", "rar", "cbr")
    val readableArchiveExtensions = setOf("zip", "cbz", "7z")

    fun isImage(extension: String): Boolean = extension.lowercase() in imageExtensions
    fun isVideo(extension: String): Boolean = extension.lowercase() in videoExtensions
    fun isArchive(extension: String): Boolean = extension.lowercase() in archiveExtensions
    fun isReadableArchive(extension: String): Boolean = extension.lowercase() in readableArchiveExtensions
    fun isPreviewable(extension: String): Boolean = isImage(extension) || isVideo(extension)
}
