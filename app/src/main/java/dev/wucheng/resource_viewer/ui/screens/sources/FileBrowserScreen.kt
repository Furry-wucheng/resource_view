@file:Suppress("UnsafeOptInUsageError")

package dev.wucheng.resource_viewer.ui.screens.sources

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.wucheng.resource_viewer.data.local.datastore.FileSortMode
import dev.wucheng.resource_viewer.data.local.datastore.FileViewMode
import dev.wucheng.resource_viewer.data.repository.FilesystemRepository
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.ui.components.FileThumbnailCard
import dev.wucheng.resource_viewer.ui.components.fileTypeColor
import dev.wucheng.resource_viewer.ui.components.fileTypeIcon
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    sourceId: String,
    onNavigateBack: () -> Unit,
    onOpenFile: (sourceId: String, filePath: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    viewModel: FileBrowserViewModel = koinViewModel { parametersOf(sourceId) },
    filesystemRepository: FilesystemRepository = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    // 目录树显示状态，根据设置初始化
    var showDirectoryTree by remember { mutableStateOf(uiState.showDirectoryTree) }

    // 显式滚动状态，用于跨目录恢复位置
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    // 保存当前目录滚动位置
    val saveScrollPosition: () -> Unit = {
        val path = uiState.currentPath
        when (uiState.viewMode) {
            FileViewMode.LIST -> viewModel.saveScrollPosition(
                path, listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset
            )
            FileViewMode.GRID -> viewModel.saveScrollPosition(
                path, gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset
            )
        }
    }

    // 统一的目录切换：先保存当前位置，再跳转
    val performOpenDirectory: (String) -> Unit = { path ->
        saveScrollPosition()
        viewModel.openDirectory(path)
    }
    val performGoUp: () -> Boolean = {
        saveScrollPosition()
        viewModel.goUp()
    }
    val performNavigateToSegment: (Int) -> Unit = { index ->
        saveScrollPosition()
        viewModel.navigateToSegment(index)
    }
    val performNavigateToRoot: () -> Unit = {
        saveScrollPosition()
        viewModel.openDirectory("")
    }

    LaunchedEffect(sourceId) { viewModel.load() }
    LaunchedEffect(uiState.showDirectoryTree) { showDirectoryTree = uiState.showDirectoryTree }

    // 返回键：多选模式独立处理，避免状态突变导致 fallthrough
    BackHandler(enabled = uiState.isMultiSelectMode) {
        viewModel.exitMultiSelect()
    }
    BackHandler(enabled = !uiState.isMultiSelectMode) {
        if (!performGoUp()) onNavigateBack()
    }

    Column(modifier = modifier.fillMaxSize()) {
        // 顶栏
        TopAppBar(
            title = { Text(uiState.source?.name ?: "文件浏览") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            },
            actions = {
                if (uiState.isMultiSelectMode) {
                    val allSelected = uiState.selectedPaths.size == uiState.entries.size && uiState.entries.isNotEmpty()
                    TextButton(onClick = { if (allSelected) viewModel.deselectAll() else viewModel.selectAll() }) {
                        Text(if (allSelected) "取消" else "全选")
                    }
                    TextButton(onClick = { viewModel.exitMultiSelect() }) {
                        Text("退出")
                    }
                } else {
                    // 目录树按钮
                    if (uiState.showDirectoryTree) {
                        IconButton(onClick = { showDirectoryTree = !showDirectoryTree }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "目录树",
                                tint = if (showDirectoryTree) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // 视图切换
                    IconButton(onClick = {
                        viewModel.setViewMode(
                            if (uiState.viewMode == FileViewMode.LIST) FileViewMode.GRID else FileViewMode.LIST
                        )
                    }) {
                        Icon(
                            imageVector = if (uiState.viewMode == FileViewMode.LIST) Icons.Default.GridView else Icons.Default.List,
                            contentDescription = if (uiState.viewMode == FileViewMode.LIST) "网格" else "列表",
                        )
                    }

                    // 排序菜单
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "排序")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("名称 A→Z") },
                                onClick = { viewModel.setSortMode(FileSortMode.NAME_ASC); showSortMenu = false },
                                leadingIcon = { if (uiState.sortMode == FileSortMode.NAME_ASC) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) }
                            )
                            DropdownMenuItem(
                                text = { Text("名称 Z→A") },
                                onClick = { viewModel.setSortMode(FileSortMode.NAME_DESC); showSortMenu = false },
                                leadingIcon = { if (uiState.sortMode == FileSortMode.NAME_DESC) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) }
                            )
                            DropdownMenuItem(
                                text = { Text("修改时间 旧→新") },
                                onClick = { viewModel.setSortMode(FileSortMode.MODIFIED_ASC); showSortMenu = false },
                                leadingIcon = { if (uiState.sortMode == FileSortMode.MODIFIED_ASC) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) }
                            )
                            DropdownMenuItem(
                                text = { Text("修改时间 新→旧") },
                                onClick = { viewModel.setSortMode(FileSortMode.MODIFIED_DESC); showSortMenu = false },
                                leadingIcon = { if (uiState.sortMode == FileSortMode.MODIFIED_DESC) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) }
                            )
                        }
                    }
                }
            },
        )

        // 内容区：目录树 + 文件列表
        Row(modifier = Modifier.weight(1f)) {
            // 目录树面板（在 content 内部）
            if (showDirectoryTree) {
                Box(
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    uiState.source?.let { source ->
                        DirectoryTree(
                            source = source,
                            filesystemRepository = filesystemRepository,
                            currentPath = uiState.currentPath,
                            onDirectoryTap = performOpenDirectory,
                        )
                    }
                }
            }

            // 文件列表
            Box(modifier = Modifier.weight(1f)) {
                Column {
                    // 面包屑导航
                    BreadcrumbBar(
                        pathSegments = uiState.pathSegments,
                        sourceName = uiState.source?.name ?: "",
                        onNavigateToSegment = performNavigateToSegment,
                        onNavigateToRoot = performNavigateToRoot,
                    )
                    // 文件内容
                    FileContentArea(
                        uiState = uiState,
                        viewModel = viewModel,
                        onOpenFile = onOpenFile,
                        listState = listState,
                        gridState = gridState,
                        onOpenDirectory = performOpenDirectory,
                    )
                }
            }
        }

        // 底部栏（多选模式）
        if (uiState.isMultiSelectMode) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                    Text("批量添加资源")
                }
            }
        }

        // 批量添加弹窗
        if (uiState.showBatchAddDialog) {
            BatchAddResourcesDialog(
                selectedCount = uiState.selectedPaths.size,
                allTags = uiState.allTags,
                onConfirm = { orgMode, tagIds -> viewModel.confirmBatchAdd(orgMode, tagIds) },
                onCreateTag = viewModel::createTag,
                onDismiss = { viewModel.hideBatchAddDialog() },
            )
        }
    }
}

