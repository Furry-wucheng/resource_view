package dev.wucheng.resource_viewer.shared.content

import dev.wucheng.resource_viewer.domain.model.FileEntry
import dev.wucheng.resource_viewer.shared.media.MediaFormats
import dev.wucheng.resource_viewer.shared.util.NaturalOrderComparator
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipInputStream

internal const val MAX_IN_MEMORY_ARCHIVE_THUMBNAIL_BYTES: Long = 32L * 1024 * 1024

data class ArchiveImageEntry(
    val name: String,
    val size: Long,
)

class UnsupportedArchiveFormatException(extension: String) :
    IOException("Unsupported archive format: $extension")

object ArchiveImageReader {
    fun listImageEntries(archiveBytes: ByteArray, extension: String): List<ArchiveImageEntry> {
        return when (extension.lowercase()) {
            "zip", "cbz" -> listZipImageEntries(archiveBytes)
            "7z" -> listSevenZImageEntries(archiveBytes)
            else -> throw UnsupportedArchiveFormatException(extension)
        }.sortedWith(compareBy<ArchiveImageEntry, String>(NaturalOrderComparator) { it.name })
    }

    fun readImageEntry(archiveBytes: ByteArray, extension: String, entryName: String): ByteArray {
        return when (extension.lowercase()) {
            "zip", "cbz" -> readZipImageEntry(archiveBytes, entryName)
            "7z" -> readSevenZImageEntry(archiveBytes, entryName)
            else -> throw UnsupportedArchiveFormatException(extension)
        }
    }

    fun firstImageEntry(archiveBytes: ByteArray, extension: String): Pair<ArchiveImageEntry, ByteArray>? {
        val first = listImageEntries(archiveBytes, extension).firstOrNull() ?: return null
        return first to readImageEntry(archiveBytes, extension, first.name)
    }

    private fun listZipImageEntries(bytes: ByteArray): List<ArchiveImageEntry> {
        return ZipInputStream(bytes.inputStream().buffered()).use { zip ->
            generateSequence { zip.nextEntry }
                .filter { !it.isDirectory && MediaFormats.isImage(it.name.substringAfterLast('.', "")) }
                .map { ArchiveImageEntry(it.name, it.size.coerceAtLeast(0L)) }
                .toList()
        }
    }

    private fun readZipImageEntry(bytes: ByteArray, entryName: String): ByteArray {
        ZipInputStream(bytes.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory && entry.name == entryName) {
                    return zip.readBytes()
                }
            }
        }
        throw IOException("Archive entry not found: $entryName")
    }

    private fun listSevenZImageEntries(bytes: ByteArray): List<ArchiveImageEntry> {
        return openSevenZ(bytes).use { sevenZ ->
            val result = mutableListOf<ArchiveImageEntry>()
            while (true) {
                val entry = sevenZ.nextEntry ?: break
                val name = entry.name ?: continue
                if (!entry.isDirectory && MediaFormats.isImage(name.substringAfterLast('.', ""))) {
                    result += ArchiveImageEntry(name, entry.size.coerceAtLeast(0L))
                }
            }
            result
        }
    }

    private fun readSevenZImageEntry(bytes: ByteArray, entryName: String): ByteArray {
        openSevenZ(bytes).use { sevenZ ->
            while (true) {
                val entry = sevenZ.nextEntry ?: break
                if (!entry.isDirectory && entry.name == entryName) {
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = sevenZ.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                    }
                    return output.toByteArray()
                }
            }
        }
        throw IOException("Archive entry not found: $entryName")
    }

    private fun openSevenZ(bytes: ByteArray): SevenZFile {
        return SevenZFile(SeekableInMemoryByteChannel(bytes))
    }
}

fun FileEntry.archiveExtension(): String = extension.ifBlank { name.substringAfterLast('.', "") }
