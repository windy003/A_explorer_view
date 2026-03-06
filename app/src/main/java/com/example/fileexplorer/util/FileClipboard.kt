package com.example.fileexplorer.util

import java.io.File

object FileClipboard {

    enum class Operation { COPY, CUT }

    var files: List<File> = emptyList()
        private set
    var operation: Operation = Operation.COPY
        private set

    val hasContent: Boolean get() = files.isNotEmpty()

    fun copy(files: List<File>) {
        this.files = files.toList()
        this.operation = Operation.COPY
    }

    fun cut(files: List<File>) {
        this.files = files.toList()
        this.operation = Operation.CUT
    }

    fun clear() {
        files = emptyList()
    }

    /**
     * 粘贴到目标目录，返回成功/失败结果信息
     */
    fun pasteInto(destDir: File): String {
        if (files.isEmpty()) return "剪贴板为空"
        var successCount = 0
        var failCount = 0
        val isCut = operation == Operation.CUT

        for (src in files) {
            val dest = File(destDir, src.name)
            try {
                if (src.isDirectory) {
                    copyDirectory(src, dest)
                } else {
                    src.copyTo(dest, overwrite = true)
                }
                if (isCut) src.deleteRecursively()
                successCount++
            } catch (e: Exception) {
                failCount++
            }
        }

        if (isCut) clear()

        return if (failCount == 0) "已粘贴 $successCount 个文件"
        else "粘贴完成：成功 $successCount，失败 $failCount"
    }

    private fun copyDirectory(src: File, dest: File) {
        dest.mkdirs()
        src.listFiles()?.forEach { child ->
            val destChild = File(dest, child.name)
            if (child.isDirectory) copyDirectory(child, destChild)
            else child.copyTo(destChild, overwrite = true)
        }
    }
}
