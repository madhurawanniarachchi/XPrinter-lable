package com.kmpgaraj.xprinter_label.printer

/** The kinds of label the app can print. */
enum class LabelType(val displayName: String) {
    EXPIRY("Expiry date"),
    ADDRESS("Address");

    /** The size picked when nothing suitable has been chosen yet. Always one of [LabelSize.standard]. */
    val defaultSize: LabelSize
        get() = when (this) {
            EXPIRY -> LabelSize.default
            ADDRESS -> LabelSize(100, 150) // the common 4 x 6 in shipping label
        }

    /** The smallest size this type needs, for display, or null when any size will do. */
    val minimumSizeText: String?
        get() = when (this) {
            EXPIRY -> null
            ADDRESS -> "6 × 4 cm"
        }

    /**
     * Whether this type of label can be read at [size]. A framed address label
     * holds up to three blocks of text, so it needs at least 6 × 4 cm in area
     * and 4 cm on its short side, whichever way up the label is. Any size suits
     * an expiry label.
     */
    fun allows(size: LabelSize): Boolean = when (this) {
        EXPIRY -> true
        ADDRESS -> minOf(size.widthMm, size.heightMm) >= ADDRESS_MIN_SHORT_SIDE_MM &&
            size.widthMm * size.heightMm >= ADDRESS_MIN_AREA_MM2
    }

    private companion object {
        const val ADDRESS_MIN_SHORT_SIDE_MM = 40
        const val ADDRESS_MIN_AREA_MM2 = 60 * 40
    }
}

/**
 * A date picked as dd/MM/yyyy, as it is printed: unchanged, or as MM/yyyy when
 * [monthYearOnly]. Text that is not in that form (for example a placeholder) is
 * left as it is.
 */
fun formatLabelDate(date: String, monthYearOnly: Boolean): String {
    val text = date.trim()
    if (!monthYearOnly) return text
    val parts = text.split('/')
    return if (parts.size == 3) "${parts[1]}/${parts[2]}" else text
}

/** What is written on a label. One subtype per [LabelType]. */
sealed interface LabelContent {

    /**
     * Product label with price, manufacture date, expiry date and batch number.
     * Every field is optional; an empty one is left off the printed label.
     * The dates are held as picked (dd/MM/yyyy); with [monthYearOnly] they are
     * printed as MM/yyyy, leaving the day off.
     */
    data class Expiry(
        val mrp: String = "",
        val mfd: String = "",
        val exp: String = "",
        val batchNo: String = "",
        val monthYearOnly: Boolean = false
    ) : LabelContent {
        val isEmpty: Boolean get() = listOf(mrp, mfd, exp, batchNo).all { it.isBlank() }
    }

    /**
     * Delivery label: the [receiver] ("TO"), optionally the [sender] ("FROM") and
     * the [order] details, each printed in its own block. A block with nothing
     * filled in is left off.
     */
    data class Address(
        val receiver: Party,
        val sender: Party = Party(),
        val order: Order = Order()
    ) : LabelContent
}

/** What is in the parcel and how it is paid for. Free text, so units and currency are up to the user. */
data class Order(
    val number: String = "",
    /** dd/MM/yyyy. */
    val date: String = "",
    /** Number of pieces. */
    val items: String = "",
    /** With its unit, for example "1.5 kg". */
    val weight: String = "",
    /** Cash on delivery: the amount the courier collects, with its currency. */
    val cod: String = ""
) {
    val isEmpty: Boolean get() = listOf(number, date, items, weight, cod).all { it.isBlank() }
}

/**
 * One end of a parcel. Fields follow the order of a postal address (UPU S42 /
 * ISO 19160-4): addressee, delivery address, postcode and place, country.
 * An empty field is left off the label.
 */
data class Party(
    val name: String = "",
    /** Street, number, flat; may span several lines. */
    val address: String = "",
    /**
     * Postcode and place on one line, in the order the destination country uses,
     * for example "22767 HAMBURG" or "COLOMBO 00300".
     */
    val postcodeCity: String = "",
    /** Destination country; printed in capitals on the last line. Leave empty for domestic mail. */
    val country: String = "",
    val phone: String = ""
) {
    val isEmpty: Boolean get() = listOf(name, address, postcodeCity, country, phone).all { it.isBlank() }
}

/**
 * Everything needed to print one label design.
 *
 * [widthMm] x [heightMm] is a single label. On a roll with several labels side
 * by side, [columns] and [columnGapMm] describe the layout, and [copies] is the
 * total number of labels wanted (they are spread across the columns).
 */
data class LabelJob(
    val content: LabelContent,
    val widthMm: Int,
    val heightMm: Int,
    val copies: Int = 1,
    val gapMm: Double = DEFAULT_GAP_MM,
    val columns: Int = 1,
    val columnGapMm: Int = DEFAULT_COLUMN_GAP_MM
) {
    init {
        require(widthMm > 0 && heightMm > 0) { "Label size must be positive" }
        require(copies > 0) { "Copies must be at least 1" }
        require(columns in 1..MAX_COLUMNS) { "Columns must be between 1 and $MAX_COLUMNS" }
        require(columnGapMm >= 0) { "Column gap cannot be negative" }
    }

    /** Width of one printed row: every column plus the gaps between them. */
    val rowWidthMm: Int get() = columns * widthMm + (columns - 1) * columnGapMm

    /** Rows the roll advances. The last one may hold fewer labels than [columns]. */
    val rows: Int get() = (copies + columns - 1) / columns

    /** How many labels are printed in the last row (1..[columns]). */
    val lastRowLabels: Int get() = copies - (rows - 1) * columns

    companion object {
        const val DEFAULT_GAP_MM = 2.0
        const val DEFAULT_COLUMN_GAP_MM = 2
        const val MAX_COLUMNS = 6
        const val MAX_COLUMN_GAP_MM = 50
    }
}

/** A USB device that can be selected as the label printer. */
data class UsbPrinterInfo(
    /** Stable-ish id: "productId-vendorId". */
    val id: String,
    val name: String
)
