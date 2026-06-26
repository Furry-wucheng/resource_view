package dev.wucheng.resource_viewer.ui.screens.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.wucheng.resource_viewer.data.repository.FilesystemRepository
import dev.wucheng.resource_viewer.data.repository.ResourceRepository
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.model.ViewerItem
import dev.wucheng.resource_viewer.shared.content.ContentProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 查看器 UI 状态。
 */
sealed class ViewerUiState {
    /** 加载中 */
    data object Loading : ViewerUiState()

    /** 加载成功 */
    data class Success(
        val items: List<ViewerItem>,
        val resourceName: String,
    ) : ViewerUiState()

    /** 加载失败 */
    data class Error(val message: String) : ViewerUiState()
}

/**
 * 查看器 ViewModel。
 * 管理当前页、页面列表、预加载状态。
 *
 * 注意：此实现遵循 doc/mvp/M14-basic-viewer.md 中的 M14.2 子任务。
 */
class ViewerViewModel(
    private val resourceId: String,
    private val resourceRepository: ResourceRepository,
    private val filesystemRepository: FilesystemRepository,
) : ViewModel() {
    /** UI 状态 */
    private val _uiState = MutableStateFlow<ViewerUiState>(ViewerUiState.Loading)
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    /** 当前页码（0-based） */
    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    /** 总页数 */
    private val _totalPages = MutableStateFlow(0)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    /** 资源名称 */
    private val _resourceName = MutableStateFlow("")
    val resourceName: StateFlow<String> = _resourceName.asStateFlow()

    /** ContentProvider 实例 */
    private var contentProvider: ContentProvider? = null

    /**
     * 加载资源。
     */
    fun loadResource() {
        viewModelScope.launch {
            _uiState.value = ViewerUiState.Loading

            when (val result = resourceRepository.getById(resourceId)) {
                is Result.Ok -> {
                    val resource = result.value
                    if (resource == null) {
                        _uiState.value = ViewerUiState.Error("Resource not found")
                        return@launch
                    }

                    _resourceName.value = resource.name

                    // 获取 FileSource
                    when (val fsResult = filesystemRepository.getFileSource(resource.sourceId)) {
                        is Result.Ok -> {
                            // TODO: 根据资源类型创建 ContentProvider
                            // 目前使用 ImageFolderProvider
                            val fileSource = fsResult.value
                            val provider = dev.wucheng.resource_viewer.shared.content.ImageFolderProvider(
                                fileSource = fileSource,
                                relativePath = resource.relativePath,
                            )
                            contentProvider = provider
                            _totalPages.value = provider.pageCount

                            // 创建 ViewerItem 列表
                            val items = (0 until provider.pageCount).map { index ->
                                ViewerItem.ImagePage(
                                    title = resource.name,
                                    pageIndex = index,
                                    providerKey = resourceId,
                                )
                            }

                            _uiState.value = ViewerUiState.Success(
                                items = items,
                                resourceName = resource.name,
                            )
                        }
                        is Result.Err -> {
                            _uiState.value = ViewerUiState.Error("Failed to get file source")
                        }
                    }
                }
                is Result.Err -> {
                    _uiState.value = ViewerUiState.Error("Failed to load resource")
                }
            }
        }
    }

    /**
     * 导航到下一页。
     */
    fun nextPage() {
        val current = _currentPage.value
        val total = _totalPages.value
        if (current < total - 1) {
            _currentPage.value = current + 1
        }
    }

    /**
     * 导航到上一页。
     */
    fun previousPage() {
        val current = _currentPage.value
        if (current > 0) {
            _currentPage.value = current - 1
        }
    }

    /**
     * 导航到指定页。
     * @param page 目标页码（0-based）
     */
    fun goToPage(page: Int) {
        val total = _totalPages.value
        if (page in 0 until total) {
            _currentPage.value = page
        }
    }

    /**
     * 释放资源。
     */
    override fun onCleared() {
        super.onCleared()
        contentProvider?.dispose()
    }
}
