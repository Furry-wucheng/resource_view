package dev.wucheng.resource_viewer.ui.screens.viewer

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.data.local.converter.SourceType
import dev.wucheng.resource_viewer.data.local.converter.DoublePageMode
import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.data.local.converter.PageDirection
import dev.wucheng.resource_viewer.data.local.dao.AppConfigDao
import dev.wucheng.resource_viewer.data.local.entity.AppConfigEntity
import dev.wucheng.resource_viewer.data.remote.smb.SmbDataSourceFactory
import dev.wucheng.resource_viewer.data.repository.FilesystemRepository
import dev.wucheng.resource_viewer.data.repository.ResourceRepository
import dev.wucheng.resource_viewer.domain.error.DomainError
import dev.wucheng.resource_viewer.domain.error.MediaType
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.model.Chapter
import dev.wucheng.resource_viewer.domain.model.VideoMediaSource
import dev.wucheng.resource_viewer.domain.model.ViewerItem
import dev.wucheng.resource_viewer.shared.content.ContentProvider
import dev.wucheng.resource_viewer.shared.content.ImageFolderProvider
import dev.wucheng.resource_viewer.shared.content.MixedFolderProvider
import dev.wucheng.resource_viewer.shared.content.PdfContentProvider
import dev.wucheng.resource_viewer.shared.media.MediaFormats
import dev.wucheng.resource_viewer.shared.filesource.DocumentTreeFileSource
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import dev.wucheng.resource_viewer.shared.organization.ChapterGalleryStrategy
import dev.wucheng.resource_viewer.shared.organization.ChapterStrategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

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
 * 支持图片文件夹（M14）、视频资源（M19）、PDF（M22）。
 *
 * 注意：此实现遵循 doc/mvp/M14-basic-viewer.md + doc/mvp/M19-video-player.md + doc/mvp/M22-pdf-viewer.md。
 */
