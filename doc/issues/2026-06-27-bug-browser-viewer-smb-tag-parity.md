# bug: 文件浏览、混合查看器、缩略图、标签与 SMB 回归修复

> 日期: 2026-06-27 | 类型: bug | 状态: ✅ 已完成

## 现象

1. 数据源文件浏览页顶部存在额外安全区留白，与三个主 Tab 的顶栏不一致。
2. 图片/视频混合浏览时，视频会进入双页配对；智能双页未按图片宽高判断；查看器未稳定进入沉浸全屏；视频滑向下一页前会黑屏且无法恢复。
3. 文件浏览器中的图片、视频和文件夹没有实际缩略图；首页资源缩略图生成链路也不完整，卡片样式弱于 Flutter 版。
4. 系统返回键从子目录直接退出文件浏览器，没有先返回上一级目录。
5. 标签管理和资源标签展示/操作链路不完整。
6. SMB 浏览成功后批量加入资源库可能报“SMB 认证失败”。
7. 批量添加资源的标签选择和新建标签能力未与 Flutter 版对齐。
8. 支持的图片扩展名集合缺少 AVIF。

## 复现步骤

1. 添加本地或 SMB 数据源并进入多层目录。
2. 观察文件浏览顶栏，按系统返回键；切换列表/网格并查看媒体条目。
3. 打开同目录内包含图片和视频的文件序列，切换单页/双页/智能双页并连续滑动。
4. 多选 SMB 路径并批量添加，尝试选择或新建标签。

## 期望效果

- 文件浏览页与主 Tab 共用一致的 edge-to-edge 顶栏布局；返回键优先退出多选/关闭侧栏/返回上级目录。
- 视频始终独占一个视觉页；智能双页只配对适合双页显示的窄图，宽图独占；查看器可靠沉浸全屏；滑页不会破坏视频渲染。
- 文件显示自身缩略图，文件夹递归寻找下一层首个支持媒体作为封面，找不到时显示美观的类型回退图。
- 首页和文件浏览器都能显示已生成缩略图，支持 AVIF。
- SMB 批量添加复用已认证的文件源，不因探活或重复建连丢失会话。
- 批量添加可选择现有标签并即时新建标签，入库后标签关联可见。

## 影响分析

| 维度 | 内容 |
|------|------|
| 根因 | 外层/内层 Scaffold 安全区职责重叠；返回行为只绑定顶栏按钮；混合页用固定 `index / 2` 配对且视频页共享播放器 ViewModel；缩略图生成器只覆盖部分 ResourceType 且浏览器未消费；媒体扩展名散落且缺 AVIF；SMB 工厂探活/缓存和批量扫描共用连接的生命周期不稳定；批量弹窗未提供建标签入口 |
| 修改文件 | AppShell、FileBrowserScreen/ViewModel、ViewerScreen/ViewModel、MixedFolderProvider、VideoPlayer、缩略图生成/展示、BatchAddResourcesDialog、TagRepository、SMB 文件源/工厂及对应测试 |
| 影响 stage | M13、M14、M15、M17、M19、M23、M24、M27、M28 的既有产物；作为 stage 外回归提案修复 |

## 执行计划

1. 提取并测试媒体类型、混合视觉页分组、返回优先级等纯逻辑。
2. 修复 AppShell/FileBrowser 的窗口 Insets 与 BackHandler。
3. 重构混合查看器视觉页模型：视频独占、智能双页按图片宽高、播放器实例按视频页隔离并正确释放。
4. 建立文件/文件夹缩略图解析与回退卡片，接通首页缩略图生成，统一加入 AVIF。
5. 修复 SMB 会话复用，补齐批量添加标签选择和新建标签。
6. 执行单测、build、lint，回填产出和验收结果。

## 产出

| 文件 | 操作 | 说明 |
|------|------|------|
| `ui/components/AppShell.kt`、`ui/screens/sources/FileBrowserScreen.kt` | 修改 | 统一窗口 Insets/全屏外壳，系统返回键逐级返回 |
| `ViewerSpread.kt`、`ViewerScreen.kt`、`VideoPlayerViewModel.kt` | 新增/修改 | 视频独占视觉页、智能宽图单页、播放器实例隔离和离页暂停 |
| `MixedFolderProvider.kt`、`MediaFormats.kt` | 新增 | 图片视频混合序列、尺寸元数据与含 AVIF 的统一格式集合 |
| `PageBitmapLoader.kt` | 新增 | SMB 大图分块下载、取消检查、断线重试、本地页面缓存与采样解码 |
| `FileEntryThumbnailLoader.kt`、`ImageThumbnailGenerator.kt` | 新增/修改 | 图片/AVIF、视频范围读取首帧、文件夹递归封面和类型回退 |
| `FileBrowserThumbnailDiskCache.kt`、`ThumbnailTaskPool.kt` | 新增 | 指纹磁盘缓存、负缓存、容量淘汰、设置并发限制和 SMB 并发降级 |
| `FileBrowserViewModel.kt`、`BatchAddResourcesDialog.kt` | 修改 | 缩略图缓存、现有标签选择、即时新建并选中标签 |
| `FileSourceFactory.kt` | 修改 | 复用已认证会话、移除每次取实例时的阻塞探活、支持 Guest 空密码 |
| `SmbFileSource.kt`、`SmbClientWrapper.kt` | 修改 | Connection reset 后自动重连一次，传播 stat 网络错误并随流释放远端句柄 |
| `ViewerSpreadTest.kt`、`MediaFormatsTest.kt`、`FileSourceFactoryTest.kt` | 新增/修改 | 覆盖视频独占、智能双页、AVIF 和 Guest SMB |

## 验收结果

- `./gradlew.bat testDebugUnitTest --no-daemon`：通过
- `./gradlew.bat lintDebug --no-daemon --max-workers=1`：通过，无 error
- `./gradlew.bat build --no-daemon --max-workers=1`：通过（100 tasks）
- SMB 真机/NAS 凭据组合与视频 Surface 切换仍建议在目标设备上做一轮交互验收。
