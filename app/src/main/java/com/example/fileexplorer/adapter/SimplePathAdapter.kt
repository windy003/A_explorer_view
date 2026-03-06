package com.example.fileexplorer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fileexplorer.R

class SimplePathAdapter(
    private val onItemClick: (String) -> Unit,
    private val onDeleteClick: ((String) -> Unit)? = null
) : RecyclerView.Adapter<SimplePathAdapter.ViewHolder>() {

    private val items = mutableListOf<String>()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPath: TextView = view.findViewById(R.id.tvPath)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_path, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val path = items[position]
        holder.tvPath.text = path
        holder.itemView.setOnClickListener { onItemClick(path) }

        if (onDeleteClick != null) {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnDelete.setOnClickListener { onDeleteClick.invoke(path) }
        } else {
            holder.btnDelete.visibility = View.GONE
        }
    }

    override fun getItemCount() = items.size

    fun submitList(list: List<String>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun removeItem(path: String) {
        val index = items.indexOf(path)
        if (index >= 0) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}
