package dev.wucheng.resource_viewer.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.*
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
    onNavigateToViewer: (Resource) -> Unit,
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
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val isMultiSelect by viewModel.isMultiSelect.collectAsStateWithLifecycle()
    val selectedResourceIds by viewModel.selectedResourceIds.collectAsStateWithLifecycle()
    val hasMore by viewModel.hasMore.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()

    HomeScreenContent(
        resources = resources,
        tags = tags,
        selectedTagIds = selectedTagIds,
        uiState = uiState,
        detailResource = detailResource,
        allTags = tags,
        detailTagIds = detailTagIds,
        detailOrgMode = detailOrgMode,
        searchQuery = searchQuery,
        sort = sort,
        isMultiSelect = isMultiSelect,
        selectedResourceIds = selectedResourceIds,
        hasMore = hasMore,
        isLoadingMore = isLoadingMore,
        onTagClick = { tagId ->
            if (tagId == null) {
                viewModel.clearFilter()
            } else {
                viewModel.selectTag(tagId)
            }
        },
        onResourceClick = { resource ->
            if (isMultiSelect) viewModel.toggleResourceSelection(resource.id) else onNavigateToViewer(resource)
        },
        onResourceLongClick = { viewModel.openResourceDetail(it) },
        onAddSource = onNavigateToAddSource,
        onClearFilter = { viewModel.clearFilter() },
        onDetailTagToggle = { viewModel.toggleDetailTag(it) },
        onDetailOrgModeChange = { viewModel.setDetailOrgMode(it) },
        onDetailSave = { viewModel.saveResourceDetail() },
        onDetailDismiss = { viewModel.closeResourceDetail() },
        onSearchQueryChange = viewModel::setSearchQuery,
        onSortChange = viewModel::setSort,
        onEnterMultiSelect = viewModel::enterMultiSelectMode,
        onExitMultiSelect = viewModel::exitMultiSelectMode,
        onSelectAll = viewModel::toggleSelectAllVisible,
        onBatchDelete = viewModel::batchDeleteSelectedResources,
        onToggleFavorite = viewModel::toggleFavorite,
        onLoadMore = viewModel::loadMore,
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
    searchQuery: String,
    sort: HomeViewModel.ResourceSort,
    isMultiSelect: Boolean,
    selectedResourceIds: Set<String>,
    hasMore: Boolean = false,
    isLoadingMore: Boolean = false,
    onTagClick: (String?) -> Unit,
    onResourceClick: (Resource) -> Unit,
    onResourceLongClick: (Resource) -> Unit,
    onAddSource: () -> Unit,
    onClearFilter: () -> Unit,
    onDetailTagToggle: (String) -> Unit,
    onDetailOrgModeChange: (dev.wucheng.resource_viewer.data.local.converter.OrganizationMode) -> Unit,
    onDetailSave: () -> Unit,
    onDetailDismiss: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSortChange: (HomeViewModel.ResourceSort) -> Unit,
    onEnterMultiSelect: () -> Unit,
    onExitMultiSelect: () -> Unit,
    onSelectAll: () -> Unit,
    onBatchDelete: () -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit = { _, _ -> },
    onLoadMore: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var searchVisible by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searchVisible) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("搜索资源") },
                            singleLine = true,
                        )
                    } else Text(if (isMultiSelect) "已选 ${selectedResourceIds.size} 项" else "资源库")
                },
                navigationIcon = {
                    if (isMultiSelect) IconButton(onClick = onExitMultiSelect) {
                        Icon(Icons.Default.Close, contentDescription = "退出多选")
                    }
                },
                actions = {
                    if (isMultiSelect) {
                        IconButton(onClick = onSelectAll) { Icon(Icons.Default.Checklist, contentDescription = "全选") }
                    } else {
                        IconButton(onClick = {
                            searchVisible = !searchVisible
                            if (!searchVisible) onSearchQueryChange("")
                        }) { Icon(Icons.Default.Search, contentDescription = "搜索") }
                        IconButton(onClick = {
                            val values = HomeViewModel.ResourceSort.entries
                            onSortChange(values[(sort.ordinal + 1) % values.size])
                        }) { Icon(Icons.Default.Sort, contentDescription = "排序") }
                        IconButton(onClick = onEnterMultiSelect) { Icon(Icons.Default.Checklist, contentDescription = "多选") }
                    }
                },
            )
        },
        bottomBar = {
            if (isMultiSelect) Button(
                onClick = onBatchDelete,
                enabled = selectedResourceIds.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Text("批量删除")
            }
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
                        selectedResourceIds = selectedResourceIds,
                        onToggleFavorite = onToggleFavorite,
                        hasMore = hasMore,
                        isLoadingMore = isLoadingMore,
                        onLoadMore = onLoadMore,
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
 * 使用 LazyVerticalGrid 显示缩略图卡片，支持长按和分页加载。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ResourceGrid(
    resources: List<Resource>,
    onResourceClick: (Resource) -> Unit,
    onResourceLongClick: (Resource) -> Unit,
    selectedResourceIds: Set<String>,
    onToggleFavorite: (String, Boolean) -> Unit = { _, _ -> },
    hasMore: Boolean = false,
    isLoadingMore: Boolean = false,
    onLoadMore: () -> Unit = {},
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
                onClick = { onResourceClick(resource) },
                onLongClick = { onResourceLongClick(resource) },
                selected = resource.id in selectedResourceIds,
                onToggleFavorite = { onToggleFavorite(resource.id, !resource.favorited) },
            )
        }

        // 加载更多指示器
        if (hasMore) {
            item(key = "load_more") {
                LaunchedEffect(Unit) {
                    onLoadMore()
                }
                if (isLoadingMore) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }
            }
        }
    }
}
