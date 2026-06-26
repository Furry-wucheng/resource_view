package dev.wucheng.resource_viewer.ui.screens.sources

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                onClick = { viewModel.showAddSmbDialog() },
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
                    onAddSource = { viewModel.showAddSmbDialog() },
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

    // SMB 添加弹窗
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
