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
import dev.wucheng.resource_viewer.data.local.entity.TagEntity
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import dev.wucheng.resource_viewer.shared.thumbnail.FileEntryThumbnailLoader
import dev.wucheng.resource_viewer.shared.thumbnail.ThumbnailSearchPolicy
import dev.wucheng.resource_viewer.shared.thumbnail.ThumbnailTaskPool
import dev.wucheng.resource_viewer.shared.thumbnail.FileBrowserThumbnailDiskCache
import dev.wucheng.resource_viewer.data.local.dao.AppConfigDao
import dev.wucheng.resource_viewer.data.local.converter.SourceType
import dev.wucheng.resource_viewer.data.local.datastore.FileBrowserPrefsStore
import dev.wucheng.resource_viewer.data.local.datastore.FileSortMode
import dev.wucheng.resource_viewer.data.local.datastore.FileViewMode
import android.graphics.Bitmap
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.util.UUID

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
    val viewMode: FileViewMode = FileViewMode.GRID,
    val sortMode: FileSortMode = FileSortMode.NAME_ASC,
    val showBatchAddDialog: Boolean = false,
    val allTags: List<Tag> = emptyList(),
    val error: String? = null,
    val lastAddResult: ScanResult? = null,
    /** 当前路径段列表，用于面包屑渲染 */
    val pathSegments: List<String> = emptyList(),
    /** 已入库的资源路径集合（用于标记"已入库"） */
    val importedPaths: Set<String> = emptySet(),
    /** 是否显示目录树导航 */
    val showDirectoryTree: Boolean = true,
)

class FileBrowserViewModel(
    private val sourceId: String,
    private val filesystemRepository: FilesystemRepository,
    private val batchAddResourcesUseCase: BatchAddResourcesUseCase,
    private val tagRepository: dev.wucheng.resource_viewer.data.repository.TagRepository,
    private val appConfigDao: AppConfigDao? = null,
    private val thumbnailDiskCache: FileBrowserThumbnailDiskCache? = null,
    private val prefsStore: FileBrowserPrefsStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FileBrowserUiState())
    val uiState: StateFlow<FileBrowserUiState> = _uiState.asStateFlow()

    private var fileSource: FileSource? = null
    private var thumbnailLoader: FileEntryThumbnailLoader? = null
    private var thumbnailPool = ThumbnailTaskPool(DEFAULT_THUMBNAIL_CONCURRENCY)
    private val inFlightThumbnails = mutableMapOf<String, Deferred<Bitmap?>>()
    private val thumbnailMisses = mutableSetOf<String>()
    private val thumbnailCache = object : LinkedHashMap<String, Bitmap>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean = size > 64
    }

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
                            val configured = config?.thumbnailConcurrency ?: DEFAULT_THUMBNAIL_CONCURRENCY
                            thumbnailDiskCache?.configureCapacity(config?.thumbnailCacheLimitMB ?: 500)
                            val effectiveConcurrency = if (source.type == SourceType.SMB) {
                                ((configured + 1) / 2).coerceAtLeast(1)
                            } else configured
                            thumbnailPool = ThumbnailTaskPool(effectiveConcurrency)
                            fileSource = fsResult.value
                            thumbnailLoader = FileEntryThumbnailLoader(fsResult.value)
                            val showDirectoryTree = config?.showDirectoryTree ?: true
                            _uiState.update { it.copy(source = source, showDirectoryTree = showDirectoryTree) }
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

    // ===== 视图模式和排序 =====

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
        synchronized(thumbnailCache) { thumbnailCache[entry.relativePath] }?.let { return it }
        if (synchronized(thumbnailMisses) { entry.relativePath in thumbnailMisses }) return null
        val loader = thumbnailLoader ?: return null
        val candidate = viewModelScope.async {
            thumbnailPool.run {
                val policy = ThumbnailSearchPolicy.DIRECT_CHILD
                val cached = thumbnailDiskCache?.get(sourceId, entry, policy)
                if (cached?.isCached == true) return@run cached.bitmap
                loader.load(entry, policy = policy).also { bitmap ->
                    thumbnailDiskCache?.put(sourceId, entry, policy, bitmap)
                }
            }
        }
        val task = synchronized(inFlightThumbnails) {
            inFlightThumbnails[entry.relativePath] ?: candidate.also {
                inFlightThumbnails[entry.relativePath] = it
            }
        }
        if (task !== candidate) candidate.cancel()
        return try {
            val bitmap = task.await()
            if (bitmap == null) synchronized(thumbnailMisses) { thumbnailMisses += entry.relativePath }
            else synchronized(thumbnailCache) { thumbnailCache[entry.relativePath] = bitmap }
            bitmap
        } finally {
            if (task.isCompleted) synchronized(inFlightThumbnails) {
                if (inFlightThumbnails[entry.relativePath] === task) inFlightThumbnails.remove(entry.relativePath)
            }
        }
    }

    companion object {
        private const val DEFAULT_THUMBNAIL_CONCURRENCY = 4
    }

    private fun loadDirectory(path: String) {
        val source = _uiState.value.source ?: return
        viewModelScope.launch {
            synchronized(inFlightThumbnails) {
                inFlightThumbnails.values.forEach { it.cancel() }
                inFlightThumbnails.clear()
            }
            synchronized(thumbnailCache) { thumbnailCache.clear() }
            synchronized(thumbnailMisses) { thumbnailMisses.clear() }
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
                    FileSortMode.NAME_ASC -> comparator.thenBy { it.name.lowercase() }
                    FileSortMode.NAME_DESC -> comparator.thenByDescending { it.name.lowercase() }
                    FileSortMode.MODIFIED_ASC -> comparator.thenBy { it.modifiedAt }
                    FileSortMode.MODIFIED_DESC -> comparator.thenByDescending { it.modifiedAt }
                }
            }
        )
    }
}
