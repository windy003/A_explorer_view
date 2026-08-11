package com.example.fileexplorer

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fileexplorer.adapter.RecycleAdapter
import com.example.fileexplorer.util.RecycleBinManager
import com.google.android.material.button.MaterialButton

class RecycleBinActivity : AppCompatActivity() {

    private lateinit var adapter: RecycleAdapter
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recycle_bin)

        RecycleBinManager.purgeExpired(this)

        tvEmpty = findViewById(R.id.tvEmpty)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = RecycleAdapter(
            onRestore = { entry ->
                val ok = RecycleBinManager.restore(this, entry)
                Toast.makeText(
                    this,
                    if (ok) "已恢复到 ${entry.originalPath}" else "恢复失败",
                    Toast.LENGTH_SHORT
                ).show()
                refresh()
            },
            onDeleteForever = { entry ->
                AlertDialog.Builder(this)
                    .setTitle("彻底删除")
                    .setMessage("彻底删除「${entry.originalName}」？此操作不可恢复。")
                    .setPositiveButton("删除") { _, _ ->
                        RecycleBinManager.deleteForever(this, entry)
                        refresh()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        )
        recyclerView.adapter = adapter

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnClearAll).setOnClickListener {
            if (RecycleBinManager.getEntries(this).isEmpty()) return@setOnClickListener
            AlertDialog.Builder(this)
                .setTitle("清空回收站")
                .setMessage("彻底删除回收站中的所有文件？此操作不可恢复。")
                .setPositiveButton("清空") { _, _ ->
                    RecycleBinManager.clearAll(this)
                    refresh()
                }
                .setNegativeButton("取消", null)
                .show()
        }

        refresh()
    }

    private fun refresh() {
        val entries = RecycleBinManager.getEntries(this)
        adapter.submitList(entries)
        tvEmpty.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
    }
}