// ===== 文件内容区域 =====

@Composable
private fun FileContentArea(
    uiState: FileBrowserUiState,
    viewModel: FileBrowserViewModel,
    onOpenFile: (String, String) -> Unit,
    listState: LazyListState,
    gridState: LazyGridState,
    onOpenDirectory: (String) -> Unit,
) {
    // 恢复滚动位置
    LaunchedEffect(uiState.currentPath, uiState.viewMode) {
        val (index, offset) = viewModel.getScrollPosition(uiState.currentPath) ?: (0 to 0)
        when (uiState.viewMode) {
            FileViewMode.LIST -> listState.scrollToItem(index, offset)
            FileViewMode.GRID -> gridState.scrollToItem(index, offset)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState.viewMode) {
            FileViewMode.LIST -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(uiState.entries, key = { it.relativePath }) { entry ->
                        FileEntryRow(
                            entry = entry,
                            isMultiSelect = uiState.isMultiSelectMode,
                            selected = entry.relativePath in uiState.selectedPaths,
                            onClick = {
                                if (uiState.isMultiSelectMode) viewModel.toggleSelection(entry.relativePath)
                                else if (entry.isDirectory) onOpenDirectory(entry.relativePath)
                                else onOpenFile(uiState.source?.id ?: "", entry.relativePath)
                            },
                            onLongClick = {
                                if (!uiState.isMultiSelectMode) {
                                    viewModel.enterMultiSelect()
                                    viewModel.toggleSelection(entry.relativePath)
                                }
                            },
                            onToggleSelect = { viewModel.toggleSelection(entry.relativePath) },
                            thumbnail = { FileThumbnail(entry, viewModel) },
                        )
                    }
                }
            }
            FileViewMode.GRID -> {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.entries, key = { it.relativePath }) { entry ->
                        val isMultiSelect = uiState.isMultiSelectMode
                        val selected = entry.relativePath in uiState.selectedPaths
                        val hasThumbnail by produceState(false, entry.relativePath) {
                            value = viewModel.loadThumbnail(entry) != null
                        }
                        FileThumbnailCard(
                            entry = entry,
                            loadThumbnail = { viewModel.loadThumbnail(it) },
                            selected = selected && isMultiSelect,
                            onClick = {
                                if (isMultiSelect) viewModel.toggleSelection(entry.relativePath)
                                else if (entry.isDirectory) onOpenDirectory(entry.relativePath)
                                else onOpenFile(uiState.source?.id ?: "", entry.relativePath)
                            },
                            onLongClick = {
                                if (!isMultiSelect) {
                                    viewModel.enterMultiSelect()
                                    viewModel.toggleSelection(entry.relativePath)
                                }
                            },
                            trailingIcon = {
                                if (isMultiSelect) {
                                    Icon(
                                        if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = if (selected) "取消选择" else "选择",
                                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.clickable { viewModel.toggleSelection(entry.relativePath) },
                                    )
                                }
                            },
                            bottomEndBadge = {
                                if (entry.isDirectory && hasThumbnail) {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = "文件夹",
                                        tint = Color(0xFFFFC107),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            },
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
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action = { TextButton(onClick = { viewModel.clearMessage() }) { Text("关闭") } },
            ) { Text(message) }
        }

        uiState.lastAddResult?.let { result ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action = { TextButton(onClick = { viewModel.clearMessage() }) { Text("关闭") } },
            ) { Text("添加 ${result.successCount} 项，跳过 ${result.skipCount} 项，失败 ${result.failures.size} 项") }
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
    thumbnail: @Composable () -> Unit,
) {
    androidx.compose.material3.ListItem(
        headlineContent = { Text(entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onToggleSelect() },
                )
            } else {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { thumbnail() }
            }
        },
        trailingContent = {
            if (!isMultiSelect && entry.isDirectory) {
                Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
    )
}

@Composable
private fun FileThumbnail(entry: FileEntry, viewModel: FileBrowserViewModel, modifier: Modifier = Modifier.fillMaxSize()) {
    val bitmap by produceState<android.graphics.Bitmap?>(null, entry.relativePath) { value = viewModel.loadThumbnail(entry) }
    if (bitmap != null) {
        Image(bitmap!!.asImageBitmap(), entry.name, modifier, contentScale = ContentScale.Crop)
    } else {
        Box(modifier.background(fileTypeColor(entry)), contentAlignment = Alignment.Center) {
            Icon(fileTypeIcon(entry), null, Modifier.size(32.dp), tint = Color.White.copy(alpha = 0.72f))
        }
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
    else -> "${"%.2f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
}

// ===== 面包屑导航 =====

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
        // 根目录
        Text(
            text = sourceName.ifBlank { "/" },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { onNavigateToRoot() }
                .padding(horizontal = 4.dp, vertical = 2.dp),
        )
        // 路径段
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
