package dev.wucheng.resource_viewer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.wucheng.resource_viewer.domain.model.Tag

/**
 * 标签筛选栏。
 * 水平滚动显示所有标签，点击切换选中状态。
 *
 * @param tags 所有标签列表
 * @param selectedTagIds 当前选中的标签 ID 集合
 * @param onTagClick 标签点击回调，null 表示点击"全部"
 * @param modifier Modifier
 */
@Composable
fun FilterBar(
    tags: List<Tag>,
    selectedTagIds: Set<String>,
    onTagClick: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // "全部" 按钮
        item {
            FilterChip(
                selected = selectedTagIds.isEmpty(),
                onClick = { onTagClick(null) },
                label = { Text("全部") },
            )
        }

        // 标签列表
        items(tags, key = { it.id }) { tag ->
            val isSelected = selectedTagIds.contains(tag.id)
            val tagColor = try {
                Color(android.graphics.Color.parseColor(tag.color))
            } catch (e: Exception) {
                MaterialTheme.colorScheme.outline
            }

            FilterChip(
                selected = isSelected,
                onClick = { onTagClick(tag.id) },
                label = { Text(tag.name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = tagColor.copy(alpha = 0.2f),
                    selectedLabelColor = tagColor,
                ),
            )
        }
    }
}
