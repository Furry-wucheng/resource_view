package dev.wucheng.resource_viewer.data.remote.smb

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dev.wucheng.resource_viewer.shared.filesource.FileSourceFactory
import dev.wucheng.resource_viewer.shared.filesource.SmbFileSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * SMB 连接健康监测器。
 *
 * 监听应用生命周期，当从后台回到前台时，对所有已缓存的 SMB 连接执行探活检查。
 * 探活失败（或抛出异常）的连接会被主动断开并从 FileSourceFactory 缓存中清除，
 * 以便下次访问时重新建立连接，避免用户操作时才感知到掉线。
 *
 * 使用方式：
 * ```
 * val monitor = SmbConnectionMonitor(scope = ProcessLifecycleOwner.get().lifecycleScope)
 * monitor.start(ProcessLifecycleOwner.get())
 * ```
 */
class SmbConnectionMonitor(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val getSmbSources: () -> List<SmbFileSource> = {
        FileSourceFactory.getCachedSmbSources()
    },
    private val evictSource: (String) -> Unit = { sourceId ->
        FileSourceFactory.evict(sourceId)
    },
    private val logger: (String, Throwable?) -> Unit = { _, _ -> }
) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        checkConnections()
    }

    /**
     * 开始监听 [lifecycleOwner] 的生命周期事件。
     * 通常在 [androidx.lifecycle.ProcessLifecycleOwner] 上调用，以感知应用前后台切换。
     */
    fun start(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    /**
     * 停止监听生命周期事件。
     */
    fun stop(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.removeObserver(this)
    }

    /**
     * 立即对所有缓存中的 SMB 源执行一次探活检查。
     * 探活失败的源会被断开并移出缓存。
     */
    fun checkConnections() {
        scope.launch(dispatcher) {
            getSmbSources().forEach { source ->
                val ok = try {
                    source.testConnection()
                } catch (e: Exception) {
                    logger("SMB probe failed: ${source.sourceId}", e)
                    false
                }
                if (!ok) {
                    logger("SMB connection stale, evicting: ${source.sourceId}", null)
                    try {
                        source.disconnect()
                    } catch (_: Exception) {
                        // 清理时忽略异常，避免因为一个源的关闭错误影响其他源
                    }
                    evictSource(source.sourceId)
                }
            }
        }
    }
}
