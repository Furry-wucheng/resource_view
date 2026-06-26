package dev.wucheng.resource_viewer.shared.filesource

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import dev.wucheng.resource_viewer.domain.model.FileEntry
import java.io.InputStream

class DocumentTreeFileSource(
    override val sourceId: String,
    private val context: Context,
    private val treeUri: Uri,
) : FileSource {
    private val resolver = context.contentResolver
    private val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)

    override suspend fun listDirectory(relativePath: String): List<FileEntry> {
        val directory = findDocument(relativePath.trim('/')) ?: return emptyList()
        if (!directory.isDirectory) return emptyList()
        return listChildren(directory.documentId, relativePath.trim('/'))
    }

    override suspend fun stat(relativePath: String): FileEntry? {
        val normalized = relativePath.trim('/')
        val document = findDocument(normalized) ?: return null
        return document.toFileEntry(normalized.substringBeforeLast("/", missingDelimiterValue = ""))
    }

    override suspend fun readFile(relativePath: String): ByteArray {
        return openInputStream(relativePath).use { it.readBytes() }
    }

    override suspend fun readRange(relativePath: String, offset: Long, length: Long): ByteArray {
        require(offset >= 0) { "offset must be >= 0" }
        require(length >= 0) { "length must be >= 0" }

        openInputStream(relativePath).use { input ->
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
        val document = findDocument(relativePath.trim('/'))
            ?: throw IllegalArgumentException("Document does not exist: $relativePath")
        require(!document.isDirectory) { "Cannot open directory as stream: $relativePath" }
        val documentUri = getDocumentUri(relativePath)
        return resolver.openInputStream(documentUri)
            ?: throw IllegalStateException("Unable to open document: $relativePath")
    }

    fun getDocumentUri(relativePath: String): Uri {
        val document = findDocument(relativePath.trim('/'))
            ?: throw IllegalArgumentException("Document does not exist: $relativePath")
        return DocumentsContract.buildDocumentUriUsingTree(treeUri, document.documentId)
    }

    override suspend fun testConnection(): Boolean {
        return findDocument("")?.isDirectory == true
    }

    override fun disconnect() {
        // Persisted SAF permissions are owned by ContentResolver, no connection to close.
    }

    private fun findDocument(relativePath: String): DocumentInfo? {
        if (relativePath.isEmpty()) {
            return queryDocument(rootDocumentId)
        }

        var current = queryDocument(rootDocumentId) ?: return null
        for (segment in relativePath.split('/').filter { it.isNotBlank() }) {
            if (!current.isDirectory) return null
            current = listChildDocuments(current.documentId)
                .firstOrNull { it.name == segment }
                ?: return null
        }
        return current
    }

    private fun listChildren(parentDocumentId: String, parentPath: String): List<FileEntry> {
        return listChildDocuments(parentDocumentId)
            .map { it.toFileEntry(parentPath) }
            .sortedWith(compareBy<FileEntry> { !it.isDirectory }.thenBy { it.name.lowercase() })
    }

    private fun listChildDocuments(parentDocumentId: String): List<DocumentInfo> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        return resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            buildList {
                val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                val modifiedIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                while (cursor.moveToNext()) {
                    add(
                        DocumentInfo(
                            documentId = cursor.getString(idIndex),
                            name = cursor.getString(nameIndex) ?: "",
                            mimeType = cursor.getString(mimeIndex) ?: "",
                            size = cursor.getLong(sizeIndex),
                            modifiedAt = cursor.getLong(modifiedIndex),
                        )
                    )
                }
            }
        }.orEmpty()
    }

    private fun queryDocument(documentId: String): DocumentInfo? {
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        return resolver.query(documentUri, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            DocumentInfo(
                documentId = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)) ?: "",
                mimeType = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)) ?: "",
                size = cursor.getLong(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)),
                modifiedAt = cursor.getLong(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)),
            )
        }
    }

    private fun DocumentInfo.toFileEntry(parentPath: String): FileEntry {
        val path = listOf(parentPath.trim('/'), name)
            .filter { it.isNotEmpty() }
            .joinToString("/")
        return FileEntry(
            name = name,
            relativePath = path,
            isDirectory = isDirectory,
            size = if (isDirectory) 0L else size,
            modifiedAt = modifiedAt,
            extension = if (isDirectory) "" else name.substringAfterLast('.', ""),
        )
    }

    private data class DocumentInfo(
        val documentId: String,
        val name: String,
        val mimeType: String,
        val size: Long,
        val modifiedAt: Long,
    ) {
        val isDirectory: Boolean
            get() = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
    }
}
