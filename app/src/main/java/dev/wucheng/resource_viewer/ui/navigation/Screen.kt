package dev.wucheng.resource_viewer.ui.navigation

sealed class Screen(val route: String) {
    // === M03 创建：底部 Tab 路由 ===
    data object Home : Screen("home")
    data object Sources : Screen("sources")
    data object Settings : Screen("settings")

    // === M16 创建：底部标签栏路由 ===
    data object Knowledge : Screen("knowledge")
    data object Toolbox : Screen("toolbox")
    data object Profile : Screen("profile")

    // === M03 创建：全屏路由占位 ===
    data object Viewer : Screen("viewer/{resourceId}") {
        fun createRoute(resourceId: String) = "viewer/$resourceId"
    }
    data object TagManager : Screen("tags/manager")

    // === 后续 stage 在此追加 ===
}
