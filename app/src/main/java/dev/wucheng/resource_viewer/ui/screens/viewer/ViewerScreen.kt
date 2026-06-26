@file:Suppress("UnsafeOptInUsageError")

package dev.wucheng.resource_viewer.ui.screens.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.wucheng.resource_viewer.domain.model.ViewerItem
import dev.wucheng.resource_viewer.ui.screens.viewer.components.SlideBar
import dev.wucheng.resource_viewer.ui.screens.viewer.components.VideoPlayer
import dev.wucheng.resource_viewer.ui.screens.viewer.components.ViewerToolbar
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * 基础查看器页面。
 * HorizontalPager 翻页 + SlideBar 滑动条 + ViewerToolbar 工具栏。
 *
 * 支持图片页面 (M14) 和视频播放 (M19)。
 *
 * 注意：此实现遵循 doc/mvp/M14-basic-viewer.md + doc/mvp/M19-video-player.md。
 */
@Composable
fun ViewerScreen(
    resourceId: String,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    viewModel: ViewerViewModel = koinViewModel { parametersOf(resourceId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentPage by viewModel.currentPage.collectAsStateWithLifecycle()
    val totalPages by viewModel.totalPages.collectAsStateWithLifecycle()
    val resourceName by viewModel.resourceName.collectAsStateWithLifecycle()

    // 工具栏可见性
    var toolbarVisible by remember { mutableStateOf(true) }

    // 加载资源
    LaunchedEffect(resourceId) {
        viewModel.loadResource()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when (val state = uiState) {
            is ViewerUiState.Loading -> {
                // 加载中
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            is ViewerUiState.Error -> {
                // 错误状态
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = state.message,
                        color = Color.White,
                    )
                    Text(
                        text = "点击重试",
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.clickable {
                            viewModel.loadResource()
                        },
                    )
                }
            }

            is ViewerUiState.Success -> {
                val items = state.items

                // 检查是否为纯视频资源（只有 1 个 Video item）
                val isVideoOnly = items.size == 1 && items[0] is ViewerItem.Video

                if (isVideoOnly) {
                    // 纯视频模式：直接显示 VideoPlayer
                    val videoItem = items[0] as ViewerItem.Video
                    VideoPageContent(
                        videoItem = videoItem,
                        onToggleToolbar = { toolbarVisible = !toolbarVisible },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    // 图片/PDF 模式：HorizontalPager 翻页
                    val pagerState = rememberPagerState(
                        initialPage = currentPage,
                        pageCount = { totalPages },
                    )

                    // 同步 ViewModel 的 currentPage
                    LaunchedEffect(pagerState.currentPage) {
                        viewModel.goToPage(pagerState.currentPage)
                    }

                    // HorizontalPager
                    HorizontalPager(
                        state = pagerState,
                        beyondViewportPageCount = 2,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                toolbarVisible = !toolbarVisible
                            },
                    ) { page ->
                        val item = items[page]
                        when (item) {
                            is ViewerItem.ImagePage -> {
                                PageContent(
                                    resourceId = resourceId,
                                    pageIndex = item.pageIndex,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            is ViewerItem.Video -> {
                                // 混合模式中的视频页（少见，但支持）
                                VideoPageContent(
                                    videoItem = item,
                                    onToggleToolbar = { toolbarVisible = !toolbarVisible },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }

                    // 底部 SlideBar
                    SlideBar(
                        currentPage = currentPage,
                        totalPages = totalPages,
                        onPageChange = { page ->
                            viewModel.goToPage(page)
                        },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }

                // 顶部工具栏（视频和图片模式都显示）
                ViewerToolbar(
                    visible = toolbarVisible,
                    resourceName = resourceName,
                    pageInfo = if (isVideoOnly) "" else "${currentPage + 1} / $totalPages",
                    onBackClick = onNavigateBack,
                    onSettingsClick = { /* TODO: M14.5 设置入口 */ },
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }
    }
}

/**
 * 视频页面内容。
 * 使用 [VideoPlayer] Composable 播放视频。
 */
@Composable
private fun VideoPageContent(
    videoItem: ViewerItem.Video,
    onToggleToolbar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val videoPlayerViewModel: VideoPlayerViewModel = koinViewModel<VideoPlayerViewModel>()

    // 加载视频
    LaunchedEffect(videoItem) {
        videoPlayerViewModel.loadMedia(videoItem.videoSource)
    }

    VideoPlayer(
        viewModel = videoPlayerViewModel,
        onToggleToolbar = onToggleToolbar,
        modifier = modifier,
    )
}

/**
 * 单页内容（图片占位符）。
 */
@Composable
private fun PageContent(
    resourceId: String,
    pageIndex: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // TODO: 使用 Coil 加载图片
    // 目前显示占位符
    Box(
        modifier = modifier.background(Color.DarkGray),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Page $pageIndex",
            color = Color.White.copy(alpha = 0.4f),
        )
    }
}
