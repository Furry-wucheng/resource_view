package dev.wucheng.resource_viewer.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import dev.wucheng.resource_viewer.ui.components.ResourceDetailSheet
import dev.wucheng.resource_viewer.ui.components.ResourceGridItem
import dev.wucheng.resource_viewer.ui.theme.ThumbnailTokens
import org.koin.androidx.compose.koinViewModel

/**
 * 首页完整实现。
 * 显示资源缩略图网格，支持标签筛选。
 * 长按资源可打开详情弹窗编辑标签和组织模式。
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
    val detailResource by viewModel.detailResource.collectAsStateWithLifecycle()
    val detailTagIds by viewModel.detailTagIds.collectAsStateWithLifecycle()
    val detailOrgMode by viewModel.detailOrgMode.collectAsStateWithLifecycle()

    HomeScreenContent(
        resources = resources,
        tags = tags,
        selectedTagIds = selectedTagIds,
        uiState = uiState,
        detailResource = detailResource,
        allTags = tags,
        detailTagIds = detailTagIds,
        detailOrgMode = detailOrgMode,
        onTagClick = { tagId ->
            if (tagId == null) {
                viewModel.clearFilter()
            } else {
                viewModel.selectTag(tagId)
            }
        },
        onResourceClick = onNavigateToViewer,
        onResourceLongClick = { viewModel.openResourceDetail(it) },
        onAddSource = onNavigateToAddSource,
        onClearFilter = { viewModel.clearFilter() },
        onDetailTagToggle = { viewModel.toggleDetailTag(it) },
        onDetailOrgModeChange = { viewModel.setDetailOrgMode(it) },
        onDetailSave = { viewModel.saveResourceDetail() },
        onDetailDismiss = { viewModel.closeResourceDetail() },
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
    detailResource: Resource?,
    allTags: List<Tag>,
    detailTagIds: Set<String>,
    detailOrgMode: dev.wucheng.resource_viewer.data.local.converter.OrganizationMode,
    onTagClick: (String?) -> Unit,
    onResourceClick: (String) -> Unit,
    onResourceLongClick: (Resource) -> Unit,
    onAddSource: () -> Unit,
    onClearFilter: () -> Unit,
    onDetailTagToggle: (String) -> Unit,
    onDetailOrgModeChange: (dev.wucheng.resource_viewer.data.local.converter.OrganizationMode) -> Unit,
    onDetailSave: () -> Unit,
    onDetailDismiss: () -> Unit,
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
                    EmptyState(
                        hasResources = false,
                        onAddSource = onAddSource,
                    )
                }
                resources.isEmpty() && selectedTagIds.isNotEmpty() -> {
                    EmptyState(
                        hasResources = true,
                        isFiltered = true,
                        onClearFilter = onClearFilter,
                    )
                }
                else -> {
                    ResourceGrid(
                        resources = resources,
                        onResourceClick = onResourceClick,
                        onResourceLongClick = onResourceLongClick,
                    )
                }
            }
        }
    }

    // 资源详情弹窗
    detailResource?.let { resource ->
        ResourceDetailSheet(
            resource = resource,
            allTags = allTags,
            selectedTagIds = detailTagIds,
            selectedOrgMode = detailOrgMode,
            onTagToggle = onDetailTagToggle,
            onOrgModeChange = onDetailOrgModeChange,
            onSave = onDetailSave,
            onDismiss = onDetailDismiss,
        )
    }
}

/**
 * 资源网格。
 * 使用 LazyVerticalGrid 显示缩略图卡片，支持长按。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ResourceGrid(
    resources: List<Resource>,
    onResourceClick: (String) -> Unit,
    onResourceLongClick: (Resource) -> Unit,
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
                onLongClick = { onResourceLongClick(resource) },
            )
        }
    }
}
