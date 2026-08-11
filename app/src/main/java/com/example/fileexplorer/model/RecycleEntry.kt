package com.example.fileexplorer.model

data class RecycleEntry(
    val recycleName: String,
    val originalPath: String,
    val deletedTime: Long,
    val isDirectory: Boolean
) {
    val originalName: String get() = originalPath.substringAfterLast('/', originalPath)
}
