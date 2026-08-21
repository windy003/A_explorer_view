package com.example.fileexplorer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fileexplorer.R
import com.example.fileexplorer.model.RecycleEntry
import com.example.fileexplorer.util.FileNameDisplay
import com.example.fileexplorer.util.RecycleBinManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class RecycleAdapter(
    private val onRestore: (RecycleEntry) -> Unit,
    private val onDeleteForever: (RecycleEntry) -> Unit
) : RecyclerView.Adapter<RecycleAdapter.ViewHolder>() {

    private val items = mutableListOf<RecycleEntry>()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvPath: TextView = view.findViewById(R.id.tvPath)
        val tvRemaining: TextView = view.findViewById(R.id.tvRemaining)
        val btnRestore: ImageButton = view.findViewById(R.id.btnRestore)
        val btnDeleteForever: ImageButton = view.findViewById(R.id.btnDeleteForever)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recycle, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = items[position]
        FileNameDisplay.set(holder.tvName, entry.originalName)
        holder.tvPath.text = entry.originalPath

        val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        val elapsed = System.currentTimeMillis() - entry.deletedTime
        val remainMillis = (RecycleBinManager.RETENTION_MILLIS - elapsed).coerceAtLeast(0)
        val remainDays = TimeUnit.MILLISECONDS.toHours(remainMillis) / 24 + 1
        holder.tvRemaining.text = "删除于 ${sdf.format(Date(entry.deletedTime))}，$remainDays 天后自动清除"

        holder.btnRestore.setOnClickListener { onRestore(entry) }
        holder.btnDeleteForever.setOnClickListener { onDeleteForever(entry) }
    }

    override fun getItemCount() = items.size

    fun submitList(list: List<RecycleEntry>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }
}
