package dev.wucheng.resource_viewer.ui.screens.sources

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.wucheng.resource_viewer.data.local.dao.AppConfigDao
import dev.wucheng.resource_viewer.data.local.datastore.FileBrowserPrefsStore
import dev.wucheng.resource_viewer.data.local.datastore.FileSortMode
import dev.wucheng.resource_viewer.data.local.datastore.FileViewMode
import dev.wucheng.resource_viewer.data.local.entity.TagEntity
import dev.wucheng.resource_viewer.data.repository.FilesystemRepository
import dev.wucheng.resource_viewer.data.repository.ResourceRepository
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.error.ScanResult
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.Source
import dev.wucheng.resource_viewer.domain.model.Tag
import dev.wucheng.resource_viewer.domain.usecase.BatchAddResourcesUseCase
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import dev.wucheng.resource_viewer.shared.thumbnail.ThumbnailLoadManager
import dev.wucheng.resource_viewer.shared.util.NaturalOrderComparator
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class FileBrowserUiState(
    val source: Source? = null,
    val currentPath: String = "",
    val entries: List<FileEntry> = emptyList(),
    val selectedPaths: Set<String> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val isLoading: Boolean = false,
    val isAdding: Boolean = false,
    val viewMode: FileViewMode = FileViewMode.GRID,
    val sortMode: FileSortMode = FileSortMode.NAME_ASC,
    val showBatchAddDialog: Boolean = false,
    val allTags: List<Tag> = emptyList(),
    val error: String? = null,
    val lastAddResult: ScanResult? = null,
    val pathSegments: List<String> = emptyList(),
    val importedPaths: Set<String> = emptySet(),
    val showDirectoryTree: Boolean = true,
)

