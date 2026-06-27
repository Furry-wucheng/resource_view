package dev.wucheng.resource_viewer.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.wucheng.resource_viewer.data.local.converter.AutoSyncInterval
import dev.wucheng.resource_viewer.data.local.converter.DoublePageMode
import dev.wucheng.resource_viewer.data.local.converter.PageDirection
import dev.wucheng.resource_viewer.data.local.converter.ThemeMode
import org.koin.androidx.compose.koinViewModel

/**
 * M25: 设置页面
 *
 * 提供完整设置功能：缓存管理、外观设置、查看器默认设置、数据源同步、关于。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showResetDefaultsDialog by remember { mutableStateOf(false) }

    // 监听缓存清理状态
    LaunchedEffect(uiState.cacheCleared) {
        if (uiState.cacheCleared) {
            snackbarHostState.showSnackbar("缓存已清理")
            viewModel.resetCacheCleared()
        }
    }

    // 监听错误状态
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ========== 缓存管理 ==========
            SettingsGroup(title = "缓存管理") {
                // 缓存大小显示
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "缩略图缓存",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "已用: ${formatBytes(uiState.cacheSizeBytes)} / 上限: ${uiState.cacheLimitMB} MB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (uiState.cacheLimitMB > 0) {
                                (uiState.cacheSizeBytes.toFloat() / (uiState.cacheLimitMB * 1024 * 1024)).coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // 容量上限选择
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(
                        text = "容量上限",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(500, 1000, 1500, 2000).forEach { limit ->
                            FilterChip(
                                selected = uiState.cacheLimitMB == limit,
                                onClick = { viewModel.updateCacheLimit(limit) },
                                label = { Text("$limit MB") },
                            )
                        }
                        // 自定义按钮
                        FilterChip(
                            selected = uiState.cacheLimitMB !in listOf(500, 1000, 1500, 2000),
                            onClick = { viewModel.showCustomCapacityDialog() },
                            label = { Text("自定义") },
                        )
                    }
                }

                // 清理缓存按钮
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { showClearCacheDialog = true },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "清理缩略图缓存",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }

                // 缓存位置
                Text(
                    text = "缓存位置: ${uiState.cachePath}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            // ========== 外观 ==========
            SettingsGroup(title = "外观") {
                SettingItem(
                    label = "深色模式",
                    value = getThemeModeLabel(uiState.themeMode),
                    options = listOf(
                        ThemeMode.SYSTEM to "跟随系统",
                        ThemeMode.LIGHT to "浅色",
                        ThemeMode.DARK to "深色",
                    ),
                    onOptionSelected = { viewModel.updateThemeMode(it) },
                )
            }

            // ========== 查看器默认设置 ==========
            SettingsGroup(title = "查看器默认设置") {
                SettingItem(
                    label = "默认翻页方向",
                    subtitle = "日漫默认为右向左阅读",
                    value = getPageDirectionLabel(uiState.pageDirection),
                    options = listOf(
                        PageDirection.RIGHT_TO_LEFT to "右→左",
                        PageDirection.LEFT_TO_RIGHT to "左→右",
                        PageDirection.VERTICAL to "垂直滚动",
                    ),
                    onOptionSelected = { viewModel.updatePageDirection(it) },
                )

                SettingItem(
                    label = "双页显示",
                    subtitle = "宽度≥900dp时自动双页",
                    value = getDoublePageModeLabel(uiState.doublePageMode),
                    options = listOf(
                        DoublePageMode.AUTO to "自动",
                        DoublePageMode.SINGLE to "始终单页",
                        DoublePageMode.DOUBLE to "始终双页",
                    ),
                    onOptionSelected = { viewModel.updateDoublePageMode(it) },
                )

                SettingSwitch(
                    label = "跨章节连续阅读",
                    subtitle = "末页继续滑动切换下一章",
                    checked = uiState.crossChapter,
                    onCheckedChange = { viewModel.updateCrossChapter(it) },
                )

                SettingConcurrency(
                    label = "缩略图并发加载",
                    subtitle = "控制同时加载缩略图的并发数量",
                    value = uiState.thumbnailConcurrency,
                    onValueChange = { viewModel.updateThumbnailConcurrency(it) },
                )
            }

            // ========== 数据源同步 ==========
            SettingsGroup(title = "数据源同步") {
                SettingItem(
                    label = "自动同步间隔",
                    subtitle = "自动检查新增/变更文件",
                    value = getAutoSyncIntervalLabel(uiState.autoSyncInterval),
                    options = listOf(
                        null to "关闭",
                        AutoSyncInterval.MINUTES_15 to "每 15 分钟",
                        AutoSyncInterval.MINUTES_30 to "每 30 分钟",
                        AutoSyncInterval.HOUR_1 to "每小时",
                    ),
                    onOptionSelected = { viewModel.updateAutoSyncInterval(it) },
                )
            }

            // ========== 关于 ==========
            SettingsGroup(title = "关于") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    AboutRow(label = "应用名称", value = "Resource Viewer")
                    AboutRow(label = "版本号", value = "0.1.0")
                    AboutRow(label = "开源许可", value = "MIT License")
                }
            }

            // ========== 恢复默认设置 ==========
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showResetDefaultsDialog = true },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "恢复默认设置",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 底部间距
            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    // 清理缓存确认对话框
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = {
                Text(
                    text = "确认清理缓存",
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text("将清除 ${formatBytes(uiState.cacheSizeBytes)} 的缩略图缓存，下次浏览时需重新生成。确定吗？")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCacheDialog = false
                        viewModel.clearThumbnailCache()
                    },
                ) {
                    Text(
                        text = "确认清理",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("取消")
                }
            },
        )
    }

    // 恢复默认设置确认对话框
    if (showResetDefaultsDialog) {
        AlertDialog(
            onDismissRequest = { showResetDefaultsDialog = false },
            title = {
                Text(
                    text = "确认恢复默认设置",
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text("将重置所有设置为默认值，确定吗？")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDefaultsDialog = false
                        viewModel.resetToDefaults()
                    },
                ) {
                    Text("确认恢复")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDefaultsDialog = false }) {
                    Text("取消")
                }
            },
        )
    }

    // 自定义容量对话框
    if (uiState.showCustomCapacityDialog) {
        var customCapacityText by remember { mutableStateOf(uiState.cacheLimitMB.toString()) }
        AlertDialog(
            onDismissRequest = { viewModel.hideCustomCapacityDialog() },
            title = {
                Text(
                    text = "自定义缓存容量",
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column {
                    Text("请输入缓存容量上限（MB），最小 500 MB")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customCapacityText,
                        onValueChange = { customCapacityText = it },
                        label = { Text("容量 (MB)") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val capacity = customCapacityText.toIntOrNull()
                        if (capacity != null && capacity >= 500) {
                            viewModel.updateCacheLimit(capacity)
                            viewModel.hideCustomCapacityDialog()
                        }
                    },
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideCustomCapacityDialog() }) {
                    Text("取消")
                }
            },
        )
    }
}

/**
 * 设置分组
 */
