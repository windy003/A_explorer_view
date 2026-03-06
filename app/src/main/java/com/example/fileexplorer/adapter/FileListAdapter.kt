package com.example.fileexplorer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fileexplorer.R
import com.example.fileexplorer.model.FileItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileListAdapter(
    private val onItemClick: (FileItem) -> Unit,
    private val onItemLongClick: (FileItem) -> Unit,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<FileListAdapter.FileViewHolder>() {

    private val items = mutableListOf<FileItem>()
    var isSelectionMode = false
        private set

    inner class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.ivIcon)
        val name: TextView = view.findViewById(R.id.tvName)
        val info: TextView = view.findViewById(R.id.tvInfo)
        val checkbox: CheckBox = view.findViewById(R.id.checkbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val item = items[position]

        holder.name.text = item.name
        holder.icon.setImageResource(
            if (item.isDirectory) R.drawable.ic_folder else R.drawable.ic_file
        )

        val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        holder.info.text = if (item.isDirectory) {
            sdf.format(Date(item.lastModified))
        } else {
            "${formatSize(item.length)}  ${sdf.format(Date(item.lastModified))}"
        }

        // 选择模式
        if (isSelectionMode) {
            holder.checkbox.visibility = View.VISIBLE
            holder.checkbox.isChecked = item.isSelected
        } else {
            holder.checkbox.visibility = View.GONE
            item.isSelected = false
        }

        holder.itemView.isActivated = item.isSelected

        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                item.isSelected = !item.isSelected
                holder.checkbox.isChecked = item.isSelected
                holder.itemView.isActivated = item.isSelected
                onSelectionChanged(getSelectedItems().size)
            } else {
                onItemClick(item)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (!isSelectionMode) {
                startSelectionMode()
                item.isSelected = true
                notifyItemChanged(position)
                onSelectionChanged(1)
                onItemLongClick(item)
            }
            true
        }

        holder.checkbox.setOnClickListener {
            item.isSelected = holder.checkbox.isChecked
            holder.itemView.isActivated = item.isSelected
            onSelectionChanged(getSelectedItems().size)
        }
    }

    override fun getItemCount() = items.size

    fun submitList(newItems: List<FileItem>) {
        items.clear()
        items.addAll(newItems)
        isSelectionMode = false
        notifyDataSetChanged()
    }

    fun startSelectionMode() {
        isSelectionMode = true
        notifyDataSetChanged()
    }

    fun exitSelectionMode() {
        isSelectionMode = false
        items.forEach { it.isSelected = false }
        notifyDataSetChanged()
    }

    fun getSelectedItems(): List<FileItem> = items.filter { it.isSelected }

    fun selectAll() {
        items.forEach { it.isSelected = true }
        notifyDataSetChanged()
        onSelectionChanged(items.size)
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
}
