package com.example.fileexplorer.model

import java.io.File

data class FileItem(
    val file: File,
    var isSelected: Boolean = false
) {
    val name: String get() = file.name
    val isDirectory: Boolean get() = file.isDirectory
    val lastModified: Long get() = file.lastModified()
    val length: Long get() = file.length()
    val path: String get() = file.absolutePath
}
