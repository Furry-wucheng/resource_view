package dev.wucheng.resource_viewer.ui.screens.viewer.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.wucheng.resource_viewer.data.local.converter.DoublePageMode
import dev.wucheng.resource_viewer.data.local.converter.PageDirection

private val PanelBg = Color.Black.copy(alpha = 0.65f)
private val PanelShape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)

@Composable
fun ViewerToolbar(
    visible: Boolean,
    resourceName: String,
    pageInfo: String,
    onBackClick: () -> Unit,
    pageDirection: PageDirection,
    doublePageMode: DoublePageMode,
    onPageDirectionClick: () -> Unit,
    onDoublePageModeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSettings by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PanelShape)
                .background(PanelBg)
                .padding(
                    top = WindowInsets.statusBars
                        .asPaddingValues()
                        .calculateTopPadding(),
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            // 主栏：返回 + 标题 + 页码 + 设置按钮
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White,
                    modifier = Modifier
                        .size(22.dp)
                        .clickableWithoutRipple(onBackClick),
                )

                Text(
                    text = resourceName,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                if (pageInfo.isNotBlank()) {
                    Text(
                        text = pageInfo,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                    )
                }

                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "设置",
                    tint = if (showSettings) Color.White else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(20.dp)
                        .clickableWithoutRipple { showSettings = !showSettings },
                )
            }

            // 设置面板
            AnimatedVisibility(visible = showSettings) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // 翻页方向
                    Text(
                        text = "翻页方向",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val directions = listOf(
                            PageDirection.LEFT_TO_RIGHT to "从左到右",
                            PageDirection.RIGHT_TO_LEFT to "从右到左",
                            PageDirection.VERTICAL to "纵向",
                        )
                        directions.forEachIndexed { index, (dir, label) ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = directions.size,
                                ),
                                onClick = { if (pageDirection != dir) onPageDirectionClick() },
                                selected = pageDirection == dir,
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = Color.White.copy(alpha = 0.15f),
                                    inactiveContainerColor = Color.Transparent,
                                    activeContentColor = Color.White,
                                    inactiveContentColor = Color.White.copy(alpha = 0.5f),
                                    activeBorderColor = Color.White.copy(alpha = 0.3f),
                                    inactiveBorderColor = Color.White.copy(alpha = 0.15f),
                                ),
                            ) {
                                Text(label, fontSize = 12.sp)
                            }
                        }
                    }

                    // 双页模式
                    Text(
                        text = "双页模式",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val modes = listOf(
                            DoublePageMode.AUTO to "自动",
                            DoublePageMode.SINGLE to "单页",
                            DoublePageMode.DOUBLE to "双页",
                        )
                        modes.forEachIndexed { index, (mode, label) ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = modes.size,
                                ),
                                onClick = { if (doublePageMode != mode) onDoublePageModeClick() },
                                selected = doublePageMode == mode,
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = Color.White.copy(alpha = 0.15f),
                                    inactiveContainerColor = Color.Transparent,
                                    activeContentColor = Color.White,
                                    inactiveContentColor = Color.White.copy(alpha = 0.5f),
                                    activeBorderColor = Color.White.copy(alpha = 0.3f),
                                    inactiveBorderColor = Color.White.copy(alpha = 0.15f),
                                ),
                            ) {
                                Text(label, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Modifier.clickableWithoutRipple(onClick: () -> Unit): Modifier {
    return this.then(
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        )
    )
}
