@file:Suppress("UnsafeOptInUsageError")

package dev.wucheng.resource_viewer.ui.screens.sources

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.ui.screens.sources.BatchAddResourcesDialog
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    sourceId: String,
    onNavigateBack: () -> Unit,
    onOpenFile: (sourceId: String, filePath: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    viewModel: FileBrowserViewModel = koinViewModel { parametersOf(sourceId) },
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(sourceId) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.source?.name ?: "文件浏览")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!viewModel.goUp()) onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (uiState.isMultiSelectMode) {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "全选")
                        }
                        TextButton(onClick = { viewModel.exitMultiSelect() }) {
                            Text("退出")
                        }
                    } else {
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                imageVector = if (uiState.viewMode == FileViewMode.LIST) Icons.Default.GridView else Icons.Default.List,
                                contentDescription = if (uiState.viewMode == FileViewMode.LIST) "网格" else "列表",
                            )
                        }
                        IconButton(onClick = { viewModel.enterMultiSelect() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "多选")
                        }
                        IconButton(onClick = { viewModel.toggleDirectoryTree() }) {
                            Icon(Icons.Default.Menu, contentDescription = "目录树")
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (uiState.isMultiSelectMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "已选 ${uiState.selectedPaths.size} 项",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = { viewModel.showBatchAddDialog() },
                        enabled = uiState.selectedPaths.isNotEmpty() && !uiState.isAdding,
                    ) {
                        if (uiState.isAdding) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                        Text("批量添加资源")
                    }
                }
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val isWideScreen = maxWidth >= 900.dp
            val showTreePanel = uiState.showDirectoryTree

            if (isWideScreen && showTreePanel) {
                Row(modifier = Modifier.fillMaxSize()) {
                    DirectoryTreePanel(
                        currentPath = uiState.currentPath,
                        onNavigateToSegment = { viewModel.navigateToSegment(it) },
                        onNavigateToRoot = { viewModel.openDirectory("") },
                        modifier = Modifier
                            .width(240.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        BreadcrumbBar(
                            pathSegments = uiState.pathSegments,
                            sourceName = uiState.source?.name ?: "",
                            onNavigateToSegment = { viewModel.navigateToSegment(it) },
                            onNavigateToRoot = { viewModel.openDirectory("") },
                        )
                        FileContentArea(
                            uiState = uiState,
                            viewModel = viewModel,
                            onOpenFile = onOpenFile,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    BreadcrumbBar(
                        pathSegments = uiState.pathSegments,
                        sourceName = uiState.source?.name ?: "",
                        onNavigateToSegment = { viewModel.navigateToSegment(it) },
                        onNavigateToRoot = { viewModel.openDirectory("") },
                    )
                    FileContentArea(
                        uiState = uiState,
                        viewModel = viewModel,
                        onOpenFile = onOpenFile,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (!isWideScreen && showTreePanel) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { viewModel.hideDirectoryTree() },
                    )
                    DirectoryTreePanel(
                        currentPath = uiState.currentPath,
                        onNavigateToSegment = {
                            viewModel.navigateToSegment(it)
                            viewModel.hideDirectoryTree()
                        },
                        onNavigateToRoot = {
                            viewModel.openDirectory("")
                            viewModel.hideDirectoryTree()
                        },
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(280.dp)
                            .align(Alignment.CenterStart)
                            .background(MaterialTheme.colorScheme.surface),
                    )
                }
            }

            // 批量添加弹窗
            if (uiState.showBatchAddDialog) {
                BatchAddResourcesDialog(
                    selectedCount = uiState.selectedPaths.size,
                    allTags = uiState.allTags,
                    onConfirm = { orgMode, tagIds -> viewModel.confirmBatchAdd(orgMode, tagIds) },
                    onDismiss = { viewModel.hideBatchAddDialog() },
                )
            }
        }
    }
}

// ===== 面包屑 =====

