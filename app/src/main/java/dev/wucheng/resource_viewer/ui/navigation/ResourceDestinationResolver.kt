package dev.wucheng.resource_viewer.ui.navigation

import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode

enum class ResourceDestination {
    CHAPTER_LIST,
    FLAT_GRID,
    GALLERY,
    VIEWER,
}

fun resolveResourceDestination(mode: OrganizationMode?): ResourceDestination = when (mode) {
    OrganizationMode.CHAPTER,
    OrganizationMode.CHAPTER_GALLERY,
    -> ResourceDestination.CHAPTER_LIST
    OrganizationMode.FLATGRID -> ResourceDestination.FLAT_GRID
    OrganizationMode.GALLERY -> ResourceDestination.GALLERY
    null -> ResourceDestination.VIEWER
}
