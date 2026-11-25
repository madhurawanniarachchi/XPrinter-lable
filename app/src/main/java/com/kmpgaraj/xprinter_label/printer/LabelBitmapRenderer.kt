package com.kmpgaraj.xprinter_label.printer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.createBitmap
import kotlin.math.roundToInt

/**
 * Draws a label to a bitmap sized exactly to the label in printer dots.
 *
 * Printing a bitmap (instead of TSPL text commands) keeps any script/font
 * working, e.g. Sinhala or Tamil, because Android does the text shaping.
 */
object LabelBitmapRenderer {

    /** Xprinter label models are 203 dpi, which is 8 dots per millimetre. */
    const val DOTS_PER_MM = 8

    private const val MARGIN_DOTS = 8

    fun render(job: LabelJob): Bitmap {
        val width = job.widthMm * DOTS_PER_MM
        val height = job.heightMm * DOTS_PER_MM
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val textWidth = width - 2 * MARGIN_DOTS
        val title = layout(job.title, textWidth, height * 0.22f, bold = true, maxLines = 3)
        val subtitle = job.subtitle.takeIf { it.isNotBlank() }
            ?.let { layout(it, textWidth, height * 0.14f, bold = false, maxLines = 3) }

        val gap = if (subtitle != null) MARGIN_DOTS else 0
        val contentHeight = title.height + gap + (subtitle?.height ?: 0)
        var top = ((height - contentHeight) / 2f).coerceAtLeast(0f)

        canvas.save()
        canvas.translate(MARGIN_DOTS.toFloat(), top)
        title.draw(canvas)
        canvas.restore()
        top += title.height + gap

        if (subtitle != null) {
            canvas.save()
            canvas.translate(MARGIN_DOTS.toFloat(), top)
            subtitle.draw(canvas)
            canvas.restore()
        }
        return bitmap
    }

    private fun layout(
        text: String,
        width: Int,
        textSize: Float,
        bold: Boolean,
        maxLines: Int
    ): StaticLayout {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            this.textSize = textSize.roundToInt().toFloat()
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
    }
}
