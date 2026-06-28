# 2026-06-29 — SMB/本地源 GIF 查看无限加载修复

> 时间: 2026-06-29 | Agent: opencode | 状态: ✅ 已完成 | 对应 Issue: 2026-06-29-bug-smb-gif-loading

## 设计决策

### D-001: 统一远程文件本地缓存入口为 `PageBitmapLoader.ensureLocalFile`
- **背景**: `ImageFolderProvider.getPageUri`  originally 直接将 `FileEntry.relativePath` 包装为 `Uri.fromFile(File(...))`，该路径对 SMB 和所有非根本地文件均无效。静态图片能看是因为走了 `PageBitmapLoader.load()` → `fileSource.openInputStream()`，而 GIF 走 Coil `AsyncImage` 需要 `Uri`，两者路径解析不一致。
- **选择**: 在 `PageBitmapLoader` 中新增 `ensureLocalFile(entry): File`，统一处理：
  - `LocalFileSource` → `resolveAbsoluteFile()` 直接返回原始文件（零拷贝）
  - SMB / DocumentTree / 未来协议 → `cacheFileInternal()` 流式下载到 `cacheDir/image_cache/`，复用已有的 `cacheMutex` + SHA-256 键 + LRU `evict`
- **备选**: 在 `ImageFolderProvider`/`MixedFolderProvider` 各自实现缓存逻辑 → 拒绝，会导致代码重复、缓存策略不统一
- **影响文件**: `shared/content/PageBitmapLoader.kt:24-98`
- **被依赖**: `ImageFolderProvider.getPageUri`、`MixedFolderProvider.getPageUri` 消费此接口

### D-002: 移除 `image_cache/pages` 冗余子目录，统一为 `image_cache/`
- **背景**: `PageBitmapLoader.cacheSmbFile` 使用 `image_cache/pages`，而 `ensureLocalFile` 若再用不同子目录会导致缓存分散、多个容量上限逻辑冲突。两者本质都是远程文件的本地副本。
- **选择**: 统一使用 `cacheDirectory!!.resolve("image_cache")`，所有远程文件（静态图片解码前副本、GIF 供 Coil 消费的副本）共享同一目录，统一 `evict` 清理。
- **影响文件**: `shared/content/PageBitmapLoader.kt:34`

### D-003: `MixedFolderProvider` 补充 `getPageExtension` 与 `getPageUri`
- **背景**: `ViewerViewModel.getPageUri` 仅处理 `ImageFolderProvider`，文件浏览器场景使用 `MixedFolderProvider` 时直接抛 `UnsupportedOperationException`，导致 GIF 永远 `imageUri == null`、持续 Loading。
- **选择**: `MixedFolderProvider` 新增 `getPageExtension(index)` 和 `suspend getPageUri(index)`，内部通过 `bitmapLoader.ensureLocalFile` 获取本地文件。`ViewerViewModel.getPageUri` 增加 `is MixedFolderProvider` 分支。
- **影响文件**: `shared/content/MixedFolderProvider.kt:139-156`, `ui/screens/viewer/ViewerViewModel.kt:605-613`

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `shared/filesource/LocalFileSource.kt` | ✏️ 修改 | 新增 `internal fun resolveAbsoluteFile`，供 `PageBitmapLoader` 直接获取本地绝对路径 |
| `shared/content/PageBitmapLoader.kt` | ✏️ 修改 | 去掉 `pages` 子目录；提取通用 `cacheFileInternal()`；新增 `ensureLocalFile(entry)`；`load()` 统一使用 `ensureLocalFile` |
| `shared/content/ImageFolderProvider.kt` | ✏️ 修改 | `getPageUri` 改为 `suspend`，调用 `bitmapLoader.ensureLocalFile` 替代错误 `Uri.fromFile(File(...))` |
| `shared/content/MixedFolderProvider.kt` | ✏️ 修改 | 新增 `getPageExtension()` 和 `suspend getPageUri()`，复用 `bitmapLoader.ensureLocalFile` |
| `ui/screens/viewer/ViewerViewModel.kt` | ✏️ 修改 | `getPageUri` 增加 `is MixedFolderProvider` 分支 |
| `shared/content/ImageFolderProviderTest.kt` | ✏️ 修改 | 补充 `getPageUri` 正常路径测试（mock `Uri.fromFile`） |
| `shared/content/MixedFolderProviderTest.kt` | 🆕 新增 | 测试 `buildViewerItems`、`getPageExtension`、`getPageUri` |
| `doc/issues/2026-06-29-bug-smb-gif-loading.md` | 🆕 新增 | Bug 报告 |
| `doc/decisions/2026-06-29-smb-gif-uri-fix.md` | 🆕 新增 | 本决策日志 |

> 操作图例: 🆕 新增 · ✏️ 修改 · 🗑️ 删除

## 已知问题 / TODO

- [ ] `PageBitmapLoader` 对 SMB 大 GIF（>100MB）的流式缓存缺乏进度反馈，用户可能感觉"卡顿"
- [ ] `PageBitmapLoader.evict` 目前按 LRU + 容量清理，未区分文件类型；如果未来需要保留 GIF 缓存更久，需扩展策略
