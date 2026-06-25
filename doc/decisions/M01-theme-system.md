# M01 — 主题系统与设计令牌

> 时间: 2026-06-25 | Agent: opencode/stellar-orchid | 状态: ✅ 已完成 | 前置: M00

## 设计决策

### D-001: 禁用动态颜色（Dynamic Color）
- **背景**: Android 12+ 支持动态颜色（Material You），会根据壁纸自动调整主题色
- **选择**: 禁用动态颜色，使用固定的 LightColorScheme / DarkColorScheme
- **备选**: 启用动态颜色，让系统自动适配。放弃原因：Resource Viewer 的品牌色需要一致性，动态颜色会破坏 12 色标签预设的视觉协调性
- **影响文件**: `ui/theme/Theme.kt:6-15`
- **被依赖**: 所有 UI Screen 通过 `ResourceViewerTheme` 包装

### D-002: 查看器背景色强制黑色
- **背景**: 查看器（Viewer）需要沉浸式体验，背景必须为纯黑
- **选择**: `ViewerColors.Background = Color.Black`，不跟随主题切换
- **备选**: 使用 `MaterialTheme.colorScheme.background`。放弃原因：深色主题下 background 是 #121212（深灰），不够纯黑
- **影响文件**: `ui/theme/Color.kt:52-56`

### D-003: 主题函数命名 ResourceViewerTheme
- **背景**: Android Studio 模板生成的函数名为 `Resource_viewerTheme`（snake_case），不符合 Compose PascalCase 规范
- **选择**: 重命名为 `ResourceViewerTheme`
- **备选**: 保留原名。放弃原因：违反 `doc/share/08-code-conventions.md` 的 Composable 函数 PascalCase 规范
- **影响文件**: `ui/theme/Theme.kt:8`, `MainActivity.kt:14,21,44`

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `ui/theme/Color.kt` | ✏️ 修改 | 重写：TagColors 12色预设 + FunctionalColors + LightColorScheme + DarkColorScheme + ViewerColors |
| `ui/theme/Type.kt` | ✏️ 修改 | 重写：AppTypography（titleLarge/titleMedium/bodyLarge/bodyMedium/labelMedium） |
| `ui/theme/Shape.kt` | 🆕 新增 | AppShapes（small 4dp/medium 8dp/large 12dp） |
| `ui/theme/Theme.kt` | ✏️ 修改 | 重写：ResourceViewerTheme wrapper，禁用动态颜色 |
| `MainActivity.kt` | ✏️ 修改 | 更新 import 和调用：Resource_viewerTheme → ResourceViewerTheme |
| `doc/mvp/AGENTS.md` | ✏️ 修改 | M01 状态 ⬜ → 🔵 |
| `local.properties` | 🆕 新增 | SDK 路径配置 |

## 已知问题 / TODO

- [ ] 无
