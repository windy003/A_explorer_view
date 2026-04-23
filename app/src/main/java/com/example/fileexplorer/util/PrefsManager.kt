package com.example.fileexplorer.util

import android.content.Context
import android.content.SharedPreferences

object PrefsManager {

    private const val PREFS_NAME = "file_explorer_prefs"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_RECENT = "recent"
    private const val MAX_RECENT = 30

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ---- 收藏夹 ----

    fun getFavorites(): List<String> {
        val raw = prefs.getString(KEY_FAVORITES, "") ?: ""
        return if (raw.isEmpty()) emptyList() else raw.split("\n").filter { it.isNotBlank() }
    }

    fun addFavorite(path: String) {
        val list = getFavorites().toMutableList()
        if (!list.contains(path)) {
            list.add(0, path)
            prefs.edit().putString(KEY_FAVORITES, list.joinToString("\n")).apply()
        }
    }

    fun removeFavorite(path: String) {
        val list = getFavorites().toMutableList()
        list.remove(path)
        prefs.edit().putString(KEY_FAVORITES, list.joinToString("\n")).apply()
    }

    fun isFavorite(path: String): Boolean = getFavorites().contains(path)

    // ---- 最近访问 ----

    fun getRecent(): List<String> {
        val raw = prefs.getString(KEY_RECENT, "") ?: ""
        return if (raw.isEmpty()) emptyList() else raw.split("\n").filter { it.isNotBlank() }
    }

    fun addRecent(path: String) {
        val list = getRecent().toMutableList()
        list.remove(path)
        list.add(0, path)
        if (list.size > MAX_RECENT) list.subList(MAX_RECENT, list.size).clear()
        prefs.edit().putString(KEY_RECENT, list.joinToString("\n")).apply()
    }

    // ---- 标签页状态记忆 ----

    private const val KEY_LAST_TAB = "last_tab"
    private const val KEY_TAB_PATH_PREFIX = "tab_path_"

    fun saveLastTab(index: Int) {
        prefs.edit().putInt(KEY_LAST_TAB, index).apply()
    }

    fun getLastTab(): Int = prefs.getInt(KEY_LAST_TAB, 0)

    fun saveTabPath(tabIndex: Int, path: String) {
        prefs.edit().putString(KEY_TAB_PATH_PREFIX + tabIndex, path).apply()
    }

    fun getTabPath(tabIndex: Int): String? =
        prefs.getString(KEY_TAB_PATH_PREFIX + tabIndex, null)
}
