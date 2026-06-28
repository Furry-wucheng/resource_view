package dev.wucheng.resource_viewer.ui.screens.viewer

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.data.repository.FilesystemRepository
import dev.wucheng.resource_viewer.data.repository.ResourceRepository
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.model.Chapter
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import dev.wucheng.resource_viewer.shared.media.MediaFormats
import dev.wucheng.resource_viewer.shared.organization.ChapterGalleryStrategy
import dev.wucheng.resource_viewer.shared.organization.ChapterStrategy
import dev.wucheng.resource_viewer.shared.organization.OrganizationStrategy
import dev.wucheng.resource_viewer.shared.thumbnail.FileBrowserThumbnailDiskCache
import dev.wucheng.resource_viewer.shared.thumbnail.FileEntryThumbnailLoader
import dev.wucheng.resource_viewer.shared.thumbnail.ThumbnailSearchPolicy
import dev.wucheng.resource_viewer.shared.thumbnail.ThumbnailTaskPool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ChapterViewMode { LIST, GRID }

/**
 * 章节列表 UI 状态。
 */
sealed class ChapterListUiState {
    /** 加载中 */
    data object Loading : ChapterListUiState()

    /** 加载成功 */
    data class Success(
        val chapters: List<Chapter>,
        val looseFiles: List<FileEntry>,
        val resourceName: String,
        val organizationMode: OrganizationMode,
        val viewMode: ChapterViewMode = ChapterViewMode.LIST,
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
    private val thumbnailDiskCache: FileBrowserThumbnailDiskCache? = null,
) : ViewModel() {

    /** UI 状态 */
    private val _uiState = MutableStateFlow<ChapterListUiState>(ChapterListUiState.Loading)
    val uiState: StateFlow<ChapterListUiState> = _uiState.asStateFlow()

    /** 资源信息 */
    private var resource: Resource? = null
    private var fileSource: FileSource? = null

    /** 缩略图加载 */
    private var thumbnailLoader: FileEntryThumbnailLoader? = null
    private var thumbnailPool = ThumbnailTaskPool(4)
    private val thumbnailCache = object : LinkedHashMap<String, Bitmap>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean = size > 32
    }
    private val thumbnailMisses = mutableSetOf<String>()

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
                            thumbnailLoader = FileEntryThumbnailLoader(fs)
                            thumbnailCache.clear()
                            thumbnailMisses.clear()

                            try {
                                val strategy = getStrategy(res)
                                val chapters = strategy.getChapters(res, fs)
                                val looseFiles = getLooseFiles(res, fs)

                                _uiState.value = ChapterListUiState.Success(
                                    chapters = chapters,
                                    looseFiles = looseFiles,
                                    resourceName = res.name,
                                    organizationMode = res.organizationMode ?: OrganizationMode.CHAPTER,
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
            OrganizationMode.CHAPTER -> ChapterStrategy()
            OrganizationMode.CHAPTER_GALLERY -> ChapterGalleryStrategy()
            else -> throw IllegalStateException("Unsupported organization mode: ${resource.organizationMode}")
        }
    }

    /**
     * 获取散落文件（不属于任何子目录的独立文件）。
     */
    private suspend fun getLooseFiles(resource: Resource, fileSource: FileSource): List<FileEntry> {
        val imageExtensions = MediaFormats.imageExtensions
        val videoExtensions = MediaFormats.videoExtensions
        val pdfExtensions = setOf("pdf")
        val supportedExtensions = imageExtensions + videoExtensions + pdfExtensions

        val entries = fileSource.listDirectory(resource.relativePath)
        return entries.filter { entry ->
            !entry.isDirectory && entry.extension.lowercase() in supportedExtensions
        }
    }

    /**
     * 更改组织模式。
     * 更新资源的组织模式并重新加载章节列表。
     */
    fun changeOrganizationMode(mode: OrganizationMode) {
        val res = resource ?: return
        viewModelScope.launch {
            resourceRepository.updateOrganizationMode(res.id, mode)
            loadChapters()
        }
    }

    /**
     * 切换视图模式（列表/网格）。
     */
    fun toggleViewMode() {
        val current = _uiState.value
        if (current is ChapterListUiState.Success) {
            val newMode = if (current.viewMode == ChapterViewMode.LIST) ChapterViewMode.GRID else ChapterViewMode.LIST
            _uiState.value = current.copy(viewMode = newMode)
        }
    }

    /**
     * 加载章节封面缩略图。
     * 通过 FileEntryThumbnailLoader 从 FileSource 加载，复用磁盘缓存。
     */
    suspend fun loadChapterCover(path: String): Bitmap? {
        synchronized(thumbnailCache) { thumbnailCache[path] }?.let { return it }
        if (synchronized(thumbnailMisses) { path in thumbnailMisses }) return null
        val loader = thumbnailLoader ?: return null
        return try {
            thumbnailPool.run {
                val entry = FileEntry(
                    path.substringAfterLast("/"),
                    path,
                    false,
                    0,
                    0L,
                    path.substringAfterLast(".", ""),
                )
                val cached = thumbnailDiskCache?.get(resourceId, entry, ThumbnailSearchPolicy.DIRECT_CHILD)
                val bitmap = if (cached?.isCached == true) {
                    cached.bitmap
                } else {
                    loader.load(entry, policy = ThumbnailSearchPolicy.DIRECT_CHILD)?.also {
                        thumbnailDiskCache?.put(resourceId, entry, ThumbnailSearchPolicy.DIRECT_CHILD, it)
                    }
                }
                if (bitmap == null) {
                    synchronized(thumbnailMisses) { thumbnailMisses += path }
                } else {
                    synchronized(thumbnailCache) { thumbnailCache[path] = bitmap }
                }
                bitmap
            }
        } catch (_: Exception) {
            null
        }
    }
}
