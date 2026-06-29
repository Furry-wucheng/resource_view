package dev.wucheng.resource_viewer.data.remote.smb

import dev.wucheng.resource_viewer.shared.filesource.SmbFileSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SmbConnectionMonitor 单元测试。
 *
 * TDD：验证息屏恢复后的探活与清理逻辑。
 */
class SmbConnectionMonitorTest {

    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun `should do nothing when no SMB sources cached`() = runTest(testDispatcher) {
        val monitor = SmbConnectionMonitor(
            scope = this,
            dispatcher = testDispatcher,
            getSmbSources = { emptyList() },
            evictSource = { throw AssertionError("Should not evict anything") }
        )
        monitor.checkConnections()
        advanceUntilIdle()
    }

    @Test
    fun `should keep source when testConnection succeeds`() = runTest(testDispatcher) {
        val source = mockk<SmbFileSource>(relaxed = true)
        every { source.sourceId } returns "smb-1"
        coEvery { source.testConnection() } returns true

        val evicted = mutableListOf<String>()
        val monitor = SmbConnectionMonitor(
            scope = this,
            dispatcher = testDispatcher,
            getSmbSources = { listOf(source) },
            evictSource = { evicted.add(it) }
        )
        monitor.checkConnections()
        advanceUntilIdle()

        assertTrue("should not evict healthy source", evicted.isEmpty())
        verify(exactly = 0) { source.disconnect() }
        coVerify(exactly = 1) { source.testConnection() }
    }

    @Test
    fun `should disconnect and evict source when testConnection fails`() = runTest(testDispatcher) {
        val source = mockk<SmbFileSource>(relaxed = true)
        every { source.sourceId } returns "smb-1"
        coEvery { source.testConnection() } returns false

        val evicted = mutableListOf<String>()
        val monitor = SmbConnectionMonitor(
            scope = this,
            dispatcher = testDispatcher,
            getSmbSources = { listOf(source) },
            evictSource = { evicted.add(it) }
        )
        monitor.checkConnections()
        advanceUntilIdle()

        verify(exactly = 1) { source.disconnect() }
        assertEquals(listOf("smb-1"), evicted)
        coVerify(exactly = 1) { source.testConnection() }
    }

    @Test
    fun `should disconnect and evict source when testConnection throws`() = runTest(testDispatcher) {
        val source = mockk<SmbFileSource>(relaxed = true)
        every { source.sourceId } returns "smb-1"
        coEvery { source.testConnection() } throws RuntimeException("Network broken")

        val evicted = mutableListOf<String>()
        val logs = mutableListOf<Pair<String, Throwable?>>()
        val monitor = SmbConnectionMonitor(
            scope = this,
            dispatcher = testDispatcher,
            getSmbSources = { listOf(source) },
            evictSource = { evicted.add(it) },
            logger = { msg, t -> logs.add(msg to t) }
        )
        monitor.checkConnections()
        advanceUntilIdle()

        verify(exactly = 1) { source.disconnect() }
        assertEquals(listOf("smb-1"), evicted)
        assertTrue("should log error", logs.any { it.first.contains("probe failed") })
    }

    @Test
    fun `should check all sources independently, one fail does not skip others`() = runTest(testDispatcher) {
        val okSource = mockk<SmbFileSource>(relaxed = true)
        every { okSource.sourceId } returns "smb-ok"
        coEvery { okSource.testConnection() } returns true

        val badSource = mockk<SmbFileSource>(relaxed = true)
        every { badSource.sourceId } returns "smb-bad"
        coEvery { badSource.testConnection() } returns false

        val evicted = mutableListOf<String>()
        val monitor = SmbConnectionMonitor(
            scope = this,
            dispatcher = testDispatcher,
            getSmbSources = { listOf(okSource, badSource) },
            evictSource = { evicted.add(it) }
        )
        monitor.checkConnections()
        advanceUntilIdle()

        verify(exactly = 0) { okSource.disconnect() }
        verify(exactly = 1) { badSource.disconnect() }
        assertEquals(listOf("smb-bad"), evicted)
        coVerify(exactly = 1) { okSource.testConnection() }
        coVerify(exactly = 1) { badSource.testConnection() }
    }
}
