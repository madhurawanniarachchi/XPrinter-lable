package com.kmpgaraj.xprinter_label.printer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.createBitmap
import kotlin.math.min
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
    private const val MIN_TEXT_DOTS = 8f

    /** Address labels: the frame sits 2 mm in from the label edge, so a slightly shifted roll does not cut it. */
    private const val FRAME_INSET_DOTS = 16
    private const val FRAME_STROKE_DOTS = 4f
    private const val FRAME_PADDING_DOTS = 12

    /** Share of the frame width given to the left column (sender and order details) on a wide label. */
    private const val LEFT_COLUMN_SHARE = 0.38f

    /**
     * Relative heights of the blocks that share a column. A block that is left out
     * hands its share to the others; the receiver, the part the carrier reads, stays the largest.
     */
    private const val SENDER_WEIGHT = 0.30f
    private const val RECEIVER_WEIGHT = 0.52f
    private const val ORDER_WEIGHT = 0.18f

    /** A label at least this much wider than tall puts sender and receiver side by side instead of stacked. */
    private const val SIDE_BY_SIDE_RATIO = 1.6f

    /**
     * One run of text on a label. [size] is a fraction of the reference length
     * (the label height; the shorter side for address labels).
     * With [fitWidth] a single line that is too wide is shrunk to fit, instead of cut off.
     */
    private data class Block(
        val text: String,
        val size: Float,
        val bold: Boolean = false,
        val maxLines: Int = 1,
        val fitWidth: Boolean = false
    )

    /** A block of lines on an address label, and how much of its column it gets relative to the other blocks. */
    private class Cell(val blocks: List<Block>, val weight: Float)

    /**
     * One printed row: the label repeated across [labelsInRow] columns, separated
     * by the job's column gap. Columns that are left out stay blank, which is how
     * a last row with fewer labels than columns is printed.
     */
    fun render(job: LabelJob, labelsInRow: Int = job.columns): Bitmap {
        val label = renderLabel(job)
        if (job.columns == 1) return label

        val row = createBitmap(job.rowWidthMm * DOTS_PER_MM, label.height)
        val canvas = Canvas(row)
        canvas.drawColor(Color.WHITE)
        val step = (job.widthMm + job.columnGapMm) * DOTS_PER_MM
        repeat(labelsInRow.coerceIn(0, job.columns)) { column ->
            canvas.drawBitmap(label, (column * step).toFloat(), 0f, null)
        }
        label.recycle()
        return row
    }

    /** A single label, sized exactly to the label in printer dots. */
    private fun renderLabel(job: LabelJob): Bitmap {
        val width = job.widthMm * DOTS_PER_MM
        val height = job.heightMm * DOTS_PER_MM
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        when (val content = job.content) {
            is LabelContent.Expiry -> draw(
                canvas,
                expiryBlocks(content),
                Layout.Alignment.ALIGN_NORMAL,
                Rect(MARGIN_DOTS, MARGIN_DOTS, width - MARGIN_DOTS, height - MARGIN_DOTS),
                reference = height,
                centerVertically = true
            )

            is LabelContent.Address -> drawAddress(canvas, content, width, height)
        }
        return bitmap
    }

    /** One line per filled-in field, in label order; empty fields get no line. */
    private fun expiryBlocks(content: LabelContent.Expiry) = listOfNotNull(
        content.batchNo.nonBlankOrNull()?.let { Block("Batch No: $it", 0.15f, fitWidth = true) },
        content.mfd.nonBlankOrNull()?.let { Block("MFD: $it", 0.17f, fitWidth = true) },
        content.exp.nonBlankOrNull()?.let { Block("EXP: $it", 0.19f, bold = true, fitWidth = true) },
        content.mrp.nonBlankOrNull()?.let { Block("MRP: $it", 0.22f, bold = true, fitWidth = true) }
    )

    private fun String.nonBlankOrNull(): String? = trim().takeIf { it.isNotEmpty() }

    /**
     * A framed delivery label in the layout of shipping labels (ISO 15394, GS1 logistics
     * label): a FROM block and a TO block, separated by a line, with TO the larger of
     * the two. Each block lists its lines in postal-address order (UPU S42): addressee,
     * delivery address, postcode and place, country last. Place and country are
     * capitalised, as the UPU letter-post rules require. The ORDER block (order number,
     * date, items, weight, cash on delivery) is a third block of its own.
     *
     * Blocks with nothing filled in are left off, so a receiver alone fills the whole
     * frame. A wide label puts the sender and the order details in a left column and
     * the receiver on the right; any other label stacks sender, receiver and order.
     */
    private fun drawAddress(canvas: Canvas, content: LabelContent.Address, width: Int, height: Int) {
        // Text sizes follow the shorter side, so the same label reads well wide or tall.
        val reference = min(width, height)
        // The frame margins shrink on labels under 50 mm so small labels keep room for text.
        val marginScale = (reference / 400f).coerceIn(0.5f, 1f)
        val inset = (FRAME_INSET_DOTS * marginScale).roundToInt()
        val padding = FRAME_STROKE_DOTS.roundToInt() + (FRAME_PADDING_DOTS * marginScale).roundToInt()

        val line = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = FRAME_STROKE_DOTS
        }
        val frame = RectF(inset.toFloat(), inset.toFloat(), (width - inset).toFloat(), (height - inset).toFloat())
        canvas.drawRect(frame, line)

        val inner = Rect(inset + padding, inset + padding, width - inset - padding, height - inset - padding)
        val sideBySide = width >= height * SIDE_BY_SIDE_RATIO

        val receiver = Cell(partyBlocks("TO", content.receiver, primary = true), RECEIVER_WEIGHT)
        val sender = Cell(partyBlocks("FROM", content.sender, primary = false), SENDER_WEIGHT)
            .takeUnless { content.sender.isEmpty }
        val order = Cell(orderBlocks(content.order, combineLines = !sideBySide), ORDER_WEIGHT)
            .takeUnless { content.order.isEmpty }

        val leftColumn = listOfNotNull(sender, order)
        if (sideBySide && leftColumn.isNotEmpty()) {
            val split = inner.left + (inner.width() * LEFT_COLUMN_SHARE).roundToInt()
            canvas.drawLine(split.toFloat(), frame.top, split.toFloat(), frame.bottom, line)
            val leftArea = Rect(inner.left, inner.top, split - padding, inner.bottom)
            drawCells(canvas, leftColumn, leftArea, frame.left, split.toFloat(), padding, reference, line)
            val rightArea = Rect(split + padding, inner.top, inner.right, inner.bottom)
            drawCells(canvas, listOf(receiver), rightArea, split.toFloat(), frame.right, padding, reference, line)
        } else {
            val stack = if (sideBySide) listOf(receiver) else listOfNotNull(sender, receiver, order)
            drawCells(canvas, stack, inner, frame.left, frame.right, padding, reference, line)
        }
    }

    /**
     * Stacks [cells] top to bottom inside [area], each as tall as its weight is of the
     * total, with a divider line between neighbours. The dividers run from
     * [dividerStart] to [dividerEnd], which reach the frame on both sides.
     */
    private fun drawCells(
        canvas: Canvas,
        cells: List<Cell>,
        area: Rect,
        dividerStart: Float,
        dividerEnd: Float,
        padding: Int,
        reference: Int,
        line: Paint
    ) {
        val totalWeight = cells.sumOf { it.weight.toDouble() }.toFloat()
        var weightSoFar = 0f
        var top = area.top
        cells.forEachIndexed { index, cell ->
            val isFirst = index == 0
            val isLast = index == cells.lastIndex
            weightSoFar += cell.weight
            val boundary = if (isLast) area.bottom else area.top + (area.height() * weightSoFar / totalWeight).roundToInt()
            // Keep the text clear of the divider on whichever side it is.
            val cellArea = Rect(
                area.left,
                if (isFirst) top else top + padding,
                area.right,
                if (isLast) boundary else boundary - padding
            )
            draw(canvas, cell.blocks, Layout.Alignment.ALIGN_NORMAL, cellArea, reference, centerVertically = false)
            if (!isLast) canvas.drawLine(dividerStart, boundary.toFloat(), dividerEnd, boundary.toFloat(), line)
            top = boundary
        }
    }

    /** The caption, then one block per filled-in field; empty fields get no line. */
    private fun partyBlocks(caption: String, party: Party, primary: Boolean): List<Block> {
        val captionSize = if (primary) 0.045f else 0.04f
        val nameSize = if (primary) 0.085f else 0.055f
        val textSize = if (primary) 0.06f else 0.045f
        val placeSize = if (primary) 0.075f else 0.05f
        return listOfNotNull(
            Block(caption, captionSize, bold = true),
            party.name.nonBlankOrNull()?.let { Block(it, nameSize, bold = true, maxLines = 2) },
            party.address.nonBlankOrNull()?.let { Block(it, textSize, maxLines = 4) },
            party.postcodeCity.nonBlankOrNull()?.let { Block(it.uppercase(), placeSize, bold = true, fitWidth = true) },
            party.country.nonBlankOrNull()?.let { Block(it.uppercase(), placeSize, bold = true, fitWidth = true) },
            party.phone.nonBlankOrNull()?.let { Block("Tel: $it", textSize, fitWidth = true) }
        )
    }

    /**
     * The caption, then the order lines that are filled in. With [combineLines] (a wide
     * block) order number and date share a line, and items and weight share a line.
     */
    private fun orderBlocks(order: Order, combineLines: Boolean): List<Block> {
        val number = order.number.nonBlankOrNull()?.let { "Order no: $it" }
        val date = order.date.nonBlankOrNull()?.let { "Date: $it" }
        val items = order.items.nonBlankOrNull()?.let { "Items: $it" }
        val weight = order.weight.nonBlankOrNull()?.let { "Weight: $it" }
        val lines = if (combineLines) {
            listOfNotNull(
                listOfNotNull(number, date).joinToString("    ").ifEmpty { null },
                listOfNotNull(items, weight).joinToString("    ").ifEmpty { null }
            )
        } else {
            listOfNotNull(number, date, items, weight)
        }
        return listOf(Block("ORDER", 0.04f, bold = true)) +
            lines.mapIndexed { index, text ->
                Block(text, 0.05f, bold = index == 0 && number != null, fitWidth = true)
            } +
            listOfNotNull(
                order.cod.nonBlankOrNull()?.let { Block("COD: $it", 0.065f, bold = true, fitWidth = true) }
            )
    }

    /**
     * Lays the blocks out top to bottom inside [area], either from the top or centred
     * vertically. If they do not fit, all text is scaled down together until they do.
     * Nothing is drawn outside [area].
     */
    private fun draw(
        canvas: Canvas,
        blocks: List<Block>,
        alignment: Layout.Alignment,
        area: Rect,
        reference: Int,
        centerVertically: Boolean
    ) {
        val textWidth = area.width()
        val available = area.height()

        var scale = 1f
        var layouts = blocks.map { layout(it, textWidth, reference, scale, alignment) }
        while (totalHeight(layouts) > available && scale > MIN_SCALE) {
            scale -= 0.05f
            layouts = blocks.map { layout(it, textWidth, reference, scale, alignment) }
        }

        var top = if (centerVertically) {
            (area.top + (available - totalHeight(layouts)) / 2f).coerceAtLeast(area.top.toFloat())
        } else {
            area.top.toFloat()
        }
        canvas.save()
        canvas.clipRect(area)
        for (layout in layouts) {
            canvas.save()
            canvas.translate(area.left.toFloat(), top)
            layout.draw(canvas)
            canvas.restore()
            top += layout.height + BLOCK_GAP_DOTS
        }
        canvas.restore()
    }

    private fun totalHeight(layouts: List<StaticLayout>): Int =
        layouts.sumOf { it.height } + BLOCK_GAP_DOTS * (layouts.size - 1).coerceAtLeast(0)

    private fun layout(
        block: Block,
        width: Int,
        reference: Int,
        scale: Float,
        alignment: Layout.Alignment
    ): StaticLayout {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = (reference * block.size * scale).roundToInt().coerceAtLeast(8).toFloat()
            typeface = if (block.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        if (block.fitWidth) {
            val needed = paint.measureText(block.text)
            if (needed > width) {
                paint.textSize = (paint.textSize * width / needed).coerceAtLeast(MIN_TEXT_DOTS)
            }
        }
        return StaticLayout.Builder.obtain(block.text, 0, block.text.length, paint, width)
            .setAlignment(alignment)
            .setMaxLines(block.maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
    }
}
