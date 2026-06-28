package dev.wucheng.resource_viewer.shared.filesource

import dev.wucheng.resource_viewer.domain.model.FileEntry
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * Local filesystem implementation of [FileSource].
 *
 * Paths accepted by this source are always relative to [rootPath]. Attempts to
 * escape the root directory are rejected before touching the filesystem.
 */
class LocalFileSource(
    override val sourceId: String,
    rootPath: String,
) : FileSource {
    private val rootFile: File = File(rootPath).canonicalFile

    override suspend fun listDirectory(relativePath: String): List<FileEntry> {
        val directory = resolve(relativePath)
        if (!directory.exists() || !directory.isDirectory) {
            return emptyList()
        }

        return directory.listFiles()
            ?.map { it.toFileEntry() }
            ?.sortedWith(compareBy<FileEntry> { !it.isDirectory }.thenBy { it.name.lowercase() })
            ?: emptyList()
    }

    override suspend fun stat(relativePath: String): FileEntry? {
        val file = resolve(relativePath)
        return if (file.exists()) file.toFileEntry() else null
    }

    override suspend fun readFile(relativePath: String): ByteArray {
        val file = resolveExistingFile(relativePath)
        return file.readBytes()
    }

    override suspend fun readRange(relativePath: String, offset: Long, length: Long): ByteArray {
        require(offset >= 0) { "offset must be >= 0" }
        require(length >= 0) { "length must be >= 0" }

        val file = resolveExistingFile(relativePath)
        FileInputStream(file).use { input ->
            var remainingSkip = offset
            while (remainingSkip > 0) {
                val skipped = input.skip(remainingSkip)
                if (skipped <= 0) return ByteArray(0)
                remainingSkip -= skipped
            }

            val buffer = ByteArray(length.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            val bytesRead = input.read(buffer)
            return when {
                bytesRead <= 0 -> ByteArray(0)
                bytesRead == buffer.size -> buffer
                else -> buffer.copyOf(bytesRead)
            }
        }
    }

    override fun openInputStream(relativePath: String): InputStream {
        return FileInputStream(resolveExistingFile(relativePath))
    }

    override suspend fun testConnection(): Boolean {
        return rootFile.exists() && rootFile.isDirectory && rootFile.canRead()
    }

    override fun disconnect() {
        // Local files do not hold shared connection state.
    }

    internal fun resolveAbsoluteFile(relativePath: String): File = resolve(relativePath)

    private fun resolveExistingFile(relativePath: String): File {
        val file = resolve(relativePath)
        require(file.exists() && file.isFile) { "File does not exist: $relativePath" }
        return file
    }

    private fun resolve(relativePath: String): File {
        val normalized = relativePath.trim().replace('\\', '/').trimStart('/')
        val target = if (normalized.isEmpty()) rootFile else File(rootFile, normalized)
        val canonical = target.canonicalFile
        require(canonical.path == rootFile.path || canonical.path.startsWith(rootFile.path + File.separator)) {
            "Path escapes source root: $relativePath"
        }
        return canonical
    }

    private fun File.toFileEntry(): FileEntry {
        val relative = rootFile.toPath().relativize(canonicalFile.toPath()).toString()
            .replace(File.separatorChar, '/')
        return FileEntry(
            name = name,
            relativePath = relative,
            isDirectory = isDirectory,
            size = if (isFile) length() else 0L,
            modifiedAt = lastModified(),
            extension = if (isFile) name.substringAfterLast('.', "") else "",
        )
    }
}
