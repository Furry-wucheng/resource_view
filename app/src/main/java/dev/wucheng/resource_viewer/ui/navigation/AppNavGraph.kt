package dev.wucheng.resource_viewer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.wucheng.resource_viewer.ui.screens.home.HomeScreen
import dev.wucheng.resource_viewer.ui.screens.settings.SettingsScreen
import dev.wucheng.resource_viewer.ui.screens.sources.SourceListScreen
import dev.wucheng.resource_viewer.ui.screens.tags.TagManagerScreen
import dev.wucheng.resource_viewer.ui.screens.viewer.ViewerScreen

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
                onNavigateToViewer = { resourceId ->
                    navController.navigate(Screen.Viewer.createRoute(resourceId))
                },
            )
        }
        composable(Screen.Sources.route) {
            SourceListScreen(
                onNavigateToBrowser = { sourceId ->
                    // TODO: M13 追加 FileBrowser 路由
                },
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }

        // === 全屏路由占位 ===
        composable(Screen.Viewer.route) { backStackEntry ->
            val resourceId = backStackEntry.arguments?.getString("resourceId") ?: return@composable
            ViewerScreen(resourceId = resourceId)
        }
        composable(Screen.TagManager.route) {
            TagManagerScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // === 后续 stage 在此追加 composable ===
    }
}
