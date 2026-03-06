package com.example.fileexplorer.provider

import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import com.example.fileexplorer.R
import java.io.File

class FileDocumentsProvider : DocumentsProvider() {

    companion object {
        private val ROOT_PROJECTION = arrayOf(
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_MIME_TYPES,
            Root.COLUMN_FLAGS,
            Root.COLUMN_ICON,
            Root.COLUMN_TITLE,
            Root.COLUMN_SUMMARY,
            Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_AVAILABLE_BYTES
        )
        private val DOCUMENT_PROJECTION = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_FLAGS,
            Document.COLUMN_SIZE
        )

        private const val ROOT_ID = "root"
    }

    private val rootDir: File get() = Environment.getExternalStorageDirectory()

    override fun onCreate(): Boolean = true

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val result = MatrixCursor(projection ?: ROOT_PROJECTION)
        val root = rootDir
        result.newRow().apply {
            add(Root.COLUMN_ROOT_ID, ROOT_ID)
            add(Root.COLUMN_DOCUMENT_ID, docId(root))
            add(Root.COLUMN_TITLE, context?.getString(R.string.app_name) ?: "文件浏览器")
            add(Root.COLUMN_SUMMARY, root.absolutePath)
            add(Root.COLUMN_FLAGS,
                Root.FLAG_LOCAL_ONLY or
                Root.FLAG_SUPPORTS_CREATE or
                Root.FLAG_SUPPORTS_RECENTS or
                Root.FLAG_SUPPORTS_SEARCH)
            add(Root.COLUMN_MIME_TYPES, "*/*")
            add(Root.COLUMN_ICON, R.drawable.ic_folder)
            add(Root.COLUMN_AVAILABLE_BYTES, runCatching { root.freeSpace }.getOrDefault(0L))
        }
        return result
    }

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        val result = MatrixCursor(projection ?: DOCUMENT_PROJECTION)
        includeFile(result, fileFor(documentId))
        return result
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val result = MatrixCursor(projection ?: DOCUMENT_PROJECTION)
        val parent = fileFor(parentDocumentId)
        parent.listFiles()
            ?.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
            ?.forEach { includeFile(result, it) }
        return result
    }

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        val file = fileFor(documentId)
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode))
    }

    override fun openDocumentThumbnail(
        documentId: String,
        sizeHint: Point?,
        signal: CancellationSignal?
    ): AssetFileDescriptor {
        val file = fileFor(documentId)
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return AssetFileDescriptor(pfd, 0, file.length())
    }

    override fun createDocument(parentDocumentId: String, mimeType: String, displayName: String): String {
        val parent = fileFor(parentDocumentId)
        val newFile = File(parent, displayName)
        if (mimeType == Document.MIME_TYPE_DIR) newFile.mkdirs()
        else newFile.createNewFile()
        return docId(newFile)
    }

    override fun deleteDocument(documentId: String) {
        fileFor(documentId).deleteRecursively()
    }

    override fun renameDocument(documentId: String, displayName: String): String {
        val file = fileFor(documentId)
        val renamed = File(file.parentFile, displayName)
        file.renameTo(renamed)
        return docId(renamed)
    }

    override fun querySearchDocuments(
        rootId: String,
        query: String,
        projection: Array<out String>?
    ): Cursor {
        val result = MatrixCursor(projection ?: DOCUMENT_PROJECTION)
        searchRecursive(rootDir, query.lowercase(), result, limit = 50)
        return result
    }

    // -------- 工具 --------

    private fun docId(file: File): String {
        val rootPath = rootDir.absolutePath
        val filePath = file.absolutePath
        return if (filePath == rootPath) ROOT_ID
        else "$ROOT_ID:${filePath.removePrefix("$rootPath/")}"
    }

    private fun fileFor(documentId: String): File {
        return if (documentId == ROOT_ID) rootDir
        else File(rootDir, documentId.removePrefix("$ROOT_ID:"))
    }

    private fun mimeFor(file: File): String {
        if (file.isDirectory) return Document.MIME_TYPE_DIR
        val ext = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }

    private fun includeFile(cursor: MatrixCursor, file: File) {
        val mime = mimeFor(file)
        var flags = 0
        if (file.isDirectory) flags = flags or Document.FLAG_DIR_SUPPORTS_CREATE
        if (file.canWrite()) {
            flags = flags or Document.FLAG_SUPPORTS_WRITE or
                    Document.FLAG_SUPPORTS_DELETE or
                    Document.FLAG_SUPPORTS_RENAME
        }
        val isImage = mime.startsWith("image/")
        if (isImage) flags = flags or Document.FLAG_SUPPORTS_THUMBNAIL

        cursor.newRow().apply {
            add(Document.COLUMN_DOCUMENT_ID, docId(file))
            add(Document.COLUMN_DISPLAY_NAME, file.name)
            add(Document.COLUMN_MIME_TYPE, mime)
            add(Document.COLUMN_SIZE, if (file.isDirectory) null else file.length())
            add(Document.COLUMN_LAST_MODIFIED, file.lastModified())
            add(Document.COLUMN_FLAGS, flags)
        }
    }

    private fun searchRecursive(dir: File, query: String, cursor: MatrixCursor, limit: Int, count: Int = 0): Int {
        var found = count
        if (found >= limit) return found
        dir.listFiles()?.forEach { file ->
            if (found >= limit) return found
            if (file.name.lowercase().contains(query)) {
                includeFile(cursor, file)
                found++
            }
            if (file.isDirectory) found = searchRecursive(file, query, cursor, limit, found)
        }
        return found
    }
}
