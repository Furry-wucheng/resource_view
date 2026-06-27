package dev.wucheng.resource_viewer.ui.screens.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.data.repository.FilesystemRepository
import dev.wucheng.resource_viewer.data.repository.ResourceRepository
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import dev.wucheng.resource_viewer.shared.organization.GalleryStrategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ContentGridMode { FLAT_GRID, GALLERY }

data class ContentGridUiState(
    val title: String = "",
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
) : ViewModel() {
    private val _uiState = MutableStateFlow(ContentGridUiState())
    val uiState = _uiState.asStateFlow()
    private var fileSource: FileSource? = null

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
            .filter { it.isDirectory || it.extension.lowercase() in IMAGE_EXTENSIONS }
            .sortedWith(compareBy<FileEntry> { !it.isDirectory }.thenBy { it.relativePath })

    private fun fail(message: String) {
        _uiState.value = _uiState.value.copy(isLoading = false, error = message)
    }

    /**
     * 更改组织模式。
     * 更新资源的组织模式。
     */
    fun changeOrganizationMode(mode: OrganizationMode) {
        val state = _uiState.value
        _uiState.value = state.copy(organizationMode = mode)
        viewModelScope.launch {
            resourceRepository.updateOrganizationMode(resourceId, mode)
        }
    }

    companion object {
        val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "bmp", "gif")
    }
}
