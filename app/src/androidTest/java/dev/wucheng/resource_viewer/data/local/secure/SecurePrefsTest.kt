package dev.wucheng.resource_viewer.data.local.secure

import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * SecurePrefs 测试。
 * 使用 MockK 进行单元测试。
 */
@RunWith(AndroidJUnit4::class)
class SecurePrefsTest {
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var securePrefs: SecurePrefs

    @Before
    fun setup() {
        mockPrefs = mockk()
        mockEditor = mockk()
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.remove(any()) } returns mockEditor
        every { mockEditor.apply() } returns Unit
        securePrefs = SecurePrefs(mockPrefs)
    }

    @Test
    fun `should store password with correct key`() {
        securePrefs.putPassword("source-123", "password123")
        verify { mockPrefs.edit() }
        verify { mockEditor.putString("password_source-123", "password123") }
        verify { mockEditor.apply() }
    }

    @Test
    fun `should get password with correct key`() {
        every { mockPrefs.getString("password_source-123", null) } returns "password123"
        val password = securePrefs.getPassword("source-123")
        assertEquals("password123", password)
    }

    @Test
    fun `should return null when password not found`() {
        every { mockPrefs.getString("password_source-123", null) } returns null
        val password = securePrefs.getPassword("source-123")
        assertNull(password)
    }

    @Test
    fun `should remove password with correct key`() {
        securePrefs.removePassword("source-123")
        verify { mockPrefs.edit() }
        verify { mockEditor.remove("password_source-123") }
        verify { mockEditor.apply() }
    }
}
