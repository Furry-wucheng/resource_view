package dev.wucheng.resource_viewer.ui.screens.viewer

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.wucheng.resource_viewer.data.local.dao.AppConfigDao
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
import dev.wucheng.resource_viewer.shared.thumbnail.ThumbnailLoadManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class ChapterViewMode { LIST, GRID }

sealed class ChapterListUiState {
    data object Loading : ChapterListUiState()
    data class Success(
        val chapters: List<Chapter>,
        val looseFiles: List<FileEntry>,
        val resourceName: String,
        val sourceId: String,
        val organizationMode: OrganizationMode,
        val viewMode: ChapterViewMode = ChapterViewMode.LIST,
    ) : ChapterListUiState()
    data class Error(val message: String) : ChapterListUiState()
}

class ChapterListViewModel(
    private val resourceId: String,
    private val resourceRepository: ResourceRepository,
    private val filesystemRepository: FilesystemRepository,
    private val thumbnailDiskCache: FileBrowserThumbnailDiskCache? = null,
    private val appConfigDao: AppConfigDao? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChapterListUiState>(ChapterListUiState.Loading)
    val uiState: StateFlow<ChapterListUiState> = _uiState.asStateFlow()

    private var resource: Resource? = null
    private var fileSource: FileSource? = null
    private var thumbnailManager: ThumbnailLoadManager? = null

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
                            val loader = FileEntryThumbnailLoader(fs)
                            val concurrency = appConfigDao?.getConfig()?.first()?.thumbnailConcurrency ?: 4
                            thumbnailManager = ThumbnailLoadManager(
                                sourceId = resourceId,
                                thumbnailLoader = loader,
                                diskCache = thumbnailDiskCache,
                                maxConcurrency = concurrency,
                            )

                            try {
                                val strategy = getStrategy(res)
                                val chapters = strategy.getChapters(res, fs)
                                val looseFiles = getLooseFiles(res, fs)

                                _uiState.value = ChapterListUiState.Success(
                                    chapters = chapters,
                                    looseFiles = looseFiles,
                                    resourceName = res.name,
                                    sourceId = res.sourceId,
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

    private fun getStrategy(resource: Resource): OrganizationStrategy {
        return when (resource.organizationMode) {
            OrganizationMode.CHAPTER -> ChapterStrategy()
            OrganizationMode.CHAPTER_GALLERY -> ChapterGalleryStrategy()
            else -> throw IllegalStateException("Unsupported organization mode: ${resource.organizationMode}")
        }
    }

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

    fun changeOrganizationMode(mode: OrganizationMode) {
        val res = resource ?: return
        viewModelScope.launch {
            resourceRepository.updateOrganizationMode(res.id, mode)
            loadChapters()
        }
    }

    fun toggleViewMode() {
        val current = _uiState.value
        if (current is ChapterListUiState.Success) {
            val newMode = if (current.viewMode == ChapterViewMode.LIST) ChapterViewMode.GRID else ChapterViewMode.LIST
            _uiState.value = current.copy(viewMode = newMode)
        }
    }

    suspend fun loadChapterCover(path: String): Bitmap? = thumbnailManager?.load(path)
}
