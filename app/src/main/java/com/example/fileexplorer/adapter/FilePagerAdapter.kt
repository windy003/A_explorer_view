package com.example.fileexplorer.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.fileexplorer.fragment.FileBrowserFragment

class FilePagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    private val fragments = Array(3) { index -> FileBrowserFragment.newInstance(index) }

    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment = fragments[position]

    fun getFragment(position: Int): FileBrowserFragment = fragments[position]
}
