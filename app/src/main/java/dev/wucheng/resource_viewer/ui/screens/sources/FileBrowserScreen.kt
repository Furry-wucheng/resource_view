package dev.wucheng.resource_viewer.ui.screens.sources

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
    modifier: Modifier = Modifier,
    viewModel: FileBrowserViewModel = koinViewModel { parametersOf(sourceId) },
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(sourceId) {
        viewModel.load()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.source?.name ?: "文件浏览")
                        Text(
                            text = uiState.currentPath.ifBlank { "/" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 目录树按钮
                    IconButton(onClick = { viewModel.toggleDirectoryTree() }) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "目录导航",
                            tint = if (uiState.showDirectoryTree) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    // 视图切换按钮
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            imageVector = if (uiState.viewMode == FileViewMode.LIST) Icons.Default.GridView else Icons.Default.List,
                            contentDescription = if (uiState.viewMode == FileViewMode.LIST) "网格视图" else "列表视图",
                        )
                    }
                    if (uiState.currentPath.isNotEmpty()) {
                        TextButton(onClick = { viewModel.goUp() }) {
                            Text("上级")
                        }
                    }
                },
            )
        },
        bottomBar = {
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
                    Text("添加入库")
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
                // Wide screen: persistent left panel
                Row(modifier = Modifier.fillMaxSize()) {
                    DirectoryTreePanel(
                        currentPath = uiState.currentPath,
                        onNavigateToSegment = { viewModel.navigateToPathSegment(it) },
                        onNavigateToRoot = { viewModel.openDirectory("") },
                        modifier = Modifier
                            .width(240.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    FileContentArea(
                        uiState = uiState,
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                // Narrow screen or tree hidden
                FileContentArea(
                    uiState = uiState,
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                )

                // Overlay directory tree on narrow screens
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
                            viewModel.navigateToPathSegment(it)
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
                    onConfirm = { orgMode, tagIds ->
                        viewModel.confirmBatchAdd(orgMode, tagIds)
                    },
                    onDismiss = { viewModel.hideBatchAddDialog() },
                )
            }

            // 文件预览覆盖层
            uiState.previewFile?.let { entry ->
                FilePreviewOverlay(
                    entry = entry,
                    onLoadBitmap = { viewModel.loadPreviewBitmap(it) },
                    onClose = { viewModel.closeFilePreview() },
                )
            }
        }
    }
}

@Composable
private fun FileEntryRow(
    entry: FileEntry,
    selected: Boolean,
    onOpen: () -> Unit,
    onToggleSelection: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(entry.name) },
        supportingContent = {
            Text(
                text = if (entry.isDirectory) "文件夹" else "${entry.extension.uppercase()} · ${entry.size} bytes",
                style = MaterialTheme.typography.bodySmall,
            )
        },
        leadingContent = {
            Icon(
                imageVector = if (entry.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                contentDescription = null,
            )
        },
        trailingContent = {
            IconButton(onClick = onToggleSelection) {
                Icon(
                    imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (selected) "取消选择" else "选择",
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        modifier = Modifier.clickable {
            if (entry.isDirectory) onOpen() else onToggleSelection()
        },
    )
}

/**
 * 网格视图中的文件/文件夹卡片。
 */
@Composable
private fun FileEntryGridItem(
    entry: FileEntry,
    selected: Boolean,
    onOpen: () -> Unit,
    onToggleSelection: () -> Unit,
) {
    Card(
        onClick = {
            if (entry.isDirectory) onOpen() else onToggleSelection()
        },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = when {
                        entry.isDirectory -> Icons.Default.Folder
                        entry.extension.lowercase() in setOf("mp4", "mkv", "avi", "mov", "webm") -> Icons.Default.PlayCircle
                        else -> Icons.AutoMirrored.Filled.InsertDriveFile
                    },
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = if (entry.isDirectory) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                // 选中指示器
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "已选中",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            // 文件名
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 文件预览覆盖层。
 * 全屏显示图片预览，支持双击缩放和拖动。
 */
@Composable
private fun FilePreviewOverlay(
    entry: FileEntry,
    onLoadBitmap: suspend (FileEntry) -> android.graphics.Bitmap,
    onClose: () -> Unit,
) {
    val ext = entry.extension.lowercase()
    val isImage = ext in setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (isImage) {
            // 图片预览
            val bitmapState by produceState<android.graphics.Bitmap?>(initialValue = null, entry) {
                value = try {
                    onLoadBitmap(entry)
                } catch (_: Exception) {
                    null
                }
            }

            val bitmap = bitmapState
            if (bitmap != null) {
                var scale by remember { mutableStateOf(1f) }
                var offsetX by remember { mutableStateOf(0f) }
                var offsetY by remember { mutableStateOf(0f) }

                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = entry.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, _, zoom ->
                                if (zoom != 1f) {
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                }
                                if (scale > 1f) {
                                    val maxX = (scale - 1f) * size.width / 2f
                                    val maxY = (scale - 1f) * size.height / 2f
                                    offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                                    offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                                }
                            }
                        },
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                )
            }
        } else {
            // 非图片文件：显示文件信息
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = when {
                        ext in setOf("mp4", "mkv", "avi", "mov", "webm") -> Icons.Default.PlayCircle
                        else -> Icons.AutoMirrored.Filled.InsertDriveFile
                    },
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.White,
                )
                Text(
                    text = entry.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "此文件类型暂不支持预览",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        // 关闭按钮
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    top = WindowInsets.statusBars
                        .asPaddingValues()
                        .calculateTopPadding(),
                ),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭预览",
                tint = Color.White,
            )
        }

        // 文件名
        Text(
            text = entry.name,
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
        )
    }
}

@Composable
private fun FileContentArea(
    uiState: FileBrowserUiState,
    viewModel: FileBrowserViewModel,
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
                            selected = entry.relativePath in uiState.selectedPaths,
                            onOpen = {
                                if (entry.isDirectory) {
                                    viewModel.openDirectory(entry.relativePath)
                                } else if (viewModel.isPreviewable(entry)) {
                                    viewModel.openFilePreview(entry)
                                }
                            },
                            onToggleSelection = { viewModel.toggleSelection(entry.relativePath) },
                        )
                    }
                }
            }
            FileViewMode.GRID -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.entries, key = { it.relativePath }) { entry ->
                        FileEntryGridItem(
                            entry = entry,
                            selected = entry.relativePath in uiState.selectedPaths,
                            onOpen = {
                                if (entry.isDirectory) {
                                    viewModel.openDirectory(entry.relativePath)
                                } else if (viewModel.isPreviewable(entry)) {
                                    viewModel.openFilePreview(entry)
                                }
                            },
                            onToggleSelection = { viewModel.toggleSelection(entry.relativePath) },
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
                    TextButton(onClick = { viewModel.clearMessage() }) {
                        Text("关闭")
                    }
                },
            ) {
                Text(message)
            }
        }

        uiState.lastAddResult?.let { result ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearMessage() }) {
                        Text("关闭")
                    }
                },
            ) {
                Text("添加 ${result.successCount} 项，跳过 ${result.skipCount} 项，失败 ${result.failures.size} 项")
            }
        }
    }
}

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

        // Root
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

        // Path segments
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
