package dev.wucheng.resource_viewer.ui.navigation

import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.domain.model.Resource

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

fun resolveResourceDestination(resource: Resource): ResourceDestination = when (resource.type) {
    ResourceType.PDF,
    ResourceType.ARCHIVE,
    ResourceType.VIDEO,
    -> ResourceDestination.VIEWER
    ResourceType.FOLDER -> resolveResourceDestination(resource.organizationMode)
}
