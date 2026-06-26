package dev.wucheng.resource_viewer.ui.screens.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.wucheng.resource_viewer.domain.model.Tag

/**
 * 12 色标签预设
 */
private val TAG_COLOR_PRESETS = listOf(
    "#E53935",  // Red 500
    "#D81B60",  // Pink 600
    "#8E24AA",  // Purple 600
    "#5E35B1",  // Deep Purple 600
    "#3949AB",  // Indigo 600
    "#1E88E5",  // Blue 600
    "#00ACC1",  // Cyan 600
    "#00897B",  // Teal 600
    "#43A047",  // Green 600
    "#FDD835",  // Yellow 600
    "#FB8C00",  // Orange 600
    "#757575",  // Grey 600
)

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
 * 标签创建/编辑弹窗
 *
 * @param tag 编辑的标签，null 表示创建新标签
 * @param name 当前输入的名称
 * @param color 当前选择的颜色
 * @param nameError 名称错误提示
 * @param onNameChange 名称变化回调
 * @param onColorChange 颜色变化回调
 * @param onConfirm 确认回调
 * @param onDismiss 取消回调
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagEditorDialog(
    tag: Tag?,
    name: String,
    color: String,
    nameError: String?,
    onNameChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (tag != null) "编辑标签" else "新建标签")
        },
        text = {
            Column {
                // 名称输入框
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("标签名称") },
                    placeholder = { Text("输入标签名称") },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = tag?.isBuiltIn != true,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 颜色选择器
                Text(
                    text = "选择颜色",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TAG_COLOR_PRESETS.forEach { presetColor ->
                        val isSelected = color == presetColor
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(presetColor.toColor())
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            width = 3.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape,
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { onColorChange(presetColor) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = name.isNotBlank() && color.isNotEmpty(),
            ) {
                Text("确定")
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
