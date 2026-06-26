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
 * 注意：此定义来自 doc/share/02-interfaces.md 共享契约。
 */
object FileSourceFactory {
    fun create(source: Source, password: String? = null, context: Context? = null): FileSource {
        return when (source.type) {
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
                    password = password ?: throw IllegalArgumentException("SMB source requires password"),
                    wrapper = wrapper
                )
            }
            SourceType.FTP -> throw UnsupportedOperationException("FTP not yet supported")
            SourceType.WEBDAV -> throw UnsupportedOperationException("WebDAV not yet supported")
        }
    }
}
