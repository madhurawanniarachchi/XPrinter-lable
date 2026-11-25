package com.kmpgaraj.xprinter_label.printer

/** Everything needed to print one label design. */
data class LabelJob(
    val title: String,
    val subtitle: String = "",
    val widthMm: Int,
    val heightMm: Int,
    val copies: Int = 1,
    val gapMm: Double = DEFAULT_GAP_MM
) {
    init {
        require(widthMm > 0 && heightMm > 0) { "Label size must be positive" }
        require(copies > 0) { "Copies must be at least 1" }
    }

    companion object {
        const val DEFAULT_GAP_MM = 2.0
    }
}

/** A USB device that can be selected as the label printer. */
data class UsbPrinterInfo(
    /** Stable-ish id: "productId-vendorId". */
    val id: String,
    val name: String
)
