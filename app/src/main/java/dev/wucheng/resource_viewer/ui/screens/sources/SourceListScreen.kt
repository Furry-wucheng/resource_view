package dev.wucheng.resource_viewer.ui.screens.sources

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.wucheng.resource_viewer.domain.model.Source
import dev.wucheng.resource_viewer.ui.components.EmptyState
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
                windowInsets = WindowInsets(0.dp),
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
                            onToggleEnabled = { viewModel.toggleSourceEnabled(source) },
                            onDelete = { viewModel.deleteSource(source.id) },
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
}

/**
 * 数据源卡片组件。
 */
@Composable
private fun SourceCard(
    source: Source,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
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
                )
                Text(
                    text = source.rootPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = when (source.type) {
                        dev.wucheng.resource_viewer.data.local.converter.SourceType.LOCAL -> "本地"
                        dev.wucheng.resource_viewer.data.local.converter.SourceType.SMB -> "SMB"
                        else -> "未知"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // 开关
            Switch(
                checked = source.enabled,
                onCheckedChange = { onToggleEnabled() },
            )
        }
    }
}
