package dev.wucheng.resource_viewer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 错误视图尺寸级别。
 */
enum class ErrorViewLevel {
    /** 页面级：大图标、完整布局 */
    PAGE,

    /** 卡片级：中等图标 */
    CARD,

    /** 行内级：紧凑布局 */
    INLINE,
}

/**
 * 可复用的错误状态组件。
 *
 * @param message 错误消息文本
 * @param canRetry 是否可重试
 * @param onRetry 点击重试按钮的回调
 * @param level 尺寸级别，默认为页面级
 * @param modifier Modifier
 */
@Composable
fun ErrorView(
    message: String,
    canRetry: Boolean = false,
    onRetry: () -> Unit = {},
    level: ErrorViewLevel = ErrorViewLevel.PAGE,
    modifier: Modifier = Modifier,
) {
    when (level) {
        ErrorViewLevel.PAGE -> ErrorViewPage(message, canRetry, onRetry, modifier)
        ErrorViewLevel.CARD -> ErrorViewCard(message, canRetry, onRetry, modifier)
        ErrorViewLevel.INLINE -> ErrorViewInline(message, canRetry, onRetry, modifier)
    }
}

/**
 * 页面级错误视图：占据整个页面，大图标居中显示。
 */
@Composable
private fun ErrorViewPage(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "出错了",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (canRetry) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("重试")
            }
        }
    }
}

/**
 * 卡片级错误视图：适用于卡片/列表项内显示。
 */
@Composable
private fun ErrorViewCard(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (canRetry) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("重试")
            }
        }
    }
}

/**
 * 行内级错误视图：紧凑的单行/双行布局。
 */
@Composable
private fun ErrorViewInline(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (canRetry) {
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}
