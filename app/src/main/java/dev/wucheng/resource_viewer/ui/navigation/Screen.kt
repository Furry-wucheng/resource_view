package dev.wucheng.resource_viewer.ui.navigation

sealed class Screen(val route: String) {
    // === M03 创建：底部 Tab 路由 ===
    data object Home : Screen("home")
    data object Sources : Screen("sources")
    data object Settings : Screen("settings")

    // === M03 创建：全屏路由占位 ===
    data object Viewer : Screen("viewer/{resourceId}") {
        fun createRoute(resourceId: String) = "viewer/$resourceId"
    }
    data object TagManager : Screen("tags/manager")

    data object FileBrowser : Screen("sources/{sourceId}/browser") {
        fun createRoute(sourceId: String) = "sources/$sourceId/browser"
    }

    // === M21 创建：章节列表路由 ===
    data object ChapterList : Screen("chapters/{resourceId}") {
        fun createRoute(resourceId: String) = "chapters/$resourceId"
    }

    // === 后续 stage 在此追加 ===
}
