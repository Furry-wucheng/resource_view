package dev.wucheng.resource_viewer.ui.screens.viewer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 底部滑动条组件。
 * 显示当前页/总页数 + 拖动跳转。
 *
 * 注意：此实现遵循 doc/mvp/M14-basic-viewer.md 中的 M14.4 子任务。
 */
@Composable
fun SlideBar(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var sliderWidth by remember { mutableIntStateOf(0) }
    val progress = if (totalPages > 0) currentPage.toFloat() / (totalPages - 1).coerceAtLeast(1) else 0f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 当前页码
        Text(
            text = "${currentPage + 1}",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(24.dp),
        )

        // 滑动条
        Box(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .onSizeChanged { size ->
                    sliderWidth = size.width
                }
                .pointerInput(totalPages) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        if (sliderWidth > 0) {
                            val x = change.position.x.coerceIn(0f, sliderWidth.toFloat())
                            val fraction = x / sliderWidth
                            val newPage = (fraction * (totalPages - 1)).toInt().coerceIn(0, totalPages - 1)
                            onPageChange(newPage)
                        }
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            // 背景轨道
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.25f)),
            )

            // 填充轨道
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White),
            )

            // 滑块
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress)
                    .wrapContentWidth(Alignment.End)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }

        // 总页数
        Text(
            text = "$totalPages",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(24.dp),
        )
    }
}
