package dev.wucheng.resource_viewer.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

// 标签预设12色
object TagColors {
    val PRESETS = listOf(
        Color(0xFFE53935),  // Red 500
        Color(0xFFD81B60),  // Pink 600
        Color(0xFF8E24AA),  // Purple 600
        Color(0xFF5E35B1),  // Deep Purple 600
        Color(0xFF3949AB),  // Indigo 600
        Color(0xFF1E88E5),  // Blue 600
        Color(0xFF00ACC1),  // Cyan 600
        Color(0xFF00897B),  // Teal 600
        Color(0xFF43A047),  // Green 600
        Color(0xFFFDD835),  // Yellow 600
        Color(0xFFFB8C00),  // Orange 600
        Color(0xFF757575),  // Grey 600
    )
    val FAVORITE = Color(0xFFFFC107) // 内置标签"收藏"专用
}

// 功能色
object FunctionalColors {
    val Error = Color(0xFFE53935)
    val Success = Color(0xFF43A047)
    val Warning = Color(0xFFFB8C00)
    val Info = Color(0xFF1E88E5)
}

// 浅色主题
val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1E88E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    secondary = Color(0xFF43A047),
    onSecondary = Color.White,
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    surfaceVariant = Color(0xFFE0E0E0),
    onBackground = Color(0xFF212121),
    onSurface = Color(0xFF212121),
    onSurfaceVariant = Color(0xFF757575),
    outline = Color(0xFFBDBDBD),
    error = Color(0xFFE53935),
)

// 深色主题
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF42A5F5),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1565C0),
    secondary = Color(0xFF66BB6A),
    onSecondary = Color.Black,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2C2C2C),
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFF9E9E9E),
    outline = Color(0xFF424242),
    error = Color(0xFFEF5350),
)

// 查看器专用
object ViewerColors {
    val Background = Color.Black       // 查看器始终黑底，不受主题影响
    val ToolbarBackground = Color(0xCC000000)  // 半透明工具栏
    val ToolbarIcon = Color.White
}