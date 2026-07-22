package dev.wucheng.resource_viewer.shared.media

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class MediaFormatsTest {
    @Test fun `avif is a supported image format`() {
        assertTrue(MediaFormats.isImage("AVIF"))
    }

    @Test fun `common archive formats are recognized`() {
        listOf("zip", "cbz", "7z", "rar", "cbr").forEach {
            assertTrue(MediaFormats.isArchive(it.uppercase()))
        }
    }

    @Test fun `readable archive formats exclude rar variants`() {
        assertTrue(MediaFormats.isReadableArchive("zip"))
        assertTrue(MediaFormats.isReadableArchive("cbz"))
        assertTrue(MediaFormats.isReadableArchive("7z"))
        assertFalse(MediaFormats.isReadableArchive("rar"))
        assertFalse(MediaFormats.isReadableArchive("cbr"))
    }
}
