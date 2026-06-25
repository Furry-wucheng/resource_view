# M25 — 设置页面

> 轨道 7 · Stage 25/29 | 前置: M23 | 依赖共享: `doc/share/01-data-models.md` §1.5 `doc/share/03-di-contracts.md` | 🟢 独占

## 执行目标

实现完整设置页面：缓存管理、外观切换、查看器默认设置、数据源同步。

## 共享契约引用

- `doc/share/01-data-models.md` §1.5 — AppConfigEntity
- `@prd/08-设置页面.md` — 设置项定义
- `@design/settings.html` — 原型参考

## 子任务

### M25.1 SettingsViewModel

- 读取/更新 AppConfig（缓存上限、主题模式、翻页方向、双页模式、跨章节、同步间隔）
- 缓存管理：显示当前缓存大小 → 手动清理 → 二次确认
- 外观切换：`system` / `light` / `dark`（实时生效）

**产出物**：`ui/screens/settings/SettingsViewModel.kt`

### M25.2 SettingsScreen 完整实现

替换 M05 的占位 SettingsScreen：
- **缓存管理**：缩略图缓存大小显示 + 清理按钮
- **外观**：主题模式选择器（System / Light / Dark）
- **查看器**：默认翻页方向、双页模式开关、跨章节阅读开关
- **数据源**：自动同步间隔
- **关于**：版本号

**产出物**：`ui/screens/settings/SettingsScreen.kt`（替换骨架）

### M25.3 缩略图缓存管理

通过 Coil ImageLoader 的 DiskCache 配置管理缩略图缓存容量（默认 500MB，可调）。
设置页暴露缓存大小统计和手动清理入口。Coil DiskCache 内置 LRU 淘汰，无需手写缓存服务。

**产出物**：`ui/screens/settings/SettingsScreen.kt` 中的缓存管理分区

### M25.4 整合主题切换

确保 `SettingsViewModel` 修改 `themeMode` 后，全局主题实时切换（通过 StateFlow 驱动 `ResourceViewerTheme`）。

## 验收标准

- [ ] 缓存大小正确显示 + 清理后更新
- [ ] 主题切换实时生效
- [ ] 设置项持久化到 AppConfigEntity
- [ ] 设置从查看器工具栏 ⚙ 入口可直达对应分组
- [ ] `./gradlew build` 通过
