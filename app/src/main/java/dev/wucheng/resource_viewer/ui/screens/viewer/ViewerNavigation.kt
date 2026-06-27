package dev.wucheng.resource_viewer.ui.screens.viewer

import dev.wucheng.resource_viewer.data.local.converter.PageDirection

enum class ViewerTapAction { PREVIOUS, NEXT, TOGGLE_TOOLBAR }

fun resolveTapAction(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    direction: PageDirection,
): ViewerTapAction {
    val position = if (direction == PageDirection.VERTICAL) y / height.coerceAtLeast(1f) else x / width.coerceAtLeast(1f)
    if (position in ONE_THIRD..TWO_THIRDS) return ViewerTapAction.TOGGLE_TOOLBAR
    val leading = position < ONE_THIRD
    return when (direction) {
        PageDirection.LEFT_TO_RIGHT -> if (leading) ViewerTapAction.PREVIOUS else ViewerTapAction.NEXT
        PageDirection.RIGHT_TO_LEFT -> if (leading) ViewerTapAction.NEXT else ViewerTapAction.PREVIOUS
        PageDirection.VERTICAL -> if (leading) ViewerTapAction.PREVIOUS else ViewerTapAction.NEXT
    }
}

private const val ONE_THIRD = 1f / 3f
private const val TWO_THIRDS = 2f / 3f
