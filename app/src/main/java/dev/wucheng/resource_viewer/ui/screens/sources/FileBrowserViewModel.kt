package dev.wucheng.resource_viewer.ui.screens.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.wucheng.resource_viewer.data.repository.FilesystemRepository
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.error.ScanResult
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.Source
import dev.wucheng.resource_viewer.domain.model.Tag
import dev.wucheng.resource_viewer.domain.usecase.BatchAddResourcesUseCase
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FileViewMode { LIST, GRID }

data class FileBrowserUiState(
    val source: Source? = null,
    val currentPath: String = "",
    val entries: List<FileEntry> = emptyList(),
    /** 多选模式下的已选路径 */
    val selectedPaths: Set<String> = emptySet(),
    /** 是否处于多选模式 */
    val isMultiSelectMode: Boolean = false,
    val isLoading: Boolean = false,
    val isAdding: Boolean = false,
    val viewMode: FileViewMode = FileViewMode.LIST,
    val showBatchAddDialog: Boolean = false,
    val showDirectoryTree: Boolean = false,
    val allTags: List<Tag> = emptyList(),
    val error: String? = null,
    val lastAddResult: ScanResult? = null,
    /** 当前路径段列表，用于面包屑渲染 */
    val pathSegments: List<String> = emptyList(),
    /** 已入库的资源路径集合（用于标记"已入库"） */
    val importedPaths: Set<String> = emptySet(),
    // 文件浏览状态已移至 ViewerScreen/ViewerViewModel 管理
)

class FileBrowserViewModel(
    private val sourceId: String,
    private val filesystemRepository: FilesystemRepository,
    private val batchAddResourcesUseCase: BatchAddResourcesUseCase,
    private val tagRepository: dev.wucheng.resource_viewer.data.repository.TagRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FileBrowserUiState())
    val uiState: StateFlow<FileBrowserUiState> = _uiState.asStateFlow()

    private var fileSource: FileSource? = null

    fun load() {
        if (_uiState.value.source != null) {
            loadDirectory(_uiState.value.currentPath)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val sourceResult = filesystemRepository.getSource(sourceId)) {
                is Result.Ok -> {
                    val source = sourceResult.value
                    if (source == null) {
                        _uiState.update { it.copy(isLoading = false, error = "数据源不存在") }
                        return@launch
                    }
                    when (val fsResult = filesystemRepository.getFileSource(sourceId)) {
                        is Result.Ok -> {
                            fileSource = fsResult.value
                            _uiState.update { it.copy(source = source) }
                            loadDirectory("")
                        }
                        is Result.Err -> {
                            _uiState.update { it.copy(isLoading = false, error = fsResult.error.message) }
                        }
                    }
                }
                is Result.Err -> {
                    _uiState.update { it.copy(isLoading = false, error = sourceResult.error.message) }
                }
            }
        }
    }

    /** 进入子目录 */
    fun openDirectory(path: String) {
        loadDirectory(path)
    }

    /** 返回上层目录。返回 true 表示已导航，false 表示已在根目录。 */
    fun goUp(): Boolean {
        val current = _uiState.value.currentPath.trim('/')
        if (current.isEmpty()) return false
        val parent = current.substringBeforeLast("/", missingDelimiterValue = "")
        loadDirectory(parent)
        return true
    }

    /** 面包屑：导航到路径的第 N 段（0 = 根目录） */
    fun navigateToSegment(index: Int) {
        val segments = _uiState.value.currentPath.trim('/').split('/').filter { it.isNotEmpty() }
        val target = segments.take(index + 1).joinToString("/")
        loadDirectory(target)
    }

    fun toggleDirectoryTree() {
        _uiState.update { it.copy(showDirectoryTree = !it.showDirectoryTree) }
    }

    fun hideDirectoryTree() {
        _uiState.update { it.copy(showDirectoryTree = false) }
    }

    // ===== 多选模式 =====

    /** 进入多选模式 */
    fun enterMultiSelect() {
        _uiState.update { it.copy(isMultiSelectMode = true, selectedPaths = emptySet()) }
    }

    /** 退出多选模式 */
    fun exitMultiSelect() {
        _uiState.update { it.copy(isMultiSelectMode = false, selectedPaths = emptySet()) }
    }

    /** 切换某个文件的选中状态 */
    fun toggleSelection(path: String) {
        _uiState.update { state ->
            val next = state.selectedPaths.toMutableSet()
            if (!next.add(path)) next.remove(path)
            state.copy(selectedPaths = next, lastAddResult = null)
        }
    }

    /** 全选当前目录 */
    fun selectAll() {
        _uiState.update { state ->
            state.copy(selectedPaths = state.entries.map { it.relativePath }.toSet())
        }
    }

    /** 取消全选 */
    fun deselectAll() {
        _uiState.update { it.copy(selectedPaths = emptySet()) }
    }

    // ===== 批量添加 =====

    fun showBatchAddDialog() {
        val selected = _uiState.value.selectedPaths
        if (selected.isEmpty()) return
        viewModelScope.launch {
            val tags = tagRepository.getAllTagsOnce()
            _uiState.update { it.copy(showBatchAddDialog = true, allTags = tags) }
        }
    }

    fun hideBatchAddDialog() {
        _uiState.update { it.copy(showBatchAddDialog = false, allTags = emptyList()) }
    }

    fun confirmBatchAdd(
        organizationMode: dev.wucheng.resource_viewer.data.local.converter.OrganizationMode?,
        tagIds: List<String>,
    ) {
        val source = _uiState.value.source ?: return
        val selected = _uiState.value.selectedPaths.toList()
        val fs = fileSource ?: return
        if (selected.isEmpty()) return

        hideBatchAddDialog()
        viewModelScope.launch {
            _uiState.update { it.copy(isAdding = true, error = null, lastAddResult = null) }
            when (val result = batchAddResourcesUseCase(fs, source, selected, organizationMode, tagIds)) {
                is Result.Ok -> {
                    _uiState.update {
                        it.copy(
                            isAdding = false,
                            selectedPaths = emptySet(),
                            isMultiSelectMode = false,
                            lastAddResult = result.value,
                        )
                    }
                }
                is Result.Err -> {
                    _uiState.update { it.copy(isAdding = false, error = result.error.message) }
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(error = null, lastAddResult = null) }
    }

    fun toggleViewMode() {
        _uiState.update { state ->
            state.copy(viewMode = if (state.viewMode == FileViewMode.LIST) FileViewMode.GRID else FileViewMode.LIST)
        }
    }

    companion object {
        // 常量已移至 ViewerViewModel 中统一管理
    }

    private fun loadDirectory(path: String) {
        val source = _uiState.value.source ?: return
        viewModelScope.launch {
            val cleanPath = path.trim('/')
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    currentPath = cleanPath,
                    selectedPaths = emptySet(),
                    isMultiSelectMode = false,
                    lastAddResult = null,
                    pathSegments = if (cleanPath.isEmpty()) emptyList()
                    else cleanPath.split('/').filter { s -> s.isNotEmpty() },
                )
            }
            when (val result = filesystemRepository.listDirectory(source, cleanPath)) {
                is Result.Ok -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            entries = result.value.sortedWith(
                                compareBy<FileEntry> { !it.isDirectory }
                                    .thenBy { entry -> entry.name.lowercase() },
                            ),
                        )
                    }
                }
                is Result.Err -> {
                    _uiState.update {
                        it.copy(isLoading = false, entries = emptyList(), error = result.error.message)
                    }
                }
            }
        }
    }
}
