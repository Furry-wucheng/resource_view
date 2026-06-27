package dev.wucheng.resource_viewer.ui.screens.viewer

import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageLoaderTest {
    @Test
    fun `concurrent requests for the same page share one load`() = runTest {
        var loadCount = 0
        val loader = PageLoader<Int, String>(
            maxCacheSize = 100,
            sizeOf = { it.length.toLong() },
            load = { key ->
                loadCount += 1
                "page-$key"
            },
        )

        val first = async { loader.get(3) }
        val second = async { loader.get(3) }

        assertEquals("page-3", first.await())
        assertEquals("page-3", second.await())
        assertEquals(1, loadCount)
    }

    @Test
    fun `least recently used page is evicted when cache is full`() = runTest {
        val loader = PageLoader<Int, String>(
            maxCacheSize = 10,
            sizeOf = { it.length.toLong() },
            load = { key -> "value$key" },
        )

        loader.get(1)
        loader.get(2)

        assertFalse(loader.isCached(1))
        assertTrue(loader.isCached(2))
    }

    @Test
    fun `failed page is not cached and can be retried`() = runTest {
        var attempts = 0
        val loader = PageLoader<Int, String>(
            maxCacheSize = 100,
            sizeOf = { it.length.toLong() },
            load = {
                attempts += 1
                if (attempts == 1) error("network error")
                "recovered"
            },
        )

        runCatching { loader.get(1) }

        assertEquals("recovered", loader.get(1))
        assertEquals(2, attempts)
    }

    @Test
    fun `clear removes every cached page`() = runTest {
        val loader = PageLoader<Int, String>(
            maxCacheSize = 100,
            sizeOf = { it.length.toLong() },
            load = { "value$it" },
        )
        loader.get(1)
        loader.get(2)

        loader.clear()

        assertFalse(loader.isCached(1))
        assertFalse(loader.isCached(2))
    }
}
