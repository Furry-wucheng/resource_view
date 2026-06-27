package dev.wucheng.resource_viewer.ui.screens.viewer

import dev.wucheng.resource_viewer.data.local.converter.DoublePageMode
import dev.wucheng.resource_viewer.domain.model.ViewerItem

data class ViewerSpread(val itemIndices: List<Int>)

fun buildViewerSpreads(items: List<ViewerItem>, mode: DoublePageMode): List<ViewerSpread> {
    if (mode == DoublePageMode.SINGLE) return items.indices.map { ViewerSpread(listOf(it)) }
    val spreads = mutableListOf<ViewerSpread>()
    var index = 0
    while (index < items.size) {
        val first = items[index]
        if (first is ViewerItem.Video ||
            (mode == DoublePageMode.AUTO && first is ViewerItem.ImagePage && first.isWide())
        ) {
            spreads += ViewerSpread(listOf(index++))
            continue
        }
        val second = items.getOrNull(index + 1)
        val canPair = second is ViewerItem.ImagePage &&
            (mode == DoublePageMode.DOUBLE || !second.isWide())
        spreads += ViewerSpread(if (canPair) listOf(index, index + 1) else listOf(index))
        index += if (canPair) 2 else 1
    }
    return spreads
}

private fun ViewerItem.ImagePage.isWide(): Boolean {
    val width = pixelWidth ?: return true
    val height = pixelHeight ?: return true
    return width.toFloat() / height >= 1.2f
}

fun List<ViewerSpread>.spreadIndexForItem(itemIndex: Int): Int =
    indexOfFirst { itemIndex in it.itemIndices }.coerceAtLeast(0)
