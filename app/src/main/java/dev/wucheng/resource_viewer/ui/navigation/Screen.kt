package dev.wucheng.resource_viewer.ui.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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
    data object ChapterViewer : Screen("viewer/{resourceId}/chapter?path={path}") {
        fun createRoute(resourceId: String, chapterPath: String): String {
            val encodedPath = URLEncoder.encode(chapterPath, StandardCharsets.UTF_8.toString())
            return "viewer/$resourceId/chapter?path=$encodedPath"
        }
    }
    data object FlatGrid : Screen("grid/{resourceId}") {
        fun createRoute(resourceId: String): String = "grid/$resourceId"
    }
    data object Gallery : Screen("gallery/{resourceId}") {
        fun createRoute(resourceId: String): String = "gallery/$resourceId"
    }
    data object SequenceViewer : Screen("viewer/{resourceId}/sequence?path={path}&page={page}") {
        fun createRoute(resourceId: String, contentPath: String, initialPage: Int): String {
            val encodedPath = URLEncoder.encode(contentPath, StandardCharsets.UTF_8.toString())
            return "viewer/$resourceId/sequence?path=$encodedPath&page=$initialPage"
        }
    }
}
