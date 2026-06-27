package dev.wucheng.resource_viewer.ui.screens.viewer

import dev.wucheng.resource_viewer.data.local.converter.DoublePageMode
import dev.wucheng.resource_viewer.domain.model.VideoMediaSource
import dev.wucheng.resource_viewer.domain.model.ViewerItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ViewerSpreadTest {
    private fun image(name: String, width: Int = 800, height: Int = 1200) =
        ViewerItem.ImagePage(name, 0, pixelWidth = width, pixelHeight = height)
    private fun video(name: String) = ViewerItem.Video(name, VideoMediaSource.LocalFile(name))

    @Test fun `video always occupies its own spread`() {
        val spreads = buildViewerSpreads(listOf(image("1"), video("2"), image("3")), DoublePageMode.DOUBLE)
        assertEquals(listOf(listOf(0), listOf(1), listOf(2)), spreads.map { it.itemIndices })
    }

    @Test fun `auto mode keeps wide image on its own spread`() {
        val spreads = buildViewerSpreads(listOf(image("wide", 1600, 900), image("portrait"), image("portrait2")), DoublePageMode.AUTO)
        assertEquals(listOf(listOf(0), listOf(1, 2)), spreads.map { it.itemIndices })
    }

    @Test fun `auto mode treats unknown dimensions conservatively`() {
        val unknown = ViewerItem.ImagePage("unknown", 0)
        assertEquals(listOf(listOf(0), listOf(1)), buildViewerSpreads(listOf(unknown, image("next")), DoublePageMode.AUTO).map { it.itemIndices })
    }
}
