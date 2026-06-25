package dev.wucheng.resource_viewer.domain.error

/**
 * 进度流类型，用于扫描等流式操作发射进度更新。
 *
 * 注意：此定义来自 doc/share/06-error-handling.md 共享契约。
 */
sealed class Progress<out T> {
    data class Update<T>(val current: Int, val total: Int) : Progress<T>()
    data class Done<T>(val result: T) : Progress<T>()
    data class Error(val error: DomainError) : Progress<Nothing>()
}

/**
 * 扫描结果数据类。
 */
data class ScanResult(
    val successCount: Int,
    val skipCount: Int,
    val failures: List<Pair<String, DomainError>>,
)
