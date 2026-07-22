package dev.wucheng.resource_viewer.ui.screens.viewer

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.domain.model.Chapter
import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.ui.components.FileThumbnailCard
import dev.wucheng.resource_viewer.ui.components.GRID_CARD_TITLE_MAX_LINES
import dev.wucheng.resource_viewer.ui.screens.viewer.components.OrgModeSwitcher
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * 章节列表页面。
 * 显示 CHAPTER/CHAPTER_GALLERY 模式资源的章节列表。
 * 每项显示章节名 + 封面缩略图 + 图片数量。
 *
 * 注意：此实现遵循 doc/mvp/M21-chapter-strategies.md 中的 M21.3 子任务。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterListScreen(
    resourceId: String,
    onNavigateBack: () -> Unit,
    onNavigateToViewer: (String, String) -> Unit,
    onOpenVideo: (sourceId: String, filePath: String) -> Unit = { _, _ -> },
    onNavigateToMode: (OrganizationMode) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ChapterListViewModel = koinViewModel { parametersOf(resourceId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 加载章节
    androidx.compose.runtime.LaunchedEffect(resourceId) {
        viewModel.loadChapters()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (val state = uiState) {
                            is ChapterListUiState.Success -> state.resourceName
                            else -> "章节列表"
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                actions = {
                    // 视图切换按钮（仅 Success 状态显示）
                    val currentState = uiState
                    if (currentState is ChapterListUiState.Success) {
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                imageVector = if (currentState.viewMode == ChapterViewMode.LIST) Icons.Default.GridView else Icons.Default.List,
                                contentDescription = if (currentState.viewMode == ChapterViewMode.LIST) "网格视图" else "列表视图",
                            )
                        }
                    }
                },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (val state = uiState) {
                is ChapterListUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                is ChapterListUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "点击重试",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                viewModel.loadChapters()
                            },
                        )
                    }
                }

                is ChapterListUiState.Success -> {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val isWide = maxWidth >= 900.dp

                        if (isWide) {
                            // 宽屏布局：左侧封面面板 + 右侧章节列表
                            WideChapterLayout(
                                state = state,
                                resourceId = resourceId,
                                onNavigateToViewer = onNavigateToViewer,
                                onChangeOrgMode = { mode ->
                                    viewModel.changeOrganizationMode(mode)
                                    onNavigateToMode(mode)
                                },
                                onToggleViewMode = { viewModel.toggleViewMode() },
                                onOpenVideo = { srcId, path -> onOpenVideo(srcId, path) },
                                viewModel = viewModel,
                            )
                        } else {
                            // 窄屏布局：标准纵向
                            NarrowChapterLayout(
                                state = state,
                                resourceId = resourceId,
                                onNavigateToViewer = onNavigateToViewer,
                                onChangeOrgMode = { mode ->
                                    viewModel.changeOrganizationMode(mode)
                                    onNavigateToMode(mode)
                                },
                                onOpenVideo = { srcId, path -> onOpenVideo(srcId, path) },
                                viewModel = viewModel,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 窄屏布局（标准纵向）。
 */
@Composable
private fun NarrowChapterLayout(
    state: ChapterListUiState.Success,
    resourceId: String,
    onNavigateToViewer: (String, String) -> Unit,
    onChangeOrgMode: (OrganizationMode) -> Unit,
    onOpenVideo: (String, String) -> Unit,
    viewModel: ChapterListViewModel,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OrgModeSwitcher(
            currentMode = state.organizationMode,
            onModeChanged = onChangeOrgMode,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        if (state.chapters.isEmpty() && state.looseFiles.isEmpty()) {
            EmptyChapterState()
        } else {
            ChapterContent(
                state = state,
                resourceId = resourceId,
                onNavigateToViewer = onNavigateToViewer,
                onOpenVideo = onOpenVideo,
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * 宽屏布局（≥900dp）：左侧封面面板 + 右侧章节列表。
 */
@Composable
private fun WideChapterLayout(
    state: ChapterListUiState.Success,
    resourceId: String,
    onNavigateToViewer: (String, String) -> Unit,
    onChangeOrgMode: (OrganizationMode) -> Unit,
    onToggleViewMode: () -> Unit,
    onOpenVideo: (String, String) -> Unit,
    viewModel: ChapterListViewModel,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // 左侧封面面板
        Column(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 封面缩略图（取第一个章节的封面）
            val coverPath = state.chapters.firstOrNull()?.coverPath
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                if (coverPath != null) {
                    ChapterCoverImage(
                        coverPath = coverPath,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 资源名称
            Text(
                text = state.resourceName,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // 章节/文件统计
            Text(
                text = "${state.chapters.size} 章 · ${state.looseFiles.size} 散落文件",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // 组织模式切换器
            OrgModeSwitcher(
                currentMode = state.organizationMode,
                onModeChanged = onChangeOrgMode,
            )

            // 视图切换按钮
            IconButton(onClick = onToggleViewMode) {
                Icon(
                    imageVector = if (state.viewMode == ChapterViewMode.LIST) Icons.Default.GridView else Icons.Default.List,
                    contentDescription = if (state.viewMode == ChapterViewMode.LIST) "网格视图" else "列表视图",
                )
            }
        }

        // 右侧章节列表
        if (state.chapters.isEmpty() && state.looseFiles.isEmpty()) {
            Box(modifier = Modifier.weight(1f)) {
                EmptyChapterState()
            }
        } else {
            ChapterContent(
                state = state,
                resourceId = resourceId,
                onNavigateToViewer = onNavigateToViewer,
                onOpenVideo = onOpenVideo,
                viewModel = viewModel,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * 空章节状态。
 */
@Composable
private fun EmptyChapterState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "暂无章节",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 章节内容区域（列表/网格）。
 */
@Composable
private fun ChapterContent(
    state: ChapterListUiState.Success,
    resourceId: String,
    onNavigateToViewer: (String, String) -> Unit,
    onOpenVideo: (String, String) -> Unit,
    viewModel: ChapterListViewModel,
    modifier: Modifier = Modifier,
) {
    when (state.viewMode) {
        ChapterViewMode.LIST -> {
            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = state.chapters,
                    key = { "chapter_${it.relativePath}" },
                ) { chapter ->
                    ChapterItem(
                        chapter = chapter,
                        onClick = { onNavigateToViewer(resourceId, chapter.relativePath) },
                        viewModel = viewModel,
                    )
                }

                if (state.looseFiles.isNotEmpty()) {
                    item(key = "loose_header") {
                        Text(
                            text = "散落文件",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(
                        items = state.looseFiles,
                        key = { "loose_${it.relativePath}" },
                    ) { file ->
                        LooseFileItem(
                            file = file,
                            viewModel = viewModel,
                            onClick = {
                                onOpenVideo(state.sourceId, file.relativePath)
                            },
                        )
                    }
                }
            }
        }
        ChapterViewMode.GRID -> {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                modifier = modifier,
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = state.chapters,
                    key = { "chapter_${it.relativePath}" },
                ) { chapter ->
                    ChapterGridItem(
                        chapter = chapter,
                        onClick = { onNavigateToViewer(resourceId, chapter.relativePath) },
                        viewModel = viewModel,
                    )
                }

                if (state.looseFiles.isNotEmpty()) {
                    item(key = "loose_header") {
                        Text(
                            text = "散落文件",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    items(
                        items = state.looseFiles,
                        key = { "loose_${it.relativePath}" },
                    ) { file ->
                        FileThumbnailCard(
                            entry = file,
                            loadThumbnail = { viewModel.loadLooseFileThumbnail(it) },
                            onClick = {
                                onOpenVideo(state.sourceId, file.relativePath)
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 章节列表项。
 * 显示章节名 + 封面缩略图 + 图片数量。
 */
@Composable
private fun ChapterItem(
    chapter: Chapter,
    onClick: () -> Unit,
    viewModel: ChapterListViewModel,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 封面缩略图
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (chapter.coverPath != null) {
                    ChapterCoverImage(
                        coverPath = chapter.coverPath,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 章节信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = chapter.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${chapter.fileCount} 张图片",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * 散落文件列表项。
 * 显示文件名 + 内容缩略图。
 */
@Composable
private fun LooseFileItem(
    file: FileEntry,
    viewModel: ChapterListViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LooseFileThumbnail(
                file = file,
                viewModel = viewModel,
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small),
            )

            // 文件信息
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = file.extension.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * 章节网格项。
 * 显示封面缩略图 + 章节名 + 图片数量。
 */
@Composable
private fun ChapterGridItem(
    chapter: Chapter,
    onClick: () -> Unit,
    viewModel: ChapterListViewModel,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            // 封面缩略图
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (chapter.coverPath != null) {
                    ChapterCoverImage(
                        coverPath = chapter.coverPath,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 章节信息
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = chapter.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = GRID_CARD_TITLE_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${chapter.fileCount} 张",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LooseFileThumbnail(
    file: FileEntry,
    viewModel: ChapterListViewModel,
    modifier: Modifier = Modifier,
) {
    val bitmap by produceState<Bitmap?>(null, file.relativePath) {
        value = viewModel.loadLooseFileThumbnail(file)
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 章节封面缩略图组件。
 * 通过 FileEntryThumbnailLoader 从 FileSource 加载，有缓存 fallback 图标。
 */
@Composable
private fun ChapterCoverImage(
    coverPath: String,
    viewModel: ChapterListViewModel,
    modifier: Modifier = Modifier,
) {
    val bitmap by produceState<Bitmap?>(null, coverPath) {
        value = viewModel.loadChapterCover(coverPath)
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFF1565C0)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color.White.copy(alpha = 0.72f),
            )
        }
    }
}
