package dev.wucheng.resource_viewer.domain.error

/**
 * 统一结果包装类型。
 *
 * 注意：此定义来自 doc/share/06-error-handling.md 共享契约。
 */
sealed class Result<out T> {
    data class Ok<T>(val value: T) : Result<T>()
    data class Err(val error: DomainError) : Result<Nothing>()
}

/**
 * 便捷扩展函数：将值包装为 Ok。
 */
fun <T> T.asOk(): Result<T> = Result.Ok(this)

/**
 * 便捷扩展函数：将 DomainError 包装为 Err。
 */
fun DomainError.asErr(): Result<Nothing> = Result.Err(this)

/**
 * 安全执行块，捕获异常并转换为 Result。
 */
inline fun <T> runCatching(block: () -> T): Result<T> {
    return try {
        Result.Ok(block())
    } catch (e: Exception) {
        Result.Err(DomainError.ValidationError(e.message ?: "Unknown error"))
    }
}
