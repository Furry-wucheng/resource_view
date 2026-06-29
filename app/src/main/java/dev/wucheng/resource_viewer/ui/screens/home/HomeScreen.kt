package dev.wucheng.resource_viewer.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import dev.wucheng.resource_viewer.ui.components.ResourcePickerDialog
import dev.wucheng.resource_viewer.ui.components.ResourcePickerMode
import dev.wucheng.resource_viewer.ui.components.ResourcePickerViewModel
import dev.wucheng.resource_viewer.ui.theme.ThumbnailTokens
import org.koin.androidx.compose.koinViewModel

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

    val isSplitDialogVisible by viewModel.isSplitDialogVisible.collectAsStateWithLifecycle()
    val splitDialogResource by viewModel.splitDialogResource.collectAsStateWithLifecycle()
    val showSplitTagsDialog by viewModel.showSplitTagsDialog.collectAsStateWithLifecycle()
    val splitResultMessage by viewModel.splitResultMessage.collectAsStateWithLifecycle()
    val showBatchTagDialog by viewModel.showBatchTagDialog.collectAsStateWithLifecycle()
    val batchTagSelectedIds by viewModel.batchTagSelectedIds.collectAsStateWithLifecycle()

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
        isSplitDialogVisible = isSplitDialogVisible,
        splitDialogResource = splitDialogResource,
        showSplitTagsDialog = showSplitTagsDialog,
        splitResultMessage = splitResultMessage,
        showBatchTagDialog = showBatchTagDialog,
        batchTagSelectedIds = batchTagSelectedIds,
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
        onDetailDelete = { viewModel.deleteDetailResource() },
        onDetailCreateTag = { name, onCreated -> viewModel.createTag(name, onCreated) },
        onSearchQueryChange = viewModel::setSearchQuery,
        onSortChange = viewModel::setSort,
        onEnterMultiSelect = viewModel::enterMultiSelectMode,
        onExitMultiSelect = viewModel::exitMultiSelectMode,
        onSelectAll = viewModel::toggleSelectAllVisible,
        onBatchDelete = viewModel::batchDeleteSelectedResources,
        onBatchTag = viewModel::openBatchTagDialog,
        onBatchTagDialogTagToggle = viewModel::toggleBatchTag,
        onBatchTagDialogConfirm = viewModel::batchAddTags,
        onBatchTagDialogCreateTag = { name, onCreated -> viewModel.createTag(name, onCreated) },
        onBatchTagDialogDismiss = viewModel::hideBatchTagDialog,
        onToggleFavorite = viewModel::toggleFavorite,
        onLoadMore = viewModel::loadMore,
        onNavigateToViewer = onNavigateToViewer,
        onSplitResource = { viewModel.initiateResourceSplit(it) },
        onDismissSplitDialog = { viewModel.dismissSplitDialog() },
        onSplitEntriesSelected = { entries, deleteOriginal -> viewModel.onSplitEntriesSelected(entries, deleteOriginal) },
        onSplitTagsCancelled = { viewModel.onSplitTagsCancelled() },
        onExecuteSplit = { orgMode, tagIds -> viewModel.executeSplit(orgMode, tagIds) },
        onCreateTag = { name, onCreated -> viewModel.createTag(name, onCreated) },
        onDismissSplitResult = { viewModel.dismissSplitResult() },
        modifier = modifier,
    )
}

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
    isSplitDialogVisible: Boolean = false,
    splitDialogResource: Resource? = null,
    showSplitTagsDialog: Boolean = false,
    splitResultMessage: String? = null,
    showBatchTagDialog: Boolean = false,
    batchTagSelectedIds: Set<String> = emptySet(),
    onTagClick: (String?) -> Unit,
    onResourceClick: (Resource) -> Unit,
    onResourceLongClick: (Resource) -> Unit,
    onAddSource: () -> Unit,
    onClearFilter: () -> Unit,
    onDetailTagToggle: (String) -> Unit,
    onDetailOrgModeChange: (dev.wucheng.resource_viewer.data.local.converter.OrganizationMode) -> Unit,
    onDetailSave: () -> Unit,
    onDetailDismiss: () -> Unit,
    onDetailDelete: () -> Unit = {},
    onDetailCreateTag: (String, (String) -> Unit) -> Unit = { _, _ -> },
    onSearchQueryChange: (String) -> Unit,
    onSortChange: (HomeViewModel.ResourceSort) -> Unit,
    onEnterMultiSelect: () -> Unit,
    onExitMultiSelect: () -> Unit,
    onSelectAll: () -> Unit,
    onBatchDelete: () -> Unit,
    onBatchTag: () -> Unit = {},
    onBatchTagDialogTagToggle: (String) -> Unit = {},
    onBatchTagDialogConfirm: () -> Unit = {},
    onBatchTagDialogCreateTag: (String, (String) -> Unit) -> Unit = { _, _ -> },
    onBatchTagDialogDismiss: () -> Unit = {},
    onToggleFavorite: (String, Boolean) -> Unit = { _, _ -> },
    onLoadMore: () -> Unit = {},
    onNavigateToViewer: (Resource) -> Unit = {},
    onSplitResource: (Resource) -> Unit = {},
    onDismissSplitDialog: () -> Unit = {},
    onSplitEntriesSelected: (List<dev.wucheng.resource_viewer.domain.model.FileEntry>, Boolean) -> Unit = { _, _ -> },
    onSplitTagsCancelled: () -> Unit = {},
    onExecuteSplit: (dev.wucheng.resource_viewer.data.local.converter.OrganizationMode?, List<String>) -> Unit = { _, _ -> },
    onCreateTag: (String, (String) -> Unit) -> Unit = { _, _ -> },
    onDismissSplitResult: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var searchVisible by remember { mutableStateOf(false) }

    val pickerViewModel: ResourcePickerViewModel? = if (isSplitDialogVisible && splitDialogResource != null) {
        koinViewModel<ResourcePickerViewModel>()
    } else {
        null
    }

    LaunchedEffect(splitDialogResource) {
        splitDialogResource?.let { res ->
            pickerViewModel?.loadTree(res.sourceId, res.relativePath)
        }
    }

    LaunchedEffect(splitResultMessage) {
        splitResultMessage?.let {
            kotlinx.coroutines.delay(3000)
            onDismissSplitResult()
        }
    }

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

                        var expandedSort by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { expandedSort = true }) {
                                Icon(Icons.Default.Sort, contentDescription = "排序")
                            }
                            DropdownMenu(
                                expanded = expandedSort,
                                onDismissRequest = { expandedSort = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("按添加时间 ↑") },
                                    onClick = { onSortChange(HomeViewModel.ResourceSort.ADDED_ASC); expandedSort = false },
                                )
                                DropdownMenuItem(
                                    text = { Text("按添加时间 ↓") },
                                    onClick = { onSortChange(HomeViewModel.ResourceSort.ADDED_DESC); expandedSort = false },
                                )
                                DropdownMenuItem(
                                    text = { Text("按名称 A-Z") },
                                    onClick = { onSortChange(HomeViewModel.ResourceSort.NAME_ASC); expandedSort = false },
                                )
                                DropdownMenuItem(
                                    text = { Text("按名称 Z-A") },
                                    onClick = { onSortChange(HomeViewModel.ResourceSort.NAME_DESC); expandedSort = false },
                                )
                            }
                        }

                        IconButton(onClick = onEnterMultiSelect) { Icon(Icons.Default.Checklist, contentDescription = "多选") }
                    }
                },
            )
        },
        bottomBar = {
            if (isMultiSelect) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = onBatchTag,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("批量添加标签")
                    }
                    Button(
                        onClick = onBatchDelete,
                        enabled = selectedResourceIds.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Text("批量删除")
                    }
                }
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            if (tags.isNotEmpty()) {
                FilterBar(
                    tags = tags,
                    selectedTagIds = selectedTagIds,
                    onTagClick = onTagClick,
                )
            }

            when {
                uiState == UiState.LOADING -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("加载中...")
                    }
                }
                resources.isEmpty() && selectedTagIds.isEmpty() -> {
                    EmptyState(hasResources = false, onAddSource = onAddSource)
                }
                resources.isEmpty() && selectedTagIds.isNotEmpty() -> {
                    EmptyState(hasResources = true, isFiltered = true, onClearFilter = onClearFilter)
                }
                else -> {
                    Box(modifier = Modifier.weight(1f)) {
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

                    splitResultMessage?.let { msg ->
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = msg,
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }

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
            onSplitResource = onSplitResource,
            onDelete = { onDetailDelete() },
            onCreateTag = onDetailCreateTag,
        )
    }

    if (isSplitDialogVisible && splitDialogResource != null && pickerViewModel != null) {
        val pickerTreeNodes by pickerViewModel.treeNodes.collectAsState()
        val pickerUiState by pickerViewModel.uiState.collectAsState()
        val pickerSelectedCount by pickerViewModel.selectedCount.collectAsState()
        val pickerRootName by pickerViewModel.rootName.collectAsState()

        ResourcePickerDialog(
            rootName = pickerRootName.ifBlank { splitDialogResource.name },
            treeNodes = pickerTreeNodes,
            selectedCount = pickerSelectedCount,
            uiState = pickerUiState,
            mode = ResourcePickerMode.SPLIT_KEEP,
            onToggleExpand = { pickerViewModel.toggleExpand(it) },
            onToggleCheck = { pickerViewModel.toggleCheck(it) },
            onSelectAllChildren = { pickerViewModel.selectAllChildren(it) },
            onSelectAllRoot = { pickerViewModel.selectAllRootNodes() },
            onConfirm = {
                val entries = pickerViewModel.getSelectedEntries()
                if (entries.isNotEmpty()) onSplitEntriesSelected(entries, false)
            },
            onConfirmDelete = {
                val entries = pickerViewModel.getSelectedEntries()
                if (entries.isNotEmpty()) onSplitEntriesSelected(entries, true)
            },
            onDismiss = onDismissSplitDialog,
        )
    }

    if (showBatchTagDialog) {
        BatchTagDialog(
            tags = tags,
            selectedTagIds = batchTagSelectedIds,
            onTagToggle = onBatchTagDialogTagToggle,
            onConfirm = onBatchTagDialogConfirm,
            onCreateTag = onBatchTagDialogCreateTag,
            onDismiss = onBatchTagDialogDismiss,
        )
    }

    if (showSplitTagsDialog) {
        dev.wucheng.resource_viewer.ui.screens.sources.BatchAddResourcesDialog(
            selectedCount = splitDialogResource?.let { 1 } ?: 0,
            allTags = tags,
            onConfirm = { orgMode, tagIds -> onExecuteSplit(orgMode, tagIds) },
            onCreateTag = { name, onCreated -> onCreateTag(name, onCreated) },
            onDismiss = onSplitTagsCancelled,
        )
    }
}

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
        items(items = resources, key = { it.id }) { resource ->
            ResourceGridItem(
                resource = resource,
                onClick = { onResourceClick(resource) },
                onLongClick = { onResourceLongClick(resource) },
                selected = resource.id in selectedResourceIds,
                onToggleFavorite = { onToggleFavorite(resource.id, !resource.favorited) },
            )
        }

        if (hasMore) {
            item(key = "load_more") {
                LaunchedEffect(Unit) { onLoadMore() }
                if (isLoadingMore) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchTagDialog(
    tags: List<Tag>,
    selectedTagIds: Set<String>,
    onTagToggle: (String) -> Unit,
    onConfirm: () -> Unit,
    onCreateTag: (String, (String) -> Unit) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
) {
    var newTagName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("批量添加标签") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (tags.isEmpty()) {
                    Text("暂无标签", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(tags) { tag ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = tag.id in selectedTagIds,
                                    onCheckedChange = { onTagToggle(tag.id) },
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(tag.name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = newTagName,
                        onValueChange = { newTagName = it.take(20) },
                        label = { Text("新建标签") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        enabled = newTagName.isNotBlank(),
                        onClick = {
                            onCreateTag(newTagName) { id -> onTagToggle(id) }
                            newTagName = ""
                        },
                    ) { Text("创建") }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = selectedTagIds.isNotEmpty()) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
