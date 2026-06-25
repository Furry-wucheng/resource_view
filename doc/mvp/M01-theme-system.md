# M01 — 主题系统与设计令牌

> 轨道 0 · Stage 1/29 | 前置: M00 | 依赖共享: `doc/share/05-theme-tokens.md` | 🟢 独占文件

## 执行目标

将设计令牌翻译为 Compose `MaterialTheme`，支持浅色/深色主题切换。

## 共享契约引用

- `doc/share/05-theme-tokens.md` — 颜色值、排版、形状常量（直接搬运实现）
- `@design/design-tokens.css` — 原始设计令牌

## 子任务

### M01.1 Color.kt

定义所有颜色常量：12 色标签预设、功能色、主题色、查看器专用色、内置标签收藏色。

**产出物**：`ui/theme/Color.kt`

### M01.2 Type.kt

定义文字排版：`titleLarge`、`titleMedium`、`bodyLarge`、`bodyMedium`、`labelMedium`。

**产出物**：`ui/theme/Type.kt`

### M01.3 Shape.kt

定义圆角常量：`small`(4dp)、`medium`(8dp)、`large`(12dp)。

**产出物**：`ui/theme/Shape.kt`

### M01.4 Theme.kt

组装 `MaterialTheme`，包含 `LightColorScheme`、`DarkColorScheme`、`AppTypography`、`AppShapes`。提供 `ResourceViewerTheme` Composable wrapper。

**产出物**：`ui/theme/Theme.kt`

## 验收标准

- [ ] 浅色主题含完整 ColorScheme/Typography
- [ ] 深色主题背景色与设计令牌一致
- [ ] 12 色标签预设颜色值与 PRD `@prd/02-标签系统.md` §4.2 一致
- [ ] 查看器背景色强制黑色（不受主题切换影响）
- [ ] `./gradlew build` 通过
