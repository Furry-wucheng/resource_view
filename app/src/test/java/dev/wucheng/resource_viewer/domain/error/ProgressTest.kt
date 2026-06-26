package dev.wucheng.resource_viewer.domain.error

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressTest {

    @Test
    fun `should create Update with current and total`() {
        // When
        val progress = Progress.Update<String>(5, 10)

        // Then
        assertEquals(5, progress.current)
        assertEquals(10, progress.total)
    }

    @Test
    fun `should create Done with result`() {
        // Given
        val result = "completed"

        // When
        val progress = Progress.Done(result)

        // Then
        assertEquals("completed", progress.result)
    }

    @Test
    fun `should create Done with null result`() {
        // When
        val progress = Progress.Done<String?>(null)

        // Then
        assertNull(progress.result)
    }

    @Test
    fun `should create Error with DomainError`() {
        // Given
        val error = DomainError.NetworkTimeoutError("timeout")

        // When
        val progress = Progress.Error(error)

        // Then
        assertEquals(error, progress.error)
    }

    @Test
    fun `should create ScanResult with counts and failures`() {
        // Given
        val failures = listOf(
            "file1.jpg" to DomainError.FileNotFoundError("not found"),
            "file2.pdf" to DomainError.MediaLoadError(MediaType.PDF, "load failed"),
        )

        // When
        val result = ScanResult(
            successCount = 10,
            skipCount = 2,
            failures = failures,
        )

        // Then
        assertEquals(10, result.successCount)
        assertEquals(2, result.skipCount)
        assertEquals(2, result.failures.size)
        assertEquals("file1.jpg", result.failures[0].first)
        assertTrue(result.failures[0].second is DomainError.FileNotFoundError)
        assertEquals("file2.pdf", result.failures[1].first)
        assertTrue(result.failures[1].second is DomainError.MediaLoadError)
    }

    @Test
    fun `should create ScanResult with empty failures`() {
        // When
        val result = ScanResult(
            successCount = 5,
            skipCount = 0,
            failures = emptyList(),
        )

        // Then
        assertEquals(5, result.successCount)
        assertEquals(0, result.skipCount)
        assertEquals(0, result.failures.size)
    }
}
