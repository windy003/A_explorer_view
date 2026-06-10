package com.example.fileexplorer

import android.app.ProgressDialog
import android.os.Bundle
import android.text.method.KeyListener
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.io.File

class TextEditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PATH = "file_path"
        private const val MAX_SIZE = 2 * 1024 * 1024L  // 超过 2MB 不在内置编辑器打开
    }

    private lateinit var etContent: EditText
    private lateinit var tvFileName: TextView
    private lateinit var btnEditSave: MaterialButton
    private lateinit var file: File

    private var editKeyListener: KeyListener? = null
    private var isEditing = false
    private var originalText = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_editor)

        val path = intent.getStringExtra(EXTRA_PATH)
        file = if (path != null) File(path) else File("")
        if (path == null || !file.exists() || !file.isFile) {
            Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        if (file.length() > MAX_SIZE) {
            Toast.makeText(this, "文件过大，无法在内置编辑器中打开", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        etContent = findViewById(R.id.etContent)
        tvFileName = findViewById(R.id.tvFileName)
        btnEditSave = findViewById(R.id.btnEditSave)

        // 通过 configChanges 处理旋转，避免系统对大文本做状态保存
        etContent.isSaveEnabled = false
        editKeyListener = etContent.keyListener
        tvFileName.text = file.name
        setEditing(false)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { onBackPressed() }
        btnEditSave.setOnClickListener {
            if (isEditing) saveFile() else setEditing(true)
        }

        loadFile()
    }

    private fun setEditing(editing: Boolean) {
        isEditing = editing
        if (editing) {
            etContent.keyListener = editKeyListener
            etContent.isCursorVisible = true
            etContent.requestFocus()
            btnEditSave.text = "保存"
        } else {
            // 置空 keyListener 进入只读模式，仍可滚动和选择复制
            etContent.keyListener = null
            etContent.isCursorVisible = false
            btnEditSave.text = "编辑"
        }
    }

    private fun isModified() = etContent.text.toString() != originalText

    @Suppress("DEPRECATION")
    private fun loadFile() {
        val progress = ProgressDialog(this).apply {
            setMessage("正在加载...")
            setCancelable(false)
            show()
        }
        Thread {
            val result = runCatching { file.readText() }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                progress.dismiss()
                result.fold(
                    onSuccess = { text ->
                        originalText = text
                        etContent.setText(text)
                    },
                    onFailure = { e ->
                        Toast.makeText(this, "读取失败：${e.message}", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                )
            }
        }.start()
    }

    @Suppress("DEPRECATION")
    private fun saveFile(finishAfter: Boolean = false) {
        val content = etContent.text.toString()
        val progress = ProgressDialog(this).apply {
            setMessage("正在保存...")
            setCancelable(false)
            show()
        }
        Thread {
            val error = runCatching { file.writeText(content) }.exceptionOrNull()
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                progress.dismiss()
                if (error == null) {
                    originalText = content
                    Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
                    if (finishAfter) finish() else setEditing(false)
                } else {
                    Toast.makeText(this, "保存失败：${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isEditing && isModified()) {
            AlertDialog.Builder(this)
                .setTitle("未保存的修改")
                .setMessage("是否保存对 ${file.name} 的修改？")
                .setPositiveButton("保存") { _, _ -> saveFile(finishAfter = true) }
                .setNegativeButton("不保存") { _, _ -> finish() }
                .setNeutralButton("取消", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }
}
