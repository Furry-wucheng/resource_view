package dev.wucheng.resource_viewer.ui.components

const val WIDE_LAYOUT_MIN_WIDTH_DP = 900
const val STACKED_DIALOG_MAX_WIDTH_DP = 480
const val COMPACT_SOURCE_CARD_MAX_WIDTH_DP = 600

fun isWideLayout(widthDp: Int): Boolean = widthDp >= WIDE_LAYOUT_MIN_WIDTH_DP

fun useStackedDialogFields(availableWidthDp: Float): Boolean =
    availableWidthDp < STACKED_DIALOG_MAX_WIDTH_DP

fun useCompactSourceActions(availableWidthDp: Float): Boolean =
    availableWidthDp < COMPACT_SOURCE_CARD_MAX_WIDTH_DP

fun shouldOpenDirectoryTreeInitially(isWide: Boolean, isEnabled: Boolean): Boolean =
    isWide && isEnabled

fun shouldCloseDirectoryTreeAfterNavigation(isWide: Boolean): Boolean = !isWide
