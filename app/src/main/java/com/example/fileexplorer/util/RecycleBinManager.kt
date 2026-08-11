package com.example.fileexplorer.util

import android.content.Context
import com.example.fileexplorer.model.RecycleEntry
import java.io.File

object RecycleBinManager {

    /** 回收站保留时长：3 天 */
    const val RETENTION_MILLIS = 3 * 24 * 60 * 60 * 1000L

    private const val FIELD_SEP = ""

    fun getRecycleBinDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), ".recycle_bin")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** 将文件移入回收站，并记录原始路径与删除时间 */
    fun moveToRecycleBin(context: Context, file: File): Boolean {
        val binDir = getRecycleBinDir(context)
        var dest = File(binDir, "${System.currentTimeMillis()}_${file.name}")
        var counter = 1
        while (dest.exists()) {
            dest = File(binDir, "${System.currentTimeMillis()}_${counter}_${file.name}")
            counter++
        }

        val moved = runCatching {
            if (!file.renameTo(dest)) {
                copyRecursively(file, dest)
                file.deleteRecursively()
            }
            true
        }.getOrDefault(false)

        if (moved) {
            val entry = RecycleEntry(dest.name, file.absolutePath, System.currentTimeMillis(), dest.isDirectory)
            val list = getEntries(context).toMutableList()
            list.add(0, entry)
            saveEntries(list)
        }
        return moved
    }

    fun getEntries(context: Context): List<RecycleEntry> =
        PrefsManager.getRecycleRecords().mapNotNull { decode(it) }

    private fun saveEntries(list: List<RecycleEntry>) {
        PrefsManager.saveRecycleRecords(list.map { encode(it) })
    }

    /** 恢复到原始路径；若原路径已被占用则自动重命名 */
    fun restore(context: Context, entry: RecycleEntry): Boolean {
        val src = File(getRecycleBinDir(context), entry.recycleName)
        if (!src.exists()) {
            removeEntry(entry)
            return false
        }

        var dest = File(entry.originalPath)
        dest.parentFile?.mkdirs()
        if (dest.exists()) {
            dest = generateUniqueName(dest)
        }

        val ok = runCatching {
            if (!src.renameTo(dest)) {
                copyRecursively(src, dest)
                src.deleteRecursively()
            }
            true
        }.getOrDefault(false)

        if (ok) removeEntry(entry)
        return ok
    }

    fun deleteForever(context: Context, entry: RecycleEntry): Boolean {
        val target = File(getRecycleBinDir(context), entry.recycleName)
        val ok = !target.exists() || target.deleteRecursively()
        if (ok) removeEntry(entry)
        return ok
    }

    fun clearAll(context: Context) {
        getRecycleBinDir(context).listFiles()?.forEach { it.deleteRecursively() }
        saveEntries(emptyList())
    }

    /** 清除超过保留期限的回收站文件 */
    fun purgeExpired(context: Context) {
        val now = System.currentTimeMillis()
        val entries = getEntries(context)
        val expired = entries.filter { now - it.deletedTime >= RETENTION_MILLIS }
        if (expired.isEmpty()) return

        val binDir = getRecycleBinDir(context)
        expired.forEach { File(binDir, it.recycleName).deleteRecursively() }
        saveEntries(entries - expired.toSet())
    }

    private fun removeEntry(entry: RecycleEntry) {
        val list = PrefsManager.getRecycleRecords().mapNotNull { decode(it) }.toMutableList()
        list.removeAll { it.recycleName == entry.recycleName }
        saveEntries(list)
    }

    private fun generateUniqueName(dest: File): File {
        val nameWithoutExt = dest.nameWithoutExtension
        val ext = if (dest.extension.isNotEmpty()) ".${dest.extension}" else ""
        var counter = 1
        var candidate: File
        do {
            candidate = File(dest.parentFile, "$nameWithoutExt ($counter)$ext")
            counter++
        } while (candidate.exists())
        return candidate
    }

    private fun copyRecursively(src: File, dest: File) {
        if (src.isDirectory) {
            dest.mkdirs()
            src.listFiles()?.forEach { child -> copyRecursively(child, File(dest, child.name)) }
        } else {
            src.copyTo(dest, overwrite = true)
        }
    }

    private fun encode(entry: RecycleEntry): String = listOf(
        entry.recycleName, entry.originalPath, entry.deletedTime.toString(), entry.isDirectory.toString()
    ).joinToString(FIELD_SEP)

    private fun decode(raw: String): RecycleEntry? {
        val parts = raw.split(FIELD_SEP)
        if (parts.size != 4) return null
        val deletedTime = parts[2].toLongOrNull() ?: return null
        return RecycleEntry(
            recycleName = parts[0],
            originalPath = parts[1],
            deletedTime = deletedTime,
            isDirectory = parts[3].toBoolean()
        )
    }
}
