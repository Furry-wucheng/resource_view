package dev.wucheng.resource_viewer.domain.usecase

import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import dev.wucheng.resource_viewer.shared.media.MediaFormats

/**
 * 自动检测文件夹组织模式用例。
 *
 * 规则：
 * - 文件夹下仅图片 → FLATGRID
 * - 文件夹下有子文件夹（含图片） → CHAPTER
 * - 文件夹下有子文件夹（含子子文件夹+图片） → CHAPTER_GALLERY
 * - 文件夹下有图片 + PDF + 视频混合 → FLATGRID
 *
 * 注意：此实现遵循 doc/mvp/M20-gallery-flatgrid-strategies.md + doc/mvp/M21-chapter-strategies.md。
 */
class DetectOrganizationModeUseCase {
    /** 支持的图片扩展名 */
    private val imageExtensions = MediaFormats.imageExtensions

    /** 支持的非图片媒体扩展名 */
    private val nonImageMediaExtensions = setOf("pdf", "mp4", "avi", "mkv", "mov", "wmv")

    /**
     * 检测指定文件夹的组织模式。
     *
     * @param fileSource 文件源
     * @param relativePath 相对路径
     * @return 检测到的组织模式
     */
    suspend operator fun invoke(fileSource: FileSource, relativePath: String): OrganizationMode {
        val entries = fileSource.listDirectory(relativePath)

        if (entries.isEmpty()) {
            return OrganizationMode.FLATGRID
        }

        val directories = entries.filter { it.isDirectory }
        val files = entries.filter { !it.isDirectory }

        // 检查是否有子文件夹
        if (directories.isNotEmpty()) {
            // 检查子文件夹是否包含图片
            val hasSubfoldersWithImages = checkSubfoldersContainImages(fileSource, relativePath, directories)
            if (hasSubfoldersWithImages) {
                // 检查子文件夹是否包含子子文件夹（CHAPTER_GALLERY 模式）
                val hasSubSubfolders = checkSubfoldersContainSubfolders(fileSource, relativePath, directories)
                return if (hasSubSubfolders) {
                    OrganizationMode.CHAPTER_GALLERY
                } else {
                    OrganizationMode.CHAPTER
                }
            }
        }

        // 检查文件类型
        val imageFiles = files.filter { it.extension.lowercase() in imageExtensions }
        val nonImageMediaFiles = files.filter { it.extension.lowercase() in nonImageMediaExtensions }

        // 仅图片 → FLATGRID
        if (imageFiles.isNotEmpty() && nonImageMediaFiles.isEmpty() && directories.isEmpty()) {
            return OrganizationMode.FLATGRID
        }

        // 混合类型（图片 + PDF/视频） → FLATGRID
        if (imageFiles.isNotEmpty() && nonImageMediaFiles.isNotEmpty()) {
            return OrganizationMode.FLATGRID
        }

        // 默认使用 FLATGRID
        return OrganizationMode.FLATGRID
    }

    /**
     * 检查子文件夹是否包含图片文件。
     */
    private suspend fun checkSubfoldersContainImages(
        fileSource: FileSource,
        parentPath: String,
        directories: List<dev.wucheng.resource_viewer.domain.model.FileEntry>,
    ): Boolean {
        for (dir in directories) {
            val subEntries = fileSource.listDirectory("$parentPath/${dir.name}")
            val hasImages = subEntries.any { !it.isDirectory && it.extension.lowercase() in imageExtensions }
            if (hasImages) {
                return true
            }
        }
        return false
    }

    /**
     * 检查子文件夹是否包含子子文件夹。
     * 用于区分 CHAPTER 和 CHAPTER_GALLERY 模式。
     */
    private suspend fun checkSubfoldersContainSubfolders(
        fileSource: FileSource,
        parentPath: String,
        directories: List<dev.wucheng.resource_viewer.domain.model.FileEntry>,
    ): Boolean {
        for (dir in directories) {
            val subEntries = fileSource.listDirectory("$parentPath/${dir.name}")
            val hasSubSubfolders = subEntries.any { it.isDirectory }
            if (hasSubSubfolders) {
                return true
            }
        }
        return false
    }
}
