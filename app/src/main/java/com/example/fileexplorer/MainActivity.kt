package com.example.fileexplorer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.text.format.Formatter
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.fileexplorer.adapter.FilePagerAdapter
import com.example.fileexplorer.sheet.FavoritesBottomSheet
import com.example.fileexplorer.sheet.RecentBottomSheet
import com.example.fileexplorer.util.PrefsManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var pagerAdapter: FilePagerAdapter
    private lateinit var tvStorageInfo: TextView
    private lateinit var pbStorage: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PrefsManager.init(this)

        tvStorageInfo = findViewById(R.id.tvStorageInfo)
        pbStorage = findViewById(R.id.pbStorage)
        updateStorageInfo()

        // 工具栏按钮
        findViewById<MaterialButton>(R.id.btnFavorites).setOnClickListener {
            FavoritesBottomSheet { path ->
                getCurrentFragment()?.navigateTo(File(path))
            }.show(supportFragmentManager, "favorites")
        }

        findViewById<MaterialButton>(R.id.btnAddFavorite).setOnClickListener {
            val path = getCurrentFragment()?.getCurrentPath() ?: return@setOnClickListener
            PrefsManager.addFavorite(path)
            Toast.makeText(this, "已添加到收藏夹", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btnRecent).setOnClickListener {
            RecentBottomSheet { path ->
                getCurrentFragment()?.navigateTo(File(path))
            }.show(supportFragmentManager, "recent")
        }

        // ViewPager2 + TabLayout
        viewPager = findViewById(R.id.viewPager)
        pagerAdapter = FilePagerAdapter(this)
        viewPager.adapter = pagerAdapter
        viewPager.offscreenPageLimit = 2  // 保持所有 tab 存活

        tabLayout = findViewById(R.id.tabLayout)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = "标签 ${position + 1}"
        }.attach()

        // 每个 fragment 目录变化时更新对应 tab 标题
        repeat(3) { index ->
            pagerAdapter.getFragment(index).onDirChanged = { folderName ->
                tabLayout.getTabAt(index)?.text = folderName
            }
        }

        checkPermissions()
    }

    private fun updateStorageInfo() {
        val path = Environment.getExternalStorageDirectory()
        val stat = StatFs(path.absolutePath)
        val total = stat.totalBytes
        val free = stat.availableBytes
        val used = total - free
        val usedStr = Formatter.formatFileSize(this, used)
        val totalStr = Formatter.formatFileSize(this, total)
        tvStorageInfo.text = "$usedStr / $totalStr"
        pbStorage.progress = if (total > 0) ((used * 100) / total).toInt() else 0
    }

    override fun onResume() {
        super.onResume()
        updateStorageInfo()
    }

    private fun getCurrentFragment() =
        pagerAdapter.getFragment(viewPager.currentItem)

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val fragment = getCurrentFragment()
        if (fragment.canGoBack()) {
            fragment.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        } else {
            val readPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            val writePerm = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (readPerm != PackageManager.PERMISSION_GRANTED || writePerm != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1
                )
            }
        }
    }
}
