package dev.wucheng.resource_viewer.ui.screens.sources

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.wucheng.resource_viewer.data.repository.FilesystemRepository
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.domain.model.Source
import kotlinx.coroutines.launch

data class FlatTreeNode(
    val name: String,
    val path: String,
    val depth: Int,
    val isExpanded: Boolean,
    val hasChildren: Boolean,
)

@Composable
fun DirectoryTree(
    source: Source,
    filesystemRepository: FilesystemRepository,
    currentPath: String,
    onDirectoryTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    // 使用 mutableStateOf 包装 Map，这样变化会触发重组
    var expandedPaths by remember { mutableStateOf(mapOf<String, Boolean>()) }
    var childrenMap by remember { mutableStateOf(mapOf<String, List<FileEntry>>()) }
    var loadingPaths by remember { mutableStateOf(mapOf<String, Boolean>()) }

    var rootChildren by remember { mutableStateOf<List<FileEntry>>(emptyList()) }
    var rootLoading by remember { mutableStateOf(true) }

    // 用于触发重组的计数器
    var updateCounter by remember { mutableStateOf(0) }

    LaunchedEffect(source.id) {
        rootLoading = true
        when (val result = filesystemRepository.listDirectory(source, "")) {
            is Result.Ok -> {
                rootChildren = result.value.filter { it.isDirectory }
                rootLoading = false
            }
            is Result.Err -> {
                rootLoading = false
            }
        }
    }

    // 当 currentPath 变化时，自动展开祖先路径
    LaunchedEffect(currentPath) {
        if (currentPath.isNotEmpty()) {
            val segments = currentPath.split("/").filter { it.isNotEmpty() }
            var path = ""
            var changed = false
            segments.forEach { segment ->
                path = if (path.isEmpty()) segment else "$path/$segment"
                if (expandedPaths[path] != true) {
                    expandedPaths = expandedPaths + (path to true)
                    changed = true
                    if (childrenMap[path] == null) {
                        scope.launch {
                            loadingPaths = loadingPaths + (path to true)
                            when (val result = filesystemRepository.listDirectory(source, path)) {
                                is Result.Ok -> {
                                    childrenMap = childrenMap + (path to result.value.filter { it.isDirectory })
                                    loadingPaths = loadingPaths - path
                                    updateCounter++
                                }
                                is Result.Err -> {
                                    loadingPaths = loadingPaths - path
                                }
                            }
                        }
                    }
                }
            }
            if (changed) updateCounter++
        }
    }

    fun toggleExpand(path: String) {
        val isExpanded = expandedPaths[path] == true
        expandedPaths = if (isExpanded) {
            expandedPaths - path
        } else {
            expandedPaths + (path to true)
        }
        updateCounter++

        if (!isExpanded && childrenMap[path] == null) {
            scope.launch {
                loadingPaths = loadingPaths + (path to true)
                when (val result = filesystemRepository.listDirectory(source, path)) {
                    is Result.Ok -> {
                        childrenMap = childrenMap + (path to result.value.filter { it.isDirectory })
                        loadingPaths = loadingPaths - path
                        updateCounter++
                    }
                    is Result.Err -> {
                        loadingPaths = loadingPaths - path
                    }
                }
            }
        }
    }

    // 构建扁平化节点列表 - 每次 updateCounter 变化都重新计算
    val flatNodes = remember(updateCounter, rootChildren, expandedPaths, childrenMap) {
        buildFlatNodes(rootChildren, expandedPaths, childrenMap)
    }

    if (rootLoading) {
        Box(
            modifier = modifier.fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
    } else {
        LazyColumn(modifier = modifier) {
            items(items = flatNodes, key = { "${it.path}_${it.isExpanded}" }) { node ->
                TreeNodeRow(
                    node = node,
                    isCurrentPath = currentPath == node.path,
                    isLoading = loadingPaths[node.path] == true,
                    onToggleExpand = { toggleExpand(node.path) },
                    onDirectoryTap = onDirectoryTap,
                )
            }
        }
    }
}

private fun buildFlatNodes(
    rootChildren: List<FileEntry>,
    expandedPaths: Map<String, Boolean>,
    childrenMap: Map<String, List<FileEntry>>,
): List<FlatTreeNode> {
    val result = mutableListOf<FlatTreeNode>()

    fun addChildren(children: List<FileEntry>, depth: Int) {
        children.forEach { entry ->
            val isExpanded = expandedPaths[entry.relativePath] == true
            val subChildren = childrenMap[entry.relativePath]
            val hasChildren = subChildren?.isNotEmpty() ?: true

            result.add(
                FlatTreeNode(
                    name = entry.name,
                    path = entry.relativePath,
                    depth = depth,
                    isExpanded = isExpanded,
                    hasChildren = hasChildren,
                )
            )

            if (isExpanded && subChildren != null) {
                addChildren(subChildren, depth + 1)
            }
        }
    }

    addChildren(rootChildren, 0)
    return result
}

@Composable
private fun TreeNodeRow(
    node: FlatTreeNode,
    isCurrentPath: Boolean,
    isLoading: Boolean,
    onToggleExpand: () -> Unit,
    onDirectoryTap: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isCurrentPath) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .padding(
                start = (node.depth * 16 + 8).dp,
                top = 6.dp,
                bottom = 6.dp,
                end = 8.dp
            )
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 展开/收起箭头
        Box(
            modifier = Modifier
                .size(28.dp)
                .clickable { onToggleExpand() },
            contentAlignment = Alignment.Center,
        ) {
            when {
                isLoading -> CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
                node.isExpanded -> Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "收起",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "展开",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // 文件夹图标和名称
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable { onDirectoryTap(node.path) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (node.isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                contentDescription = null,
                tint = if (isCurrentPath) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = node.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isCurrentPath) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
