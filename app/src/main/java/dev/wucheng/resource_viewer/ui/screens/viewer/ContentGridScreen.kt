package dev.wucheng.resource_viewer.ui.screens.viewer

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.domain.model.FileEntry
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
                        GridEntryCard(entry = entry, viewModel = viewModel) {
                            if (entry.isDirectory) {
                                viewModel.openDirectory(entry.relativePath)
                            } else {
                                val images = state.entries.filter { !it.isDirectory }
                                val index = images.indexOfFirst { it.relativePath == entry.relativePath }
                                if (index >= 0) onOpenViewer(state.currentPath, index)
                            }
                        }
                    }
                }
                if (state.isLoading) CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error?.let { Text(it, modifier = Modifier.align(Alignment.Center)) }
            }
        }
    }
}

@Composable
private fun GridEntryCard(entry: FileEntry, viewModel: ContentGridViewModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (!entry.isDirectory) {
                val bitmap by produceState<Bitmap?>(null, entry.relativePath) {
                    value = viewModel.loadEntryThumbnail(entry)
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = entry.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color(0xFF757575)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color.White.copy(alpha = 0.72f),
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF1565C0)),
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

            // 底部渐变 + 标题
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))))
                    .padding(start = 6.dp, end = 6.dp, top = 40.dp, bottom = 8.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Text(
                    text = entry.name,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
