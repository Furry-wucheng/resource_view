# bug: SMB 大型压缩包缩略图导致 OOM

> 日期: 2026-07-23 | 类型: bug | 状态: ✅ 已完成

## 现象

浏览包含大型压缩包的 SMB 目录时，后台缩略图任务会突然导致应用进程退出。崩溃日志显示 `SmbClientWrapper.readFile()` 通过 `readBytes()` 扩容约 128 MiB 时发生 `OutOfMemoryError`。

## 复现步骤

1. 连接 SMB 数据源。
2. 打开包含大型 ZIP、CBZ 或 7z 文件的目录。
3. 等待文件缩略图加载。
4. 应用因 Java 堆内存耗尽而退出。

## 期望效果

大型压缩包不应通过完整内存读取生成缩略图；目录仍可正常浏览，小型压缩包继续生成缩略图。

## 影响分析

| 维度 | 内容 |
|------|------|
| 根因 | 文件浏览和资源库封面生成均调用 `FileSource.readFile()`，将完整 SMB 压缩包读取为 `ByteArray`；数组扩容峰值超过进程堆上限 |
| 修改文件 | `ArchiveImageReader.kt`、`FileEntryThumbnailLoader.kt`、`ArchiveThumbnailGenerator.kt` 及对应测试 |
| 影响 stage | 修正压缩包资源阅读提案对 M23 Thumbnail 的扩展，不修改共享契约 |

## 执行计划

1. RED：添加大型压缩包不调用 `readFile()` 的回归测试，覆盖文件浏览和资源库封面。
2. GREEN：统一设置压缩包内存缩略图安全上限，超过上限直接跳过。
3. REFACTOR：复用同一策略常量并保持小型压缩包现有行为。
4. 执行定向单测、全量测试、构建和 lint。

## 产出

| 文件 | 操作 | 说明 |
|------|------|------|
| `shared/content/ArchiveImageReader.kt` | 修改 | 定义 32 MiB 内存缩略图安全上限 |
| `shared/thumbnail/FileEntryThumbnailLoader.kt` | 修改 | 文件浏览大型压缩包跳过完整读取 |
| `shared/thumbnail/ArchiveThumbnailGenerator.kt` | 修改 | 资源库大型压缩包跳过完整读取 |
| `shared/thumbnail/FileEntryThumbnailLoaderTest.kt` | 修改 | 验证超限文件不会调用 `readFile()` |
| `shared/thumbnail/ArchiveThumbnailGeneratorTest.kt` | 修改 | 验证资源封面超限时不会读取文件 |

验证：定向测试先 RED 后 GREEN；`./gradlew test build lint` 全部通过。
