package dev.wucheng.resource_viewer.ui.base

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全局致命错误持有者。
 *
 * 用于在 Application 层捕获不可恢复的异常（如数据库损坏），
 * UI 层可观察 [fatalError] 来显示全局错误页面。
 */
object FatalErrorHolder {

    private val _fatalError = MutableStateFlow<String?>(null)
    val fatalError: StateFlow<String?> = _fatalError.asStateFlow()

    /**
     * 设置致命错误消息。
     */
    fun setFatalError(message: String) {
        _fatalError.value = message
    }

    /**
     * 清除致命错误（用于重试）。
     */
    fun clear() {
        _fatalError.value = null
    }
}
