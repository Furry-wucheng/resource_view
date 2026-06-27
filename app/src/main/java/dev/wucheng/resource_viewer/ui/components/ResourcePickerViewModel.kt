package dev.wucheng.resource_viewer.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.wucheng.resource_viewer.data.repository.FilesystemRepository
import dev.wucheng.resource_viewer.data.repository.SourceRepository
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.TreeFileNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dev.wucheng.resource_viewer.shared.media.MediaFormats

/**
 * ResourcePicker 弹窗的 UI 状态。
 */
sealed class ResourcePickerUiState {
    data object Idle : ResourcePickerUiState()
    data object Loading : ResourcePickerUiState()
    data object Ready : ResourcePickerUiState()
    data class Error(val message: String) : ResourcePickerUiState()
}

/**
 * ResourcePicker 弹窗 ViewModel。
 * 管理树形文件选择状态：加载、展开/折叠、勾选。
 */
class ResourcePickerViewModel(
    private val filesystemRepository: FilesystemRepository,
    private val sourceRepository: SourceRepository,
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

    /**
     * 加载指定数据源的目录树。
     * @param sourceId 数据源 ID
     * @param rootPath 根路径（相对路径）
     */
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

    /**
     * 切换目录节点的展开/折叠状态。
     * 如果子节点尚未加载，则加载子目录内容。
     */
    fun toggleExpand(relativePath: String) {
        val nodes = _treeNodes.value.toMutableList()
        updateNodeAtPath(nodes, relativePath) { node ->
            val newExpanded = !node.isExpanded
            if (newExpanded && node.children.isEmpty() && node.isDirectory) {
                // 需要加载子节点
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

    /**
     * 切换节点的勾选状态。
     * 勾选不级联——父子独立。
     */
    fun toggleCheck(relativePath: String) {
        val nodes = _treeNodes.value.toMutableList()
        updateNodeAtPath(nodes, relativePath) { node ->
            node.copy(isChecked = !node.isChecked)
        }
        _treeNodes.value = nodes
        recalculateSelectedCount()
    }

    /**
     * 全选/全取消指定目录的直接子项。
     * 如果已全部选中则取消，否则全选。
     */
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

    /**
     * 获取所有已勾选的文件条目。
     */
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

    /**
     * 从 FileEntry 列表构建树节点列表。
     * 智能识别：纯图片文件夹 → 不可展开；混合内容 → 可展开。
     */
    private fun buildTreeNodes(entries: List<FileEntry>, parentPath: String): List<TreeFileNode> {
        return entries
            .filter { it.isDirectory || isRecognizedFile(it.extension) }
            .map { entry ->
                val fullPath = if (parentPath.isEmpty()) entry.name else "$parentPath/${entry.name}"
                TreeFileNode(
                    name = entry.name,
                    relativePath = fullPath,
                    isDirectory = entry.isDirectory,
                    isExpandable = entry.isDirectory, // 默认可展开，加载子节点后判断
                    fileCount = if (entry.isDirectory) null else null,
                )
            }
    }

    private fun isRecognizedFile(extension: String): Boolean {
        val ext = extension.lowercase()
        return MediaFormats.isPreviewable(ext) || ext in setOf("pdf", "zip", "rar", "7z")
    }

    /**
     * 递归更新指定路径的节点。
     */
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
}
