package com.example.fileexplorer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.widget.ImageView
import java.io.File
import java.util.concurrent.Executors

object ThumbnailLoader {

    private val executor = Executors.newFixedThreadPool(3)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cache = LruCache<String, Bitmap>(maxMemory / 8)

    private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp")

    fun isImage(file: File): Boolean = file.extension.lowercase() in imageExtensions

    fun isApk(file: File): Boolean = file.extension.lowercase() == "apk"

    /**
     * 异步加载缩略图到 ImageView。
     * 用 file.absolutePath 作为 tag 防止图片错位。
     */
    fun load(context: Context, file: File, imageView: ImageView, placeholderRes: Int) {
        val key = file.absolutePath
        val cached = cache.get(key)
        if (cached != null) {
            imageView.setImageBitmap(cached)
            return
        }

        // 打标记，防止 RecyclerView 复用时图片错位
        imageView.tag = key
        imageView.setImageResource(placeholderRes)

        val appContext = context.applicationContext
        executor.execute {
            val bitmap = try {
                when {
                    isImage(file) -> loadImageThumbnail(file)
                    isApk(file) -> loadApkIcon(appContext, file)
                    else -> null
                }
            } catch (e: Exception) {
                null
            }

            if (bitmap != null) {
                cache.put(key, bitmap)
            }

            mainHandler.post {
                if (imageView.tag == key && bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                }
            }
        }
    }

    private fun loadImageThumbnail(file: File): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null

        options.inSampleSize = calculateInSampleSize(options, 128, 128)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    private fun loadApkIcon(context: Context, file: File): Bitmap? {
        val pm = context.packageManager
        val pkgInfo = pm.getPackageArchiveInfo(file.absolutePath, 0) ?: return null
        pkgInfo.applicationInfo?.apply {
            sourceDir = file.absolutePath
            publicSourceDir = file.absolutePath
        } ?: return null

        val drawable = pkgInfo.applicationInfo.loadIcon(pm)
        if (drawable is BitmapDrawable) return drawable.bitmap

        val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 128
        val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 128
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, w, h)
        drawable.draw(canvas)
        return bitmap
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqW: Int, reqH: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqH || width > reqW) {
            val halfH = height / 2
            val halfW = width / 2
            while (halfH / inSampleSize >= reqH && halfW / inSampleSize >= reqW) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun clearCache() = cache.evictAll()
}
