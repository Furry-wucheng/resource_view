package dev.wucheng.resource_viewer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 可复用的空状态组件
 *
 * @param hasResources 是否有资源数据
 * @param isFiltered 是否处于筛选状态
 * @param onAddSource 点击添加数据源按钮的回调
 * @param onClearFilter 点击清除筛选按钮的回调
 * @param modifier Modifier
 */
@Composable
fun EmptyState(
    hasResources: Boolean = false,
    isFiltered: Boolean = false,
    onAddSource: () -> Unit = {},
    onClearFilter: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (!hasResources) {
            // 完全空白：引导去添加数据源
            Text(
                text = "还没有资源",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "添加数据源后即可浏览资源",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAddSource) {
                Text("去添加数据源")
            }
        } else if (isFiltered) {
            // 筛选结果为空
            Text(
                text = "没有匹配的资源",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "尝试清除筛选条件",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onClearFilter) {
                Text("清除筛选")
            }
        }
    }
}
