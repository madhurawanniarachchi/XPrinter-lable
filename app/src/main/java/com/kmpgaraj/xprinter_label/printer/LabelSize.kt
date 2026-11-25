package com.kmpgaraj.xprinter_label.printer

import java.util.Locale
import kotlin.math.roundToInt

/** A label stock size. Stored in millimetres, shown to people in centimetres. */
data class LabelSize(val widthMm: Int, val heightMm: Int) {

    /** e.g. "5 x 4 cm" or "10 x 15 cm" or "4.5 x 3 cm". */
    val displayCm: String get() = "${cm(widthMm)} x ${cm(heightMm)} cm"

    private fun cm(mm: Int): String =
        if (mm % 10 == 0) (mm / 10).toString() else String.format(Locale.US, "%.1f", mm / 10.0)

    companion object {
        /**
         * Common thermal label roll sizes (width x height), sorted small to large.
         * These are the sizes sold for Xprinter-style 203 dpi label printers and
         * the ones the SalesplayPOS project already supports (marked below),
         * plus the standard 4x6 in shipping label.
         */
        val standard: List<LabelSize> = listOf(
            LabelSize(25, 15),
            LabelSize(30, 20),   // SalesplayPOS
            LabelSize(30, 30),
            LabelSize(40, 20),
            LabelSize(40, 30),   // SalesplayPOS
            LabelSize(40, 40),
            LabelSize(45, 40),   // SalesplayPOS
            LabelSize(50, 25),   // SalesplayPOS
            LabelSize(50, 30),   // SalesplayPOS
            LabelSize(50, 40),   // SalesplayPOS
            LabelSize(50, 50),   // SalesplayPOS
            LabelSize(40, 60),   // SalesplayPOS
            LabelSize(60, 40),
            LabelSize(60, 60),
            LabelSize(70, 50),
            LabelSize(75, 50),
            LabelSize(80, 50),
            LabelSize(100, 50),
            LabelSize(100, 100),
            LabelSize(100, 150)  // 4 x 6 in shipping label
        )

        val default: LabelSize = LabelSize(40, 30)

        /** Converts a centimetre value typed by the user to whole millimetres, or null if invalid. */
        fun cmToMm(text: String): Int? =
            text.replace(',', '.').toDoubleOrNull()
                ?.let { (it * 10).roundToInt() }
                ?.takeIf { it in MIN_MM..MAX_MM }

        const val MIN_MM = 10
        const val MAX_MM = 200
    }
}
