@file:Suppress("UnsafeOptInUsageError")

package dev.wucheng.resource_viewer.ui.screens.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.wucheng.resource_viewer.domain.model.ViewerItem
import dev.wucheng.resource_viewer.data.local.converter.PageDirection
import dev.wucheng.resource_viewer.data.local.converter.DoublePageMode
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
    contentPath: String = "",
    initialPage: Int = 0,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    viewModel: ViewerViewModel = koinViewModel { parametersOf(resourceId, contentPath, initialPage) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentPage by viewModel.currentPage.collectAsStateWithLifecycle()
    val totalPages by viewModel.totalPages.collectAsStateWithLifecycle()
    val resourceName by viewModel.resourceName.collectAsStateWithLifecycle()
    val pageDirection by viewModel.pageDirection.collectAsStateWithLifecycle()
    val doublePageMode by viewModel.doublePageMode.collectAsStateWithLifecycle()

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
                    val configuration = LocalConfiguration.current
                    val useDoublePage = pageDirection != PageDirection.VERTICAL && when (doublePageMode) {
                        DoublePageMode.DOUBLE -> true
                        DoublePageMode.SINGLE -> false
                        DoublePageMode.AUTO -> configuration.screenWidthDp > configuration.screenHeightDp
                    }
                    val visualPageCount = if (useDoublePage) (totalPages + 1) / 2 else totalPages
                    // 图片/PDF 模式：HorizontalPager 翻页
                    val pagerState = rememberPagerState(
                        initialPage = if (useDoublePage) currentPage / 2 else currentPage,
                        pageCount = { visualPageCount },
                    )

                    // 同步 ViewModel 的 currentPage
                    LaunchedEffect(pagerState.currentPage) {
                        viewModel.goToPage(if (useDoublePage) pagerState.currentPage * 2 else pagerState.currentPage)
                    }
                    LaunchedEffect(currentPage, useDoublePage) {
                        val visualPage = if (useDoublePage) currentPage / 2 else currentPage
                        if (pagerState.currentPage != visualPage) {
                            pagerState.animateScrollToPage(visualPage)
                        }
                    }

                    val pagerModifier = Modifier
                        .fillMaxSize()
                        .pointerInput(pageDirection, totalPages) {
                            detectTapGestures { offset ->
                                when (resolveTapAction(offset.x, offset.y, size.width.toFloat(), size.height.toFloat(), pageDirection)) {
                                    ViewerTapAction.PREVIOUS -> viewModel.goToPage(currentPage - if (useDoublePage) 2 else 1)
                                    ViewerTapAction.NEXT -> viewModel.goToPage(currentPage + if (useDoublePage) 2 else 1)
                                    ViewerTapAction.TOGGLE_TOOLBAR -> toolbarVisible = !toolbarVisible
                                }
                            }
                        }

                    if (pageDirection == PageDirection.VERTICAL) {
                        VerticalPager(
                            state = pagerState,
                            beyondViewportPageCount = 0,
                            modifier = pagerModifier,
                        ) { page ->
                            ViewerPagerContent(items, page, useDoublePage, pageDirection, viewModel) {
                                toolbarVisible = !toolbarVisible
                            }
                        }
                    } else {
                        HorizontalPager(
                            state = pagerState,
                            reverseLayout = pageDirection == PageDirection.RIGHT_TO_LEFT,
                            beyondViewportPageCount = 0,
                            modifier = pagerModifier,
                        ) { page ->
                            ViewerPagerContent(items, page, useDoublePage, pageDirection, viewModel) {
                                toolbarVisible = !toolbarVisible
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
                        reverseDirection = pageDirection == PageDirection.RIGHT_TO_LEFT,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }

                // 顶部工具栏（视频和图片模式都显示）
                ViewerToolbar(
                    visible = toolbarVisible,
                    resourceName = resourceName,
                    pageInfo = if (isVideoOnly) "" else "${currentPage + 1} / $totalPages",
                    onBackClick = onNavigateBack,
                    pageDirection = pageDirection,
                    doublePageMode = doublePageMode,
                    onPageDirectionClick = viewModel::cyclePageDirection,
                    onDoublePageModeClick = viewModel::cycleDoublePageMode,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }
    }
}

@Composable
private fun ViewerPagerPage(
    item: ViewerItem,
    viewModel: ViewerViewModel,
    onToggleToolbar: () -> Unit,
) {
    when (item) {
        is ViewerItem.ImagePage -> PageContent(viewModel, item.pageIndex, Modifier.fillMaxSize())
        is ViewerItem.Video -> VideoPageContent(item, onToggleToolbar, Modifier.fillMaxSize())
    }
}

@Composable
private fun ViewerPagerContent(
    items: List<ViewerItem>,
    visualPage: Int,
    useDoublePage: Boolean,
    pageDirection: PageDirection,
    viewModel: ViewerViewModel,
    onToggleToolbar: () -> Unit,
) {
    if (!useDoublePage) {
        ViewerPagerPage(items[visualPage], viewModel, onToggleToolbar)
        return
    }
    val firstIndex = visualPage * 2
    val pair = listOfNotNull(items.getOrNull(firstIndex), items.getOrNull(firstIndex + 1))
    val orderedPair = if (pageDirection == PageDirection.RIGHT_TO_LEFT) pair.reversed() else pair
    Row(Modifier.fillMaxSize()) {
        orderedPair.forEach { item ->
            Box(Modifier.weight(1f).fillMaxHeight()) {
                ViewerPagerPage(item, viewModel, onToggleToolbar)
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
    viewModel: ViewerViewModel,
    pageIndex: Int,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.background(Color.Black)) {
        val density = LocalDensity.current
        val targetWidth = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
        val targetHeight = with(density) { maxHeight.roundToPx() }.coerceAtLeast(1)
        var retryCount by remember(pageIndex, targetWidth, targetHeight) { mutableIntStateOf(0) }
        val pageState by produceState<PageBitmapState>(
            initialValue = PageBitmapState.Loading,
            pageIndex,
            targetWidth,
            targetHeight,
            retryCount,
        ) {
            value = try {
                PageBitmapState.Success(
                    viewModel.loadPageBitmap(pageIndex, targetWidth, targetHeight)
                )
            } catch (e: Exception) {
                PageBitmapState.Error(e.message ?: "页面加载失败")
            }
        }

        when (val state = pageState) {
            PageBitmapState.Loading -> {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            is PageBitmapState.Success -> {
                Image(
                    bitmap = state.bitmap.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            is PageBitmapState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = state.message,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    Text(
                        text = "重试",
                        color = Color.White,
                        modifier = Modifier.clickable { retryCount += 1 },
                    )
                }
            }
        }
    }
}

private sealed class PageBitmapState {
    data object Loading : PageBitmapState()
    data class Success(val bitmap: android.graphics.Bitmap) : PageBitmapState()
    data class Error(val message: String) : PageBitmapState()
}
