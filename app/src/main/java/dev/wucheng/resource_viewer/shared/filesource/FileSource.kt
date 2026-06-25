package dev.wucheng.resource_viewer.shared.filesource

import dev.wucheng.resource_viewer.domain.model.FileEntry
import java.io.InputStream

/**
 * 统一本地/SMB/未来协议的文件访问接口。
 * 策略模式：不同来源实现此接口。
 *
 * 注意：此接口定义来自 doc/share/02-interfaces.md 共享契约。
 * 完整实现（LocalFileSource、SmbFileSource）将在后续 Stage 中添加。
 */
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
