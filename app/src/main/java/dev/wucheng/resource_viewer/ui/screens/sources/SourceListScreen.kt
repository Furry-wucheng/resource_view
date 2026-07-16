package dev.wucheng.resource_viewer.ui.screens.sources

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.wucheng.resource_viewer.domain.model.Source
import dev.wucheng.resource_viewer.ui.components.EmptyState
import dev.wucheng.resource_viewer.ui.components.useCompactSourceActions
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceListScreen(
    onNavigateToBrowser: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SourceListViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (_: SecurityException) {
                // Some providers grant transient access only; keep the URI so the user can still proceed.
            }
            viewModel.updateLocalForm(rootPath = uri.toString())
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadSources()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据源") },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showSourceTypePicker() },
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加数据源")
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            if (uiState.sources.isEmpty() && !uiState.isLoading) {
                EmptyState(
                    hasResources = false,
                    onAddSource = { viewModel.showSourceTypePicker() },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.sources) { source ->
                        SourceCard(
                            source = source,
                            resourceCount = uiState.resourceCounts[source.id] ?: 0,
                            onToggleEnabled = { viewModel.toggleSourceEnabled(source) },
                            onRename = { viewModel.showRenameDialog(source) },
                            onEditSmb = if (source.type == dev.wucheng.resource_viewer.data.local.converter.SourceType.SMB) {
                                { viewModel.showEditSmbDialog(source) }
                            } else null,
                            onDelete = { viewModel.showDeleteConfirmDialog(source) },
                            onClick = { onNavigateToBrowser(source.id) },
                        )
                    }
                }
            }

            // 加载指示器
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }

    // 错误提示
    uiState.error?.let { error ->
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("关闭")
                }
            },
        ) {
            Text(error)
        }
    }

    // 数据源类型选择弹窗
    if (uiState.showSourceTypePicker) {
        AlertDialog(
            onDismissRequest = { viewModel.hideSourceTypePicker() },
            title = { Text("选择数据源类型") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { viewModel.showAddLocalDialog() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("📁 本地文件夹")
                    }
                    TextButton(
                        onClick = { viewModel.showAddSmbDialog() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("🌐 SMB 网络共享")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.hideSourceTypePicker() }) {
                    Text("取消")
                }
            },
        )
    }

    // 本地添加弹窗
    if (uiState.showAddLocalDialog) {
        AddLocalDialog(
            formData = uiState.localForm,
            onNameChange = { name -> viewModel.updateLocalForm(name = name) },
            onPickFolder = { folderPickerLauncher.launch(null) },
            onConfirm = { viewModel.addLocalSource() },
            onDismiss = { viewModel.hideAddLocalDialog() },
        )
    }

    if (uiState.showAddSmbDialog) {
        AddSmbDialog(
            formData = uiState.smbForm,
            isTestingConnection = uiState.isTestingConnection,
            testConnectionSuccess = uiState.testConnectionSuccess,
            testConnectionError = uiState.testConnectionError,
            onFormChange = { name, host, port, username, password, domain, shareName ->
                viewModel.updateSmbForm(name, host, port, username, password, domain, shareName)
            },
            onTestConnection = { viewModel.testSmbConnection() },
            onConfirm = { viewModel.addSmbSource() },
            onDismiss = { viewModel.hideAddSmbDialog() },
        )
    }

    // 编辑 SMB 源对话框
    if (uiState.showEditSmbDialog) {
        AddSmbDialog(
            formData = uiState.smbForm,
            isTestingConnection = uiState.isTestingConnection,
            testConnectionSuccess = uiState.testConnectionSuccess,
            testConnectionError = uiState.testConnectionError,
            onFormChange = { name, host, port, username, password, domain, shareName ->
                viewModel.updateSmbForm(name, host, port, username, password, domain, shareName)
            },
            onTestConnection = { viewModel.testSmbConnection() },
            onConfirm = { viewModel.updateSmbSource() },
            onDismiss = { viewModel.hideEditSmbDialog() },
            title = "编辑 SMB 凭据",
            confirmText = "保存",
        )
    }

    // 重命名对话框
    if (uiState.showRenameDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideRenameDialog() },
            title = { Text("重命名数据源") },
            text = {
                OutlinedTextField(
                    value = uiState.renameName,
                    onValueChange = { viewModel.updateRenameName(it) },
                    label = { Text("名称") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.renameSource() }) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideRenameDialog() }) {
                    Text("取消")
                }
            },
        )
    }

    // 删除确认对话框
    if (uiState.showDeleteConfirmDialog) {
        val sourceToDelete = uiState.sourceToDelete
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirmDialog() },
            title = { Text("删除数据源") },
            text = {
                Text(
                    if (sourceToDelete != null) {
                        "确定要删除数据源「${sourceToDelete.name}」吗？该源下的所有资源将被移除。"
                    } else {
                        "确定要删除该数据源吗？"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDeleteSource() },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteConfirmDialog() }) {
                    Text("取消")
                }
            },
        )
    }
}

/**
 * 数据源卡片组件。
 */
@Composable
private fun SourceCard(
    source: Source,
    resourceCount: Int = 0,
    onToggleEnabled: () -> Unit,
    onRename: () -> Unit,
    onEditSmb: (() -> Unit)? = null,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val compactActions = useCompactSourceActions(maxWidth.value)
        var showActionsMenu by remember { mutableStateOf(false) }
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 图标
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = when (source.type) {
                    dev.wucheng.resource_viewer.data.local.converter.SourceType.LOCAL ->
                        MaterialTheme.colorScheme.primaryContainer
                    dev.wucheng.resource_viewer.data.local.converter.SourceType.SMB ->
                        MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = when (source.type) {
                            dev.wucheng.resource_viewer.data.local.converter.SourceType.LOCAL -> "📁"
                            dev.wucheng.resource_viewer.data.local.converter.SourceType.SMB -> "🌐"
                            else -> "📄"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            }

            // 信息
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = source.rootPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = when (source.type) {
                        dev.wucheng.resource_viewer.data.local.converter.SourceType.LOCAL -> "本地 · $resourceCount 个资源"
                        dev.wucheng.resource_viewer.data.local.converter.SourceType.SMB -> "SMB · $resourceCount 个资源"
                        else -> "未知 · $resourceCount 个资源"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // 操作按钮
            if (compactActions) {
                Box {
                    IconButton(onClick = { showActionsMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多操作")
                    }
                    DropdownMenu(
                        expanded = showActionsMenu,
                        onDismissRequest = { showActionsMenu = false },
                    ) {
                        if (onEditSmb != null) {
                            DropdownMenuItem(
                                text = { Text("编辑凭据") },
                                onClick = { showActionsMenu = false; onEditSmb() },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("重命名") },
                            onClick = { showActionsMenu = false; onRename() },
                        )
                        DropdownMenuItem(
                            text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                            onClick = { showActionsMenu = false; onDelete() },
                        )
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (onEditSmb != null) {
                        IconButton(onClick = onEditSmb) {
                            Icon(Icons.Default.Settings, "编辑凭据", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onRename) {
                        Icon(Icons.Default.Edit, "重命名", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // 开关
            Switch(
                checked = source.enabled,
                onCheckedChange = { onToggleEnabled() },
            )
        }
    }
    }
}
