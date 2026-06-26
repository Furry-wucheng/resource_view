package dev.wucheng.resource_viewer.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.domain.model.Tag

/**
 * 资源缩略图卡片。
 * 竖版卡片，显示封面图片、资源名称（最多 2 行）、标签颜色小圆点（前 3 个）。
 * 支持点击和长按。
 *
 * @param resource 资源数据
 * @param onClick 点击回调
 * @param onLongClick 长按回调（可选）
 * @param modifier Modifier
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ResourceGridItem(
    resource: Resource,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        // 缩略图封面
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(THUMBNAIL_ASPECT_RATIO)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (resource.thumbnailPath != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(resource.thumbnailPath)
                        .build(),
                    contentDescription = resource.name,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        // 资源名称（最多 2 行）
        Text(
            text = resource.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = MAX_TITLE_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )

        // 标签颜色小圆点（前 3 个）
        if (resource.tags.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                resource.tags.take(MAX_TAG_DOTS).forEach { tag ->
                    TagDot(tag = tag)
                }
            }
        }
    }
}

/**
 * 标签颜色小圆点
 */
@Composable
private fun TagDot(tag: Tag) {
    val color = try {
        Color(android.graphics.Color.parseColor(tag.color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = Modifier
            .size(TAG_DOT_SIZE)
            .semantics { contentDescription = "标签: ${tag.name}" }
            .clip(CircleShape)
            .background(color),
    )
}

/** 缩略图宽高比 */
private const val THUMBNAIL_ASPECT_RATIO = 2f / 3f

/** 标题最大行数 */
private const val MAX_TITLE_LINES = 2

/** 最大标签圆点数 */
private const val MAX_TAG_DOTS = 3

/** 标签圆点大小 */
private val TAG_DOT_SIZE = 8.dp
