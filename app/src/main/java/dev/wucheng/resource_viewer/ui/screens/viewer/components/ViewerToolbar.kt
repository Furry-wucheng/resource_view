package dev.wucheng.resource_viewer.ui.screens.viewer.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 顶部工具栏组件。
 * 可点击显隐，显示资源名称 + 返回按钮 + 设置入口。
 *
 * 注意：此实现遵循 doc/mvp/M14-basic-viewer.md 中的 M14.5 子任务。
 */
@Composable
fun ViewerToolbar(
    visible: Boolean,
    resourceName: String,
    pageInfo: String,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Transparent,
                        )
                    )
                )
                .padding(
                    top = WindowInsets.statusBars
                        .asPaddingValues()
                        .calculateTopPadding(),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 返回按钮
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onBackClick),
                )

                // 资源名称
                Text(
                    text = resourceName,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                // 页码信息
                Text(
                    text = pageInfo,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                )

                // 设置按钮
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "设置",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onSettingsClick),
                )
            }
        }
    }
}
