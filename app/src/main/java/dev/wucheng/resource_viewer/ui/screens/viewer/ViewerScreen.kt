@file:Suppress("UnsafeOptInUsageError")

package dev.wucheng.resource_viewer.ui.screens.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.wucheng.resource_viewer.domain.model.ViewerItem
import dev.wucheng.resource_viewer.data.local.converter.PageDirection
import dev.wucheng.resource_viewer.data.local.converter.DoublePageMode
import dev.wucheng.resource_viewer.ui.screens.viewer.components.SlideBar
import dev.wucheng.resource_viewer.ui.screens.viewer.components.VideoPlayer
import dev.wucheng.resource_viewer.ui.screens.viewer.components.ViewerToolbar
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import net.engawapg.lib.zoomable.ScrollGesturePropagation
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

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
    LaunchedEffect(resourceId) {
        viewModel.loadResource()
        viewModel.loadChapters()
    }
    ViewerScreenContent(viewModel = viewModel, onNavigateBack = onNavigateBack, modifier = modifier)
}

/**
 * 查看器核心内容。被 ViewerScreen(resourceId) 和 ViewerScreen(sourceId, filePath) 共用。
 */
@Composable
fun ViewerScreenContent(
    viewModel: ViewerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentPage by viewModel.currentPage.collectAsStateWithLifecycle()
    val totalPages by viewModel.totalPages.collectAsStateWithLifecycle()
    val resourceName by viewModel.resourceName.collectAsStateWithLifecycle()
    val pageDirection by viewModel.pageDirection.collectAsStateWithLifecycle()
    val doublePageMode by viewModel.doublePageMode.collectAsStateWithLifecycle()
    val chapterHint by viewModel.chapterHint.collectAsStateWithLifecycle()
    val isFavorited by viewModel.isFavorited.collectAsStateWithLifecycle()

    // 工具栏可见性
    var toolbarVisible by remember { mutableStateOf(true) }

    // 沉浸模式：进入时隐藏系统栏，退出时恢复
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as android.app.Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
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
                        toolbarVisible = toolbarVisible,
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
                    val spreads = remember(items, useDoublePage, doublePageMode) {
                        buildViewerSpreads(items, if (useDoublePage) doublePageMode else DoublePageMode.SINGLE)
                    }
                    val visualPageCount = spreads.size
                    // 图片/PDF 模式：HorizontalPager 翻页
                    val pagerState = rememberPagerState(
                        initialPage = spreads.spreadIndexForItem(currentPage),
                        pageCount = { visualPageCount },
                    )

                    // 同步 ViewModel 的 currentPage
                    LaunchedEffect(pagerState.currentPage) {
                        spreads.getOrNull(pagerState.currentPage)?.itemIndices?.firstOrNull()?.let(viewModel::goToPage)
                    }
                    LaunchedEffect(currentPage, spreads) {
                        val visualPage = spreads.spreadIndexForItem(currentPage)
                        if (pagerState.currentPage != visualPage) {
                            pagerState.animateScrollToPage(visualPage)
                        }
                    }

                    // 跨章节导航：检测是否到达最后一页
                    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
                        if (!pagerState.isScrollInProgress && viewModel.canNavigateChapter()) {
                            if (pagerState.currentPage >= visualPageCount - 1) {
                                // 到达最后一页，尝试导航到下一章
                                viewModel.navigateToNextChapter()
                            } else if (pagerState.currentPage == 0) {
                                // 到达第一页，尝试导航到上一章
                                viewModel.navigateToPrevChapter()
                            }
                        }
                    }

                    val pagerModifier = Modifier.fillMaxSize()

                    if (pageDirection == PageDirection.VERTICAL) {
                        VerticalPager(
                            state = pagerState,
                            beyondViewportPageCount = 0,
                            modifier = pagerModifier,
                        ) { page ->
                            ViewerPagerContent(items, spreads[page], pageDirection, viewModel, toolbarVisible) {
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
                            ViewerPagerContent(items, spreads[page], pageDirection, viewModel, toolbarVisible) {
                                toolbarVisible = !toolbarVisible
                            }
                        }
                    }

                    // 底部 SlideBar（视频模式或工具栏隐藏时隐藏）
                    val currentItem = items.getOrNull(currentPage)
                    val isCurrentVideo = currentItem is ViewerItem.Video
                    if (!isCurrentVideo && toolbarVisible) {
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

                // 章节过渡提示
                AnimatedVisibility(
                    visible = chapterHint != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter),
                ) {
                    chapterHint?.let { hint ->
                        Box(
                            modifier = Modifier
                                .padding(top = 80.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.7f),
                                    shape = MaterialTheme.shapes.medium,
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = hint,
                                color = Color.White,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 文件浏览器直接查看文件的入口。
 * 复用完整 ViewerScreen 基础设施（翻页、手势、滑动条、预加载、视频播放）。
 */
@Composable
fun ViewerScreen(
    sourceId: String,
    filePath: String,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    viewModel: ViewerViewModel = koinViewModel { parametersOf("__file__", "", 0) },
) {
    // 使用 loadFromSource 代替 loadResource
    LaunchedEffect(sourceId, filePath) {
        viewModel.loadFromSource(sourceId, filePath)
    }

    // 复用同一个 ViewerScreen 主体
    ViewerScreenContent(
        viewModel = viewModel,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@Composable
private fun ViewerPagerPage(
    item: ViewerItem,
    viewModel: ViewerViewModel,
    toolbarVisible: Boolean,
    onToggleToolbar: () -> Unit,
) {
    when (item) {
        is ViewerItem.ImagePage -> PageContent(viewModel, item.pageIndex, onToggleToolbar, Modifier.fillMaxSize())
        is ViewerItem.Video -> VideoPageContent(item, toolbarVisible, onToggleToolbar, Modifier.fillMaxSize())
    }
}

@Composable
private fun ViewerPagerContent(
    items: List<ViewerItem>,
    spread: ViewerSpread,
    pageDirection: PageDirection,
    viewModel: ViewerViewModel,
    toolbarVisible: Boolean,
    onToggleToolbar: () -> Unit,
) {
    if (spread.itemIndices.size == 1) {
        ViewerPagerPage(items[spread.itemIndices.first()], viewModel, toolbarVisible, onToggleToolbar)
        return
    }
    val pair = spread.itemIndices.map(items::get)
    val orderedPair = if (pageDirection == PageDirection.RIGHT_TO_LEFT) pair.reversed() else pair
    Row(Modifier.fillMaxSize()) {
        orderedPair.forEach { item ->
            Box(Modifier.weight(1f).fillMaxHeight()) {
                ViewerPagerPage(item, viewModel, toolbarVisible, onToggleToolbar)
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
    toolbarVisible: Boolean,
    onToggleToolbar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playerKey = remember(videoItem.videoSource) { "video:${videoItem.videoSource.hashCode()}" }
    val videoPlayerViewModel: VideoPlayerViewModel = koinViewModel(key = playerKey)

    LaunchedEffect(videoItem) {
        videoPlayerViewModel.loadMedia(videoItem.videoSource)
    }

    DisposableEffect(videoPlayerViewModel) {
        onDispose { videoPlayerViewModel.pause() }
    }

    VideoPlayer(
        viewModel = videoPlayerViewModel,
        toolbarVisible = toolbarVisible,
        onToggleToolbar = onToggleToolbar,
        modifier = modifier,
    )
}

/**
 * 单页内容（图片）。
 * 使用 zoomable 库支持捏合缩放、双击缩放、边缘滑动翻页。
 */
@Composable
private fun PageContent(
    viewModel: ViewerViewModel,
    pageIndex: Int,
    onToggleToolbar: () -> Unit,
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
                val bitmap = remember(state.bitmap) { state.bitmap.asImageBitmap() }
                val zoomState = rememberZoomState(contentSize = androidx.compose.ui.geometry.Size(
                    state.bitmap.width.toFloat(), state.bitmap.height.toFloat()
                ))

                Image(
                    bitmap = bitmap,
                    contentDescription = "Page ${pageIndex + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .zoomable(
                            zoomState = zoomState,
                            scrollGesturePropagation = ScrollGesturePropagation.ContentEdge,
                            onTap = { onToggleToolbar() },
                        ),
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
