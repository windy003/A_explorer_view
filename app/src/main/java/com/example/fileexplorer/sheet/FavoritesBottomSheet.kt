package com.example.fileexplorer.sheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fileexplorer.R
import com.example.fileexplorer.adapter.SimplePathAdapter
import com.example.fileexplorer.util.PrefsManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FavoritesBottomSheet(
    private val onPathSelected: (String) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var adapter: SimplePathAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.tvTitle).text = "收藏夹"

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = SimplePathAdapter(
            onItemClick = { path ->
                onPathSelected(path)
                dismiss()
            },
            onDeleteClick = { path ->
                PrefsManager.removeFavorite(path)
                adapter.removeItem(path)
            }
        )
        recyclerView.adapter = adapter

        val favorites = PrefsManager.getFavorites()
        if (favorites.isEmpty()) {
            view.findViewById<TextView>(R.id.tvEmpty).visibility = View.VISIBLE
        } else {
            view.findViewById<TextView>(R.id.tvEmpty).visibility = View.GONE
            adapter.submitList(favorites)
        }
    }
}
