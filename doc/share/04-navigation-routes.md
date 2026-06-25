# 04 — 导航路由

> 🔵 路由定义骨架。M03 创建，后续 stage 按规则追加。

---

## 路由定义

```kotlin
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

    // === 后续 stage 在此追加 ===
    // M13 追加:
    // data object FileBrowser : Screen("sources/{sourceId}/browser") {
    //     fun createRoute(sourceId: String) = "sources/$sourceId/browser"
    // }
    // M14 追加:
    // data object FileViewer : Screen("viewer/file/{sourceId}/{path}") {
    //     fun createRoute(sourceId: String, path: String) = "viewer/file/$sourceId/$path"
    // }
    // M21 追加:
    // data object ChapterList : Screen("viewer/{resourceId}/chapters") {
    //     fun createRoute(resourceId: String) = "viewer/$resourceId/chapters"
    // }
    // M24: ResourcePicker / TagEditor 作为 Dialog 弹窗，不走路由
}
}
```

## NavHost 结构

```kotlin
@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController, startDestination = Screen.Home.route) {
        // === M03 创建：底部 Tab ===
        composable(Screen.Home.route) {
            HomeScreen(onNavigateToViewer = { resId -> ... })
        }
        composable(Screen.Sources.route) {
            SourceListScreen(onNavigateToBrowser = { srcId -> ... })
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
            TagManagerScreen()
        }

        // === 后续 stage 在此追加 composable ===
    }
}
```

## 底部导航栏

```kotlin
@Composable
fun BottomNavBar(navController: NavHostController) {
    NavigationBar {
        NavigationBarItem(
            selected = ...,
            onClick = { navController.navigate(Screen.Home.route) { ... } },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("首页库") },
        )
        NavigationBarItem(
            selected = ...,
            onClick = { navController.navigate(Screen.Sources.route) { ... } },
            icon = { Icon(Icons.Default.Folder, contentDescription = null) },
            label = { Text("数据源") },
        )
        NavigationBarItem(
            selected = ...,
            onClick = { navController.navigate(Screen.Settings.route) { ... } },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("设置") },
        )
    }
}
```

## 追加规则

1. 新路由在 `Screen` sealed class 末尾追加
2. 新 `composable()` 在 `NavHost` 的 `composable` 块末尾追加
3. 全屏页面（Viewer, TagManager, FileBrowser）覆盖底部导航栏，用 `fullscreen` dialog 模式
4. ResourcePicker、TagEditor 等作为 `Dialog` 弹窗，不走路由
5. 追加后需 rebase 到最新 main，确保 `Screen` 和 `NavHost` 与上游一致
