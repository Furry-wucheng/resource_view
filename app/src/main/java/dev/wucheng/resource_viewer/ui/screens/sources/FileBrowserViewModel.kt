package dev.wucheng.resource_viewer.ui.screens.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.wucheng.resource_viewer.data.repository.FilesystemRepository
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.error.ScanResult
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.Source
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
    val selectedPaths: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isAdding: Boolean = false,
    val viewMode: FileViewMode = FileViewMode.LIST,
    val previewFile: FileEntry? = null,
    val showBatchAddDialog: Boolean = false,
    val showDirectoryTree: Boolean = false,
    val allTags: List<dev.wucheng.resource_viewer.domain.model.Tag> = emptyList(),
    val error: String? = null,
    val lastAddResult: ScanResult? = null,
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

    fun openDirectory(path: String) {
        loadDirectory(path)
    }

    fun goUp() {
        val current = _uiState.value.currentPath.trim('/')
        val parent = current.substringBeforeLast("/", missingDelimiterValue = "")
        loadDirectory(parent)
    }

    fun toggleDirectoryTree() {
        _uiState.update { it.copy(showDirectoryTree = !it.showDirectoryTree) }
    }

    fun hideDirectoryTree() {
        _uiState.update { it.copy(showDirectoryTree = false) }
    }

    /** Navigate to a specific path segment (e.g., clicking "folder" in "root/folder/sub") */
    fun navigateToPathSegment(segmentIndex: Int) {
        val segments = _uiState.value.currentPath.trim('/').split('/').filter { it.isNotEmpty() }
        val targetPath = segments.take(segmentIndex + 1).joinToString("/")
        loadDirectory(targetPath)
    }

    fun toggleSelection(path: String) {
        _uiState.update { state ->
            val next = state.selectedPaths.toMutableSet()
            if (!next.add(path)) {
                next.remove(path)
            }
            state.copy(selectedPaths = next, lastAddResult = null)
        }
    }

    /**
     * 显示批量添加弹窗。
     */
    fun showBatchAddDialog() {
        val selected = _uiState.value.selectedPaths
        if (selected.isEmpty()) return
        viewModelScope.launch {
            val tags = tagRepository.getAllTagsOnce()
            _uiState.update { it.copy(showBatchAddDialog = true, allTags = tags) }
        }
    }

    /**
     * 隐藏批量添加弹窗。
     */
    fun hideBatchAddDialog() {
        _uiState.update { it.copy(showBatchAddDialog = false, allTags = emptyList()) }
    }

    /**
     * 确认批量添加。
     */
    fun confirmBatchAdd(organizationMode: dev.wucheng.resource_viewer.data.local.converter.OrganizationMode?, tagIds: List<String>) {
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

    /**
     * 切换视图模式（列表/网格）。
     */
    fun toggleViewMode() {
        _uiState.update { state ->
            state.copy(
                viewMode = if (state.viewMode == FileViewMode.LIST) FileViewMode.GRID else FileViewMode.LIST,
            )
        }
    }

    /**
     * 判断文件是否为可预览类型（图片/视频/PDF）。
     */
    fun isPreviewable(entry: FileEntry): Boolean {
        if (entry.isDirectory) return false
        val ext = entry.extension.lowercase()
        return ext in IMAGE_EXTENSIONS || ext in VIDEO_EXTENSIONS || ext == "pdf"
    }

    /**
     * 打开文件预览。
     */
    fun openFilePreview(entry: FileEntry) {
        _uiState.update { it.copy(previewFile = entry) }
    }

    /**
     * 关闭文件预览。
     */
    fun closeFilePreview() {
        _uiState.update { it.copy(previewFile = null) }
    }

    /**
     * 加载预览图片的 Bitmap。
     */
    suspend fun loadPreviewBitmap(entry: FileEntry): android.graphics.Bitmap {
        val fs = fileSource ?: throw IllegalStateException("FileSource not initialized")
        val bytes = fs.readFile(entry.relativePath)
        return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IllegalStateException("Failed to decode image")
    }

    companion object {
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
        private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "mov", "webm")
    }

    private fun loadDirectory(path: String) {
        val source = _uiState.value.source ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    currentPath = path.trim('/'),
                    selectedPaths = emptySet(),
                    lastAddResult = null,
                )
            }
            when (val result = filesystemRepository.listDirectory(source, path.trim('/'))) {
                is Result.Ok -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            entries = result.value.sortedWith(
                                compareBy<FileEntry> { !it.isDirectory }.thenBy { entry -> entry.name.lowercase() }
                            ),
                        )
                    }
                }
                is Result.Err -> {
                    _uiState.update { it.copy(isLoading = false, entries = emptyList(), error = result.error.message) }
                }
            }
        }
    }
}
