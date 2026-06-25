# 05 — 主题与设计令牌

> 🔵 定义颜色、排版、形状常量。M01 实现，所有 UI stage 引用。
> 依据：`@design/design-tokens.css` `@prd/02-标签系统.md` §4.2

---

## 标签预设颜色（12 色）

```kotlin
object TagColors {
    val PRESETS = listOf(
        "#E53935",  // Red 500
        "#D81B60",  // Pink 600
        "#8E24AA",  // Purple 600
        "#5E35B1",  // Deep Purple 600
        "#3949AB",  // Indigo 600
        "#1E88E5",  // Blue 600
        "#00ACC1",  // Cyan 600
        "#00897B",  // Teal 600
        "#43A047",  // Green 600
        "#FDD835",  // Yellow 600
        "#FB8C00",  // Orange 600
        "#757575",  // Grey 600
    )

    val FAVORITE = "#FFC107" // 内置标签"收藏"专用
}
```

## 功能色

```kotlin
object FunctionalColors {
    val Error = Color(0xFFE53935)
    val Success = Color(0xFF43A047)
    val Warning = Color(0xFFFB8C00)
    val Info = Color(0xFF1E88E5)
}
```

## 浅色主题

```kotlin
private val LightColorScheme = lightColorScheme(
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
```

## 深色主题

```kotlin
private val DarkColorScheme = darkColorScheme(
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
```

## 查看器专用

```kotlin
object ViewerColors {
    val Background = Color.Black       // 查看器始终黑底，不受主题影响
    val ToolbarBackground = Color(0xCC000000)  // 半透明工具栏
    val ToolbarIcon = Color.White
}
```

## 文字排版

```kotlin
val AppTypography = Typography(
    titleLarge = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp,
    ),
)
```

## 圆角/形状

```kotlin
val AppShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
)
```

## 缩略图卡片

```kotlin
object ThumbnailTokens {
    val ASPECT_RATIO = 2f / 3f        // 宽高比
    val CORNER_RADIUS = 8.dp           // 卡片圆角
    val GRID_SPACING = 8.dp            // 网格间距
    val LABEL_MAX_LINES = 2            // 标题最大行数
}
```
