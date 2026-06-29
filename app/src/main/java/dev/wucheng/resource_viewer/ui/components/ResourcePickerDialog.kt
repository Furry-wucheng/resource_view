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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.RadioButtonUnchecked
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.wucheng.resource_viewer.domain.model.TreeFileNode

enum class ResourcePickerMode { BATCH_ADD, SPLIT_KEEP, SPLIT_DELETE }

@Composable
fun ResourcePickerDialog(
    rootName: String,
    treeNodes: List<TreeFileNode>,
    selectedCount: Int,
    uiState: ResourcePickerUiState,
    mode: ResourcePickerMode = ResourcePickerMode.BATCH_ADD,
    onToggleExpand: (String) -> Unit,
    onToggleCheck: (String) -> Unit,
    onSelectAllChildren: (String) -> Unit,
    onSelectAllRoot: () -> Unit = {},
    onConfirm: () -> Unit,
    onConfirmDelete: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleText = when (mode) {
        ResourcePickerMode.BATCH_ADD -> "扫描入库：$rootName"
        ResourcePickerMode.SPLIT_KEEP, ResourcePickerMode.SPLIT_DELETE -> "拆分资源：$rootName"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = titleText, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "已选 $selectedCount 项",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        text = {
            when (uiState) {
                is ResourcePickerUiState.Loading -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is ResourcePickerUiState.Error -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { Text(uiState.message, color = MaterialTheme.colorScheme.error) }
                else -> LazyColumn(Modifier.fillMaxWidth()) {
                    item(key = "select_all_root") {
                        Text(
                            "全选子项 (${treeNodes.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 4.dp).clickable { onSelectAllRoot() },
                        )
                    }
                    items(treeNodes, key = { it.relativePath }) { node ->
                        TreeNodeItem(node, depth = 0, onToggleExpand, onToggleCheck, onSelectAllChildren)
                    }
                }
            }
        },
        confirmButton = {
            when (mode) {
                ResourcePickerMode.BATCH_ADD -> {
                    TextButton(onClick = onConfirm, enabled = selectedCount > 0) { Text("批量添加资源 ($selectedCount)") }
                }
                ResourcePickerMode.SPLIT_KEEP, ResourcePickerMode.SPLIT_DELETE -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onConfirmDelete, enabled = selectedCount > 0) {
                            Text("删除原资源 ($selectedCount)", color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick = onConfirm, enabled = selectedCount > 0) {
                            Text("保留原资源 ($selectedCount)")
                        }
                    }
                }
            }
        },
        dismissButton = {
            if (mode == ResourcePickerMode.BATCH_ADD) {
                TextButton(onClick = onDismiss) { Text("取消") }
            } else {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
        modifier = modifier,
    )
}

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (depth * 24 + 8).dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (node.isDirectory && node.isExpandable) {
                IconButton(onClick = { onToggleExpand(node.relativePath) }, modifier = Modifier.size(20.dp)) {
                    Icon(
                        Icons.Default.ChevronRight, if (node.isExpanded) "折叠" else "展开",
                        modifier = Modifier.rotate(if (node.isExpanded) 90f else 0f),
                    )
                }
            } else {
                Spacer(Modifier.width(20.dp))
            }

            Spacer(Modifier.width(4.dp))

            Icon(
                if (node.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.width(6.dp))

            Text(node.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))

            if (node.isImported) {
                Surface(shape = CircleShape, color = Color(0xFF4CAF50), modifier = Modifier.padding(end = 6.dp).size(18.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, "已入库", Modifier.size(11.dp), tint = Color.White) }
                }
            } else {
                // 复选框 — 已入库节点不显示
                Icon(
                    imageVector = if (node.isChecked) Icons.Default.Check else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (node.isChecked) "取消勾选 ${node.name}" else "勾选 ${node.name}",
                    tint = if (node.isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(22.dp)
                        .semantics { contentDescription = if (node.isChecked) "取消勾选 ${node.name}" else "勾选 ${node.name}" }
                        .clickable { onToggleCheck(node.relativePath) },
                )
            }
        }

        if (node.isDirectory && node.isExpandable && node.isExpanded) {
            Text(
                "全选子项 (${node.children.size})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = (depth * 24 + 32).dp, bottom = 4.dp).clickable { onSelectAllChildren(node.relativePath) },
            )
        }

        AnimatedVisibility(visible = node.isExpanded) {
            Column { node.children.forEach { child -> TreeNodeItem(child, depth + 1, onToggleExpand, onToggleCheck, onSelectAllChildren) } }
        }
    }
}
