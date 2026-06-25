# 02 — 共享接口

> 🔵 所有 Agent 共享，不得在 stage 开发中修改。变更需 PR review。
> 依据：`@tech/02-架构设计.md` §4

---

## 1. FileSource 接口

> 统一本地/SMB/未来协议的文件访问。策略模式。

```kotlin
interface FileSource {
    val sourceId: String

    /** 列出目录内容 */
    suspend fun listDirectory(relativePath: String): List<FileEntry>

    /** 获取文件/目录元数据 */
    suspend fun stat(relativePath: String): FileEntry?

    /**
     * 读取完整文件内容。
     * ⚠️ 仅用于小文件（缩略图生成、元数据解析）。
     * 大文件（视频、大 PDF）必须使用 openInputStream() 做流式处理。
     */
    suspend fun readFile(relativePath: String): ByteArray

    /** 范围读取（视频流、分片下载） */
    suspend fun readRange(relativePath: String, offset: Long, length: Long): ByteArray

    /** 打开输入流。Coil、ExoPlayer 可直接消费，避免全量 ByteArray OOM */
    fun openInputStream(relativePath: String): InputStream

    /** 测试连接可达性 */
    suspend fun testConnection(): Boolean

    /** 释放资源 */
    fun disconnect()
}

data class FileEntry(
    val name: String,
    val relativePath: String,
    val isDirectory: Boolean,
    val size: Long,
    val modifiedAt: Long,
    val extension: String = "",
)
```

### FileSourceFactory

```kotlin
object FileSourceFactory {
    fun create(source: Source, password: String? = null): FileSource {
        return when (source.type) {
            SourceType.LOCAL -> LocalFileSource(source.sourceId, source.rootPath)
            SourceType.SMB -> SmbFileSource(source, password ?: throw ...)
            SourceType.FTP -> throw UnsupportedOperationException("FTP not yet supported")
            SourceType.WEBDAV -> throw UnsupportedOperationException("WebDAV not yet supported")
        }
    }
}
```

### 实现类清单

| 实现 | Stage | 文件 |
|------|-------|------|
| `LocalFileSource` | M12 | `shared/filesource/LocalFileSource.kt` |
| `SmbFileSource` | M17 | `shared/filesource/SmbFileSource.kt` |
| `FtpFileSource` | 预留 | — |
| `WebDavFileSource` | 预留 | — |

---

## 2. ContentProvider 接口

> 查看器内容抽象。图片文件夹、PDF、压缩包各自实现。

```kotlin
interface ContentProvider {
    /** 总页数 */
    val pageCount: Int

    /**
     * 加载指定页并按目标尺寸渲染。
     * @param index 页码（0-based）
     * @param targetWidth 目标宽度（px），用于控制解码/渲染分辨率
     * @param targetHeight 目标高度（px）
     * @return 按目标尺寸解码的 Bitmap
     */
    suspend fun loadPage(index: Int, targetWidth: Int, targetHeight: Int): Bitmap

    /** 释放资源（文件句柄、PDF 文档等） */
    fun dispose()
}
```

### 实现类清单

| 实现 | Stage | 说明 |
|------|-------|------|
| `ImageFolderProvider` | M14 | 图片文件夹按文件名排序 |
| `PdfContentProvider` | M22 | pdfium 逐页渲染 |
| `ArchiveContentProvider` | P2 | 压缩包内图片列表 |

---

## 3. OrganizationStrategy 接口

> 组织模式策略。每种模式实现自己的章节拆解和内容获取逻辑。

```kotlin
interface OrganizationStrategy {
    val mode: OrganizationMode

    /** 获取章节列表（CHAPTER/CHAPTER_GALLERY 模式） */
    suspend fun getChapters(resource: Resource, fileSource: FileSource): List<Chapter>

    /** 获取内容列表。GALLERY 模式返回递归展开的全部文件，大数据量时用 Sequence 懒遍历。 */
    suspend fun getContents(resource: Resource, fileSource: FileSource): List<FileEntry>

    /** 为指定章节创建 ContentProvider */
    fun createProvider(
        resource: Resource,
        fileSource: FileSource,
        chapter: Chapter? = null,
    ): ContentProvider
}

data class Chapter(
    val name: String,
    val relativePath: String,
    val fileCount: Int = 0,
    val coverPath: String? = null,
)
```

### 实现类清单

| 实现 | Stage | 说明 |
|------|-------|------|
| `FlatGridStrategy` | M20 | 一层图片 → 直接网格 |
| `GalleryStrategy` | M20 | 一层图片 → 画廊呈现 |
| `ChapterStrategy` | M21 | 子文件夹 → 章节列表 → 选章阅读 |
| `ChapterGalleryStrategy` | M21 | 根层章节 + 章内递归扁平阅读 |

---

## 4. ThumbnailGenerator 接口

> 缩略图生成策略。按来源类型选择实现。

```kotlin
interface ThumbnailGenerator {
    /** 是否可处理该资源类型 */
    fun canHandle(type: ResourceType): Boolean

    /** 生成缩略图，返回缓存文件路径 */
    suspend fun generate(
        resource: Resource,
        fileSource: FileSource,
        cacheDir: File,
    ): File?
}
```

### 实现类清单

| 实现 | Stage | 说明 |
|------|-------|------|
| `ImageThumbnailGenerator` | M23 | Coil 加载 + decode + resize |
| `PdfThumbnailGenerator` | M22 | 渲染 page 0 → Bitmap |
| `VideoThumbnailGenerator` | M19 | MediaMetadataRetriever 提取首帧 |
| `ArchiveThumbnailGenerator` | P2 | zip4j 读取首图 |