@Composable
private fun BreadcrumbBar(
    pathSegments: List<String>,
    sourceName: String,
    onNavigateToSegment: (Int) -> Unit,
    onNavigateToRoot: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = sourceName.ifBlank { "/" },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { onNavigateToRoot() }
                .padding(horizontal = 4.dp, vertical = 2.dp),
        )
        pathSegments.forEachIndexed { index, segment ->
            Icon(
                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val isLast = index == pathSegments.lastIndex
            Text(
                text = segment,
                style = MaterialTheme.typography.labelMedium,
                color = if (isLast) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (isLast) Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                else Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onNavigateToSegment(index) }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}

// ===== 文件内容区域 =====

@Composable
private fun FileContentArea(
    uiState: FileBrowserUiState,
    viewModel: FileBrowserViewModel,
    onOpenFile: (sourceId: String, filePath: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        when (uiState.viewMode) {
            FileViewMode.LIST -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(uiState.entries, key = { it.relativePath }) { entry ->
                        FileEntryRow(
                            entry = entry,
                            isMultiSelect = uiState.isMultiSelectMode,
                            selected = entry.relativePath in uiState.selectedPaths,
                            onClick = {
                                if (uiState.isMultiSelectMode) {
                                    viewModel.toggleSelection(entry.relativePath)
                                } else if (entry.isDirectory) {
                                    viewModel.openDirectory(entry.relativePath)
                                } else {
                                    onOpenFile(uiState.source?.id ?: "", entry.relativePath)
                                }
                            },
                            onLongClick = {
                                if (!uiState.isMultiSelectMode) {
                                    viewModel.enterMultiSelect()
                                    viewModel.toggleSelection(entry.relativePath)
                                }
                            },
                            onToggleSelect = { viewModel.toggleSelection(entry.relativePath) },
                        )
                    }
                }
            }
            FileViewMode.GRID -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.entries, key = { it.relativePath }) { entry ->
                        FileEntryGridItem(
                            entry = entry,
                            isMultiSelect = uiState.isMultiSelectMode,
                            selected = entry.relativePath in uiState.selectedPaths,
                            onClick = {
                                if (uiState.isMultiSelectMode) {
                                    viewModel.toggleSelection(entry.relativePath)
                                } else if (entry.isDirectory) {
                                    viewModel.openDirectory(entry.relativePath)
                                } else {
                                    onOpenFile(uiState.source?.id ?: "", entry.relativePath)
                                }
                            },
                            onLongClick = {
                                if (!uiState.isMultiSelectMode) {
                                    viewModel.enterMultiSelect()
                                    viewModel.toggleSelection(entry.relativePath)
                                }
                            },
                            onToggleSelect = { viewModel.toggleSelection(entry.relativePath) },
                        )
                    }
                }
            }
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        uiState.error?.let { message ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearMessage() }) { Text("关闭") }
                },
            ) { Text(message) }
        }

        uiState.lastAddResult?.let { result ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearMessage() }) { Text("关闭") }
                },
            ) {
                Text("添加 ${result.successCount} 项，跳过 ${result.skipCount} 项，失败 ${result.failures.size} 项")
            }
        }
    }
}

// ===== 文件列表行 =====

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileEntryRow(
    entry: FileEntry,
    isMultiSelect: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelect: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                text = if (entry.isDirectory) "文件夹" else formatFileSize(entry.size),
                style = MaterialTheme.typography.bodySmall,
            )
        },
        leadingContent = {
            if (isMultiSelect) {
                Icon(
                    imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (selected) "取消选择" else "选择",
                    tint = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onToggleSelect() },
                )
            } else {
                FileIcon(entry)
            }
        },
        trailingContent = {
            if (!isMultiSelect && entry.isDirectory) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        ),
    )
}

// ===== 网格项 =====

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileEntryGridItem(
    entry: FileEntry,
    isMultiSelect: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelect: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected && isMultiSelect) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(fileTypeColor(entry)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = fileIcon(entry),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = Color.White.copy(alpha = 0.6f),
                )
                if (isMultiSelect && selected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "已选择",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(20.dp),
                    )
                }
            }
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

// ===== 目录树面板 =====

@Composable
private fun DirectoryTreePanel(
    currentPath: String,
    onNavigateToSegment: (Int) -> Unit,
    onNavigateToRoot: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val segments = currentPath.trim('/').split('/').filter { it.isNotEmpty() }

    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "目录导航",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        ListItem(
            headlineContent = { Text("/") },
            leadingContent = {
                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            modifier = Modifier.clickable { onNavigateToRoot() },
            colors = if (segments.isEmpty()) {
                ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            } else {
                ListItemDefaults.colors()
            },
        )
        segments.forEachIndexed { index, segment ->
            ListItem(
                headlineContent = { Text(segment) },
                leadingContent = {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                modifier = Modifier.clickable { onNavigateToSegment(index) },
                colors = if (index == segments.lastIndex) {
                    ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                } else {
                    ListItemDefaults.colors()
                },
            )
        }
    }
}

// ===== 工具函数 =====

@Composable
private fun FileIcon(entry: FileEntry): ImageVector {
    return when {
        entry.isDirectory -> Icons.Default.Folder
        entry.extension.lowercase() in setOf("mp4", "mkv", "avi", "mov", "webm") -> Icons.Default.Movie
        entry.extension.lowercase() == "pdf" -> Icons.Default.PictureAsPdf
        else -> Icons.Default.Folder
    }
}

private fun fileTypeColor(entry: FileEntry): Color = when {
    entry.isDirectory -> Color(0xFF1565C0)
    entry.extension.lowercase() in setOf("mp4", "mkv", "avi", "mov", "webm") -> Color(0xFF2E7D32)
    entry.extension.lowercase() == "pdf" -> Color(0xFFC62828)
    else -> Color(0xFF757575)
}

private fun fileIcon(entry: FileEntry): ImageVector = when {
    entry.isDirectory -> Icons.Default.Folder
    entry.extension.lowercase() in setOf("mp4", "mkv", "avi", "mov", "webm") -> Icons.Default.Movie
    entry.extension.lowercase() == "pdf" -> Icons.Default.PictureAsPdf
    else -> Icons.Default.Folder
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
    else -> "${"%.2f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
}
