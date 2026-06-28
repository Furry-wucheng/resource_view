# home-grid-style-thumbnail-perf — 主页样式统一 + 缩略图修复 + 视频缩略图性能优化

> 时间: 2026-06-29 | Agent: opencode/clever-planet | 状态: ✅ 已完成 | 前置: M23, M19, M27, fix, cache-refactor

## 设计决策

### D-001: 主页 ResourceGridItem 样式与文件浏览器对齐
- **背景**: 用户反馈主页样式不如文件浏览器好看，两者视觉风格不一致
- **选择**: 完全复用 `FileEntryGridItem` 的视觉语言：
  - `Card` 包裹 + `aspectRatio(3f/4f)`
  - 无缩略图时显示 `ResourceType` 对应颜色 + 图标（FOLDER=蓝/Folder、PDF=红/PictureAsPdf、VIDEO=绿/Movie、ARCHIVE=灰/Folder）
  - 底部 `Brush.verticalGradient(Transparent → Black 88%)` + 白色标题文字
  - 保留左上角收藏星标（去掉之前底部的标签圆点）
- **备选**: 保持原有样式仅微调 → 放弃，因为原有样式缺乏类型识别和视觉层次
- **影响文件**: `ui/components/ResourceGridItem.kt`
- **被依赖**: M23 消费此组件 → 不可随意改动签名

### D-002: Coil 3 本地文件加载修复
- **背景**: `thumbnailPath` 是磁盘绝对路径（如 `/data/data/.../thumb_xxx.jpg`），Coil 3 `AsyncImage(model = String)` 会把非 URL scheme 的字符串当作网络 URL，导致缩略图始终加载失败
- **选择**: 传入 `File(thumbnailPath)` 对象，Coil 3 的 `FileMapper` 会正确识别为本地文件路径
- **备选**: 添加 `file://` scheme 前缀 → 放弃，`File` 对象更直接且避免 scheme 解析问题
- **影响文件**: `ui/components/ResourceGridItem.kt:76-80`
- **被依赖**: M23

### D-003: VideoThumbnailGenerator 分块读取替代全量 readFile
- **背景**: `VideoThumbnailGenerator.generate()` 使用 `fileSource.readFile()` 全量读取视频文件字节，再写入临时文件。对于大视频（GB 级）或 SMB 来源，这会导致：OOM、长时间阻塞、完整网络下载
- **选择**: 改用 `MediaMetadataRetriever.setDataSource(MediaDataSource)`，配合 `FileSourceMediaDataSource`（基于 `FileSource.readRange` 分块读取）。`MediaMetadataRetriever` 只按需读取视频头部元数据即可提取首帧
- **备选**: 限制 `readFile` 文件大小阈值 → 放弃，阈值选择困难且仍无法解决 SMB 大文件问题；改用 `openInputStream` 流式读取 → `MediaMetadataRetriever` 不支持 `InputStream`
- **影响文件**: `shared/thumbnail/VideoThumbnailGenerator.kt`
- **被依赖**: M19, cache-refactor

### D-004: FileSourceMediaDataSource 可见性提升
- **背景**: `FileSourceMediaDataSource` 原定义为 `private class` 在 `FileEntryThumbnailLoader.kt` 内，但 `VideoThumbnailGenerator` 也需要使用
- **选择**: 改为 `internal class`，使其在同模块内共享
- **备选**: 复制一份到 `VideoThumbnailGenerator.kt` → 放弃，DRY 原则
- **影响文件**: `shared/thumbnail/FileEntryThumbnailLoader.kt:93`

### D-005: BatchAddResourcesUseCase 缩略图异常日志可见性
- **背景**: `generateThumbnails()` 中所有异常均为 `catch (_: Exception) { }`，缩略图失败完全静默，无法排查具体是哪个资源/哪个环节出问题
- **选择**: 改为 `catch (e: Exception) { Log.e(TAG, "...", e) }`，保持"缩略图失败不中断主流程"的同时提供日志
- **备选**: 将异常累积到 `ScanResult` → 放弃，缩略图是异步后台任务，不应阻塞用户交互反馈
- **影响文件**: `domain/usecase/BatchAddResourcesUseCase.kt:125-137`

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `ui/components/ResourceGridItem.kt` | ✏️ 重写 | Card 卡片、3:4 比例、类型图标 fallback、渐变遮罩、Coil File 对象修复、保留收藏星标、去掉标签圆点 |
| `shared/thumbnail/VideoThumbnailGenerator.kt` | ✏️ 重写 | FileSourceMediaDataSource 分块读取，移除 readFile 全量读取 |
| `domain/usecase/BatchAddResourcesUseCase.kt` | ✏️ 修改 | 缩略图异常添加 Log.e 日志 |
| `shared/thumbnail/ImageThumbnailGenerator.kt` | ✏️ 清理 | 移除未使用的 decodeAndScale 死代码，提取 MAX_THUMBNAIL_SIZE const |
| `shared/thumbnail/FileEntryThumbnailLoader.kt` | ✏️ 修改 | FileSourceMediaDataSource private → internal |
| `shared/thumbnail/VideoThumbnailGeneratorTest.kt` | ✏️ 更新 | 验证不再调用 readFile、验证缓存复用 |

> 操作图例: 🆕 新增 · ✏️ 修改 · 🗑️ 删除

## 已知问题 / TODO

- [ ] ImageThumbnailGenerator 仍为 `context: Context` 参数但实际未使用（可后续清理）
- [ ] `ARCHIVE` 类型尚未注册缩略图生成器（当前显示灰色 fallback 图标，P2 阶段处理）
