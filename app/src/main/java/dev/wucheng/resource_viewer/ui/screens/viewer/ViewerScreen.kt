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
import dev.wucheng.resource_viewer.ui.screens.viewer.components.SlideBar
import dev.wucheng.resource_viewer.ui.screens.viewer.components.ViewerToolbar
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * 基础查看器页面。
 * HorizontalPager 翻页 + SlideBar 滑动条 + ViewerToolbar 工具栏。
 *
 * 注意：此实现遵循 doc/mvp/M14-basic-viewer.md 中的 M14.3 子任务。
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
                // 内容区域
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
                    // 每一页的内容
                    PageContent(
                        resourceId = resourceId,
                        pageIndex = page,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                // 顶部工具栏
                ViewerToolbar(
                    visible = toolbarVisible,
                    resourceName = resourceName,
                    pageInfo = "${currentPage + 1} / $totalPages",
                    onBackClick = onNavigateBack,
                    onSettingsClick = { /* TODO: M14.5 设置入口 */ },
                    modifier = Modifier.align(Alignment.TopCenter),
                )

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
        }
    }
}

/**
 * 单页内容。
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