@androidx.media3.common.util.UnstableApi
class ViewerViewModel(
    private val resourceId: String,
    private val contentPath: String = "",
    private val initialPage: Int = 0,
    private val resourceRepository: ResourceRepository,
    private val filesystemRepository: FilesystemRepository,
    private val context: Context,
    private val appConfigDao: AppConfigDao? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    /** UI 状态 */
    private val _uiState = MutableStateFlow<ViewerUiState>(ViewerUiState.Loading)
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    /** 当前页码（0-based） */
    private val _currentPage = MutableStateFlow(initialPage.coerceAtLeast(0))
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    /** 总页数 */
    private val _totalPages = MutableStateFlow(0)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    /** 资源名称 */
    private val _resourceName = MutableStateFlow("")
    val resourceName: StateFlow<String> = _resourceName.asStateFlow()

    private val _pageDirection = MutableStateFlow(PageDirection.RIGHT_TO_LEFT)
    val pageDirection: StateFlow<PageDirection> = _pageDirection.asStateFlow()

    private val _doublePageMode = MutableStateFlow(DoublePageMode.AUTO)
    val doublePageMode: StateFlow<DoublePageMode> = _doublePageMode.asStateFlow()

    /** 章节列表（用于跨章节导航） */
    private val _chapters = MutableStateFlow<List<Chapter>>(emptyList())
    val chapters: StateFlow<List<Chapter>> = _chapters.asStateFlow()

    /** 当前章节索引 */
    private val _currentChapterIndex = MutableStateFlow(-1)
    val currentChapterIndex: StateFlow<Int> = _currentChapterIndex.asStateFlow()

    /** 跨章节连续阅读开关 */
    private val _crossChapter = MutableStateFlow(true)
    val crossChapter: StateFlow<Boolean> = _crossChapter.asStateFlow()

    /** 章节过渡提示文本 */
    private val _chapterHint = MutableStateFlow<String?>(null)
    val chapterHint: StateFlow<String?> = _chapterHint.asStateFlow()

    /** 收藏状态 */
    private val _isFavorited = MutableStateFlow(false)
    val isFavorited: StateFlow<Boolean> = _isFavorited.asStateFlow()

    /** ContentProvider 实例（图片/PDF 模式） */
    private var contentProvider: ContentProvider? = null

    /** 解码后的页面缓存；200MB 与 Flutter 参考实现保持一致。 */
    private var pageLoader: PageLoader<PageRequest, Bitmap>? = null

    /** 旧页面的预取必须可取消，避免与用户当前页争抢 SMB 带宽。 */
    private var prefetchJob: Job? = null

    /** 页面缓存容量（从配置读取） */
    private var pageCacheLimitBytes: Long = 500L * 1024 * 1024

    /** 缓存的资源引用（用于跨章节导航） */
    private var currentResource: dev.wucheng.resource_viewer.domain.model.Resource? = null

    /** 缓存的 FileSource 引用 */
    private var cachedFileSource: FileSource? = null

    /** 是否正在导航章节（防止重复触发） */
    private val _isNavigatingChapter = MutableStateFlow(false)    /**
     * 从文件浏览器直接加载文件（不需要 resourceId）。
     * 根据文件扩展名判断类型，创建对应的 ContentProvider。
     */
    fun loadFromSource(sourceId: String, filePath: String) {
        prefetchJob?.cancel()
        contentProvider?.dispose()
        contentProvider = null
        pageLoader = null
        viewModelScope.launch {
            _uiState.value = ViewerUiState.Loading
            loadViewerConfig()

            when (val sourceResult = filesystemRepository.getSource(sourceId)) {
                is Result.Ok -> {
                    val source = sourceResult.value
                    if (source == null) {
                        _uiState.value = ViewerUiState.Error("数据源不存在")
                        return@launch
                    }
                    when (val fsResult = filesystemRepository.getFileSource(sourceId)) {
                        is Result.Ok -> {
                            val fileSource = fsResult.value
                            val ext = filePath.substringAfterLast('.', "").lowercase()
                            val fileName = filePath.substringAfterLast('/')

                            when {
                                ext == "pdf" -> {
                                    // PDF 文件：独立加载
                                    _resourceName.value = fileName
                                    try {
                                        val provider = withContext(ioDispatcher) {
                                            PdfContentProvider(context, fileSource, filePath)
                                        }
                                        setupContentProvider(provider, "file:$sourceId:$filePath", fileName)
                                    } catch (e: IOException) {
                                        _uiState.value = ViewerUiState.Error("加密 PDF 暂不支持")
                                    } catch (e: Exception) {
                                        _uiState.value = ViewerUiState.Error("加载 PDF 失败")
                                    }
                                }
                                MediaFormats.isPreviewable(ext) -> {
                                    // 图片或视频：用 MixedFolderProvider 加载所在目录，支持图片/视频无缝浏览
                                    val dirPath = filePath.substringBeforeLast('/', "")
                                    try {
                                        // 为 SMB 源创建视频 DataSource.Factory
                                        val videoFactory = if (source.type == SourceType.SMB) {
                                            val password = filesystemRepository.getPassword(source.id) ?: ""
                                            SmbDataSourceFactory(source, password)
                                        } else null

                                        val (provider, viewerItems) = withContext(ioDispatcher) {
                                        val mixedProvider = MixedFolderProvider(
                                                fileSource = fileSource,
                                                relativePath = dirPath,
                                                sourceId = sourceId,
                                                videoDataSourceFactory = videoFactory,
                                                pageCacheDirectory = context.cacheDir,
                                                pageCacheLimitBytes = pageCacheLimitBytes,
                                            )
                                            mixedProvider to mixedProvider.buildViewerItems()
                                        }
                                        if (viewerItems.isEmpty()) {
                                            _uiState.value = ViewerUiState.Error("目录中没有可浏览的文件")
                                            return@launch
                                        }
                                        val currentIndex = provider.findIndex(filePath)
                                        _resourceName.value = fileName
                                        contentProvider = provider
                                        pageLoader = PageLoader(
                                            maxCacheSize = PAGE_CACHE_BYTES,
                                            sizeOf = { bitmap -> bitmap.allocationByteCount.toLong() },
                                            load = { request ->
                                                provider.loadPage(request.pageIndex, request.targetWidth, request.targetHeight)
                                            },
                                        )
                                        _totalPages.value = viewerItems.size
                                        _currentPage.value = currentIndex.coerceIn(0, viewerItems.size - 1)
                                        _uiState.value = ViewerUiState.Success(
                                            items = viewerItems,
                                            resourceName = fileName,
                                        )
                                    } catch (e: Exception) {
                                        _uiState.value = ViewerUiState.Error("加载失败: ${e.message}")
                                    }
                                }
                                else -> {
                                    _uiState.value = ViewerUiState.Error("不支持的文件类型")
                                }
                            }
                        }
                        is Result.Err -> {
                            _uiState.value = ViewerUiState.Error("获取文件源失败")
                        }
                    }
                }
                is Result.Err -> {
                    _uiState.value = ViewerUiState.Error("获取数据源失败")
                }
            }
        }
    }

    private suspend fun setupContentProvider(
        provider: ContentProvider,
        providerKey: String,
        resourceName: String,
        initialPage: Int = 0,
    ) {
        contentProvider = provider
        pageLoader = PageLoader(
            maxCacheSize = PAGE_CACHE_BYTES,
            sizeOf = { bitmap -> bitmap.allocationByteCount.toLong() },
            load = { request ->
                provider.loadPage(request.pageIndex, request.targetWidth, request.targetHeight)
            },
        )
        _totalPages.value = provider.pageCount
        _currentPage.value = initialPage.coerceIn(0, (provider.pageCount - 1).coerceAtLeast(0))

        val items = (0 until provider.pageCount).map { index ->
            val extension = when (provider) {
                is ImageFolderProvider -> provider.getPageExtension(index)
                else -> ""
            }
            ViewerItem.ImagePage(
                title = resourceName,
                pageIndex = index,
                providerKey = providerKey,
                extension = extension,
            )
        }

        _uiState.value = ViewerUiState.Success(
            items = items,
            resourceName = resourceName,
        )
    }

    private suspend fun loadVideoFromSource(
        source: dev.wucheng.resource_viewer.domain.model.Source,
        fileSource: dev.wucheng.resource_viewer.shared.filesource.FileSource,
        filePath: String,
        fileName: String,
    ) {
        val videoSource = when (source.type) {
            SourceType.LOCAL -> {
                if (source.rootPath.startsWith("content://")) {
                    val documentSource = fileSource as? DocumentTreeFileSource
                    if (documentSource == null) {
                        _uiState.value = ViewerUiState.Error("不支持的本地源类型")
                        return
                    }
                    VideoMediaSource.LocalFile(path = documentSource.getDocumentUri(filePath).toString())
                } else {
                    VideoMediaSource.LocalFile(path = "${source.rootPath.trimEnd('/')}/$filePath")
                }
            }
            SourceType.SMB -> {
                val password = filesystemRepository.getPassword(source.id) ?: ""
                val smbFactory = SmbDataSourceFactory(source, password)
                VideoMediaSource.SmbFile(
                    dataSourceFactory = smbFactory,
                    relativePath = filePath,
                    fileSize = 0L,
                )
            }
            else -> {
                _uiState.value = ViewerUiState.Error("不支持的源类型")
                return
            }
        }

        _resourceName.value = fileName
        _totalPages.value = 1
        _uiState.value = ViewerUiState.Success(
            items = listOf(ViewerItem.Video(title = fileName, videoSource = videoSource)),
            resourceName = fileName,
        )
    }

    /**
     * 加载资源。
     * 根据资源类型（VIDEO / PDF / FOLDER 等）创建不同的 ViewerItem 列表。
     */
    fun loadResource() {
        prefetchJob?.cancel()
        contentProvider?.dispose()
        contentProvider = null
        pageLoader = null
        viewModelScope.launch {
            _uiState.value = ViewerUiState.Loading
            loadViewerConfig()

            when (val result = resourceRepository.getById(resourceId)) {
                is Result.Ok -> {
                    val resource = result.value
                    if (resource == null) {
                        _uiState.value = ViewerUiState.Error("Resource not found")
                        return@launch
                    }

                    _resourceName.value = resource.name
                    _isFavorited.value = resource.favorited

                    if (resource.type == ResourceType.VIDEO) {
                        // 视频资源
                        loadVideoResource(resource)
                    } else {
                        // 图片/PDF/压缩包资源
                        loadContentProviderResource(resource)
                    }
                }
                is Result.Err -> {
                    _uiState.value = ViewerUiState.Error("Failed to load resource")
                }
            }
        }
    }

    /**
     * 加载视频资源。
     * 使用 MixedFolderProvider 加载视频所在目录，支持同目录下图片/视频无缝翻页。
     */
    private suspend fun loadVideoResource(resource: dev.wucheng.resource_viewer.domain.model.Resource) {
        when (val sourceResult = filesystemRepository.getSource(resource.sourceId)) {
            is Result.Ok -> {
                val source = sourceResult.value
                if (source == null) {
                    _uiState.value = ViewerUiState.Error("Source not found")
                    return
                }
                when (val fsResult = filesystemRepository.getFileSource(source.id)) {
                    is Result.Ok -> {
                        val fileSource = fsResult.value
                        val dirPath = resource.relativePath.substringBeforeLast('/', "").ifBlank { "." }
                        val videoFactory = if (source.type == SourceType.SMB) {
                            val password = filesystemRepository.getPassword(source.id) ?: ""
                            SmbDataSourceFactory(source, password)
                        } else null

                        try {
                            val (provider, rawItems) = withContext(ioDispatcher) {
                                val mixedProvider = MixedFolderProvider(
                                    fileSource = fileSource,
                                    relativePath = dirPath,
                                    sourceId = source.id,
                                    videoDataSourceFactory = videoFactory,
                                    pageCacheDirectory = context.cacheDir,
                                    pageCacheLimitBytes = pageCacheLimitBytes,
                                )
                                mixedProvider to mixedProvider.buildViewerItems()
                            }
                            if (rawItems.isEmpty()) {
                                _uiState.value = ViewerUiState.Error("目录中没有可浏览的文件")
                                return
                            }

                            val viewerItems = if (source.type == SourceType.LOCAL) {
                                rawItems.map { item ->
                                    if (item is ViewerItem.Video && item.videoSource is VideoMediaSource.LocalFile) {
                                        val absPath = if (source.rootPath.startsWith("content://")) {
                                            val documentSource =
                                                fileSource as? DocumentTreeFileSource
                                                    ?: return@map item
                                            documentSource.getDocumentUri(
                                                item.videoSource.path
                                            ).toString()
                                        } else {
                                            "${source.rootPath.trimEnd('/')}/${item.videoSource.path}"
                                        }
                                        item.copy(videoSource = VideoMediaSource.LocalFile(path = absPath))
                                    } else item
                                }
                            } else rawItems

                            val currentIndex = provider.findIndex(resource.relativePath)

                            contentProvider = provider
                            pageLoader = PageLoader(
                                maxCacheSize = PAGE_CACHE_BYTES,
                                sizeOf = { bitmap -> bitmap.allocationByteCount.toLong() },
                                load = { request ->
                                    provider.loadPage(
                                        request.pageIndex, request.targetWidth, request.targetHeight
                                    )
                                },
                            )
                            _totalPages.value = viewerItems.size
                            _currentPage.value = currentIndex.coerceIn(0, viewerItems.size - 1)
                            _uiState.value = ViewerUiState.Success(
                                items = viewerItems,
                                resourceName = resource.name,
                            )
                        } catch (e: Exception) {
                            _uiState.value = ViewerUiState.Error("加载失败: ${e.message}")
                        }
                    }
                    is Result.Err -> {
                        _uiState.value = ViewerUiState.Error("Failed to get file source")
                    }
                }
            }
            is Result.Err -> {
                _uiState.value = ViewerUiState.Error("Failed to get source")
            }
        }
    }

    /**
     * 加载图片/PDF/压缩包资源。
     * 对于文件夹类资源，使用 MixedFolderProvider 支持图片+视频无缝浏览。
     */
    private suspend fun loadContentProviderResource(resource: dev.wucheng.resource_viewer.domain.model.Resource) {
        currentResource = resource
        when (val fsResult = filesystemRepository.getFileSource(resource.sourceId)) {
            is Result.Ok -> {
                val fileSource = fsResult.value
                cachedFileSource = fileSource
                try {
                    if (resource.type == ResourceType.PDF) {
                        val provider = withContext(ioDispatcher) {
                            PdfContentProvider(
                                context = context,
                                fileSource = fileSource,
                                relativePath = resource.relativePath,
                            )
                        }
                        contentProvider = provider
                        pageLoader = PageLoader(
                            maxCacheSize = PAGE_CACHE_BYTES,
                            sizeOf = { bitmap -> bitmap.allocationByteCount.toLong() },
                            load = { request ->
                                provider.loadPage(request.pageIndex, request.targetWidth, request.targetHeight)
                            },
                        )
                        _totalPages.value = provider.pageCount
                        _currentPage.value = initialPage.coerceIn(0, (provider.pageCount - 1).coerceAtLeast(0))

                        val items = (0 until provider.pageCount).map { index ->
                            ViewerItem.ImagePage(
                                title = resource.name,
                                pageIndex = index,
                                providerKey = resourceId,
                                extension = "",
                            )
                        }
                        _uiState.value = ViewerUiState.Success(
                            items = items,
                            resourceName = resource.name,
                        )
                    } else {
                        // 图片文件夹/画廊/章节：使用 MixedFolderProvider 支持图片+视频无缝浏览
                        val isRecursive = resource.organizationMode in setOf(
                            OrganizationMode.GALLERY,
                            OrganizationMode.CHAPTER_GALLERY,
                        )
                        val videoFactory = when (val srcResult = filesystemRepository.getSource(resource.sourceId)) {
                            is Result.Ok -> {
                                val source = srcResult.value
                                if (source != null && source.type == SourceType.SMB) {
                                    val password = filesystemRepository.getPassword(source.id) ?: ""
                                    SmbDataSourceFactory(source, password)
                                } else null
                            }
                            is Result.Err -> null
                        }

                        val provider = withContext(ioDispatcher) {
                            MixedFolderProvider(
                                fileSource = fileSource,
                                relativePath = contentPath.ifBlank { resource.relativePath },
                                sourceId = resource.sourceId,
                                videoDataSourceFactory = videoFactory,
                                recursive = isRecursive,
                                pageCacheDirectory = context.cacheDir,
                                pageCacheLimitBytes = pageCacheLimitBytes,
                            )
                        }
                        val viewerItems = provider.buildViewerItems()

                        contentProvider = provider
                        pageLoader = PageLoader(
                            maxCacheSize = PAGE_CACHE_BYTES,
                            sizeOf = { bitmap -> bitmap.allocationByteCount.toLong() },
                            load = { request ->
                                provider.loadPage(request.pageIndex, request.targetWidth, request.targetHeight)
                            },
                        )
                        _totalPages.value = viewerItems.size
                        _currentPage.value = initialPage.coerceIn(0, (viewerItems.size - 1).coerceAtLeast(0))

                        _uiState.value = ViewerUiState.Success(
                            items = viewerItems,
                            resourceName = resource.name,
                        )
                    }
                } catch (e: IOException) {
                    // 加密 PDF 或其他 IO 错误
                    _uiState.value = ViewerUiState.Error(
                        DomainError.MediaEncryptedError(
                            mediaType = MediaType.PDF,
                            message = "加密 PDF 暂不支持",
                        ).message
                    )
                } catch (e: Exception) {
                    _uiState.value = ViewerUiState.Error("Failed to load resource")
                }
            }
            is Result.Err -> {
                _uiState.value = ViewerUiState.Error("Failed to get file source")
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

    private suspend fun loadViewerConfig() {
        val config = appConfigDao?.getConfig()?.first() ?: AppConfigEntity()
        _pageDirection.value = config.pageDirection
        _doublePageMode.value = config.doublePageMode
        _crossChapter.value = config.crossChapter
        pageCacheLimitBytes = config.pageCacheLimitMB.toLong() * 1024 * 1024
    }

    fun cyclePageDirection() {
        val next = when (_pageDirection.value) {
            PageDirection.RIGHT_TO_LEFT -> PageDirection.LEFT_TO_RIGHT
            PageDirection.LEFT_TO_RIGHT -> PageDirection.VERTICAL
            PageDirection.VERTICAL -> PageDirection.RIGHT_TO_LEFT
        }
        _pageDirection.value = next
        persistViewerConfig(pageDirection = next)
    }

    fun cycleDoublePageMode() {
        val next = when (_doublePageMode.value) {
            DoublePageMode.AUTO -> DoublePageMode.SINGLE
            DoublePageMode.SINGLE -> DoublePageMode.DOUBLE
            DoublePageMode.DOUBLE -> DoublePageMode.AUTO
        }
        _doublePageMode.value = next
        persistViewerConfig(doublePageMode = next)
    }

    private fun persistViewerConfig(
        pageDirection: PageDirection? = null,
        doublePageMode: DoublePageMode? = null,
    ) {
        val dao = appConfigDao ?: return
        viewModelScope.launch {
            val current = dao.getConfig().first() ?: AppConfigEntity()
            dao.save(
                current.copy(
                    pageDirection = pageDirection ?: current.pageDirection,
                    doublePageMode = doublePageMode ?: current.doublePageMode,
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    /**
     * 加载当前 ContentProvider 中的页面 Bitmap。
     */
    suspend fun loadPageBitmap(pageIndex: Int, targetWidth: Int, targetHeight: Int): Bitmap {
        val loader = pageLoader ?: throw IllegalStateException("Content provider is not ready")
        val request = PageRequest(pageIndex, targetWidth, targetHeight)
        val isCurrentPage = pageIndex == _currentPage.value

        if (isCurrentPage) prefetchJob?.cancel()
        val bitmap = withContext(ioDispatcher) { loader.get(request) }

        if (isCurrentPage) {
            prefetchJob = viewModelScope.launch(ioDispatcher) {
                preloadAround(request, loader)
            }
        }
        return bitmap
    }

    /**
     * 获取指定页面的 URI。
     * 用于 Coil 加载动画图片（GIF/animated WebP）。
     */
    suspend fun getPageUri(pageIndex: Int): android.net.Uri {
        val provider = contentProvider ?: throw IllegalStateException("Content provider is not ready")
        return withContext(ioDispatcher) {
            when (provider) {
                is ImageFolderProvider -> provider.getPageUri(pageIndex)
                is MixedFolderProvider -> provider.getPageUri(pageIndex)
                else -> throw UnsupportedOperationException("URI not available for this provider type")
            }
        }
    }

    private suspend fun preloadAround(
        current: PageRequest,
        loader: PageLoader<PageRequest, Bitmap>,
    ) {
        val pageOrder = listOf(
            current.pageIndex + 1,
            current.pageIndex - 1,
            current.pageIndex + 2,
            current.pageIndex + 3,
        )
        pageOrder
            .filter { it in 0 until _totalPages.value }
            .forEach { pageIndex ->
                try {
                    loader.get(current.copy(pageIndex = pageIndex))
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    // 单个预取失败不影响当前页；用户翻到该页时仍可主动重试。
                }
            }
    }

    /**
     * 加载章节列表（用于跨章节导航）。
     */
    fun loadChapters() {
        viewModelScope.launch {
            val result = resourceRepository.getById(resourceId)
            if (result is Result.Ok) {
                val resource = result.value ?: return@launch
                if (resource.organizationMode == OrganizationMode.CHAPTER ||
                    resource.organizationMode == OrganizationMode.CHAPTER_GALLERY) {
                    when (val fsResult = filesystemRepository.getFileSource(resource.sourceId)) {
                        is Result.Ok -> {
                            try {
                                val strategy = when (resource.organizationMode) {
                                    OrganizationMode.CHAPTER -> ChapterStrategy()
                                    OrganizationMode.CHAPTER_GALLERY -> ChapterGalleryStrategy()
                                    else -> return@launch
                                }
                                val chapterList = strategy.getChapters(resource, fsResult.value)
                                _chapters.value = chapterList
                                // 查找当前章节索引
                                val currentIndex = chapterList.indexOfFirst { it.relativePath == contentPath }
                                _currentChapterIndex.value = if (currentIndex >= 0) currentIndex else 0
                            } catch (_: Exception) {
                                // 忽略章节加载失败
                            }
                        }
                        is Result.Err -> { /* 忽略 */ }
                    }
                }
            }
        }
    }

    /**
     * 导航到下一章（仅 CHAPTER / CHAPTER_GALLERY 模式触发）。
     * @return 下一章名称，如果没有下一章返回 null
     */
    fun navigateToNextChapter(): String? {
        val chapterList = _chapters.value
        val currentIndex = _currentChapterIndex.value
        if (chapterList.isEmpty() || currentIndex < 0 || currentIndex >= chapterList.size - 1) {
            return null
        }
        if (_isNavigatingChapter.value) return null
        val nextChapter = chapterList[currentIndex + 1]
        _currentChapterIndex.value = currentIndex + 1
        viewModelScope.launch {
            reloadChapterContent(nextChapter.relativePath, startPage = 0)
            _chapterHint.value = "下一章: ${nextChapter.name}"
            kotlinx.coroutines.delay(2000)
            _chapterHint.value = null
        }
        return nextChapter.name
    }

    /**
     * 导航到上一章（仅 CHAPTER / CHAPTER_GALLERY 模式触发）。
     * @return 上一章名称，如果没有上一章返回 null
     */
    fun navigateToPrevChapter(): String? {
        val chapterList = _chapters.value
        val currentIndex = _currentChapterIndex.value
        if (chapterList.isEmpty() || currentIndex <= 0) {
            return null
        }
        if (_isNavigatingChapter.value) return null
        val prevChapter = chapterList[currentIndex - 1]
        _currentChapterIndex.value = currentIndex - 1
        viewModelScope.launch {
            reloadChapterContent(prevChapter.relativePath, startPage = Int.MAX_VALUE)
            _chapterHint.value = "上一章: ${prevChapter.name}"
            kotlinx.coroutines.delay(2000)
            _chapterHint.value = null
        }
        return prevChapter.name
    }

    /**
     * 检查是否可以跨章节导航。
     * 仅在 CHAPTER / CHAPTER_GALLERY 模式下才可能返回 true。
     */
    fun canNavigateChapter(): Boolean {
        return _crossChapter.value && _chapters.value.isNotEmpty() && !_isNavigatingChapter.value
    }

    /**
     * 重新加载指定章节的内容。
     * 仅用于 CHAPTER / CHAPTER_GALLERY 模式下的跨章节切换。
     */
    private suspend fun reloadChapterContent(chapterPath: String, startPage: Int) {
        val resource = currentResource ?: return
        if (_isNavigatingChapter.value) return
        _isNavigatingChapter.value = true

        try {
            // 取消旧的预取并释放旧的 ContentProvider
            prefetchJob?.cancel()
            contentProvider?.dispose()
            contentProvider = null
            pageLoader = null

            val fileSource = cachedFileSource
                ?: when (val fsResult = filesystemRepository.getFileSource(resource.sourceId)) {
                    is Result.Ok -> fsResult.value
                    is Result.Err -> {
                        _chapterHint.value = "无法获取文件源"
                        return
                    }
                }

            val isRecursive = resource.organizationMode in setOf(
                OrganizationMode.CHAPTER_GALLERY
            )
            val videoFactory = when (val srcResult = filesystemRepository.getSource(resource.sourceId)) {
                is Result.Ok -> {
                    val source = srcResult.value
                    if (source != null && source.type == SourceType.SMB) {
                        val password = filesystemRepository.getPassword(source.id) ?: ""
                        SmbDataSourceFactory(source, password)
                    } else null
                }
                is Result.Err -> null
            }

            val provider = withContext(ioDispatcher) {
                MixedFolderProvider(
                    fileSource = fileSource,
                    relativePath = chapterPath,
                    sourceId = resource.sourceId,
                    videoDataSourceFactory = videoFactory,
                    recursive = isRecursive,
                    pageCacheDirectory = context.cacheDir,
                    pageCacheLimitBytes = pageCacheLimitBytes,
                )
            }
            val viewerItems = provider.buildViewerItems()

            if (viewerItems.isEmpty()) {
                _chapterHint.value = "章节无内容"
                return
            }

            contentProvider = provider
            pageLoader = PageLoader(
                maxCacheSize = PAGE_CACHE_BYTES,
                sizeOf = { bitmap -> bitmap.allocationByteCount.toLong() },
                load = { request ->
                    provider.loadPage(request.pageIndex, request.targetWidth, request.targetHeight)
                },
            )

            val resolvedPage = startPage.coerceIn(0, (viewerItems.size - 1).coerceAtLeast(0))
            _totalPages.value = viewerItems.size
            _currentPage.value = resolvedPage
            _uiState.value = ViewerUiState.Success(
                items = viewerItems,
                resourceName = resource.name,
            )
        } catch (e: Exception) {
            _chapterHint.value = "加载失败"
        } finally {
            _isNavigatingChapter.value = false
        }
    }

    /**
     * 切换收藏状态。
     */
    fun toggleFavorite() {
        val newState = !_isFavorited.value
        _isFavorited.value = newState
        viewModelScope.launch {
            resourceRepository.toggleFavorite(resourceId, newState)
        }
    }

    /**
     * 释放资源。
     */
    override fun onCleared() {
        prefetchJob?.cancel()
        super.onCleared()
        contentProvider?.dispose()
    }

    private data class PageRequest(
        val pageIndex: Int,
        val targetWidth: Int,
        val targetHeight: Int,
    )

    private companion object {
        const val PAGE_CACHE_BYTES: Long = 200L * 1024L * 1024L
    }
}
