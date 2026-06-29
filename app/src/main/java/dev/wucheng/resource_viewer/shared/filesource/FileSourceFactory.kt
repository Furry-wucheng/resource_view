package dev.wucheng.resource_viewer.shared.filesource

import android.content.Context
import android.net.Uri
import com.hierynomus.smbj.SMBClient
import dev.wucheng.resource_viewer.data.local.converter.SourceType
import dev.wucheng.resource_viewer.data.remote.smb.SmbClientWrapper
import dev.wucheng.resource_viewer.domain.model.Source

/**
 * FileSource 工厂对象。
 * 根据 SourceType 返回对应的 FileSource 实现。
 *
 * SMB 连接会按 sourceId 缓存，避免重复创建连接导致认证丢失。
 */
object FileSourceFactory {
    /** 缓存的 FileSource 实例（按 sourceId） */
    private val cache = mutableMapOf<String, FileSource>()

    fun create(source: Source, password: String? = null, context: Context? = null): FileSource {
        // 检查缓存
        cache[source.id]?.let { return it }

        val fileSource = when (source.type) {
            SourceType.LOCAL -> {
                if (source.rootPath.startsWith("content://")) {
                    DocumentTreeFileSource(
                        sourceId = source.id,
                        context = context ?: throw IllegalArgumentException("Document tree source requires context"),
                        treeUri = Uri.parse(source.rootPath),
                    )
                } else {
                    LocalFileSource(source.id, source.rootPath)
                }
            }
            SourceType.SMB -> {
                val wrapper = SmbClientWrapper(SMBClient())
                SmbFileSource(
                    source = source,
                    // SMB guest/anonymous accounts intentionally use an empty password.
                    password = password.orEmpty(),
                    wrapper = wrapper
                )
            }
            SourceType.FTP -> throw UnsupportedOperationException("FTP not yet supported")
            SourceType.WEBDAV -> throw UnsupportedOperationException("WebDAV not yet supported")
        }

        cache[source.id] = fileSource
        return fileSource
    }

    /** 移除指定源的缓存（断开连接） */
    fun evict(sourceId: String) {
        cache.remove(sourceId)?.let {
            try { it.disconnect() } catch (_: Exception) {}
        }
    }

    /** 清除所有缓存 */
    fun clearAll() {
        cache.values.forEach {
            try { it.disconnect() } catch (_: Exception) {}
        }
        cache.clear()
    }

    /** 获取当前缓存中的所有 SMB 文件源（用于连接监控探活） */
    fun getCachedSmbSources(): List<SmbFileSource> {
        return cache.values.filterIsInstance<SmbFileSource>()
    }
}
