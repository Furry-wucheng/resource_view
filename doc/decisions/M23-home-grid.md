# M23 — 首页网格完整实现

> 时间: 2026-06-26 | Agent: claude | 状态: ✅ 已完成 | 前置: M10, M16

## 设计决策

### D-001: HomeViewModel 状态管理方式
- **背景**: 首页需要管理资源列表、标签筛选、UI 状态三种数据，且它们之间有联动关系（选中标签 → 资源列表过滤）
- **选择**: 使用 `flatMapLatest` 实现标签选择自动切换数据源，`selectedTagIds` 作为唯一状态源
- **备选**: 在 ViewModel 中手动调用 `filterByTags()` 并维护 `_resources` 变量，放弃原因是需要手动同步状态，容易出现不一致
- **影响文件**: `ui/screens/home/HomeViewModel.kt:45-55`
- **被依赖**: M24 (ResourcePicker) 可复用相同的筛选模式

### D-002: FilterBar 组件设计
- **背景**: M16 规划了筛选栏，但实际代码中未实现，需要在 M23 补充
- **选择**: 独立的 `FilterBar` Composable，接收 `tags` + `selectedTagIds` + `onTagClick` 回调，"全部"按钮传 `null`
- **备选**: 将 FilterBar 内嵌到 HomeScreen 中，放弃原因是不利于复用和测试
- **影响文件**: `ui/components/FilterBar.kt`
- **被依赖**: M25 (设置页面可能需要标签筛选)

### D-003: CoilModule 缓存策略
- **背景**: 需要配置全局 Coil ImageLoader 的缓存策略
- **选择**: 内存缓存 25%，磁盘缓存 2%，启用 crossfade
- **备选**: 使用 Coil 默认配置（内存 15%，磁盘 2%），放弃原因是缩略图场景下需要更多内存缓存以提升滑动流畅度
- **影响文件**: `di/CoilModule.kt`
- **被依赖**: 全局 ImageLoader，所有 AsyncImage 使用

### D-004: ImageThumbnailGenerator 实现策略
- **背景**: 需要为文件夹类型的资源生成缩略图
- **选择**: 列出目录内容，找到第一张图片文件，解码缩放后保存为 JPEG
- **备选**: 使用 Coil 直接加载原图并缓存，放弃原因是需要统一缩略图格式和尺寸，且 Coil 缓存不适合持久化缩略图
- **影响文件**: `shared/thumbnail/ImageThumbnailGenerator.kt`
- **被依赖**: ThumbnailRepository 使用

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `ui/screens/home/HomeViewModel.kt` | 🆕 新增 | 首页 ViewModel，管理资源列表和标签筛选 |
| `ui/screens/home/HomeScreen.kt` | ✏️ 修改 | 替换骨架为完整实现 |
| `ui/components/ResourceGridItem.kt` | 🆕 新增 | 缩略图卡片 Composable |
| `ui/components/FilterBar.kt` | 🆕 新增 | 标签筛选栏组件 |
| `di/CoilModule.kt` | 🆕 新增 | Coil ImageLoader Koin 模块 |
| `shared/thumbnail/ImageThumbnailGenerator.kt` | 🆕 新增 | 图片缩略图生成器 |
| `di/ViewModelModule.kt` | ✏️ 修改 | 追加 HomeViewModel 注入 |
| `di/RepositoryModule.kt` | ✏️ 修改 | 追加 ImageThumbnailGenerator |
| `ResourceViewerApp.kt` | ✏️ 修改 | 注册 coilModule |
| `ui/navigation/AppNavGraph.kt` | ✏️ 修改 | 更新 HomeScreen 参数 |
| `ui/theme/Color.kt` | ✏️ 修改 | 追加 ThumbnailTokens 对象 |
| `ui/screens/home/HomeViewModelTest.kt` | 🆕 新增 | ViewModel 单元测试（7 个） |
| `ui/components/FilterBarTest.kt` | 🆕 新增 | FilterBar Compose UI 测试 |
| `ui/components/ResourceGridItemTest.kt` | 🆕 新增 | ResourceGridItem Compose UI 测试 |

> 操作图例: 🆕 新增 · ✏️ 修改 · 🗑️ 删除

## 已知问题 / TODO

- [ ] FilterBar 和 ResourceGridItem 的 Compose UI 测试需要设备/模拟器运行（`connectedAndroidTest`）
- [ ] ImageThumbnailGenerator 目前只取目录中第一张图片，后续可优化为取随机/最新
- [ ] Coil SmbFetcher 未实现（M23 范围外，SMB 缩略图走 ThumbnailRepository）
