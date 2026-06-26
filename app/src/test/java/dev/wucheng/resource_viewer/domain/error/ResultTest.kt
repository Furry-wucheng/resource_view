package dev.wucheng.resource_viewer.domain.error

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultTest {

    @Test
    fun `should create Ok with value`() {
        // Given
        val value = "test data"

        // When
        val result = Result.Ok(value)

        // Then
        assertTrue(result is Result.Ok)
        assertEquals("test data", (result as Result.Ok).value)
    }

    @Test
    fun `should create Err with DomainError`() {
        // Given
        val error = DomainError.NetworkTimeoutError("timeout")

        // When
        val result = Result.Err(error)

        // Then
        assertTrue(result is Result.Err)
        assertEquals(error, (result as Result.Err).error)
    }

    @Test
    fun `should wrap value as Ok using asOk extension`() {
        // Given
        val value = 42

        // When
        val result = value.asOk()

        // Then
        assertTrue(result is Result.Ok)
        assertEquals(42, (result as Result.Ok).value)
    }

    @Test
    fun `should wrap error as Err using asErr extension`() {
        // Given
        val error = DomainError.ValidationError("invalid")

        // When
        val result = error.asErr()

        // Then
        assertTrue(result is Result.Err)
        assertEquals(error, (result as Result.Err).error)
    }

    @Test
    fun `should wrap null value as Ok`() {
        // Given
        val value: String? = null

        // When
        val result = value.asOk()

        // Then
        assertTrue(result is Result.Ok<*>)
        assertNull((result as Result.Ok<*>).value)
    }

    @Test
    fun `should catch exception and wrap as Err in runCatching`() {
        // Given
        val block: () -> String = { throw RuntimeException("test error") }

        // When
        val result = runCatching(block)

        // Then
        assertTrue(result is Result.Err)
        val error = (result as Result.Err).error
        assertTrue(error is DomainError.ValidationError)
        assertEquals("test error", error.message)
    }

    @Test
    fun `should return Ok with value when runCatching succeeds`() {
        // Given
        val block: () -> String = { "success" }

        // When
        val result = runCatching(block)

        // Then
        assertTrue(result is Result.Ok<*>)
        assertEquals("success", (result as Result.Ok<*>).value)
    }

    @Test
    fun `should catch exception with null message in runCatching`() {
        // Given
        val block: () -> String = { throw RuntimeException() }

        // When
        val result = runCatching(block)

        // Then
        assertTrue(result is Result.Err)
        val error = (result as Result.Err).error
        assertTrue(error is DomainError.ValidationError)
        assertEquals("Unknown error", error.message)
    }
}
