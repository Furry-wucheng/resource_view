package dev.wucheng.resource_viewer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.wucheng.resource_viewer.domain.error.ScanResult

/**
 * 扫描进度条组件。
 *
 * 显示扫描进度 + 当前/总数 + 失败项统计。
 *
 * @param current 当前已处理项数
 * @param total 总项数
 * @param failCount 已失败项数
 * @param modifier Modifier
 */
@Composable
fun ScanProgressBar(
    current: Int,
    total: Int,
    failCount: Int = 0,
    modifier: Modifier = Modifier,
) {
    val progress = if (total > 0) current.toFloat() / total else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "scan_progress")

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "扫描中 $current / $total",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (failCount > 0) {
                Text(
                    text = "失败 $failCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

/**
 * 扫描完成结果摘要组件。
 *
 * @param result 扫描结果
 * @param modifier Modifier
 */
@Composable
fun ScanResultSummary(
    result: ScanResult,
    modifier: Modifier = Modifier,
) {
    val total = result.successCount + result.skipCount + result.failures.size
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "扫描完成",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "共 $total 项，成功 ${result.successCount}，跳过 ${result.skipCount}，失败 ${result.failures.size}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (result.failures.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            result.failures.take(3).forEach { (name, error) ->
                Text(
                    text = "· $name: ${error.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (result.failures.size > 3) {
                Text(
                    text = "…还有 ${result.failures.size - 3} 个失败项",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
