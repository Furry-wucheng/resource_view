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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
    var showClearCacheDialog by remember { mutableStateOf<ClearCacheType?>(null) }
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
                // 封面缓存
                CacheSettingItem(
                    label = "封面缓存",
                    subtitle = "资源库封面，默认永久存储",
                    sizeBytes = uiState.coverCacheSizeBytes,
                    limitMB = uiState.coverCacheLimitMB,
                    onLimitChange = { viewModel.updateCoverCacheLimit(it) },
                    onClear = { showClearCacheDialog = ClearCacheType.COVER },
                    presets = listOf(0, 500, 1000, 2000),
                    presetLabels = listOf("无限制", "500 MB", "1000 MB", "2000 MB"),
                    isUnlimited = uiState.coverCacheLimitMB == 0,
                )

                // 页面缓存
                CacheSettingItem(
                    label = "页面缓存",
                    subtitle = "SMB 远程文件本地缓存",
                    sizeBytes = uiState.pageCacheSizeBytes,
                    limitMB = uiState.pageCacheLimitMB,
                    onLimitChange = { viewModel.updatePageCacheLimit(it) },
                    onClear = { showClearCacheDialog = ClearCacheType.PAGE },
                    presets = listOf(500, 1000, 2000, 5000),
                    presetLabels = listOf("500 MB", "1000 MB", "2000 MB", "5000 MB"),
                )

                // 缩略图缓存
                CacheSettingItem(
                    label = "缩略图缓存",
                    subtitle = "文件浏览器缩略图",
                    sizeBytes = uiState.thumbnailCacheSizeBytes,
                    limitMB = uiState.thumbnailCacheLimitMB,
                    onLimitChange = { viewModel.updateThumbnailCacheLimit(it) },
                    onClear = { showClearCacheDialog = ClearCacheType.THUMBNAIL },
                    presets = listOf(500, 1000, 2000, 5000),
                    presetLabels = listOf("500 MB", "1000 MB", "2000 MB", "5000 MB"),
                )

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
    showClearCacheDialog?.let { cacheType ->
        val (title, message) = when (cacheType) {
            ClearCacheType.COVER -> Pair(
                "清理封面缓存",
                "将清除 ${formatBytes(uiState.coverCacheSizeBytes)} 的封面缓存，下次打开资源库时需重新生成。确定吗？",
            )
            ClearCacheType.PAGE -> Pair(
                "清理页面缓存",
                "将清除 ${formatBytes(uiState.pageCacheSizeBytes)} 的 SMB 页面缓存，下次浏览时需重新下载。确定吗？",
            )
            ClearCacheType.THUMBNAIL -> Pair(
                "清理缩略图缓存",
                "将清除 ${formatBytes(uiState.thumbnailCacheSizeBytes)} 的缩略图缓存，下次浏览时需重新生成。确定吗？",
            )
        }
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(message)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCacheDialog = null
                        when (cacheType) {
                            ClearCacheType.COVER -> viewModel.clearCoverCache()
                            ClearCacheType.PAGE -> viewModel.clearPageCache()
                            ClearCacheType.THUMBNAIL -> viewModel.clearThumbnailCache()
                        }
                    },
                ) {
                    Text(
                        text = "确认清理",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = null }) {
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

    // 自定义容量对话框（已移除，改为内嵌输入）
}

/**
 * 缓存类型枚举
 */
private enum class ClearCacheType {
    COVER, PAGE, THUMBNAIL
}

/**
 * 缓存设置项
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CacheSettingItem(
    label: String,
    subtitle: String,
    sizeBytes: Long,
    limitMB: Int,
    onLimitChange: (Int) -> Unit,
    onClear: () -> Unit,
    presets: List<Int>,
    presetLabels: List<String>,
    isUnlimited: Boolean = false,
) {
    var showCustomInput by remember { mutableStateOf(false) }
    var customInputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // 标题行 + 大小
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = if (isUnlimited) {
                    "${formatBytes(sizeBytes)} / 无限制"
                } else {
                    "${formatBytes(sizeBytes)} / ${limitMB} MB"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 进度条
        LinearProgressIndicator(
            progress = {
                if (isUnlimited) {
                    0f
                } else if (limitMB == 0) {
                    0f
                } else {
                    (sizeBytes.toFloat() / (limitMB * 1024 * 1024)).coerceIn(0f, 1f)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            color = if (isUnlimited) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.primary
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 容量选择行
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                presets.forEachIndexed { index, value ->
                    FilterChip(
                        selected = if (isUnlimited) value == 0 else limitMB == value,
                        onClick = {
                            onLimitChange(value)
                            showCustomInput = false
                        },
                        label = {
                            Text(
                                text = presetLabels[index],
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                    )
                }
                // 自定义按钮
                FilterChip(
                    selected = showCustomInput,
                    onClick = {
                        showCustomInput = !showCustomInput
                        if (showCustomInput) {
                            customInputText = if (limitMB == 0) "" else limitMB.toString()
                        }
                    },
                    label = {
                        Text(
                            text = "自定义",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                )
            }
            // 清理按钮
            if (sizeBytes > 0) {
                TextButton(onClick = onClear) {
                    Text(
                        text = "清理",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        // 自定义输入框（展开时显示）
        if (showCustomInput) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = customInputText,
                    onValueChange = { customInputText = it.filter { c -> c.isDigit() } },
                    label = { Text("MB") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        val value = customInputText.toIntOrNull()
                        if (value != null && (isUnlimited || value >= 100)) {
                            onLimitChange(value)
                            showCustomInput = false
                        }
                    },
                ) {
                    Text("确认")
                }
            }
        }

        // 副标题
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
