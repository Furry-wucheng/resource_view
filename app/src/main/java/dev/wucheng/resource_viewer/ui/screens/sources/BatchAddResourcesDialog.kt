package dev.wucheng.resource_viewer.ui.screens.sources

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.domain.model.Tag

/**
 * 批量添加资源弹窗。
 *
 * @param selectedCount 已选中的文件数量
 * @param allTags 所有可用标签
 * @param onConfirm 确认回调（organizationMode, tagIds）
 * @param onDismiss 关闭回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchAddResourcesDialog(
    selectedCount: Int,
    allTags: List<Tag>,
    onConfirm: (OrganizationMode?, List<String>) -> Unit,
    onCreateTag: (String, (String) -> Unit) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedOrgMode by remember { mutableStateOf<OrganizationMode?>(null) }
    var selectedTagIds by remember { mutableStateOf(setOf<String>()) }
    var autoDetect by remember { mutableStateOf(true) }
    var newTagName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("批量添加资源") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "已选择 $selectedCount 个文件/文件夹",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // 组织模式选择
                Text(
                    text = "组织模式",
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = autoDetect,
                        onClick = {
                            autoDetect = true
                            selectedOrgMode = null
                        },
                        label = { Text("自动检测") },
                    )
                    FilterChip(
                        selected = !autoDetect && selectedOrgMode == OrganizationMode.CHAPTER,
                        onClick = {
                            autoDetect = false
                            selectedOrgMode = OrganizationMode.CHAPTER
                        },
                        label = { Text("章节") },
                    )
                    FilterChip(
                        selected = !autoDetect && selectedOrgMode == OrganizationMode.FLATGRID,
                        onClick = {
                            autoDetect = false
                            selectedOrgMode = OrganizationMode.FLATGRID
                        },
                        label = { Text("平铺") },
                    )
                }

                // 标签选择
                if (allTags.isNotEmpty()) {
                    Text(
                        text = "标签（可选）",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(allTags) { tag ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = tag.id in selectedTagIds,
                                    onCheckedChange = { checked ->
                                        selectedTagIds = if (checked) {
                                            selectedTagIds + tag.id
                                        } else {
                                            selectedTagIds - tag.id
                                        }
                                    },
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tag.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
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
                            onCreateTag(newTagName) { id -> selectedTagIds = selectedTagIds + id }
                            newTagName = ""
                        },
                    ) { Text("创建") }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedOrgMode, selectedTagIds.toList())
                },
            ) {
                Text("添加")
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
