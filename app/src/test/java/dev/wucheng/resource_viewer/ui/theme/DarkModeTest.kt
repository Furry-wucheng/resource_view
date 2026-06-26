package dev.wucheng.resource_viewer.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * M28.2: 深色模式测试
 *
 * 验证深色主题颜色定义的正确性和对比度。
 */
class DarkModeTest {

    @Test
    fun `dark color scheme should have dark background`() {
        // 深色主题的背景应该足够暗
        val background = DarkColorScheme.background
        assertTrue(
            "Dark background should be dark (brightness < 0.2)",
            background.luminance() < 0.2f
        )
    }

    @Test
    fun `dark color scheme should have light text on background`() {
        // 深色主题的文字应该足够亮
        val onBackground = DarkColorScheme.onBackground
        assertTrue(
            "Text on dark background should be light (brightness > 0.7)",
            onBackground.luminance() > 0.7f
        )
    }

    @Test
    fun `dark color scheme should have sufficient contrast`() {
        // 背景和文字之间应该有足够的对比度
        val background = DarkColorScheme.background
        val onBackground = DarkColorScheme.onBackground
        val contrast = calculateContrastRatio(background, onBackground)
        assertTrue(
            "Contrast ratio should be at least 4.5:1 for readability",
            contrast >= 4.5f
        )
    }

    @Test
    fun `dark surface should be slightly lighter than background`() {
        // 深色主题的 surface 应该比背景稍亮
        val background = DarkColorScheme.background
        val surface = DarkColorScheme.surface
        assertTrue(
            "Surface should be slightly lighter than background",
            surface.luminance() > background.luminance()
        )
    }

    @Test
    fun `light color scheme should have light background`() {
        // 浅色主题的背景应该足够亮
        val background = LightColorScheme.background
        assertTrue(
            "Light background should be bright (brightness > 0.9)",
            background.luminance() > 0.9f
        )
    }

    @Test
    fun `light color scheme should have dark text on background`() {
        // 浅色主题的文字应该足够暗
        val onBackground = LightColorScheme.onBackground
        assertTrue(
            "Text on light background should be dark (brightness < 0.2)",
            onBackground.luminance() < 0.2f
        )
    }

    @Test
    fun `light color scheme should have sufficient contrast`() {
        // 背景和文字之间应该有足够的对比度
        val background = LightColorScheme.background
        val onBackground = LightColorScheme.onBackground
        val contrast = calculateContrastRatio(background, onBackground)
        assertTrue(
            "Contrast ratio should be at least 4.5:1 for readability",
            contrast >= 4.5f
        )
    }

    @Test
    fun `viewer colors should always be dark`() {
        // 查看器应该始终使用深色背景
        assertTrue(
            "Viewer background should be black",
            ViewerColors.Background == Color.Black
        )
    }

    @Test
    fun `tag presets should have good visibility`() {
        // 标签预设色应该有良好的可见性
        TagColors.PRESETS.forEach { color ->
            assertTrue(
                "Tag color ${color} should have sufficient brightness",
                color.luminance() > 0.1f
            )
        }
    }

    @Test
    fun `functional colors should be distinguishable`() {
        // 功能色应该容易区分
        val colors = listOf(
            FunctionalColors.Error,
            FunctionalColors.Success,
            FunctionalColors.Warning,
            FunctionalColors.Info,
        )
        // 确保每种功能色都有不同的色相
        val hues = colors.map { it.toHue() }
        assertTrue(
            "Functional colors should have distinct hues",
            hues.distinct().size >= 3
        )
    }

    /**
     * 计算颜色的相对亮度 (WCAG 2.0)
     */
    private fun Color.luminance(): Float {
        val r = red * 0.2126f
        val g = green * 0.7152f
        val b = blue * 0.0722f
        return r + g + b
    }

    /**
     * 计算两个颜色之间的对比度 (WCAG 2.0)
     */
    private fun calculateContrastRatio(foreground: Color, background: Color): Float {
        val l1 = foreground.luminance()
        val l2 = background.luminance()
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    /**
     * 获取颜色的色相 (0-360)
     */
    private fun Color.toHue(): Float {
        val r = red
        val g = green
        val b = blue
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min

        if (delta == 0f) return 0f

        val hue = when (max) {
            r -> ((g - b) / delta) % 6
            g -> ((b - r) / delta) + 2
            else -> ((r - g) / delta) + 4
        }
        return (hue * 60).let { if (it < 0) it + 360 else it }
    }
}
