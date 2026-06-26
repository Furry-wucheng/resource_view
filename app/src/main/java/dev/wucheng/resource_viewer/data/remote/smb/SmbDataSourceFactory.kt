package dev.wucheng.resource_viewer.data.remote.smb

import androidx.media3.datasource.DataSource
import dev.wucheng.resource_viewer.domain.model.Source

/**
 * SMB DataSource 工厂类。
 * 每次创建新的 SmbDataSource 实例。
 *
 * 注意：此实现遵循 doc/mvp/M18-smb-video-datasource.md 中的 M18.2 子任务。
 */
@androidx.media3.common.util.UnstableApi
class SmbDataSourceFactory(
    private val source: Source,
    private val password: String,
) : DataSource.Factory {

    override fun createDataSource(): DataSource {
        val wrapper = SmbClientWrapper(com.hierynomus.smbj.SMBClient())
        return SmbDataSource(source, password, wrapper)
    }
}
