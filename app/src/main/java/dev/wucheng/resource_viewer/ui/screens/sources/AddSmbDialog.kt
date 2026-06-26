package dev.wucheng.resource_viewer.ui.screens.sources

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/**
 * SMB 源添加弹窗。
 *
 * @param formData 表单数据
 * @param isTestingConnection 是否正在测试连接
 * @param testConnectionSuccess 测试连接结果（null 表示未测试）
 * @param testConnectionError 测试连接错误信息
 * @param onFormChange 表单数据变化回调
 * @param onTestConnection 测试连接回调
 * @param onConfirm 确认添加回调
 * @param onDismiss 关闭弹窗回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSmbDialog(
    formData: SmbFormData,
    isTestingConnection: Boolean,
    testConnectionSuccess: Boolean?,
    testConnectionError: String?,
    onFormChange: (String?, String?, Int?, String?, String?, String?, String?) -> Unit,
    onTestConnection: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("添加 SMB 网络共享")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 源名称
                OutlinedTextField(
                    value = formData.name,
                    onValueChange = { onFormChange(it, null, null, null, null, null, null) },
                    label = { Text("源名称 *") },
                    placeholder = { Text("如：NAS 漫画库") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // SMB 地址
                OutlinedTextField(
                    value = formData.host,
                    onValueChange = { onFormChange(null, it, null, null, null, null, null) },
                    label = { Text("SMB 地址 *") },
                    placeholder = { Text("192.168.1.100") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // 端口和域名
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = formData.port.toString(),
                        onValueChange = {
                            val port = it.toIntOrNull() ?: 445
                            onFormChange(null, null, port, null, null, null, null)
                        },
                        label = { Text("端口") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = formData.domain,
                        onValueChange = { onFormChange(null, null, null, null, null, it, null) },
                        label = { Text("域名") },
                        placeholder = { Text("WORKGROUP") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                // 用户名和密码
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = formData.username,
                        onValueChange = { onFormChange(null, null, null, it, null, null, null) },
                        label = { Text("用户名") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = formData.password,
                        onValueChange = { onFormChange(null, null, null, null, it, null, null) },
                        label = { Text("密码") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.weight(1f),
                    )
                }

                // 共享名称
                OutlinedTextField(
                    value = formData.shareName,
                    onValueChange = { onFormChange(null, null, null, null, null, null, it) },
                    label = { Text("共享名称 *") },
                    placeholder = { Text("share") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // 测试连接结果
                if (testConnectionSuccess != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (testConnectionSuccess) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = if (testConnectionSuccess) "✓ 连接成功" else "✗ 连接失败",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (testConnectionSuccess) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                },
                            )
                            if (testConnectionError != null) {
                                Text(
                                    text = testConnectionError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 测试连接按钮
                OutlinedButton(
                    onClick = onTestConnection,
                    enabled = !isTestingConnection &&
                            formData.host.isNotBlank() &&
                            formData.shareName.isNotBlank(),
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text("测试连接")
                }

                // 添加按钮
                Button(
                    onClick = onConfirm,
                    enabled = !isTestingConnection &&
                            formData.name.isNotBlank() &&
                            formData.host.isNotBlank() &&
                            formData.shareName.isNotBlank(),
                ) {
                    Text("添加")
                }
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
