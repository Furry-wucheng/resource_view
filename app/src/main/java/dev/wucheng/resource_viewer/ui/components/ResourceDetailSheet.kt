package dev.wucheng.resource_viewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.domain.model.Tag

/**
 * 组织模式显示配置。
 */
private data class OrgModeOption(
    val mode: OrganizationMode,
    val label: String,
)

private val ORG_MODE_OPTIONS = listOf(
    OrgModeOption(OrganizationMode.CHAPTER, "章节模式"),
    OrgModeOption(OrganizationMode.CHAPTER_GALLERY, "章节画廊"),
    OrgModeOption(OrganizationMode.FLATGRID, "平铺网格"),
    OrgModeOption(OrganizationMode.GALLERY, "画廊模式"),
)

/**
 * 资源类型显示文本。
 */
private fun ResourceType.displayText(): String = when (this) {
    ResourceType.FOLDER -> "文件夹"
    ResourceType.PDF -> "PDF"
    ResourceType.ARCHIVE -> "压缩包"
    ResourceType.VIDEO -> "视频"
}

/**
 * 资源详情弹窗（半屏底部弹出）。
 *
 * 显示资源信息、标签列表（可勾选）、组织模式切换。
 *
 * @param resource 资源数据
 * @param allTags 所有标签
 * @param selectedTagIds 当前已关联的标签 ID 集合
 * @param selectedOrgMode 当前选中的组织模式
 * @param onTagToggle 标签勾选切换回调
 * @param onOrgModeChange 组织模式切换回调
 * @param onSave 保存回调
 * @param onDismiss 取消回调
 * @param modifier Modifier
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ResourceDetailSheet(
    resource: Resource,
    allTags: List<Tag>,
    selectedTagIds: Set<String>,
    selectedOrgMode: OrganizationMode,
    onTagToggle: (String) -> Unit,
    onOrgModeChange: (OrganizationMode) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // 资源名称
            Text(
                text = resource.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 资源类型
            Text(
                text = "${resource.type.displayText()} · 已入库",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 资源详情行
            DetailRow(label = "路径", value = resource.relativePath)
            resource.fileCount?.let {
                DetailRow(label = "文件数", value = "$it 个文件")
            }
            resource.fileSize?.let {
                DetailRow(label = "大小", value = formatFileSize(it))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 组织模式
            Text(
                text = "组织模式",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ORG_MODE_OPTIONS.forEach { option ->
                    val isSelected = selectedOrgMode == option.mode
                    SuggestionChip(
                        onClick = { onOrgModeChange(option.mode) },
                        label = { Text(option.label) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            labelColor = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            borderColor = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                            borderWidth = if (isSelected) 2.dp else 1.dp,
                            enabled = true,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 标签
            Text(
                text = "标签",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            allTags.forEach { tag ->
                TagCheckItem(
                    tag = tag,
                    isChecked = selectedTagIds.contains(tag.id),
                    onToggle = { onTagToggle(tag.id) },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onSave) {
                    Text("保存")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 标签勾选项。
 */
@Composable
private fun TagCheckItem(
    tag: Tag,
    isChecked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tagColor = try {
        Color(android.graphics.Color.parseColor(tag.color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.outline
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 颜色圆点
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(tagColor),
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 标签名
        Text(
            text = tag.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )

        // 复选框
        Checkbox(
            checked = isChecked,
            onCheckedChange = { onToggle() },
        )
    }
}

/**
 * 详情行。
 */
@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * 格式化文件大小。
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
