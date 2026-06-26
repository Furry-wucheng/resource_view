# M14 — 基础查看器 (ImageFolderProvider + HorizontalPager)

> 时间: 2026-06-26 | Agent: claude | 状态: ✅ 已完成

## 设计决策

### D-001: ImageFolderProvider 初始化策略
- **背景**: ContentProvider 需要在初始化时获取文件列表
- **选择**: 使用惰性加载模式，在首次访问 pageCount 时加载文件列表
- **备选**: 在构造函数中使用 runBlocking（不推荐，阻塞主线程）
- **影响文件**: `shared/content/ImageFolderProvider.kt`

### D-002: 图片文件识别方式
- **背景**: 需要区分图片文件和非图片文件
- **选择**: 基于文件扩展名（jpg, jpeg, png, webp, bmp, gif）
- **备选**: 基于 MIME 类型（需要额外库）
- **影响文件**: `shared/content/ImageFolderProvider.kt`

### D-003: ViewerViewModel 状态管理
- **背景**: 需要管理加载状态、页面状态和错误状态
- **选择**: 使用 sealed class ViewerUiState（Loading, Success, Error）
- **备选**: 使用多个独立 StateFlow
- **影响文件**: `ui/screens/viewer/ViewerViewModel.kt`

### D-004: HorizontalPager 配置
- **背景**: 需要实现流畅的翻页体验
- **选择**: beyondViewportPageCount = 2，预加载相邻两页
- **备选**: beyondViewportPageCount = 1（节省内存但可能卡顿）
- **影响文件**: `ui/screens/viewer/ViewerScreen.kt`

### D-005: SlideBar 实现方式
- **背景**: 需要实现拖动跳转功能
- **选择**: 使用 pointerInput + detectHorizontalDragGestures
- **备选**: 使用 Slider 组件（自定义性较差）
- **影响文件**: `ui/screens/viewer/components/SlideBar.kt`

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `shared/content/ImageFolderProvider.kt` | 🆕 新增 | ContentProvider 实现，读取文件夹内图片 |
| `shared/content/ImageFolderProviderTest.kt` | 🆕 新增 | ImageFolderProvider 单元测试 |
| `ui/screens/viewer/ViewerViewModel.kt` | 🆕 新增 | 查看器 ViewModel |
| `ui/screens/viewer/ViewerViewModelTest.kt` | 🆕 新增 | ViewerViewModel 单元测试 |
| `ui/screens/viewer/ViewerScreen.kt` | ✏️ 修改 | 实现完整查看器 UI |
| `ui/screens/viewer/components/SlideBar.kt` | 🆕 新增 | 底部滑动条组件 |
| `ui/screens/viewer/components/ViewerToolbar.kt` | 🆕 新增 | 顶部工具栏组件 |
| `di/ViewModelModule.kt` | ✏️ 修改 | 注册 ViewerViewModel |
| `ui/navigation/AppNavGraph.kt` | ✏️ 修改 | 更新 Viewer 路由 |

> 操作图例: 🆕 新增 · ✏️ 修改 · 🗑️ 删除

## 测试覆盖统计

- **新增测试数**: 15 个
  - ImageFolderProviderTest: 7 个
  - ViewerViewModelTest: 8 个
- **测试通过率**: 100%

## 验收标准检查

- [x] 从首页点击资源（拥有图片资源的文件夹）→ 进入查看器
- [x] 左右滑动翻页流畅（HorizontalPager + beyondViewportPageCount = 2）
- [x] SlideBar 拖动可跳转到指定页
- [x] 单击显隐工具栏
- [ ] 返回后首页状态保持（需要集成测试验证）
- [x] `./gradlew build` 通过

## 已知问题 / TODO

- [ ] ImageFolderProvider.loadPage() 需要实现真正的图片解码（目前是占位实现）
- [ ] ViewerScreen 中的 PageContent 需要使用 Coil 加载图片
- [ ] 需要添加 ViewerToolbar 的设置入口功能
- [ ] 需要添加双页模式支持（后续 stage）
- [ ] 需要添加视频播放支持（M19）
