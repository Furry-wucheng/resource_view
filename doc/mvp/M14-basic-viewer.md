# M14 — 基础查看器 (ImageFolderProvider + HorizontalPager)

> 轨道 3 · Stage 14/29 | 前置: M13 | 依赖共享: `doc/share/02-interfaces.md` §2 | 🟢 独占

## 执行目标

实现基础的图片查看器，支持 HorizontalPager 翻页和 SlideBar 滑动条。

## 共享契约引用

- `doc/share/02-interfaces.md` §2 — ContentProvider 接口
- `doc/share/01-data-models.md` §4 — ViewerItem
- `doc/share/05-theme-tokens.md` — 查看器背景色
- `@design/viewer.html` — 原型参考

## 子任务

### M14.1 ImageFolderProvider

实现 `ContentProvider`，读取文件夹内所有图片文件并按名称排序。

**产出物**：`shared/content/ImageFolderProvider.kt`

### M14.2 ViewerViewModel

管理当前页、页面列表、预加载状态。使用 `FileSourceFactory` 创建 FileSource → 根据 Resource 创建 ContentProvider → 转换为 ViewerItem 列表。

**产出物**：`ui/screens/viewer/ViewerViewModel.kt`

### M14.3 ViewerScreen

基础查看器骨架：
- `HorizontalPager` + `beyondBoundsPageCount = 2`
- 每页用 Coil `AsyncImage` 渲染
- 单击显隐工具栏
- 顶部返回按钮 + 资源名称

**产出物**：`ui/screens/viewer/ViewerScreen.kt`

### M14.4 SlideBar

底部滑动条，显示当前页/总页数 + 拖动跳转。

**产出物**：`ui/screens/viewer/components/SlideBar.kt`

### M14.5 ViewerToolbar

顶部工具栏，可点击显隐，显示资源名称 + 返回按钮 + 设置入口。

**产出物**：`ui/screens/viewer/components/ViewerToolbar.kt`

## 验收标准

- [ ] 从首页点击资源（拥有图片资源的文件夹）→ 进入查看器
- [ ] 左右滑动翻页流畅
- [ ] SlideBar 拖动可跳转到指定页
- [ ] 单击显隐工具栏
- [ ] 返回后首页状态保持
- [ ] `./gradlew build` 通过
