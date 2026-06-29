package dev.wucheng.resource_viewer.ui.components

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.wucheng.resource_viewer.data.repository.FilesystemRepository
import dev.wucheng.resource_viewer.data.repository.ResourceRepository
import dev.wucheng.resource_viewer.data.repository.SourceRepository
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.TreeFileNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class ResourcePickerUiState {
    data object Idle : ResourcePickerUiState()
    data object Loading : ResourcePickerUiState()
    data object Ready : ResourcePickerUiState()
    data class Error(val message: String) : ResourcePickerUiState()
}

class ResourcePickerViewModel(
    private val filesystemRepository: FilesystemRepository,
    private val sourceRepository: SourceRepository,
    private val resourceRepository: ResourceRepository,
) : ViewModel() {

    private val _treeNodes = MutableStateFlow<List<TreeFileNode>>(emptyList())
    val treeNodes: StateFlow<List<TreeFileNode>> = _treeNodes.asStateFlow()

    private val _uiState = MutableStateFlow<ResourcePickerUiState>(ResourcePickerUiState.Idle)
    val uiState: StateFlow<ResourcePickerUiState> = _uiState.asStateFlow()

    private val _selectedCount = MutableStateFlow(0)
    val selectedCount: StateFlow<Int> = _selectedCount.asStateFlow()

    private val _rootName = MutableStateFlow("")
    val rootName: StateFlow<String> = _rootName.asStateFlow()

    private var sourceId: String = ""
    private var importedPaths: Set<String> = emptySet()

    fun loadTree(sourceId: String, rootPath: String) {
        this.sourceId = sourceId
        _uiState.value = ResourcePickerUiState.Loading

        viewModelScope.launch {
            val sourceResult = sourceRepository.getSourceById(sourceId)
            val source = when (sourceResult) {
                is dev.wucheng.resource_viewer.domain.error.Result.Ok -> sourceResult.value
                is dev.wucheng.resource_viewer.domain.error.Result.Err -> {
                    _uiState.value = ResourcePickerUiState.Error("数据源未找到")
                    return@launch
                }
            }
            if (source == null) {
                _uiState.value = ResourcePickerUiState.Error("数据源未找到")
                return@launch
            }

            _rootName.value = source.name

            importedPaths = loadImportedPaths(sourceId)
            Log.d(TAG, "loaded importedPaths=${importedPaths.size} for source=$sourceId")

            val entriesResult = filesystemRepository.listDirectory(source, rootPath)
            when (entriesResult) {
                is dev.wucheng.resource_viewer.domain.error.Result.Ok -> {
                    val entries = entriesResult.value
                    val nodes = buildTreeNodes(entries, rootPath)
                    _treeNodes.value = nodes
                    _uiState.value = ResourcePickerUiState.Ready
                }
                is dev.wucheng.resource_viewer.domain.error.Result.Err -> {
                    _uiState.value = ResourcePickerUiState.Error("加载目录失败")
                }
            }
        }
    }

    fun toggleExpand(relativePath: String) {
        val nodes = _treeNodes.value.toMutableList()
        updateNodeAtPath(nodes, relativePath) { node ->
            val newExpanded = !node.isExpanded
            if (newExpanded && node.children.isEmpty() && node.isDirectory) {
                loadChildren(relativePath) { children ->
                    val updated = _treeNodes.value.toMutableList()
                    updateNodeAtPath(updated, relativePath) { n ->
                        n.copy(isExpanded = true, children = children)
                    }
                    _treeNodes.value = updated
                }
                node.copy(isExpanded = true)
            } else {
                node.copy(isExpanded = newExpanded)
            }
        }
        _treeNodes.value = nodes
    }

    fun toggleCheck(relativePath: String) {
        val nodes = _treeNodes.value.toMutableList()
        updateNodeAtPath(nodes, relativePath) { node ->
            node.copy(isChecked = !node.isChecked)
        }
        _treeNodes.value = nodes
        recalculateSelectedCount()
    }

    fun selectAllChildren(parentPath: String) {
        val nodes = _treeNodes.value.toMutableList()
        updateNodeAtPath(nodes, parentPath) { parent ->
            val allChecked = parent.children.isNotEmpty() && parent.children.all { it.isChecked }
            parent.copy(
                children = parent.children.map { it.copy(isChecked = !allChecked) },
            )
        }
        _treeNodes.value = nodes
        recalculateSelectedCount()
    }

    fun selectAllRootNodes() {
        val nodes = _treeNodes.value
        val allChecked = nodes.isNotEmpty() && nodes.all { it.isChecked }
        _treeNodes.value = nodes.map { it.copy(isChecked = !allChecked) }
        recalculateSelectedCount()
    }

    fun getSelectedEntries(): List<FileEntry> {
        return _treeNodes.value.flatMap { it.checkedLeafNodes }.map { node ->
            FileEntry(
                name = node.name,
                relativePath = node.relativePath,
                isDirectory = node.isDirectory,
                size = 0,
                modifiedAt = 0,
            )
        }
    }

    private fun loadChildren(parentPath: String, onLoaded: (List<TreeFileNode>) -> Unit) {
        viewModelScope.launch {
            val sourceResult = sourceRepository.getSourceById(sourceId)
            val source = when (sourceResult) {
                is dev.wucheng.resource_viewer.domain.error.Result.Ok -> sourceResult.value
                is dev.wucheng.resource_viewer.domain.error.Result.Err -> return@launch
            } ?: return@launch

            val entriesResult = filesystemRepository.listDirectory(source, parentPath)
            when (entriesResult) {
                is dev.wucheng.resource_viewer.domain.error.Result.Ok -> {
                    val children = buildTreeNodes(entriesResult.value, parentPath)
                    onLoaded(children)
                }
                is dev.wucheng.resource_viewer.domain.error.Result.Err -> { /* ignore */ }
            }
        }
    }

    private suspend fun loadImportedPaths(sId: String): Set<String> {
        return try {
            resourceRepository.getVisibleResources().first()
                .filter { it.sourceId == sId }
                .map { it.relativePath }
                .toSet()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load imported paths", e)
            emptySet()
        }
    }

    private fun buildTreeNodes(entries: List<FileEntry>, parentPath: String): List<TreeFileNode> {
        return entries
            .filter { it.isDirectory }
            .map { entry ->
                val fullPath = if (parentPath.isEmpty()) entry.name else "$parentPath/${entry.name}"
                TreeFileNode(
                    name = entry.name,
                    relativePath = fullPath,
                    isDirectory = entry.isDirectory,
                    isExpandable = true,
                    isImported = importedPaths.contains(fullPath),
                )
            }
    }

    private fun updateNodeAtPath(
        nodes: MutableList<TreeFileNode>,
        targetPath: String,
        transform: (TreeFileNode) -> TreeFileNode,
    ) {
        for (i in nodes.indices) {
            val node = nodes[i]
            if (node.relativePath == targetPath) {
                nodes[i] = transform(node)
                return
            }
            if (node.children.isNotEmpty()) {
                val children = node.children.toMutableList()
                updateNodeAtPath(children, targetPath, transform)
                nodes[i] = node.copy(children = children)
            }
        }
    }

    private fun recalculateSelectedCount() {
        _selectedCount.value = _treeNodes.value.sumOf { it.checkedLeafNodes.size }
    }

    private companion object {
        const val TAG = "ResourcePickerVM"
    }
}
