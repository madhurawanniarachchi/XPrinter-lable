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
    private const val BLOCK_GAP_DOTS = 6
    private const val MIN_SCALE = 0.4f

    /** One run of text on a label. [size] is a fraction of the label height. */
    private data class Block(
        val text: String,
        val size: Float,
        val bold: Boolean = false,
        val maxLines: Int = 1
    )

    fun render(job: LabelJob): Bitmap {
        val width = job.widthMm * DOTS_PER_MM
        val height = job.heightMm * DOTS_PER_MM
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val (blocks, alignment) = when (val content = job.content) {
            is LabelContent.Expiry -> expiryBlocks(content) to Layout.Alignment.ALIGN_CENTER
            is LabelContent.Address -> addressBlocks(content) to Layout.Alignment.ALIGN_NORMAL
        }
        draw(canvas, blocks, alignment, width, height)
        return bitmap
    }

    private fun expiryBlocks(content: LabelContent.Expiry) = listOfNotNull(
        Block(content.productName, 0.20f, bold = true, maxLines = 2),
        Block("EXPIRY DATE", 0.10f),
        Block(content.expiryDate, 0.28f, bold = true),
        content.batch.takeIf { it.isNotBlank() }?.let { Block("Batch: $it", 0.10f) }
    )

    private fun addressBlocks(content: LabelContent.Address) = listOfNotNull(
        Block(content.name, 0.18f, bold = true, maxLines = 2),
        Block(content.address, 0.13f, maxLines = 6),
        content.phone.takeIf { it.isNotBlank() }?.let { Block("Tel: $it", 0.13f, bold = true) }
    )

    /**
     * Lays the blocks out top to bottom, centred vertically. If they do not fit,
     * all text is scaled down together until they do.
     */
    private fun draw(
        canvas: Canvas,
        blocks: List<Block>,
        alignment: Layout.Alignment,
        width: Int,
        height: Int
    ) {
        val textWidth = width - 2 * MARGIN_DOTS
        val available = height - 2 * MARGIN_DOTS

        var scale = 1f
        var layouts = blocks.map { layout(it, textWidth, height, scale, alignment) }
        while (totalHeight(layouts) > available && scale > MIN_SCALE) {
            scale -= 0.05f
            layouts = blocks.map { layout(it, textWidth, height, scale, alignment) }
        }

        var top = ((height - totalHeight(layouts)) / 2f).coerceAtLeast(MARGIN_DOTS.toFloat())
        for (layout in layouts) {
            canvas.save()
            canvas.translate(MARGIN_DOTS.toFloat(), top)
            layout.draw(canvas)
            canvas.restore()
            top += layout.height + BLOCK_GAP_DOTS
        }
    }

    private fun totalHeight(layouts: List<StaticLayout>): Int =
        layouts.sumOf { it.height } + BLOCK_GAP_DOTS * (layouts.size - 1).coerceAtLeast(0)

    private fun layout(
        block: Block,
        width: Int,
        labelHeight: Int,
        scale: Float,
        alignment: Layout.Alignment
    ): StaticLayout {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = (labelHeight * block.size * scale).roundToInt().coerceAtLeast(8).toFloat()
            typeface = if (block.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        return StaticLayout.Builder.obtain(block.text, 0, block.text.length, paint, width)
            .setAlignment(alignment)
            .setMaxLines(block.maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
    }
}
