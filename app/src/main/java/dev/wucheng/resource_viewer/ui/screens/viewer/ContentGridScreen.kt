package dev.wucheng.resource_viewer.ui.screens.viewer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.shared.media.MediaFormats
import dev.wucheng.resource_viewer.ui.components.FileThumbnailCard
import dev.wucheng.resource_viewer.ui.screens.viewer.components.OrgModeSwitcher
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentGridScreen(
    resourceId: String,
    mode: ContentGridMode,
    onNavigateBack: () -> Unit,
    onOpenViewer: (contentPath: String, initialPage: Int) -> Unit,
    onOpenVideo: (sourceId: String, filePath: String) -> Unit = { _, _ -> },
    onNavigateToMode: (OrganizationMode) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ContentGridViewModel = koinViewModel { parametersOf(resourceId, mode) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(resourceId, mode) { viewModel.load() }
    BackHandler {
        if (!viewModel.goUp()) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.title.ifBlank { if (mode == ContentGridMode.GALLERY) "画廊" else "平铺网格" })
                        if (mode == ContentGridMode.FLAT_GRID) {
                            Text(state.currentPath, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (!viewModel.goUp()) onNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = { Text("${state.entries.count { !it.isDirectory }} 项", modifier = Modifier.padding(16.dp)) },
            )
        },
        modifier = modifier,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OrgModeSwitcher(
                currentMode = state.organizationMode,
                onModeChanged = { m ->
                    viewModel.changeOrganizationMode(m)
                    onNavigateToMode(m)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            Box(Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(128.dp),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.entries, key = { it.relativePath }) { entry ->
                        FileThumbnailCard(
                            entry = entry,
                            loadThumbnail = { viewModel.loadEntryThumbnail(it) },
                            onClick = {
                                if (entry.isDirectory) {
                                    viewModel.openDirectory(entry.relativePath)
                                } else if (MediaFormats.isVideo(entry.extension)) {
                                    onOpenVideo(state.sourceId, entry.relativePath)
                                } else {
                                    val images = state.entries.filter { !it.isDirectory }
                                    val index = images.indexOfFirst { it.relativePath == entry.relativePath }
                                    if (index >= 0) onOpenViewer(state.currentPath, index)
                                }
                            },
                        )
                    }
                }
                if (state.isLoading) CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error?.let { Text(it, modifier = Modifier.align(Alignment.Center)) }
            }
        }
    }
}
