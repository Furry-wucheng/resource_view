# M23 — 首页网格完整实现

> 轨道 7 · Stage 23/29 | 前置: M10,M16 | 依赖共享: `doc/share/01-data-models.md` `doc/share/05-theme-tokens.md` | 🟢 独占

## 执行目标

实现首页完整的资源缩略图网格 + HomeViewModel + Coil ImageLoader 配置。

## 共享契约引用

- `doc/share/01-data-models.md` — Resource Domain Model
- `doc/share/05-theme-tokens.md` — 缩略图卡片尺寸/圆角/间距
- `doc/share/03-di-contracts.md` — CoilModule
- `@prd/01-资源库首页.md` — 首页交互
- `@design/homepage.html` — 原型参考

## 子任务

### M23.1 HomeViewModel

- `loadResources()`: 从 ResourceRepository 获取可见资源（Flow → StateFlow）
- `selectTag(tagId)`: 多选标签交集筛选
- `clearFilter()`: 清除所有筛选
- UI State: `_uiState` (IDLE/LOADING/SUCCESS/ERROR) + `_resources` + `_selectedTagIds`

**产出物**：`ui/screens/home/HomeViewModel.kt`

### M23.2 ResourceGridItem

缩略图卡片 Composable：
- 竖版 (`aspectRatio = 2/3`)
- 封面图片（Coil AsyncImage，按显示尺寸解码）
- 资源名称（最多 2 行）
- 标签颜色小圆点（前 3 个）
- 长按 → 属性（资源详情弹窗，M24）

**产出物**：`ui/components/ResourceGridItem.kt`

### M23.3 HomeScreen 完整实现

替换 M05 的占位 HomeScreen：
- 顶部 `FilterBar`（M16）
- `LazyVerticalGrid` 缩略图网格（`GridCells.Adaptive(120.dp)`）
- `key = { it.id }` 稳定身份
- 空状态 / 筛选结果为空的差别处理
- 下拉刷新（可选 P1）

**产出物**：`ui/screens/home/HomeScreen.kt`（替换骨架）

### M23.4 CoilModule

配置全局 Coil `ImageLoader`：
- 内存缓存 25%
- 磁盘缓存 2%
- 自定义 SmbFetcher（可选，本地源不需要）

**产出物**：`di/CoilModule.kt`

### M23.5 ImageThumbnailGenerator

实现 `ThumbnailGenerator`：Coil 加载原图 → resize → JPEG 压缩到磁盘缓存。

**产出物**：`shared/thumbnail/ImageThumbnailGenerator.kt`

## 验收标准

- [ ] 首页显示所有可用资源的缩略图网格
- [ ] 缩略图按显示尺寸解码（非全分辨率）
- [ ] 筛选标签后网格即时过滤
- [ ] 空状态显示引导文案
- [ ] 点击资源 → 进入查看器
- [ ] `./gradlew build` 通过