@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
            )
            content()
        }
    }
}

/**
 * 设置项（带选择器）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SettingItem(
    label: String,
    subtitle: String? = null,
    value: String,
    options: List<Pair<T, String>>,
    onOptionSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (expanded) {
            options.forEach { (optionValue, optionLabel) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onOptionSelected(optionValue)
                            expanded = false
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = optionLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (value == optionLabel) {
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 设置开关
 */
@Composable
private fun SettingSwitch(
    label: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

/**
 * 并发数设置
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingConcurrency(
    label: String,
    subtitle: String? = null,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (1..8).forEach { concurrency ->
                FilterChip(
                    selected = value == concurrency,
                    onClick = { onValueChange(concurrency) },
                    label = { Text("$concurrency") },
                )
            }
        }
    }
}

/**
 * 关于行
 */
@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * 格式化字节数
 */
private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

/**
 * 获取主题模式标签
 */
private fun getThemeModeLabel(themeMode: ThemeMode): String {
    return when (themeMode) {
        ThemeMode.SYSTEM -> "跟随系统"
        ThemeMode.LIGHT -> "浅色"
        ThemeMode.DARK -> "深色"
    }
}

/**
 * 获取翻页方向标签
 */
private fun getPageDirectionLabel(pageDirection: PageDirection): String {
    return when (pageDirection) {
        PageDirection.RIGHT_TO_LEFT -> "右→左"
        PageDirection.LEFT_TO_RIGHT -> "左→右"
        PageDirection.VERTICAL -> "垂直滚动"
    }
}

/**
 * 获取双页模式标签
 */
private fun getDoublePageModeLabel(doublePageMode: DoublePageMode): String {
    return when (doublePageMode) {
        DoublePageMode.AUTO -> "自动"
        DoublePageMode.SINGLE -> "始终单页"
        DoublePageMode.DOUBLE -> "始终双页"
    }
}

/**
 * 获取自动同步间隔标签
 */
private fun getAutoSyncIntervalLabel(interval: AutoSyncInterval?): String {
    return when (interval) {
        null -> "关闭"
        AutoSyncInterval.OFF -> "关闭"
        AutoSyncInterval.MINUTES_15 -> "每 15 分钟"
        AutoSyncInterval.MINUTES_30 -> "每 30 分钟"
        AutoSyncInterval.HOUR_1 -> "每小时"
    }
}
