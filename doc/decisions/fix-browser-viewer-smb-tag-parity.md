# fix — 文件浏览、混合查看器、缩略图、标签与 SMB 回归修复

> 时间: 2026-06-27 | Agent: claude | 状态: ✅ 已完成

## 设计决策

### D-001: 混合查看器视觉页模型重构
- **背景**: 图片/视频混合浏览时，视频会进入双页配对；智能双页未按图片宽高判断；查看器未稳定进入沉浸全屏；视频滑向下一页前会黑屏且无法恢复
- **选择**: 重构视觉页模型，视频独占一个视觉页，智能双页只配对适合双页显示的窄图，宽图独占；播放器实例按视频页隔离并正确释放
- **备选**: 保持原有固定 `index / 2` 配对方式，但无法解决视频黑屏和智能双页判断问题
- **影响文件**: `ViewerSpread.kt`, `ViewerScreen.kt`, `VideoPlayerViewModel.kt`, `MixedFolderProvider.kt`
- **被依赖**: M14, M19, M23 消费此接口 → 不可随意改动签名

### D-002: 文件/文件夹缩略图解析与回退卡片
- **背景**: 文件浏览器中的图片、视频和文件夹没有实际缩略图；首页资源缩略图生成链路也不完整
- **选择**: 建立文件/文件夹缩略图解析与回退卡片，接通首页缩略图生成，统一加入 AVIF
- **备选**: 仅支持部分媒体类型，但会导致缩略图显示不完整
- **影响文件**: `FileEntryThumbnailLoader.kt`, `ImageThumbnailGenerator.kt`, `FileBrowserThumbnailDiskCache.kt`, `ThumbnailTaskPool.kt`
- **被依赖**: M23, M24 消费此接口 → 不可随意改动签名

### D-003: SMB 会话复用与批量添加标签选择
- **背景**: SMB 浏览成功后批量加入资源库可能报"SMB 认证失败"；批量添加资源的标签选择和新建标签能力未与 Flutter 版对齐
- **选择**: 复用已认证会话、移除每次取实例时的阻塞探活、支持 Guest 空密码；批量添加可选择现有标签并即时新建标签
- **备选**: 每次创建新连接，但会导致认证失败和性能问题
- **影响文件**: `FileSourceFactory.kt`, `SmbFileSource.kt`, `SmbClientWrapper.kt`, `BatchAddResourcesDialog.kt`
- **被依赖**: M17, M27 消费此接口 → 不可随意改动签名

### D-004: 系统返回键逐级返回
- **背景**: 系统返回键从子目录直接退出文件浏览器，没有先返回上一级目录
- **选择**: 修复 AppShell/FileBrowser 的窗口 Insets 与 BackHandler，系统返回键逐级返回
- **备选**: 保持原有行为，但用户体验差
- **影响文件**: `AppShell.kt`, `FileBrowserScreen.kt`
- **被依赖**: M04, M17 消费此接口 → 不可随意改动签名

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `ui/components/AppShell.kt` | ✏️ 修改 | 统一窗口 Insets/全屏外壳 |
| `ui/screens/sources/FileBrowserScreen.kt` | ✏️ 修改 | 系统返回键逐级返回 |
| `ViewerSpread.kt` | 🆕 新增 | 视频独占视觉页、智能宽图单页 |
| `ViewerScreen.kt` | ✏️ 修改 | 播放器实例隔离和离页暂停 |
| `VideoPlayerViewModel.kt` | ✏️ 修改 | 播放器实例隔离 |
| `MixedFolderProvider.kt` | 🆕 新增 | 图片视频混合序列 |
| `MediaFormats.kt` | 🆕 新增 | 尺寸元数据与含 AVIF 的统一格式集合 |
| `PageBitmapLoader.kt` | 🆕 新增 | SMB 大图分块下载、取消检查、断线重试、本地页面缓存与采样解码 |
| `FileEntryThumbnailLoader.kt` | 🆕 新增 | 图片/AVIF、视频范围读取首帧、文件夹递归封面和类型回退 |
| `ImageThumbnailGenerator.kt` | ✏️ 修改 | 统一加入 AVIF |
| `FileBrowserThumbnailDiskCache.kt` | 🆕 新增 | 指纹磁盘缓存、负缓存、容量淘汰 |
| `ThumbnailTaskPool.kt` | 🆕 新增 | 设置并发限制和 SMB 并发降级 |
| `FileBrowserViewModel.kt` | ✏️ 修改 | 缩略图缓存 |
| `BatchAddResourcesDialog.kt` | ✏️ 修改 | 现有标签选择、即时新建并选中标签 |
| `FileSourceFactory.kt` | ✏️ 修改 | 复用已认证会话、移除每次取实例时的阻塞探活、支持 Guest 空密码 |
| `SmbFileSource.kt` | ✏️ 修改 | Connection reset 后自动重连一次 |
| `SmbClientWrapper.kt` | ✏️ 修改 | 传播 stat 网络错误并随流释放远端句柄 |
| `ViewerSpreadTest.kt` | 🆕 新增 | 覆盖视频独占、智能双页 |
| `MediaFormatsTest.kt` | 🆕 新增 | 覆盖 AVIF |
| `FileSourceFactoryTest.kt` | ✏️ 修改 | 覆盖 Guest SMB |
| `FileEntryThumbnailLoaderTest.kt` | 🆕 新增 | 覆盖缩略图加载 |
| `ThumbnailTaskPoolTest.kt` | 🆕 新增 | 覆盖任务池 |
| `doc/issues/2026-06-27-bug-browser-viewer-smb-tag-parity.md` | 🆕 新增 | 提案文档 |

> 操作图例: 🆕 新增 · ✏️ 修改 · 🗑️ 删除

## 已知问题 / TODO

- [ ] SMB 真机/NAS 凭据组合与视频 Surface 切换仍建议在目标设备上做一轮交互验收
- [ ] 部分 deprecated API 警告（如 `Icons.Filled.Sort`）可在后续版本中统一修复