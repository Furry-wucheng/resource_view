package dev.wucheng.resource_viewer.shared.media

import org.junit.Assert.assertTrue
import org.junit.Test

class MediaFormatsTest {
    @Test fun `avif is a supported image format`() {
        assertTrue(MediaFormats.isImage("AVIF"))
    }
}
