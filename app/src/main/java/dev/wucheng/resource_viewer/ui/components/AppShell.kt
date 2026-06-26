package dev.wucheng.resource_viewer.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.wucheng.resource_viewer.ui.navigation.AppNavGraph
import dev.wucheng.resource_viewer.ui.navigation.Screen

/** 底部/侧边导航栏的 Tab 定义 */
private data class NavTab(
    val label: String,
    val icon: ImageVector,
    val route: String,
)

private val NAV_TABS = listOf(
    NavTab("首页库", Icons.Default.Home, Screen.Home.route),
    NavTab("数据源", Icons.Default.Folder, Screen.Sources.route),
    NavTab("设置", Icons.Default.Settings, Screen.Settings.route),
)

@Composable
fun AppShell(
    widthSizeClass: WindowWidthSizeClass,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val useSideBar = widthSizeClass == WindowWidthSizeClass.Expanded

    if (useSideBar) {
        Row(modifier = modifier.fillMaxSize()) {
            AppNavigationRail(
                currentRoute = currentRoute,
                onNavigate = { navController.navigateWithState(it) },
            )
            Scaffold(modifier = Modifier.weight(1f)) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    AppNavGraph(
                        navController = navController,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                AppNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = { navController.navigateWithState(it) },
                )
            },
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                AppNavGraph(
                    navController = navController,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

private fun NavHostController.navigateWithState(route: String) {
    navigate(route) {
        popUpTo(Screen.Home.route) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun AppNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        NAV_TABS.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onNavigate(tab.route) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun AppNavigationRail(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(modifier = modifier) {
        NAV_TABS.forEach { tab ->
            NavigationRailItem(
                selected = currentRoute == tab.route,
                onClick = { onNavigate(tab.route) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}
