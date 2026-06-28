package dev.wucheng.resource_viewer.ui.screens.viewer

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.data.repository.FilesystemRepository
import dev.wucheng.resource_viewer.data.repository.ResourceRepository
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import dev.wucheng.resource_viewer.shared.media.MediaFormats
import dev.wucheng.resource_viewer.shared.organization.GalleryStrategy
import dev.wucheng.resource_viewer.shared.thumbnail.FileBrowserThumbnailDiskCache
import dev.wucheng.resource_viewer.shared.thumbnail.FileEntryThumbnailLoader
import dev.wucheng.resource_viewer.shared.thumbnail.ThumbnailSearchPolicy
import dev.wucheng.resource_viewer.shared.thumbnail.ThumbnailTaskPool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ContentGridMode { FLAT_GRID, GALLERY }

data class ContentGridUiState(
    val title: String = "",
    val sourceId: String = "",
    val rootPath: String = "",
    val currentPath: String = "",
    val entries: List<FileEntry> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val organizationMode: OrganizationMode = OrganizationMode.FLATGRID,
)

class ContentGridViewModel(
    private val resourceId: String,
    private val mode: ContentGridMode,
    private val resourceRepository: ResourceRepository,
    private val filesystemRepository: FilesystemRepository,
    private val thumbnailDiskCache: FileBrowserThumbnailDiskCache? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ContentGridUiState())
    val uiState = _uiState.asStateFlow()
    private var fileSource: FileSource? = null

    /** 缩略图加载 */
    private var thumbnailLoader: FileEntryThumbnailLoader? = null
    private var thumbnailPool = ThumbnailTaskPool(4)
    private val thumbnailCache = object : LinkedHashMap<String, Bitmap>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean = size > 32
    }
    private val thumbnailMisses = mutableSetOf<String>()

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val resourceResult = resourceRepository.getById(resourceId)) {
                is Result.Err -> fail(resourceResult.error.message)
                is Result.Ok -> {
                    val resource = resourceResult.value ?: return@launch fail("资源不存在")
                    when (val sourceResult = filesystemRepository.getFileSource(resource.sourceId)) {
                        is Result.Err -> fail(sourceResult.error.message)
                        is Result.Ok -> {
                            fileSource = sourceResult.value
                            thumbnailLoader = FileEntryThumbnailLoader(sourceResult.value)
                            thumbnailCache.clear()
                            thumbnailMisses.clear()
                            val entries = try {
                                if (mode == ContentGridMode.GALLERY) {
                                    GalleryStrategy().getContents(resource, sourceResult.value)
                                        .sortedBy { it.relativePath }
                                } else {
                                    listFlat(sourceResult.value, resource.relativePath)
                                }
                            } catch (exception: Exception) {
                                return@launch fail(exception.message ?: "内容加载失败")
                            }
                            _uiState.value = ContentGridUiState(
                                title = resource.name,
                                sourceId = resource.sourceId,
                                rootPath = resource.relativePath,
                                currentPath = resource.relativePath,
                                entries = entries,
                                isLoading = false,
                                organizationMode = resource.organizationMode ?: OrganizationMode.FLATGRID,
                            )
                        }
                    }
                }
            }
        }
    }

    fun openDirectory(path: String) {
        val source = fileSource ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching { listFlat(source, path) }
                .onSuccess { entries ->
                    _uiState.value = _uiState.value.copy(
                        currentPath = path,
                        entries = entries,
                        isLoading = false,
                    )
                }
                .onFailure { fail(it.message ?: "目录加载失败") }
        }
    }

    fun goUp(): Boolean {
        val state = _uiState.value
        if (mode == ContentGridMode.GALLERY || state.currentPath == state.rootPath) return false
        val parent = state.currentPath.substringBeforeLast("/", state.rootPath)
        openDirectory(if (parent.length < state.rootPath.length) state.rootPath else parent)
        return true
    }

    private suspend fun listFlat(source: FileSource, path: String): List<FileEntry> =
        source.listDirectory(path)
            .filter { it.isDirectory || it.extension.lowercase() in SUPPORTED_EXTENSIONS }
            .sortedWith(compareBy<FileEntry> { !it.isDirectory }.thenBy { it.relativePath })

    private fun fail(message: String) {
        _uiState.value = _uiState.value.copy(isLoading = false, error = message)
    }

    fun changeOrganizationMode(mode: OrganizationMode) {
        val state = _uiState.value
        _uiState.value = state.copy(organizationMode = mode)
        viewModelScope.launch {
            resourceRepository.updateOrganizationMode(resourceId, mode)
        }
    }

    suspend fun loadEntryThumbnail(entry: FileEntry): Bitmap? {
        if (entry.isDirectory) return null
        synchronized(thumbnailCache) { thumbnailCache[entry.relativePath] }?.let { return it }
        if (synchronized(thumbnailMisses) { entry.relativePath in thumbnailMisses }) return null
        val loader = thumbnailLoader ?: return null
        return try {
            thumbnailPool.run {
                val cached = thumbnailDiskCache?.get(resourceId, entry, ThumbnailSearchPolicy.DIRECT_CHILD)
                val bitmap = if (cached?.isCached == true) {
                    cached.bitmap
                } else {
                    loader.load(entry, policy = ThumbnailSearchPolicy.DIRECT_CHILD)?.also {
                        thumbnailDiskCache?.put(resourceId, entry, ThumbnailSearchPolicy.DIRECT_CHILD, it)
                    }
                }
                if (bitmap == null) {
                    synchronized(thumbnailMisses) { thumbnailMisses += entry.relativePath }
                } else {
                    synchronized(thumbnailCache) { thumbnailCache[entry.relativePath] = bitmap }
                }
                bitmap
            }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private val SUPPORTED_EXTENSIONS = MediaFormats.imageExtensions + MediaFormats.videoExtensions
    }
}