class FileBrowserViewModel(
    private val sourceId: String,
    private val filesystemRepository: FilesystemRepository,
    private val batchAddResourcesUseCase: BatchAddResourcesUseCase,
    private val tagRepository: dev.wucheng.resource_viewer.data.repository.TagRepository,
    private val appConfigDao: AppConfigDao? = null,
    private val thumbnailLoadManager: ThumbnailLoadManager,
    private val prefsStore: FileBrowserPrefsStore,
    private val resourceRepository: ResourceRepository? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FileBrowserUiState())
    val uiState: StateFlow<FileBrowserUiState> = _uiState.asStateFlow()

    private val _showFlexibleAddDialog = MutableStateFlow(false)
    val showFlexibleAddDialog: StateFlow<Boolean> = _showFlexibleAddDialog.asStateFlow()

    private val _flexibleAddRootPath = MutableStateFlow("")
    val flexibleAddRootPath: StateFlow<String> = _flexibleAddRootPath.asStateFlow()

    private val _flexibleAddResult = MutableStateFlow<String?>(null)
    val flexibleAddResult: StateFlow<String?> = _flexibleAddResult.asStateFlow()

    private val _showFlexibleTagsDialog = MutableStateFlow(false)
    val showFlexibleTagsDialog: StateFlow<Boolean> = _showFlexibleTagsDialog.asStateFlow()

    private val _flexibleAllTags = MutableStateFlow<List<Tag>>(emptyList())
    val flexibleAllTags: StateFlow<List<Tag>> = _flexibleAllTags.asStateFlow()

    private val _flexiblePendingCount = MutableStateFlow(0)
    val flexiblePendingCount: StateFlow<Int> = _flexiblePendingCount.asStateFlow()

    private var flexiblePendingEntries: List<FileEntry> = emptyList()

    private var fileSource: FileSource? = null
    private val inFlightThumbnails = mutableMapOf<String, Deferred<Bitmap?>>()

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
                            val config = appConfigDao?.getConfig()?.first()
                            thumbnailLoadManager.configureCapacity(config?.thumbnailCacheLimitMB ?: 500)
                            fileSource = fsResult.value
                            thumbnailLoadManager.setFileSource(fsResult.value)
                            val showDirectoryTree = config?.showDirectoryTree ?: true

                            val importedPaths = resourceRepository?.let {
                                try {
                                    val allResources = it.getVisibleResources().first()
                                    allResources.filter { r -> r.sourceId == sourceId }.map { r -> r.relativePath }.toSet()
                                } catch (e: Exception) {
                                    emptySet()
                                }
                            } ?: emptySet()

                            _uiState.update { it.copy(source = source, showDirectoryTree = showDirectoryTree, importedPaths = importedPaths) }
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

    fun goUp(): Boolean {
        val current = _uiState.value.currentPath.trim('/')
        if (current.isEmpty()) return false
        val parent = current.substringBeforeLast("/", missingDelimiterValue = "")
        loadDirectory(parent)
        return true
    }

    fun navigateToSegment(index: Int) {
        val segments = _uiState.value.currentPath.trim('/').split('/').filter { it.isNotEmpty() }
        val target = segments.take(index + 1).joinToString("/")
        loadDirectory(target)
    }

    fun enterMultiSelect() {
        _uiState.update { it.copy(isMultiSelectMode = true, selectedPaths = emptySet()) }
    }

    fun exitMultiSelect() {
        _uiState.update { it.copy(isMultiSelectMode = false, selectedPaths = emptySet()) }
    }

    fun toggleSelection(path: String) {
        _uiState.update { state ->
            val next = state.selectedPaths.toMutableSet()
            if (!next.add(path)) next.remove(path)
            state.copy(selectedPaths = next, lastAddResult = null)
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            state.copy(selectedPaths = state.entries.map { it.relativePath }.toSet())
        }
    }

    fun deselectAll() {
        _uiState.update { it.copy(selectedPaths = emptySet()) }
    }

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

    fun createTag(name: String, onCreated: (String) -> Unit = {}) {
        val cleanName = name.trim()
        if (cleanName.isEmpty() || cleanName.length > 20) return
        viewModelScope.launch {
            val entity = TagEntity(UUID.randomUUID().toString(), cleanName, "#6750A4")
            when (val result = tagRepository.insert(entity)) {
                is Result.Ok -> {
                    _uiState.update { it.copy(allTags = tagRepository.getAllTagsOnce()) }
                    onCreated(entity.id)
                }
                is Result.Err -> _uiState.update { it.copy(error = result.error.message) }
            }
        }
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
            when (val result = batchAddResourcesUseCase(fs, source, selected, organizationMode, tagIds, viewModelScope)) {
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

    fun getResourceEntryForPath(relativePath: String): dev.wucheng.resource_viewer.domain.model.Resource? {
        val repo = resourceRepository ?: return null
        return kotlinx.coroutines.runBlocking {
            val allResources = repo.getVisibleResources().first()
            allResources.find { it.sourceId == sourceId && it.relativePath == relativePath }
        }
    }

    // === 灵活添加资源 ===

    fun initiateFlexibleAdd() {
        _flexibleAddRootPath.value = _uiState.value.currentPath
        _flexibleAddResult.value = null
        _showFlexibleAddDialog.value = true
    }

    fun dismissFlexibleAdd() {
        _showFlexibleAddDialog.value = false
    }

    fun onFlexibleEntriesSelected(entries: List<FileEntry>) {
        if (entries.isEmpty()) return
        flexiblePendingEntries = entries
        _flexiblePendingCount.value = entries.size
        _showFlexibleAddDialog.value = false
        viewModelScope.launch {
            _flexibleAllTags.value = tagRepository.getAllTagsOnce()
        }
        _showFlexibleTagsDialog.value = true
    }

    fun onFlexibleTagsCancelled() {
        _showFlexibleTagsDialog.value = false
        _showFlexibleAddDialog.value = true
    }

    fun executeFlexibleAdd(
        selectedOrgMode: dev.wucheng.resource_viewer.data.local.converter.OrganizationMode?,
        tagIds: List<String>,
    ) {
        val source = _uiState.value.source ?: return
        val fs = fileSource ?: return
        val entries = flexiblePendingEntries
        if (entries.isEmpty()) return

        val paths = entries.map { it.relativePath }
        _showFlexibleTagsDialog.value = false

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isAdding = true, error = null) }
            when (val result = batchAddResourcesUseCase(fs, source, paths, selectedOrgMode, tagIds, viewModelScope)) {
                is Result.Ok -> {
                    _flexibleAddResult.value = "成功添加 ${result.value.successCount} 个资源" +
                        if (result.value.skipCount > 0) "，跳过 ${result.value.skipCount} 个" else ""
                    dismissFlexibleAdd()
                }
                is Result.Err -> {
                    _flexibleAddResult.value = "添加失败：${result.error.message}"
                }
            }
            _uiState.update { it.copy(isAdding = false) }
        }
    }

    fun dismissFlexibleAddResult() {
        _flexibleAddResult.value = null
    }

    fun setViewMode(mode: FileViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
        saveCurrentPrefs()
    }

    fun setSortMode(mode: FileSortMode) {
        _uiState.update { it.copy(sortMode = mode) }
        saveCurrentPrefs()
        reapplySorting()
    }

    private fun saveCurrentPrefs() {
        val state = _uiState.value
        val source = state.source ?: return
        viewModelScope.launch {
            prefsStore.savePrefs(
                sourceId = source.id,
                path = state.currentPath,
                prefs = dev.wucheng.resource_viewer.data.local.datastore.FolderPrefs(
                    viewMode = state.viewMode,
                    sortMode = state.sortMode
                )
            )
        }
    }

    private fun reapplySorting() {
        val state = _uiState.value
        val sorted = sortEntries(state.entries, state.sortMode)
        _uiState.update { it.copy(entries = sorted) }
    }

    suspend fun loadThumbnail(entry: FileEntry): Bitmap? {
        val deferred = synchronized(inFlightThumbnails) {
            inFlightThumbnails[entry.relativePath]
                ?: viewModelScope.async { thumbnailLoadManager.load(sourceId, entry) }
                    .also { inFlightThumbnails[entry.relativePath] = it }
        }
        return deferred.await().also {
            synchronized(inFlightThumbnails) {
                inFlightThumbnails.remove(entry.relativePath)
            }
        }
    }

    private fun loadDirectory(path: String) {
        val source = _uiState.value.source ?: return
        viewModelScope.launch {
            synchronized(inFlightThumbnails) {
                inFlightThumbnails.values.forEach { it.cancel() }
                inFlightThumbnails.clear()
            }
            val cleanPath = path.trim('/')

            val prefs = prefsStore.loadPrefs(sourceId, cleanPath)

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
                    viewMode = prefs.viewMode,
                    sortMode = prefs.sortMode,
                )
            }
            when (val result = filesystemRepository.listDirectory(source, cleanPath)) {
                is Result.Ok -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            entries = sortEntries(result.value, prefs.sortMode),
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

    private fun sortEntries(entries: List<FileEntry>, sortMode: FileSortMode): List<FileEntry> {
        return entries.sortedWith(
            compareBy<FileEntry> { !it.isDirectory }.let { comparator ->
                when (sortMode) {
                    FileSortMode.NAME_ASC -> comparator.thenComparing(
                        { fe: FileEntry -> fe.name }, NaturalOrderComparator
                    )
                    FileSortMode.NAME_DESC -> comparator.thenComparing(
                        { fe: FileEntry -> fe.name }, NaturalOrderComparator.reversed()
                    )
                    FileSortMode.MODIFIED_ASC -> comparator.thenBy { it.modifiedAt }
                    FileSortMode.MODIFIED_DESC -> comparator.thenByDescending { it.modifiedAt }
                }
            }
        )
    }
}
