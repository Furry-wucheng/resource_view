package dev.wucheng.resource_viewer.shared.thumbnail

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class ThumbnailTaskPoolTest {
    @Test fun `never exceeds configured concurrency`() = runTest {
        val pool = ThumbnailTaskPool(2)
        val active = AtomicInteger()
        val peak = AtomicInteger()

        (1..8).map {
            async {
                pool.run {
                    val now = active.incrementAndGet()
                    peak.updateAndGet { maxOf(it, now) }
                    delay(10)
                    active.decrementAndGet()
                }
            }
        }.awaitAll()

        assertEquals(2, peak.get())
    }
}
