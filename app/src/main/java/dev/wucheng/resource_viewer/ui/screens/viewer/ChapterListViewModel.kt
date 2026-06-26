package dev.wucheng.resource_viewer.ui.screens.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.wucheng.resource_viewer.data.repository.FilesystemRepository
import dev.wucheng.resource_viewer.data.repository.ResourceRepository
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.model.Chapter
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import dev.wucheng.resource_viewer.shared.organization.ChapterGalleryStrategy
import dev.wucheng.resource_viewer.shared.organization.ChapterStrategy
import dev.wucheng.resource_viewer.shared.organization.OrganizationStrategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 章节列表 UI 状态。
 */
sealed class ChapterListUiState {
    /** 加载中 */
    data object Loading : ChapterListUiState()

    /** 加载成功 */
    data class Success(
        val chapters: List<Chapter>,
        val resourceName: String,
    ) : ChapterListUiState()

    /** 加载失败 */
    data class Error(val message: String) : ChapterListUiState()
}

/**
 * 章节列表 ViewModel。
 * 加载资源的章节列表，支持 CHAPTER 和 CHAPTER_GALLERY 模式。
 *
 * 注意：此实现遵循 doc/mvp/M21-chapter-strategies.md 中的 M21.3 子任务。
 */
class ChapterListViewModel(
    private val resourceId: String,
    private val resourceRepository: ResourceRepository,
    private val filesystemRepository: FilesystemRepository,
) : ViewModel() {

    /** UI 状态 */
    private val _uiState = MutableStateFlow<ChapterListUiState>(ChapterListUiState.Loading)
    val uiState: StateFlow<ChapterListUiState> = _uiState.asStateFlow()

    /** 资源信息 */
    private var resource: Resource? = null
    private var fileSource: FileSource? = null

    /**
     * 加载章节列表。
     */
    fun loadChapters() {
        viewModelScope.launch {
            _uiState.value = ChapterListUiState.Loading

            when (val result = resourceRepository.getById(resourceId)) {
                is Result.Ok -> {
                    val res = result.value
                    if (res == null) {
                        _uiState.value = ChapterListUiState.Error("Resource not found")
                        return@launch
                    }

                    resource = res

                    when (val fsResult = filesystemRepository.getFileSource(res.sourceId)) {
                        is Result.Ok -> {
                            val fs = fsResult.value
                            fileSource = fs

                            try {
                                val strategy = getStrategy(res)
                                val chapters = strategy.getChapters(res, fs)

                                _uiState.value = ChapterListUiState.Success(
                                    chapters = chapters,
                                    resourceName = res.name,
                                )
                            } catch (e: Exception) {
                                _uiState.value = ChapterListUiState.Error(
                                    "Failed to load chapters: ${e.message}"
                                )
                            }
                        }
                        is Result.Err -> {
                            _uiState.value = ChapterListUiState.Error("Failed to get file source")
                        }
                    }
                }
                is Result.Err -> {
                    _uiState.value = ChapterListUiState.Error("Failed to load resource")
                }
            }
        }
    }

    /**
     * 根据资源类型获取组织策略。
     */
    private fun getStrategy(resource: Resource): OrganizationStrategy {
        return when (resource.organizationMode) {
            dev.wucheng.resource_viewer.data.local.converter.OrganizationMode.CHAPTER -> ChapterStrategy()
            dev.wucheng.resource_viewer.data.local.converter.OrganizationMode.CHAPTER_GALLERY -> ChapterGalleryStrategy()
            else -> throw IllegalStateException("Unsupported organization mode: ${resource.organizationMode}")
        }
    }
}
