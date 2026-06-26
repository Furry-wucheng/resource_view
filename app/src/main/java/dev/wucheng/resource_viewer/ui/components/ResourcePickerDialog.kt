package dev.wucheng.resource_viewer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.wucheng.resource_viewer.domain.model.TreeFileNode

/**
 * ResourcePicker 弹窗 — 树形文件选择器。
 *
 * 核心交互：
 * - 根节点仅导航 + [全选子项] 按钮
 * - 子节点复选框（独立勾选，不级联）
 * - 底部显示已选数量 + 操作按钮
 *
 * @param rootName 根节点名称（数据源名称）
 * @param treeNodes 树节点列表
 * @param selectedCount 已选数量
 * @param uiState UI 状态
 * @param onToggleExpand 展开/折叠回调
 * @param onToggleCheck 勾选切换回调
 * @param onSelectAllChildren 全选子项回调
 * @param onConfirm 确认回调
 * @param onDismiss 取消回调
 * @param modifier Modifier
 */
@Composable
fun ResourcePickerDialog(
    rootName: String,
    treeNodes: List<TreeFileNode>,
    selectedCount: Int,
    uiState: ResourcePickerUiState,
    onToggleExpand: (String) -> Unit,
    onToggleCheck: (String) -> Unit,
    onSelectAllChildren: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "扫描入库：$rootName",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "已选 $selectedCount 项",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        text = {
            when (uiState) {
                is ResourcePickerUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is ResourcePickerUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = uiState.message,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(
                            items = treeNodes,
                            key = { it.relativePath },
                        ) { node ->
                            TreeNodeItem(
                                node = node,
                                depth = 0,
                                onToggleExpand = onToggleExpand,
                                onToggleCheck = onToggleCheck,
                                onSelectAllChildren = onSelectAllChildren,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = selectedCount > 0,
            ) {
                Text("批量添加资源 ($selectedCount)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        modifier = modifier,
    )
}

/**
 * 树节点项。
 */
@Composable
private fun TreeNodeItem(
    node: TreeFileNode,
    depth: Int,
    onToggleExpand: (String) -> Unit,
    onToggleCheck: (String) -> Unit,
    onSelectAllChildren: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // 节点行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (node.isDirectory && node.isExpandable) {
                        onToggleExpand(node.relativePath)
                    } else {
                        onToggleCheck(node.relativePath)
                    }
                }
                .padding(
                    start = (depth * 24 + 8).dp,
                    end = 8.dp,
                    top = 8.dp,
                    bottom = 8.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 展开/折叠按钮
            if (node.isDirectory && node.isExpandable) {
                IconButton(
                    onClick = { onToggleExpand(node.relativePath) },
                    modifier = Modifier.size(20.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = if (node.isExpanded) "折叠" else "展开",
                        modifier = Modifier.rotate(if (node.isExpanded) 90f else 0f),
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(20.dp))
            }

            Spacer(modifier = Modifier.width(4.dp))

            // 图标
            Icon(
                imageVector = if (node.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.width(6.dp))

            // 名称
            Text(
                text = node.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )

            // 复选框（非根节点、非可展开目录显示）
            if (!node.isDirectory || !node.isExpandable) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .semantics { contentDescription = "勾选 ${node.name}" }
                        .clickable { onToggleCheck(node.relativePath) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (node.isChecked) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        // 全选子项按钮
        if (node.isDirectory && node.isExpandable && node.isExpanded) {
            Text(
                text = "全选子项 (${node.children.size})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = (depth * 24 + 32).dp, bottom = 4.dp)
                    .clickable { onSelectAllChildren(node.relativePath) },
            )
        }

        // 子节点
        AnimatedVisibility(visible = node.isExpanded) {
            Column {
                node.children.forEach { child ->
                    TreeNodeItem(
                        node = child,
                        depth = depth + 1,
                        onToggleExpand = onToggleExpand,
                        onToggleCheck = onToggleCheck,
                        onSelectAllChildren = onSelectAllChildren,
                    )
                }
            }
        }
    }
}
