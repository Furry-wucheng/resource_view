package dev.wucheng.resource_viewer.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.domain.model.Tag
import dev.wucheng.resource_viewer.ui.base.UiState
import dev.wucheng.resource_viewer.ui.components.EmptyState
import dev.wucheng.resource_viewer.ui.components.FilterBar
import dev.wucheng.resource_viewer.ui.components.ResourceGridItem
import dev.wucheng.resource_viewer.ui.theme.ThumbnailTokens
import org.koin.androidx.compose.koinViewModel

/**
 * 首页完整实现。
 * 显示资源缩略图网格，支持标签筛选。
 *
 * 注意：此实现遵循 doc/mvp/M23-home-grid.md 中的 M23.3 子任务。
 *
 * @param onNavigateToViewer 导航到查看器的回调
 * @param onNavigateToAddSource 导航到添加数据源的回调
 * @param modifier Modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToViewer: (String) -> Unit,
    onNavigateToAddSource: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val resources by viewModel.resources.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val selectedTagIds by viewModel.selectedTagIds.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreenContent(
        resources = resources,
        tags = tags,
        selectedTagIds = selectedTagIds,
        uiState = uiState,
        onTagClick = { tagId ->
            if (tagId == null) {
                viewModel.clearFilter()
            } else {
                viewModel.selectTag(tagId)
            }
        },
        onResourceClick = onNavigateToViewer,
        onAddSource = onNavigateToAddSource,
        onClearFilter = { viewModel.clearFilter() },
        modifier = modifier,
    )
}

/**
 * 首页内容（无状态版本，便于测试）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    resources: List<Resource>,
    tags: List<Tag>,
    selectedTagIds: Set<String>,
    uiState: UiState,
    onTagClick: (String?) -> Unit,
    onResourceClick: (String) -> Unit,
    onAddSource: () -> Unit,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("资源库") },
                windowInsets = WindowInsets(0.dp),
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            // 标签筛选栏
            if (tags.isNotEmpty()) {
                FilterBar(
                    tags = tags,
                    selectedTagIds = selectedTagIds,
                    onTagClick = onTagClick,
                )
            }

            // 内容区域
            when {
                uiState == UiState.LOADING -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("加载中...")
                    }
                }
                resources.isEmpty() && selectedTagIds.isEmpty() -> {
                    // 无资源，引导添加数据源
                    EmptyState(
                        hasResources = false,
                        onAddSource = onAddSource,
                    )
                }
                resources.isEmpty() && selectedTagIds.isNotEmpty() -> {
                    // 筛选结果为空
                    EmptyState(
                        hasResources = true,
                        isFiltered = true,
                        onClearFilter = onClearFilter,
                    )
                }
                else -> {
                    // 资源网格
                    ResourceGrid(
                        resources = resources,
                        onResourceClick = onResourceClick,
                    )
                }
            }
        }
    }
}

/**
 * 资源网格。
 * 使用 LazyVerticalGrid 显示缩略图卡片。
 */
@Composable
private fun ResourceGrid(
    resources: List<Resource>,
    onResourceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(ThumbnailTokens.GRID_ITEM_MIN_WIDTH),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(ThumbnailTokens.GRID_SPACING),
        horizontalArrangement = Arrangement.spacedBy(ThumbnailTokens.GRID_SPACING),
        verticalArrangement = Arrangement.spacedBy(ThumbnailTokens.GRID_SPACING),
    ) {
        items(
            items = resources,
            key = { it.id },
        ) { resource ->
            ResourceGridItem(
                resource = resource,
                onClick = { onResourceClick(resource.id) },
            )
        }
    }
}
