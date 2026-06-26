package dev.wucheng.resource_viewer.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Build
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.wucheng.resource_viewer.R
import dev.wucheng.resource_viewer.ui.navigation.AppNavGraph
import dev.wucheng.resource_viewer.ui.navigation.Screen

/**
 * M16: 底部/侧边导航栏的 Tab 定义
 *
 * 固定 5 个 Tab：首页/知识/工具箱/我的/设置
 */
private data class NavTab(
    val labelResId: Int,
    val icon: ImageVector,
    val route: String,
)

/**
 * M16: 获取导航标签列表
 *
 * 使用 remember 缓存，避免重组时重复创建。
 */
@Composable
private fun rememberNavTabs(): List<NavTab> {
    return remember {
        listOf(
            NavTab(R.string.tab_home, Icons.Default.Home, Screen.Home.route),
            NavTab(R.string.tab_knowledge, Icons.AutoMirrored.Filled.MenuBook, Screen.Knowledge.route),
            NavTab(R.string.tab_toolbox, Icons.Default.Build, Screen.Toolbox.route),
            NavTab(R.string.tab_profile, Icons.Default.Person, Screen.Profile.route),
            NavTab(R.string.tab_settings, Icons.Default.Settings, Screen.Settings.route),
        )
    }
}

/**
 * M16: 应用外壳组件
 *
 * 根据屏幕宽度选择底部导航栏或侧边导航栏。
 * 实现胶囊底栏指示器动效。
 */
@Composable
fun AppShell(
    widthSizeClass: WindowWidthSizeClass,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val useSideBar = widthSizeClass == WindowWidthSizeClass.Expanded
    val navTabs = rememberNavTabs()

    if (useSideBar) {
        Row(modifier = modifier.fillMaxSize()) {
            AppNavigationRail(
                currentRoute = currentRoute,
                navTabs = navTabs,
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
                    navTabs = navTabs,
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

/**
 * M16: 导航状态与路由同步优化
 *
 * 使用 popUpTo + saveState + restoreState 避免重组抖动。
 */
private fun NavHostController.navigateWithState(route: String) {
    navigate(route) {
        popUpTo(Screen.Home.route) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * M16: 胶囊底栏指示器
 *
 * 使用动画实现平滑的指示器移动效果。
 */
@Composable
private fun CapsuleIndicator(
    selectedIndex: Int,
    tabCount: Int,
    modifier: Modifier = Modifier,
) {
    val tabWidth = 100.dp / tabCount
    val indicatorWidth = 24.dp
    val offset by animateDpAsState(
        targetValue = tabWidth * selectedIndex + (tabWidth - indicatorWidth) / 2,
        animationSpec = tween(durationMillis = 300),
        label = "capsule_indicator_offset",
    )

    Box(
        modifier = modifier
            .offset(x = offset)
            .width(indicatorWidth)
            .size(3.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary),
    )
}

/**
 * M16: 底部导航栏
 *
 * 包含 5 个 Tab 和胶囊指示器动效。
 */
@Composable
private fun AppNavigationBar(
    currentRoute: String?,
    navTabs: List<NavTab>,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = navTabs.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    Box(modifier = modifier) {
        NavigationBar {
            navTabs.forEachIndexed { index, tab ->
                val label = stringResource(tab.labelResId)
                NavigationBarItem(
                    selected = currentRoute == tab.route,
                    onClick = { onNavigate(tab.route) },
                    icon = { Icon(tab.icon, contentDescription = label) },
                    label = { Text(label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
        }

        // 胶囊指示器
        CapsuleIndicator(
            selectedIndex = selectedIndex,
            tabCount = navTabs.size,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

/**
 * M16: 侧边导航栏
 *
 * 用于宽屏设备（Expanded）。
 */
@Composable
private fun AppNavigationRail(
    currentRoute: String?,
    navTabs: List<NavTab>,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(modifier = modifier) {
        navTabs.forEach { tab ->
            val label = stringResource(tab.labelResId)
            NavigationRailItem(
                selected = currentRoute == tab.route,
                onClick = { onNavigate(tab.route) },
                icon = { Icon(tab.icon, contentDescription = label) },
                label = { Text(label) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}
