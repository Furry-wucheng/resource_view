package dev.wucheng.resource_viewer.ui.screens.sources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddLocalDialog(
    formData: LocalFormData,
    onNameChange: (String) -> Unit,
    onPickFolder: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加本地文件夹") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = formData.name,
                    onValueChange = onNameChange,
                    label = { Text("源名称 *") },
                    placeholder = { Text("如：本地漫画库") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = if (formData.rootPath.isBlank()) "尚未选择文件夹" else formData.rootPath,
                )
                Button(onClick = onPickFolder) {
                    Text("选择文件夹")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = formData.name.isNotBlank() && formData.rootPath.isNotBlank(),
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
