package com.example.fileexplorer.fragment

import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fileexplorer.R
import com.example.fileexplorer.adapter.FileListAdapter
import com.example.fileexplorer.model.FileItem
import com.example.fileexplorer.util.FileClipboard
import com.example.fileexplorer.util.PrefsManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class FileBrowserFragment : Fragment() {

    var onDirChanged: ((folderName: String) -> Unit)? = null

    private lateinit var adapter: FileListAdapter
    private lateinit var tvPath: TextView
    private lateinit var fabPaste: FloatingActionButton
    private lateinit var selectionBar: LinearLayout
    private lateinit var tvSelectedCount: TextView
    private lateinit var btnRename: MaterialButton

    private val dirStack = ArrayDeque<File>()
    private var currentDir: File = Environment.getExternalStorageDirectory()

    companion object {
        private const val ARG_INDEX = "tab_index"
        fun newInstance(index: Int) = FileBrowserFragment().apply {
            arguments = Bundle().also { it.putInt(ARG_INDEX, index) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_file_browser, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tvPath = view.findViewById(R.id.tvPath)
        fabPaste = view.findViewById(R.id.fabPaste)
        selectionBar = view.findViewById(R.id.selectionBar)
        tvSelectedCount = view.findViewById(R.id.tvSelectedCount)
        btnRename = view.findViewById(R.id.btnRename)

        view.findViewById<ImageButton>(R.id.btnUp).setOnClickListener { goBack() }
        view.findViewById<ImageButton>(R.id.btnHome).setOnClickListener {
            adapter.exitSelectionMode()
            hideSelectionBar()
            dirStack.clear()
            navigateTo(Environment.getExternalStorageDirectory())
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = FileListAdapter(
            context = requireContext(),
            onItemClick = { item ->
                if (item.isDirectory) navigateTo(item.file)
                else openFile(item)
            },
            onItemLongClick = { _ -> showSelectionBar() },
            onSelectionChanged = { count ->
                tvSelectedCount.text = "已选 $count 项"
                // 重命名只在选中恰好 1 个时可用
                btnRename.visibility = if (count == 1) View.VISIBLE else View.GONE
                if (count == 0 && adapter.isSelectionMode) {
                    adapter.exitSelectionMode()
                    hideSelectionBar()
                }
            }
        )
        recyclerView.adapter = adapter

        view.findViewById<View>(R.id.btnSelectAll).setOnClickListener { adapter.selectAll() }
        view.findViewById<View>(R.id.btnRename).setOnClickListener { doRename() }
        view.findViewById<View>(R.id.btnCompress).setOnClickListener { doCompress() }
        view.findViewById<View>(R.id.btnCopy).setOnClickListener { doCopy() }
        view.findViewById<View>(R.id.btnCut).setOnClickListener { doCut() }
        view.findViewById<View>(R.id.btnDelete).setOnClickListener { doDelete() }
        view.findViewById<View>(R.id.btnCancel).setOnClickListener {
            adapter.exitSelectionMode()
            hideSelectionBar()
        }

        fabPaste.setOnClickListener { doPaste() }

        view.findViewById<ImageButton>(R.id.btnAdd).setOnClickListener { btn ->
            PopupMenu(requireContext(), btn).apply {
                menu.add(0, 1, 0, "新建文件夹")
                menu.add(0, 2, 1, "新建文件")
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        1 -> showCreateDialog(isFolder = true)
                        2 -> showCreateDialog(isFolder = false)
                    }
                    true
                }
                show()
            }
        }

        navigateTo(currentDir)
    }

    override fun onResume() {
        super.onResume()
        updatePasteFab()
    }

    fun navigateTo(dir: File) {
        if (!dir.exists() || !dir.isDirectory) {
            Toast.makeText(context, "目录不存在", Toast.LENGTH_SHORT).show()
            return
        }
        if (::adapter.isInitialized && adapter.isSelectionMode) {
            adapter.exitSelectionMode()
            hideSelectionBar()
        }
        dirStack.addLast(currentDir)
        currentDir = dir
        loadFiles()
        PrefsManager.addRecent(dir.absolutePath)
        updatePasteFab()
    }

    private fun loadFiles() {
        tvPath.text = currentDir.absolutePath
        onDirChanged?.invoke(currentDir.name)
        val files = currentDir.listFiles()
            ?.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
            ?.map { FileItem(it) }
            ?: emptyList()
        adapter.submitList(files)
    }

    fun canGoBack(): Boolean = adapter.isSelectionMode || dirStack.size > 1

    fun goBack() {
        if (adapter.isSelectionMode) {
            adapter.exitSelectionMode()
            hideSelectionBar()
            return
        }
        if (dirStack.size > 1) {
            currentDir = dirStack.removeLast()
            loadFiles()
            updatePasteFab()
        }
    }

    fun getCurrentPath(): String = currentDir.absolutePath

    private fun showSelectionBar() {
        selectionBar.visibility = View.VISIBLE
        fabPaste.visibility = View.GONE
    }

    private fun hideSelectionBar() {
        selectionBar.visibility = View.GONE
        updatePasteFab()
    }

    private fun updatePasteFab() {
        if (!::fabPaste.isInitialized) return
        if (selectionBar.visibility == View.VISIBLE) return
        fabPaste.visibility = if (FileClipboard.hasContent) View.VISIBLE else View.GONE
    }

    // -------- 重命名 --------

    private fun doRename() {
        val selected = adapter.getSelectedItems().firstOrNull() ?: return
        val editText = EditText(requireContext()).apply {
            setText(selected.name)
            selectAll()
            setSingleLine()
        }
        val container = LinearLayout(requireContext()).apply {
            setPadding(48, 16, 48, 0)
            addView(editText)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("重命名")
            .setView(container)
            .setPositiveButton("确定") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isEmpty()) {
                    Toast.makeText(context, "名称不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (newName == selected.name) return@setPositiveButton
                val dest = File(currentDir, newName)
                if (dest.exists()) {
                    Toast.makeText(context, "已存在同名文件", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (selected.file.renameTo(dest)) {
                    adapter.exitSelectionMode()
                    hideSelectionBar()
                    loadFiles()
                    Toast.makeText(context, "重命名成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "重命名失败", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
        editText.requestFocus()
    }

    // -------- 压缩 --------

    private fun doCompress() {
        val selected = adapter.getSelectedItems()
        if (selected.isEmpty()) return

        val defaultName = if (selected.size == 1) selected[0].name else "archive"
        val editText = EditText(requireContext()).apply {
            setText("$defaultName.zip")
            selectAll()
            setSingleLine()
        }
        val container = LinearLayout(requireContext()).apply {
            setPadding(48, 16, 48, 0)
            addView(editText)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("压缩为 ZIP")
            .setView(container)
            .setPositiveButton("压缩") { _, _ ->
                var zipName = editText.text.toString().trim()
                if (zipName.isEmpty()) zipName = "$defaultName.zip"
                if (!zipName.endsWith(".zip", ignoreCase = true)) zipName += ".zip"
                val destZip = File(currentDir, zipName)
                if (destZip.exists()) {
                    Toast.makeText(context, "同名压缩文件已存在", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                runZipAsync(selected.map { it.file }, destZip)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    @Suppress("DEPRECATION")
    private fun runZipAsync(files: List<File>, destZip: File) {
        val progress = ProgressDialog(requireContext()).apply {
            setMessage("正在压缩...")
            setCancelable(false)
            show()
        }
        Thread {
            val error = runCatching {
                ZipOutputStream(BufferedOutputStream(FileOutputStream(destZip))).use { zos ->
                    files.forEach { file ->
                        if (file.isDirectory) addDirToZip(zos, file, file.name)
                        else addFileToZip(zos, file, file.name)
                    }
                }
            }.exceptionOrNull()
            activity?.runOnUiThread {
                progress.dismiss()
                if (error == null) {
                    adapter.exitSelectionMode()
                    hideSelectionBar()
                    loadFiles()
                    Toast.makeText(context, "压缩完成：${destZip.name}", Toast.LENGTH_SHORT).show()
                } else {
                    destZip.delete()
                    Toast.makeText(context, "压缩失败：${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun addFileToZip(zos: ZipOutputStream, file: File, entryName: String) {
        zos.putNextEntry(ZipEntry(entryName))
        FileInputStream(file).use { it.copyTo(zos) }
        zos.closeEntry()
    }

    private fun addDirToZip(zos: ZipOutputStream, dir: File, entryName: String) {
        zos.putNextEntry(ZipEntry("$entryName/"))
        zos.closeEntry()
        dir.listFiles()?.forEach { child ->
            val childEntry = "$entryName/${child.name}"
            if (child.isDirectory) addDirToZip(zos, child, childEntry)
            else addFileToZip(zos, child, childEntry)
        }
    }

    // -------- 解压 --------

    private fun openFile(item: FileItem) {
        if (item.name.endsWith(".zip", ignoreCase = true)) {
            AlertDialog.Builder(requireContext())
                .setTitle(item.name)
                .setItems(arrayOf("解压到此处", "解压到同名文件夹")) { _, which ->
                    val destDir = when (which) {
                        0 -> currentDir
                        else -> File(currentDir, item.file.nameWithoutExtension)
                    }
                    runExtractAsync(item.file, destDir)
                }
                .setNegativeButton("取消", null)
                .show()
            return
        }

        val mime = getMimeType(item.name)
        if (mime == null) {
            Toast.makeText(context, "不支持的文件类型", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                item.file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            // APK 安装不能用 createChooser，否则 URI 权限无法传递给包安装程序
            if (mime == "application/vnd.android.package-archive") {
                startActivity(intent)
            } else {
                startActivity(Intent.createChooser(intent, "选择应用打开"))
            }
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "未找到可以打开此文件的应用", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getMimeType(fileName: String): String? {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            // 图片
            "jpg", "jpeg" -> "image/jpeg"
            "png"         -> "image/png"
            "gif"         -> "image/gif"
            "bmp"         -> "image/bmp"
            "webp"        -> "image/webp"
            "svg"         -> "image/svg+xml"
            // 视频
            "mp4"         -> "video/mp4"
            "mkv"         -> "video/x-matroska"
            "avi"         -> "video/x-msvideo"
            "mov"         -> "video/quicktime"
            "wmv"         -> "video/x-ms-wmv"
            "flv"         -> "video/x-flv"
            "3gp"         -> "video/3gpp"
            "webm"        -> "video/webm"
            // 音频
            "mp3"         -> "audio/mpeg"
            "wav"         -> "audio/x-wav"
            "flac"        -> "audio/flac"
            "aac"         -> "audio/aac"
            "ogg"         -> "audio/ogg"
            "m4a"         -> "audio/mp4"
            "wma"         -> "audio/x-ms-wma"
            "opus"        -> "audio/opus"
            // 文本
            "txt", "log", "md", "csv"          -> "text/plain"
            "html", "htm"                       -> "text/html"
            "xml"                               -> "text/xml"
            "json"                              -> "application/json"
            "js"                                -> "text/javascript"
            "css"                               -> "text/css"
            "java", "kt", "py", "c", "cpp", "h" -> "text/plain"
            // PDF
            "pdf"         -> "application/pdf"
            // Office
            "doc"         -> "application/msword"
            "docx"        -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls"         -> "application/vnd.ms-excel"
            "xlsx"        -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt"         -> "application/vnd.ms-powerpoint"
            "pptx"        -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            // APK
            "apk"         -> "application/vnd.android.package-archive"
            else          -> null
        }
    }

    @Suppress("DEPRECATION")
    private fun runExtractAsync(zipFile: File, destDir: File) {
        val progress = ProgressDialog(requireContext()).apply {
            setMessage("正在解压...")
            setCancelable(false)
            show()
        }
        Thread {
            val error = runCatching {
                destDir.mkdirs()
                ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val dest = File(destDir, entry.name)
                        if (entry.isDirectory) {
                            dest.mkdirs()
                        } else {
                            dest.parentFile?.mkdirs()
                            BufferedOutputStream(FileOutputStream(dest)).use { zis.copyTo(it) }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }.exceptionOrNull()
            activity?.runOnUiThread {
                progress.dismiss()
                if (error == null) {
                    loadFiles()
                    Toast.makeText(context, "解压完成", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "解压失败：${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    // -------- 其他操作 --------

    private fun doCopy() {
        val selected = adapter.getSelectedItems()
        if (selected.isEmpty()) return
        FileClipboard.copy(selected.map { it.file })
        adapter.exitSelectionMode()
        hideSelectionBar()
        Toast.makeText(context, "已复制 ${selected.size} 个文件", Toast.LENGTH_SHORT).show()
    }

    private fun doCut() {
        val selected = adapter.getSelectedItems()
        if (selected.isEmpty()) return
        FileClipboard.cut(selected.map { it.file })
        adapter.exitSelectionMode()
        hideSelectionBar()
        Toast.makeText(context, "已剪切 ${selected.size} 个文件", Toast.LENGTH_SHORT).show()
    }

    private fun doPaste() {
        val msg = FileClipboard.pasteInto(currentDir)
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        loadFiles()
        updatePasteFab()
    }

    private fun doDelete() {
        val selected = adapter.getSelectedItems()
        if (selected.isEmpty()) return
        AlertDialog.Builder(requireContext())
            .setTitle("确认删除")
            .setMessage("删除 ${selected.size} 个文件？此操作不可恢复。")
            .setPositiveButton("删除") { _, _ ->
                var ok = 0; var fail = 0
                selected.forEach { item ->
                    if (item.file.deleteRecursively()) ok++ else fail++
                }
                Toast.makeText(context, "已删除 $ok 个，失败 $fail 个", Toast.LENGTH_SHORT).show()
                adapter.exitSelectionMode()
                hideSelectionBar()
                loadFiles()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showCreateDialog(isFolder: Boolean) {
        val title = if (isFolder) "新建文件夹" else "新建文件"
        val editText = EditText(requireContext()).apply {
            hint = if (isFolder) "文件夹名称" else "文件名称"
            setSingleLine()
        }
        val container = LinearLayout(requireContext()).apply {
            setPadding(48, 16, 48, 0)
            addView(editText)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(container)
            .setPositiveButton("创建") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(context, "名称不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val target = File(currentDir, name)
                if (target.exists()) {
                    Toast.makeText(context, "已存在同名文件", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val success = if (isFolder) target.mkdirs()
                             else runCatching { target.createNewFile() }.getOrDefault(false)
                if (success) {
                    loadFiles()
                    Toast.makeText(context, "$title 成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "创建失败，请检查权限", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
        editText.requestFocus()
    }
}
