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

data class FileBrowserUiState(
    val source: Source? = null,
    val currentPath: String = "",
    val entries: List<FileEntry> = emptyList(),
    val selectedPaths: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isAdding: Boolean = false,
    val error: String? = null,
    val lastAddResult: ScanResult? = null,
)

class FileBrowserViewModel(
    private val sourceId: String,
    private val filesystemRepository: FilesystemRepository,
    private val batchAddResourcesUseCase: BatchAddResourcesUseCase,
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

    fun toggleSelection(path: String) {
        _uiState.update { state ->
            val next = state.selectedPaths.toMutableSet()
            if (!next.add(path)) {
                next.remove(path)
            }
            state.copy(selectedPaths = next, lastAddResult = null)
        }
    }

    fun addSelectedResources() {
        val source = _uiState.value.source ?: return
        val selected = _uiState.value.selectedPaths.toList()
        val fs = fileSource ?: return
        if (selected.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAdding = true, error = null, lastAddResult = null) }
            when (val result = batchAddResourcesUseCase(fs, source, selected)) {
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
