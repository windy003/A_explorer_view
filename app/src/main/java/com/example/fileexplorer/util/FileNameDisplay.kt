package com.example.fileexplorer.util

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.fileexplorer.R

/**
 * 显示文件名时保证扩展名始终可见：文本过长时只省略中间的主文件名部分，
 * 而不是让系统默认的末尾省略号吞掉扩展名；扩展名本身用红色高亮显示。
 */
object FileNameDisplay {

    private const val ELLIPSIS = "…"

    fun set(textView: TextView, fileName: String) {
        val extensionColor = ContextCompat.getColor(textView.context, R.color.file_extension)
        textView.text = colorExtension(fileName, extensionColor)
        textView.post {
            val availableWidth = textView.width - textView.paddingLeft - textView.paddingRight
            if (availableWidth <= 0) return@post
            val truncated = ellipsizeKeepingExtension(fileName, textView.paint, availableWidth)
            textView.text = colorExtension(truncated, extensionColor)
        }
    }

    private fun colorExtension(displayText: String, extensionColor: Int): CharSequence {
        val dotIndex = displayText.lastIndexOf('.')
        if (dotIndex <= 0 || dotIndex >= displayText.length - 1) return displayText
        return SpannableString(displayText).apply {
            setSpan(
                ForegroundColorSpan(extensionColor),
                dotIndex,
                displayText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun ellipsizeKeepingExtension(
        fileName: String,
        paint: android.text.TextPaint,
        availableWidth: Int
    ): String {
        if (paint.measureText(fileName) <= availableWidth) return fileName

        val dotIndex = fileName.lastIndexOf('.')
        val hasExtension = dotIndex > 0 && dotIndex < fileName.length - 1
        val extension = if (hasExtension) fileName.substring(dotIndex) else ""
        var base = if (hasExtension) fileName.substring(0, dotIndex) else fileName

        // 扩展名本身加省略号都放不下时，只能从扩展名末尾开始让步
        if (paint.measureText(ELLIPSIS + extension) > availableWidth) {
            var ext = extension
            while (ext.isNotEmpty() && paint.measureText(ELLIPSIS + ext) > availableWidth) {
                ext = ext.dropLast(1)
            }
            return ELLIPSIS + ext
        }

        while (base.isNotEmpty() && paint.measureText(base + ELLIPSIS + extension) > availableWidth) {
            base = base.dropLast(1)
        }
        return if (base.isEmpty()) ELLIPSIS + extension else base + ELLIPSIS + extension
    }
}
