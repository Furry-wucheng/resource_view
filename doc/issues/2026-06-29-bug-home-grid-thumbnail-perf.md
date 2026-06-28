# bug+perf: 主页样式统一、缩略图不显示、视频缩略图生成过慢

> 日期: 2026-06-29 | 类型: bug+perf | 状态: ✅ 已完成

## 现象

1. **主页 ResourceGridItem 样式与文件浏览器 FileEntryGridItem 不一致**：主页使用简单 Box + 底部文字，缺乏卡片感、类型标识和渐变遮罩。
2. **资源库缩略图全部不显示**：已入库资源的 `thumbnailPath` 存在数据库且文件存在磁盘，但主页网格始终显示纯色背景。
3. **批量添加资源过程极慢**：添加包含视频的文件夹时，每个视频资源的缩略图生成会全量读取整个视频文件（对 SMB 尤为严重），导致添加过程耗时数分钟甚至卡住。

## 复现步骤

1. 打开文件浏览器 → 勾选含有多个视频/图片的文件夹 → 批量添加
2. 等待进度 → 观察添加耗时（尤其是 SMB 来源）
3. 添加完成后回到主页 → 观察缩略图是否显示

## 期望效果

- 主页网格样式与文件浏览器的网格视图一致（Card 卡片、类型颜色+图标 fallback、底部渐变+标题）
- 缩略图正常显示
- 批量添加含视频的资源时，缩略图生成不需要全量读取视频文件

## 影响分析

| 维度 | 内容 |
|------|------|
| 根因 A | Coil 3 `AsyncImage` 使用 `data(String)` 时把纯文件路径字符串当作网络 URL，不识别为本地文件 |
| 根因 B | `VideoThumbnailGenerator` 使用 `fileSource.readFile()` 全量读取视频 → `MediaMetadataRetriever.setDataSource(String)` 需临时文件 → OOM & 慢 |
| 根因 C | `BatchAddResourcesUseCase.generateThumbnails()` 所有异常静默吞掉，缩略图失败不可见 |
| 修改文件 | `ResourceGridItem.kt`、`VideoThumbnailGenerator.kt`、`BatchAddResourcesUseCase.kt`、`ImageThumbnailGenerator.kt`、`FileEntryThumbnailLoader.kt` |
| 影响 stage | M23（首页网格）、M19（视频播放器）、M27（批量添加）、fix（缩略图）、cache-refactor（缩略图） |

## 是否需合并回原文档

[ ] 否 → 独立提案，不回溯

## 执行计划

1. **RED** — 确认 `VideoThumbnailGeneratorTest` 测试会验证 `readFile` 调用
2. **GREEN** — 改写 `VideoThumbnailGenerator` 使用 `FileSourceMediaDataSource`（分块读取），更新测试验证不再调用 `readFile`
3. **GREEN** — 重构 `ResourceGridItem` 为 Card 样式 + 3:4 比例 + 类型 fallback + 渐变 + 修复 Coil 加载（`File` 对象）
4. **GREEN** — 清理 `ImageThumbnailGenerator` 死代码
5. **GREEN** — `BatchAddResourcesUseCase` 添加 `Log.e` 异常日志
6. **REFACTOR** — 运行 `./gradlew build`、`./gradlew test`、`./gradlew lint`

## 产出

| 文件 | 操作 | 说明 |
|------|------|------|
| `ui/components/ResourceGridItem.kt` | ✏️ 重写 | Card 卡片、3:4 比例、类型图标 fallback、渐变遮罩、Coil `File` 修复、保留收藏星标、去掉标签圆点 |
| `shared/thumbnail/VideoThumbnailGenerator.kt` | ✏️ 重写 | 改用 `FileSourceMediaDataSource` 分块读取，不再全量 `readFile` |
| `domain/usecase/BatchAddResourcesUseCase.kt` | ✏️ 修改 | 缩略图异常添加 `Log.e` 日志，不再静默吞掉 |
| `shared/thumbnail/ImageThumbnailGenerator.kt` | ✏️ 清理 | 移除未使用的 `decodeAndScale` 死代码 |
| `shared/thumbnail/FileEntryThumbnailLoader.kt` | ✏️ 修改 | `FileSourceMediaDataSource` 改为 `internal` 可见 |
| `shared/thumbnail/VideoThumbnailGeneratorTest.kt` | ✏️ 更新 | 验证不再调用 `readFile`，验证缓存复用 |
