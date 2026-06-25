package dev.wucheng.resource_viewer.domain.error

/**
 * 领域错误类型层次结构。
 *
 * 注意：此定义来自 doc/share/06-error-handling.md 共享契约。
 */
sealed class DomainError(val message: String, val cause: Throwable? = null) {
    // I/O
    class SourceUnreachableError(message: String, cause: Throwable? = null) : DomainError(message, cause)
    class SourceAuthError(message: String, cause: Throwable? = null) : DomainError(message, cause)
    class FileNotFoundError(message: String, cause: Throwable? = null) : DomainError(message, cause)
    class FileAccessDeniedError(message: String, cause: Throwable? = null) : DomainError(message, cause)
    class NetworkTimeoutError(message: String, cause: Throwable? = null) : DomainError(message, cause)

    // Media
    class MediaLoadError(val mediaType: MediaType, message: String, cause: Throwable? = null) : DomainError(message, cause)
    class MediaEncryptedError(val mediaType: MediaType, message: String, cause: Throwable? = null) : DomainError(message, cause)
    class MediaStreamError(val mediaType: MediaType, message: String, cause: Throwable? = null) : DomainError(message, cause)

    // Validation
    class ValidationError(message: String) : DomainError(message)

    // State
    class OperationCancelledError(message: String = "操作已取消") : DomainError(message)
    class InsufficientStorageError(message: String, cause: Throwable? = null) : DomainError(message, cause)
    class UnsupportedFormatError(val format: String, message: String) : DomainError(message)

    // Database
    class DatabaseError(message: String, cause: Throwable? = null) : DomainError(message, cause)
}

enum class MediaType { IMAGE, PDF, ARCHIVE, VIDEO }

/**
 * 将 DomainError 转换为用户可读的消息。
 */
fun DomainError.toUserMessage(): String = when (this) {
    is DomainError.SourceUnreachableError -> "数据源不可达，请检查网络连接"
    is DomainError.SourceAuthError -> "认证失败，请检查用户名和密码"
    is DomainError.FileNotFoundError -> "文件或路径不存在"
    is DomainError.FileAccessDeniedError -> "权限不足"
    is DomainError.NetworkTimeoutError -> "连接超时，请稍后重试"
    is DomainError.MediaLoadError -> when (mediaType) {
        MediaType.IMAGE -> "图片加载失败"
        MediaType.PDF -> "PDF 加载失败"
        MediaType.VIDEO -> "视频加载失败"
        MediaType.ARCHIVE -> "压缩包读取失败"
    }
    is DomainError.MediaEncryptedError -> when (mediaType) {
        MediaType.PDF -> "加密 PDF 暂不支持"
        else -> "加密文件暂不支持"
    }
    is DomainError.MediaStreamError -> "媒体流中断"
    is DomainError.ValidationError -> message
    is DomainError.OperationCancelledError -> "操作已取消"
    is DomainError.InsufficientStorageError -> "存储空间不足"
    is DomainError.UnsupportedFormatError -> "不支持的格式: $format"
    is DomainError.DatabaseError -> "数据库错误"
}

/**
 * 判断错误是否可重试。
 */
val DomainError.canRetry: Boolean
    get() = this is DomainError.SourceUnreachableError ||
            this is DomainError.FileNotFoundError ||
            this is DomainError.NetworkTimeoutError ||
            this is DomainError.MediaLoadError
