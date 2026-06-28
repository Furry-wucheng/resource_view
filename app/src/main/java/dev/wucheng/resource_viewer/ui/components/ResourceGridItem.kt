package dev.wucheng.resource_viewer.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.domain.model.Resource
import java.io.File

/**
 * 资源缩略图卡片。
 * 采用文件浏览器的 Card 卡片风格：
 * - 3:4 比例，底部渐变遮罩 + 白色文字
 * - 无缩略图时显示类型颜色 + 类型图标
 * - 左上角收藏星标（保留）
 * - 右上角选中标记
 *
 * @param resource 资源数据
 * @param onClick 点击回调
 * @param onLongClick 长按回调（可选）
 * @param selected 是否处于多选选中状态
 * @param onToggleFavorite 收藏切换回调（null 则不显示星标）
 * @param modifier Modifier
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ResourceGridItem(
    resource: Resource,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(THUMBNAIL_ASPECT_RATIO)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val thumbnailPath = resource.thumbnailPath

            if (thumbnailPath != null) {
                // 有缩略图：Coil 加载本地文件（必须传 File 对象，否则会被当作 URL）
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(thumbnailPath))
                        .build(),
                    contentDescription = resource.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                // 无缩略图：类型颜色 + 类型图标
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(resourceTypeColor(resource.type)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = resourceTypeIcon(resource.type),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color.White.copy(alpha = 0.72f),
                    )
                }
            }

            // 底部渐变遮罩 + 标题
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                        )
                    )
                    .padding(start = 6.dp, end = 6.dp, top = 40.dp, bottom = 8.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Text(
                    text = resource.name,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // 收藏星标（左上角）
            if (onToggleFavorite != null) {
                Icon(
                    imageVector = if (resource.favorited) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (resource.favorited) "取消收藏" else "收藏",
                    tint = if (resource.favorited) Color(0xFFFFD700) else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(20.dp)
                        .clickable { onToggleFavorite() },
                )
            }

            // 选中标记（右上角）
            if (selected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "已选择",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp),
                )
            }
        }
    }
}

private fun resourceTypeColor(type: ResourceType): Color = when (type) {
    ResourceType.FOLDER -> Color(0xFF1565C0)
    ResourceType.PDF -> Color(0xFFC62828)
    ResourceType.VIDEO -> Color(0xFF2E7D32)
    ResourceType.ARCHIVE -> Color(0xFF757575)
}

private fun resourceTypeIcon(type: ResourceType): ImageVector = when (type) {
    ResourceType.FOLDER -> Icons.Default.Folder
    ResourceType.PDF -> Icons.Default.PictureAsPdf
    ResourceType.VIDEO -> Icons.Default.Movie
    ResourceType.ARCHIVE -> Icons.Default.Folder
}

/** 缩略图宽高比（与文件浏览器一致） */
private const val THUMBNAIL_ASPECT_RATIO = 3f / 4f
