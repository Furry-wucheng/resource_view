package dev.wucheng.resource_viewer.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * M12: 隐私政策同意对话框
 *
 * 首次启动时显示隐私政策，用户同意后不再显示。
 */
@Composable
fun PrivacyConsentDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = { /* 不允许点击外部关闭 */ },
        title = {
            Text(
                text = "隐私政策",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "欢迎使用 Resource Viewer！",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "本应用需要访问您设备上的媒体文件（图片、视频、音频）以提供资源浏览功能。" +
                            "我们承诺：",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• 所有数据仅存储在您的设备本地\n" +
                            "• 不会上传任何个人信息到服务器\n" +
                            "• 不会收集或分享您的使用数据\n" +
                            "• 您可以随时在设置中清除所有数据",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "权限说明：",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "• 媒体文件访问：用于浏览和管理您的资源文件\n" +
                            "• 存储权限：用于读取设备上的文件",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "点击\"同意\"即表示您了解并同意上述隐私政策。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text("同意")
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text("退出应用")
            }
        },
        modifier = modifier,
    )
}
