package dev.wucheng.resource_viewer.ui.screens.viewer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PanelBg = Color.Black.copy(alpha = 0.65f)
private val PanelShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)

@Composable
fun SlideBar(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit,
    reverseDirection: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val maxIndex = (totalPages - 1).coerceAtLeast(1)
    val sliderValue = if (reverseDirection) (maxIndex - currentPage).toFloat() else currentPage.toFloat()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(PanelBg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "${currentPage + 1}",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(28.dp),
        )

        Slider(
            value = sliderValue,
            onValueChange = { newValue ->
                val page = if (reverseDirection) (maxIndex - newValue).toInt() else newValue.toInt()
                onPageChange(page.coerceIn(0, totalPages - 1))
            },
            valueRange = 0f..maxIndex.toFloat(),
            steps = maxIndex.coerceAtLeast(0),
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f),
            ),
        )

        Text(
            text = "$totalPages",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(28.dp),
        )
    }
}
