package dev.wucheng.resource_viewer.ui.screens.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.wucheng.resource_viewer.domain.model.Tag
import org.koin.androidx.compose.koinViewModel

/**
 * 将十六进制颜色字符串转换为 Color
 */
private fun String.toColor(): Color {
    return try {
        val hex = removePrefix("#")
        Color(android.graphics.Color.parseColor("#$hex"))
    } catch (e: Exception) {
        Color.Gray
    }
}

/**
 * 标签管理页面
 *
 * @param onNavigateBack 返回上一页回调
 * @param modifier Modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagerScreen(
    onNavigateBack: () -> Unit = {},
    onTagClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: TagViewModel = koinViewModel(),
) {
    val tags by viewModel.tags.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val editorState by viewModel.editorState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 删除确认弹窗状态
    var tagToDelete by remember { mutableStateOf<Tag?>(null) }

    // 监听错误消息
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("标签管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "新建标签",
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { innerPadding ->
        if (tags.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "还没有标签",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "点击右下角按钮创建标签",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            // 标签列表（分组显示）
            val builtInTags = tags.filter { it.isBuiltIn }
            val customTags = tags.filter { !it.isBuiltIn }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp),
            ) {
                // 内置标签
                if (builtInTags.isNotEmpty()) {
                    item(key = "builtin_header") {
                        SectionTitle(title = "内置标签")
                    }
                    items(
                        items = builtInTags,
                        key = { it.id },
                    ) { tag ->
                        TagListItem(
                            tag = tag,
                            onClick = { onTagClick?.invoke(tag.id) },
                            onDelete = { tagToDelete = tag },
                        )
                    }
                }

                // 自定义标签
                if (customTags.isNotEmpty()) {
                    item(key = "custom_header") {
                        SectionTitle(title = "自定义标签")
                    }
                    items(
                        items = customTags,
                        key = { it.id },
                    ) { tag ->
                        TagListItem(
                            tag = tag,
                            onClick = { viewModel.showEditDialog(tag) },
                            onDelete = { tagToDelete = tag },
                        )
                    }
                }
            }
        }
    }

    // 标签编辑弹窗
    if (editorState.isVisible) {
        TagEditorDialog(
            tag = editorState.editingTag,
            name = editorState.name,
            color = editorState.color,
            nameError = editorState.nameError,
            onNameChange = { viewModel.updateEditorName(it) },
            onColorChange = { viewModel.updateEditorColor(it) },
            onConfirm = { viewModel.saveTag() },
            onDismiss = { viewModel.dismissEditor() },
        )
    }

    // 删除确认弹窗
    tagToDelete?.let { tag ->
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            title = { Text("删除标签") },
            text = {
                Text("确定要删除标签「${tag.name}」吗？删除后关联的资源不会被删除。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTag(tag)
                        tagToDelete = null
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { tagToDelete = null }) {
                    Text("取消")
                }
            },
        )
    }
}

/**
 * 分区标题
 */
@Composable
private fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

/**
 * 标签列表项
 *
 * @param tag 标签数据
 * @param onClick 点击回调
 * @param onDelete 删除回调
 */
@Composable
private fun TagListItem(
    tag: Tag,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 颜色圆点
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(tag.color.toColor()),
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 标签信息
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = tag.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (tag.isBuiltIn) "内置标签" else "${tag.resourceCount} 个资源",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // 删除按钮（内置标签不显示）
            if (!tag.isBuiltIn) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
