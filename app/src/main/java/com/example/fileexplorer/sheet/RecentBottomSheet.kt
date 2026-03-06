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

class RecentBottomSheet(
    private val onPathSelected: (String) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.tvTitle).text = "最近访问"

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val adapter = SimplePathAdapter(onItemClick = { path ->
            onPathSelected(path)
            dismiss()
        })
        recyclerView.adapter = adapter

        val recent = PrefsManager.getRecent()
        if (recent.isEmpty()) {
            view.findViewById<TextView>(R.id.tvEmpty).visibility = View.VISIBLE
        } else {
            view.findViewById<TextView>(R.id.tvEmpty).visibility = View.GONE
            adapter.submitList(recent)
        }
    }
}
