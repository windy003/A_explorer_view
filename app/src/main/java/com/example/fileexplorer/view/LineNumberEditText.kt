package com.example.fileexplorer.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import com.example.fileexplorer.R

/**
 * 带行号栏的 EditText。行号按逻辑行（\n 分隔）编号，
 * 自动换行产生的折行不重复编号。
 */
class LineNumberEditText @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatEditText(context, attrs) {

    private val density = context.resources.displayMetrics.density

    private val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.RIGHT
        color = ContextCompat.getColor(context, R.color.text_secondary)
    }
    private val gutterBgPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.path_bar_bg)
    }
    private val gutterLinePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.divider)
        strokeWidth = density
    }
    private val gutterPadding = 6 * density
    private val textMargin = 10 * density

    private var totalLines = 1
    private var countDirty = true

    override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        countDirty = true
    }

    override fun onDraw(canvas: Canvas) {
        val layout = layout
        val text = text
        if (layout == null || text == null) {
            super.onDraw(canvas)
            return
        }

        if (countDirty) {
            var n = 1
            for (c in text) if (c == '\n') n++
            totalLines = n
            countDirty = false
        }

        numberPaint.textSize = textSize * 0.8f
        val digits = totalLines.toString().length.coerceAtLeast(2)
        val gutterWidth = numberPaint.measureText("8".repeat(digits)) + gutterPadding * 2

        val desiredPaddingLeft = (gutterWidth + textMargin).toInt()
        if (paddingLeft != desiredPaddingLeft) {
            setPadding(desiredPaddingLeft, paddingTop, paddingRight, paddingBottom)
        }

        val top = scrollY.toFloat()
        val bottom = (scrollY + height).toFloat()
        canvas.drawRect(0f, top, gutterWidth, bottom, gutterBgPaint)
        canvas.drawLine(gutterWidth, top, gutterWidth, bottom, gutterLinePaint)

        val firstVisible = layout.getLineForVertical(scrollY)
        val lastVisible = layout.getLineForVertical(scrollY + height)

        // firstVisible 所在逻辑行的行号 = 它之前的换行符数 + 1
        val firstStart = layout.getLineStart(firstVisible)
        var num = 1
        for (i in 0 until firstStart) if (text[i] == '\n') num++

        val numberX = gutterWidth - gutterPadding
        for (line in firstVisible..lastVisible) {
            val lineStart = layout.getLineStart(line)
            val startsLogicalLine = lineStart == 0 || text[lineStart - 1] == '\n'
            if (line > firstVisible && startsLogicalLine) num++
            if (startsLogicalLine) {
                val baseline = getLineBounds(line, null)
                canvas.drawText(num.toString(), numberX, baseline.toFloat(), numberPaint)
            }
        }

        super.onDraw(canvas)
    }
}
