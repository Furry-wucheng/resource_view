package dev.wucheng.resource_viewer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.wucheng.resource_viewer.data.local.converter.ThemeMode
import dev.wucheng.resource_viewer.ui.screens.settings.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ResourceViewerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

/**
 * M25: 带设置支持的主题
 *
 * 从 SettingsViewModel 读取主题模式，支持实时切换。
 */
@Composable
fun ResourceViewerThemeWithSettings(
    viewModel: SettingsViewModel = koinViewModel(),
    content: @Composable () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val darkTheme = when (uiState.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
