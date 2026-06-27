package dev.wucheng.resource_viewer.ui.screens.viewer.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode

/**
 * 组织模式切换器组件。
 * 并排模式按钮，选中项白色凸起，未选中项灰色扁平。
 *
 * 支持四种模式：章节、章廊、平铺、画廊。
 * 可选择禁用章节相关模式（当资源没有子目录时）。
 *
 * @param currentMode 当前组织模式
 * @param onModeChanged 模式切换回调
 * @param chapterEnabled 是否启用章节相关模式
 * @param modifier Modifier
 */
@Composable
fun OrgModeSwitcher(
    currentMode: OrganizationMode?,
    onModeChanged: (OrganizationMode) -> Unit,
    chapterEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        OrgModeOption(
            mode = OrganizationMode.CHAPTER,
            label = "章节",
            icon = Icons.Default.MenuBook,
            isSelected = currentMode == OrganizationMode.CHAPTER,
            enabled = chapterEnabled,
            onClick = { onModeChanged(OrganizationMode.CHAPTER) },
        )
        OrgModeOption(
            mode = OrganizationMode.CHAPTER_GALLERY,
            label = "章廊",
            icon = Icons.Default.AutoStories,
            isSelected = currentMode == OrganizationMode.CHAPTER_GALLERY,
            enabled = chapterEnabled,
            onClick = { onModeChanged(OrganizationMode.CHAPTER_GALLERY) },
        )
        OrgModeOption(
            mode = OrganizationMode.FLATGRID,
            label = "平铺",
            icon = Icons.Default.GridView,
            isSelected = currentMode == OrganizationMode.FLATGRID,
            enabled = true,
            onClick = { onModeChanged(OrganizationMode.FLATGRID) },
        )
        OrgModeOption(
            mode = OrganizationMode.GALLERY,
            label = "画廊",
            icon = Icons.Default.Dashboard,
            isSelected = currentMode == OrganizationMode.GALLERY,
            enabled = true,
            onClick = { onModeChanged(OrganizationMode.GALLERY) },
        )
    }
}

@Composable
private fun OrgModeOption(
    mode: OrganizationMode,
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(durationMillis = 150),
        label = "orgModeBg",
    )

    val foregroundColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val shape = RoundedCornerShape(6.dp)

    Row(
        modifier = modifier
            .height(32.dp)
            .then(
                if (isSelected) {
                    Modifier.shadow(elevation = 1.dp, shape = shape)
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(backgroundColor)
            .then(
                if (enabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = foregroundColor,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) {
                androidx.compose.ui.text.font.FontWeight.W600
            } else {
                androidx.compose.ui.text.font.FontWeight.Normal
            },
            color = foregroundColor,
        )
    }
}
