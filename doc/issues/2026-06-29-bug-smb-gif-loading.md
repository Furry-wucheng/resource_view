# bug: SMB 源 GIF 查看无限加载，本地源 GIF 路径同样错误

> 日期: 2026-06-29 | 类型: bug | 状态: ✅ 已完成

## 现象

1. **SMB 源**：打开 GIF 文件时，查看器持续显示加载动画（CircularProgressIndicator），无法显示内容。
2. **本地源（资源库/文件浏览器）**：GIF 同样无法加载（虽可能因根目录特例未被触发）。
3. 日志中伴随 GPU 渲染超时警告：`RenderInspector DequeueBuffer time out`。

## 复现步骤

1. 添加 SMB 数据源，确保目录中包含 `.gif` 文件
2. 在文件浏览器中点击该 GIF 文件，或将其加入资源库后从首页打开
3. 观察查看器持续 Loading，无动画/内容显示

## 期望效果

GIF 文件（包括 SMB、本地 DocumentTree、本地文件系统）在所有查看入口都能正常加载并播放动画。

## 影响分析

| 维度 | 内容 |
|------|------|
| 根因 | `ImageFolderProvider.getPageUri()` 将 `FileEntry.relativePath`（相对路径）直接包装为 `Uri.fromFile(File(...))`，该路径对 SMB 和所有非根本地文件均无效；`ViewerViewModel.getPageUri()` 未处理 `MixedFolderProvider` 类型 |
| 修改文件 | `LocalFileSource.kt`, `PageBitmapLoader.kt`, `ImageFolderProvider.kt`, `MixedFolderProvider.kt`, `ViewerViewModel.kt` |
| 新增测试 | `MixedFolderProviderTest.kt` |
| 修改测试 | `ImageFolderProviderTest.kt` |
| 影响 stage | 无现有 stage 产物需修改，属于 bug 修复 |
| 聚合文件 | `doc/decisions/AGENTS.md`、`doc/decisions/cross-reference.md`（追加行） |

## 执行计划

1. **RED** — 分析路径链路，确认 `getPageUri` 对 `ImageFolderProvider` 和 `MixedFolderProvider` 均返回无效路径
2. **GREEN** — 新增 `PageBitmapLoader.ensureLocalFile(entry)`，统一处理本地源直接引用 / 远程源流式缓存；修正 `ImageFolderProvider`/`MixedFolderProvider`/`ViewerViewModel`
3. **REFACTOR** — 去掉 `image_cache/pages` 冗余子目录，统一为 `image_cache/`；提取 `cacheFileInternal` 通用方法
4. **测试** — 补充 `ImageFolderProvider.getPageUri` 正常路径测试；新建 `MixedFolderProviderTest`
5. **验证** — `./gradlew test` + `./gradlew lint` 全部通过

## 产出

| 文件 | 操作 | 说明 |
|------|------|------|
| `doc/issues/2026-06-29-bug-smb-gif-loading.md` | 新增 | 本 bug 报告 |
| `doc/decisions/2026-06-29-smb-gif-uri-fix.md` | 新增 | 决策日志 |
| `shared/filesource/LocalFileSource.kt` | 修改 | 新增 `resolveAbsoluteFile`，供 `PageBitmapLoader` 获取本地绝对路径 |
| `shared/content/PageBitmapLoader.kt` | 修改 | 去掉 `pages` 子目录；提取 `cacheFileInternal`；新增 `ensureLocalFile` |
| `shared/content/ImageFolderProvider.kt` | 修改 | `getPageUri` 改为 `suspend`，调用 `bitmapLoader.ensureLocalFile` |
| `shared/content/MixedFolderProvider.kt` | 修改 | 新增 `getPageExtension`、`getPageUri`，复用 `bitmapLoader.ensureLocalFile` |
| `ui/screens/viewer/ViewerViewModel.kt` | 修改 | `getPageUri` 增加 `MixedFolderProvider` 分支 |
| `shared/content/ImageFolderProviderTest.kt` | 修改 | 补充 `getPageUri` 正常路径测试 |
| `shared/content/MixedFolderProviderTest.kt` | 新增 | 测试 `buildViewerItems`、`getPageExtension`、`getPageUri` |

验收: build/test/lint passed
