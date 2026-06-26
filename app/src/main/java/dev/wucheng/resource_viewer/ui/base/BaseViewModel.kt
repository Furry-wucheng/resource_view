package dev.wucheng.resource_viewer.ui.base

import androidx.lifecycle.ViewModel
import dev.wucheng.resource_viewer.domain.error.DomainError
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.error.canRetry
import dev.wucheng.resource_viewer.domain.error.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI 状态枚举。
 */
enum class UiState {
    IDLE,
    LOADING,
    SUCCESS,
    ERROR,
}

/**
 * ViewModel 基类，统一全 App 的错误处理逻辑。
 *
 * 提供：
 * - [uiState] UI 状态流
 * - [errorMessage] 用户可读的错误消息
 * - [lastError] 最近一次的领域错误
 * - [canRetry] 当前错误是否可重试
 * - [handleResult] 统一处理 Result
 */
abstract class BaseViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState.IDLE)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _lastError = MutableStateFlow<DomainError?>(null)
    val lastError: StateFlow<DomainError?> = _lastError.asStateFlow()

    val canRetry: Boolean
        get() = _lastError.value?.canRetry ?: false

    /**
     * 统一处理 [Result]，更新 [uiState]、[errorMessage]、[lastError]。
     */
    protected fun <T> handleResult(result: Result<T>) {
        when (result) {
            is Result.Ok -> {
                _uiState.value = UiState.SUCCESS
                _errorMessage.value = null
                _lastError.value = null
            }
            is Result.Err -> {
                _uiState.value = UiState.ERROR
                _errorMessage.value = mapError(result.error)
                _lastError.value = result.error
            }
        }
    }

    /**
     * 设置 UI 状态为加载中。
     */
    protected fun setLoading() {
        _uiState.value = UiState.LOADING
        _errorMessage.value = null
    }

    /**
     * 设置 UI 状态为空闲。
     */
    protected fun setIdle() {
        _uiState.value = UiState.IDLE
        _errorMessage.value = null
        _lastError.value = null
    }

    /**
     * 将 [DomainError] 映射为用户可读消息。
     * 子类可覆盖以提供自定义映射。
     */
    protected open fun mapError(error: DomainError): String = error.toUserMessage()
}
