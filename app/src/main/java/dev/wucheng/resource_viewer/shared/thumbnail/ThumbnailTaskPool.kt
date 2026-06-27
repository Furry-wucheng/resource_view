package dev.wucheng.resource_viewer.shared.thumbnail

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class ThumbnailTaskPool(maxConcurrent: Int) {
    val maxConcurrent: Int = maxConcurrent.coerceIn(1, 8)
    private val semaphore = Semaphore(this.maxConcurrent)

    suspend fun <T> run(task: suspend () -> T): T = semaphore.withPermit { task() }
}
