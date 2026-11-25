package com.kmpgaraj.xprinter_label.printer

/** The kinds of label the app can print. */
enum class LabelType(val displayName: String) {
    EXPIRY("Expiry date"),
    ADDRESS("Address")
}

/** What is written on a label. One subtype per [LabelType]. */
sealed interface LabelContent {

    /** Product label showing when the item expires. */
    data class Expiry(
        val productName: String,
        val expiryDate: String,
        val batch: String = ""
    ) : LabelContent

    /** Delivery / shipping address label. */
    data class Address(
        val name: String,
        val address: String,
        val phone: String = ""
    ) : LabelContent
}

/** Everything needed to print one label design. */
data class LabelJob(
    val content: LabelContent,
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
