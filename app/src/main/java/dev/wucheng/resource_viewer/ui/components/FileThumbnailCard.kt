package dev.wucheng.resource_viewer.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.wucheng.resource_viewer.domain.model.FileEntry

/** 缩略图宽高比 */
private const val THUMBNAIL_CARD_ASPECT_RATIO = 3f / 4f

fun fileTypeColor(entry: FileEntry): Color = when {
    entry.isDirectory -> Color(0xFF1565C0)
    entry.extension.lowercase() in setOf("mp4", "mkv", "avi", "mov", "webm", "wmv") -> Color(0xFF2E7D32)
    entry.extension.lowercase() == "pdf" -> Color(0xFFC62828)
    else -> Color(0xFF757575)
}

fun fileTypeIcon(entry: FileEntry): ImageVector = when {
    entry.isDirectory -> Icons.Default.Folder
    entry.extension.lowercase() in setOf("mp4", "mkv", "avi", "mov", "webm", "wmv") -> Icons.Default.Movie
    entry.extension.lowercase() == "pdf" -> Icons.Default.PictureAsPdf
    else -> Icons.Default.Folder
}

/**
 * 统一的文件缩略图卡片组件。
 * 被 FileBrowser、ContentGrid、ChapterList 共享。
 *
 * @param entry 文件条目
 * @param loadThumbnail 缩略图加载回调（由 ViewModel 的 ThumbnailLoadManager 提供）
 * @param selected 是否处于多选选中状态
 * @param onLongClick 长按回调
 * @param leadingIcon 左上角 slot
 * @param trailingIcon 右上角 slot
 * @param bottomEndBadge 右下角 slot（如文件夹角标）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileThumbnailCard(
    entry: FileEntry,
    loadThumbnail: suspend (FileEntry) -> Bitmap?,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    bottomEndBadge: (@Composable () -> Unit)? = null,
) {
    val bitmap by produceState<Bitmap?>(null, entry.relativePath) {
        value = loadThumbnail(entry)
    }
    val hasThumbnail = bitmap != null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(THUMBNAIL_CARD_ASPECT_RATIO)
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(onClick = { }, onLongClick = onLongClick)
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (hasThumbnail) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = entry.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(fileTypeColor(entry)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = fileTypeIcon(entry),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color.White.copy(alpha = 0.72f),
                    )
                }
            }

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
                    text = entry.name,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            leadingIcon?.invoke()

            trailingIcon?.let {
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)) { it() }
            }

            bottomEndBadge?.let {
                Box(modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)) { it() }
            }
        }
    }
}
