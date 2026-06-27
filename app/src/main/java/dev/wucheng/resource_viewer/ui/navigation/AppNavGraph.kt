package dev.wucheng.resource_viewer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.wucheng.resource_viewer.ui.screens.home.HomeScreen
import dev.wucheng.resource_viewer.ui.screens.settings.SettingsScreen
import dev.wucheng.resource_viewer.ui.screens.sources.FileBrowserScreen
import dev.wucheng.resource_viewer.ui.screens.sources.SourceListScreen
import dev.wucheng.resource_viewer.ui.screens.tags.TagManagerScreen
import dev.wucheng.resource_viewer.ui.screens.viewer.ChapterListScreen
import dev.wucheng.resource_viewer.ui.screens.viewer.ContentGridMode
import dev.wucheng.resource_viewer.ui.screens.viewer.ContentGridScreen
import dev.wucheng.resource_viewer.ui.screens.viewer.ViewerScreen
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
    ) {
        // === M03 创建：底部 Tab ===
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToViewer = { resource ->
                    val route = when (resolveResourceDestination(resource.organizationMode)) {
                        ResourceDestination.CHAPTER_LIST -> Screen.ChapterList.createRoute(resource.id)
                        ResourceDestination.FLAT_GRID -> Screen.FlatGrid.createRoute(resource.id)
                        ResourceDestination.GALLERY -> Screen.Gallery.createRoute(resource.id)
                        ResourceDestination.VIEWER -> Screen.Viewer.createRoute(resource.id)
                    }
                    navController.navigate(route)
                },
                onNavigateToAddSource = {
                    navController.navigate(Screen.Sources.route)
                },
            )
        }
        composable(Screen.Sources.route) {
            SourceListScreen(
                onNavigateToBrowser = { sourceId ->
                    navController.navigate(Screen.FileBrowser.createRoute(sourceId))
                },
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }

        // === 全屏路由 ===
        composable(Screen.Viewer.route) { backStackEntry ->
            val resourceId = backStackEntry.arguments?.getString("resourceId") ?: return@composable
            ViewerScreen(
                resourceId = resourceId,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Screen.TagManager.route) {
            TagManagerScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Screen.FileBrowser.route) { backStackEntry ->
            val sourceId = backStackEntry.arguments?.getString("sourceId") ?: return@composable
            FileBrowserScreen(
                sourceId = sourceId,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // === M21 创建：章节列表 ===
        composable(Screen.ChapterList.route) { backStackEntry ->
            val resourceId = backStackEntry.arguments?.getString("resourceId") ?: return@composable
            ChapterListScreen(
                resourceId = resourceId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToViewer = { resId, chapterPath ->
                    navController.navigate(Screen.ChapterViewer.createRoute(resId, chapterPath))
                },
            )
        }

        // === 后续 stage 在此追加 composable ===
        composable(Screen.ChapterViewer.route) { backStackEntry ->
            val resourceId = backStackEntry.arguments?.getString("resourceId") ?: return@composable
            val encodedPath = backStackEntry.arguments?.getString("path") ?: return@composable
            val chapterPath = URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.toString())
            ViewerScreen(
                resourceId = resourceId,
                contentPath = chapterPath,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Screen.FlatGrid.route) { backStackEntry ->
            val resourceId = backStackEntry.arguments?.getString("resourceId") ?: return@composable
            ContentGridScreen(
                resourceId = resourceId,
                mode = ContentGridMode.FLAT_GRID,
                onNavigateBack = { navController.popBackStack() },
                onOpenViewer = { path, page ->
                    navController.navigate(Screen.SequenceViewer.createRoute(resourceId, path, page))
                },
            )
        }
        composable(Screen.Gallery.route) { backStackEntry ->
            val resourceId = backStackEntry.arguments?.getString("resourceId") ?: return@composable
            ContentGridScreen(
                resourceId = resourceId,
                mode = ContentGridMode.GALLERY,
                onNavigateBack = { navController.popBackStack() },
                onOpenViewer = { path, page ->
                    navController.navigate(Screen.SequenceViewer.createRoute(resourceId, path, page))
                },
            )
        }
        composable(Screen.SequenceViewer.route) { backStackEntry ->
            val resourceId = backStackEntry.arguments?.getString("resourceId") ?: return@composable
            val encodedPath = backStackEntry.arguments?.getString("path") ?: return@composable
            val initialPage = backStackEntry.arguments?.getString("page")?.toIntOrNull() ?: 0
            ViewerScreen(
                resourceId = resourceId,
                contentPath = URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.toString()),
                initialPage = initialPage,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
