package dev.wucheng.resource_viewer.ui.base

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FatalErrorHolderTest {

    @Before
    fun setup() {
        FatalErrorHolder.clear()
    }

    @Test
    fun `should have null error when initialized`() {
        assertNull(FatalErrorHolder.fatalError.value)
    }

    @Test
    fun `should set fatal error message`() {
        FatalErrorHolder.setFatalError("database corrupted")
        assertEquals("database corrupted", FatalErrorHolder.fatalError.value)
    }

    @Test
    fun `should clear fatal error`() {
        FatalErrorHolder.setFatalError("some error")
        FatalErrorHolder.clear()
        assertNull(FatalErrorHolder.fatalError.value)
    }

    @Test
    fun `should overwrite previous error when set again`() {
        FatalErrorHolder.setFatalError("first error")
        FatalErrorHolder.setFatalError("second error")
        assertEquals("second error", FatalErrorHolder.fatalError.value)
    }
}
