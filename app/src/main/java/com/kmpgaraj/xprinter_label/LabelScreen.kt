package com.kmpgaraj.xprinter_label

import android.util.TypedValue
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmpgaraj.xprinter_label.printer.LabelBitmapRenderer
import com.kmpgaraj.xprinter_label.printer.LabelContent
import com.kmpgaraj.xprinter_label.printer.LabelJob
import com.kmpgaraj.xprinter_label.printer.LabelSize
import com.kmpgaraj.xprinter_label.printer.LabelType
import com.kmpgaraj.xprinter_label.printer.Order
import com.kmpgaraj.xprinter_label.printer.Party
import com.kmpgaraj.xprinter_label.printer.PrinterUiState
import com.kmpgaraj.xprinter_label.printer.UsbPrinterInfo
import com.kmpgaraj.xprinter_label.printer.formatLabelDate
import com.kmpgaraj.xprinter_label.ui.AppCard
import com.kmpgaraj.xprinter_label.ui.AppHeader
import com.kmpgaraj.xprinter_label.ui.CardTitle
import com.kmpgaraj.xprinter_label.ui.DateSelectField
import com.kmpgaraj.xprinter_label.ui.ErrorText
import com.kmpgaraj.xprinter_label.ui.GhostButton
import com.kmpgaraj.xprinter_label.ui.LabeledTextField
import com.kmpgaraj.xprinter_label.ui.NoteBox
import com.kmpgaraj.xprinter_label.ui.PrimaryButton
import com.kmpgaraj.xprinter_label.ui.PrinterRow
import com.kmpgaraj.xprinter_label.ui.SegmentedTabs
import com.kmpgaraj.xprinter_label.ui.SelectField
import com.kmpgaraj.xprinter_label.ui.ToggleRow
import com.kmpgaraj.xprinter_label.ui.theme.XPrinterLabelTheme
import com.kmpgaraj.xprinter_label.ui.theme.palette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Marks the "Custom size" entry of the size dropdown. */
private const val CUSTOM_SIZE = -1

/** At this width the screen switches from one column to the two-column layout. */
private val WideLayoutMinWidth = 720.dp

private val ContentMaxWidth = 1100.dp

/** The last entry of the size dropdown, after the standard sizes. */
private const val CUSTOM_SIZE_LABEL = "Custom size"

@Composable
fun LabelScreen(
    printerState: PrinterUiState,
    onRefreshPrinters: () -> Unit,
    onSelectPrinter: (String) -> Unit,
    onPrint: (LabelJob) -> Unit,
    onTestPrint: (LabelJob) -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
    /** The sender saved last time; the Sender form starts with it. */
    initialSender: Party = Party(),
    /** Called with the new sender whenever it is edited or cleared, so it can be saved. */
    onSenderChanged: (Party) -> Unit = {},
    /** The Expiry record saved last time; the Expiry form starts with it. */
    initialExpiry: LabelContent.Expiry = LabelContent.Expiry(),
    /** Called with the new record on every edit, so the last one is always saved. */
    onExpiryChanged: (LabelContent.Expiry) -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(printerState.message) {
        printerState.message?.let {
            // Long, because a printer error message takes a while to read.
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long)
            onMessageShown()
        }
    }

    var typeIndex by rememberSaveable { mutableStateOf(0) }
    val type = LabelType.entries[typeIndex]
    var expiry by rememberSaveable(stateSaver = ExpirySaver) { mutableStateOf(initialExpiry) }
    // Every edit is saved, so the last record is always there for the next label.
    val updateExpiry: (LabelContent.Expiry) -> Unit = {
        expiry = it
        onExpiryChanged(it)
    }
    var receiver by rememberSaveable(stateSaver = PartySaver) { mutableStateOf(Party()) }
    var sender by rememberSaveable(stateSaver = PartySaver) { mutableStateOf(initialSender) }
    var order by rememberSaveable(stateSaver = OrderSaver) { mutableStateOf(Order()) }
    // Index into LabelSize.standard, or CUSTOM_SIZE for the typed-in size below.
    var sizeIndex by rememberSaveable {
        mutableStateOf(LabelSize.standard.indexOf(LabelSize.default))
    }
    var customWidthCm by rememberSaveable { mutableStateOf("4") }
    var customHeightCm by rememberSaveable { mutableStateOf("3") }
    var copies by rememberSaveable { mutableStateOf("1") }
    // Labels side by side across the roll, and the gap between them.
    var columns by rememberSaveable { mutableStateOf("1") }
    var columnGapCm by rememberSaveable { mutableStateOf("0.2") }

    // Sizes too small for this type of label are not offered.
    val allowedSizes = LabelSize.standard.filter { type.allows(it) }
    // A size picked for another label type may not suit this one; fall back to this type's default.
    val standardIndex = sizeIndex.takeIf { it == CUSTOM_SIZE || type.allows(LabelSize.standard[it]) }
        ?: LabelSize.standard.indexOf(type.defaultSize)
    val isCustom = standardIndex == CUSTOM_SIZE
    val customSize: LabelSize? = run {
        val w = LabelSize.cmToMm(customWidthCm)
        val h = LabelSize.cmToMm(customHeightCm)
        if (w != null && h != null) LabelSize(w, h) else null
    }
    val labelSize: LabelSize? = if (isCustom) {
        customSize?.takeIf { type.allows(it) }
    } else {
        LabelSize.standard[standardIndex]
    }
    val copyCount = copies.toIntOrNull()
    val columnCount = columns.toIntOrNull()?.takeIf { it in 1..LabelJob.MAX_COLUMNS }
    val columnGapMm = LabelSize.cmToMm(columnGapCm, 0..LabelJob.MAX_COLUMN_GAP_MM)
    // The gap only matters once there is more than one column.
    val columnsValid = columnCount != null && (columnCount == 1 || columnGapMm != null)
    // Null until the required fields of the chosen label type are filled in.
    val content: LabelContent? = when (type) {
        // Every field is optional, but at least one is needed to have anything to print.
        LabelType.EXPIRY ->
            expiry.takeUnless { it.isEmpty }

        // The receiver needs a name and an address; the sender is optional.
        LabelType.ADDRESS ->
            if (receiver.name.isNotBlank() && receiver.address.isNotBlank()) {
                LabelContent.Address(receiver, sender, order)
            } else null
    }
    val canPrint = printerState.canPrint && content != null &&
        labelSize != null && copyCount != null && copyCount > 0 && columnsValid

    // What the preview shows while the form is still incomplete.
    val previewContent: LabelContent = content ?: when (type) {
        LabelType.EXPIRY -> LabelContent.Expiry(
            mrp = "0.00",
            mfd = "dd/mm/yyyy",
            exp = "dd/mm/yyyy",
            batchNo = "B-2041",
            monthYearOnly = expiry.monthYearOnly
        )

        // A blank form shows a sample of both blocks; otherwise the preview is what will print.
        LabelType.ADDRESS ->
            if (receiver.isEmpty && sender.isEmpty && order.isEmpty) {
                SampleAddress
            } else {
                LabelContent.Address(
                    receiver.copy(
                        name = receiver.name.ifBlank { "Receiver name" },
                        address = receiver.address.ifBlank { "Street address" }
                    ),
                    sender,
                    order
                )
            }
    }

    // The diagnostic Test print needs a valid size and column layout, but not filled-in fields.
    val testJob = if (labelSize != null && columnsValid) {
        LabelJob(
            content = previewContent,
            widthMm = labelSize.widthMm,
            heightMm = labelSize.heightMm,
            columns = columnCount,
            columnGapMm = columnGapMm ?: LabelJob.DEFAULT_COLUMN_GAP_MM
        )
    } else null

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.palette.background,
        // The header and the scrolling content apply the system-bar insets themselves.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.navigationBarsPadding()) }
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val wide = maxWidth >= WideLayoutMinWidth
            val sidePadding = if (wide) 32.dp else 16.dp

            val typeTabs: @Composable () -> Unit = {
                SegmentedTabs(
                    options = LabelType.entries.map { it.displayName },
                    selectedIndex = typeIndex,
                    onSelect = { typeIndex = it }
                )
            }

            val detailsCard: @Composable () -> Unit = {
                when (type) {
                    LabelType.ADDRESS -> Column(verticalArrangement = Arrangement.spacedBy(if (wide) 20.dp else 16.dp)) {
                        PartyCard(
                            title = "Receiver (TO)",
                            party = receiver,
                            onChange = { receiver = it },
                            wide = wide,
                            note = "Write the postcode and place in the order the destination country uses. " +
                                "Place and country are printed in capitals, as the UPU letter-post rules ask. " +
                                "Leave the country empty for domestic parcels."
                        )
                        PartyCard(
                            title = "Sender (FROM)",
                            party = sender,
                            onChange = {
                                sender = it
                                onSenderChanged(it)
                            },
                            wide = wide,
                            note = "Optional. Saved on this device and filled in next time. " +
                                "Leave it empty to print the receiver only.",
                            onClear = {
                                sender = Party()
                                onSenderChanged(Party())
                            }
                        )
                        OrderCard(order = order, onChange = { order = it }, wide = wide)
                    }

                    LabelType.EXPIRY -> AppCard {
                        CardTitle("Label details")
                        // The fields follow the order they are printed in: MRP, MFD, EXP, batch no.
                        val mrpField: @Composable (Modifier) -> Unit = { fieldModifier ->
                            LabeledTextField(
                                label = "MRP",
                                value = expiry.mrp,
                                onValueChange = { updateExpiry(expiry.copy(mrp = it)) },
                                modifier = fieldModifier,
                                placeholder = "e.g. 250.00"
                            )
                        }
                        val mfdField: @Composable (Modifier) -> Unit = { fieldModifier ->
                            DatePickerField(
                                label = "MFD",
                                value = expiry.mfd,
                                monthYearOnly = expiry.monthYearOnly,
                                onDateSelected = { updateExpiry(expiry.copy(mfd = it)) },
                                modifier = fieldModifier
                            )
                        }
                        val expField: @Composable (Modifier) -> Unit = { fieldModifier ->
                            DatePickerField(
                                label = "EXP",
                                value = expiry.exp,
                                monthYearOnly = expiry.monthYearOnly,
                                onDateSelected = { updateExpiry(expiry.copy(exp = it)) },
                                modifier = fieldModifier
                            )
                        }
                        val batchField: @Composable (Modifier) -> Unit = { fieldModifier ->
                            LabeledTextField(
                                label = "Batch no",
                                value = expiry.batchNo,
                                onValueChange = { updateExpiry(expiry.copy(batchNo = it)) },
                                modifier = fieldModifier,
                                placeholder = "e.g. B-2041"
                            )
                        }
                        // Two to a row only when there is room; stacked on a phone.
                        if (wide) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                mrpField(Modifier.weight(1f))
                                mfdField(Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                expField(Modifier.weight(1f))
                                batchField(Modifier.weight(1f))
                            }
                        } else {
                            mrpField(Modifier)
                            mfdField(Modifier)
                            expField(Modifier)
                            batchField(Modifier)
                        }
                        ToggleRow(
                            label = "Hide the day",
                            description = "Print MFD and EXP as month/year only, for example 03/2026.",
                            checked = expiry.monthYearOnly,
                            onCheckedChange = { updateExpiry(expiry.copy(monthYearOnly = it)) }
                        )
                        NoteBox("Leave a field empty and it will not be printed. The last record is saved on this device.")
                    }
                }
            }

            val layoutCard: @Composable () -> Unit = {
                AppCard {
                    CardTitle("Size and layout")
                    SelectField(
                        label = "Label size (width × height)",
                        valueText = if (isCustom) "Custom size" else LabelSize.standard[standardIndex].displayCm,
                        options = allowedSizes.map { it.displayCm } + CUSTOM_SIZE_LABEL,
                        onSelect = {
                            // The last option is "Custom size"; the others are index into allowedSizes.
                            sizeIndex = if (it == allowedSizes.size) CUSTOM_SIZE else LabelSize.standard.indexOf(allowedSizes[it])
                        }
                    )
                    if (isCustom) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DecimalField("Width (cm)", customWidthCm, { customWidthCm = it }, Modifier.weight(1f))
                            DecimalField("Height (cm)", customHeightCm, { customHeightCm = it }, Modifier.weight(1f))
                        }
                        if (labelSize == null) {
                            if (customSize == null) {
                                ErrorText("Enter a size between ${LabelSize.MIN_MM / 10} and ${LabelSize.MAX_MM / 10} cm.")
                            } else {
                                // A valid size that is too small for this type of label.
                                ErrorText("${type.displayName} labels need at least ${type.minimumSizeText} (either way up).")
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NumberField("Copies", copies, { copies = it }, Modifier.weight(1f))
                        NumberField("Columns", columns, { columns = it }, Modifier.weight(1f))
                        if (columnCount != null && columnCount > 1) {
                            DecimalField("Gap (cm)", columnGapCm, { columnGapCm = it }, Modifier.weight(1f))
                        }
                    }
                    if (columnCount == null) {
                        ErrorText("Columns must be between 1 and ${LabelJob.MAX_COLUMNS}.")
                    } else if (columnCount > 1) {
                        if (columnGapMm == null) {
                            ErrorText("Column gap must be between 0 and ${LabelJob.MAX_COLUMN_GAP_MM / 10} cm.")
                        } else if (copyCount != null && copyCount > 0) {
                            NoteBox(columnSummary(copyCount, columnCount))
                        }
                    }
                }
            }

            val previewCard: @Composable () -> Unit = {
                PreviewCard(
                    content = previewContent,
                    size = labelSize,
                    columns = columnCount ?: 1,
                    columnGapMm = columnGapMm ?: 0
                )
            }

            val printerCard: @Composable () -> Unit = {
                PrinterCard(
                    printers = printerState.printers,
                    selectedId = printerState.selectedId,
                    onSelect = onSelectPrinter,
                    onRefresh = onRefreshPrinters,
                    onTestPrint = testJob?.takeIf { printerState.canPrint }?.let { job -> { onTestPrint(job) } }
                )
            }

            val printButton: @Composable () -> Unit = {
                PrimaryButton(
                    text = when {
                        printerState.isPrinting -> "Printing..."
                        copyCount != null && copyCount > 0 ->
                            "Print $copyCount ${if (copyCount == 1) "label" else "labels"}"

                        else -> "Print"
                    },
                    enabled = canPrint,
                    loading = printerState.isPrinting,
                    onClick = {
                        onPrint(
                            LabelJob(
                                content = content!!,
                                widthMm = labelSize!!.widthMm,
                                heightMm = labelSize.heightMm,
                                copies = copyCount!!,
                                columns = columnCount!!,
                                columnGapMm = columnGapMm ?: LabelJob.DEFAULT_COLUMN_GAP_MM
                            )
                        )
                    }
                )
            }

            Column(Modifier.fillMaxSize()) {
                AppHeader(
                    title = "XPrinter Label",
                    statusText = if (printerState.printers.isNotEmpty()) "Printer ready" else "No printer",
                    statusOk = printerState.printers.isNotEmpty(),
                    horizontalPadding = sidePadding
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = sidePadding, vertical = if (wide) 28.dp else 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth()) {
                        if (wide) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                    typeTabs()
                                    detailsCard()
                                    layoutCard()
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                    previewCard()
                                    printerCard()
                                    printButton()
                                }
                            }
                        } else {
                            // The preview stays right under the tabs so it is seen while typing.
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                typeTabs()
                                previewCard()
                                detailsCard()
                                layoutCard()
                                printerCard()
                                printButton()
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Keeps an Expiry record across rotation and process death. */
private val ExpirySaver = listSaver<LabelContent.Expiry, Any>(
    save = { listOf(it.mrp, it.mfd, it.exp, it.batchNo, it.monthYearOnly) },
    restore = {
        LabelContent.Expiry(
            mrp = it[0] as String,
            mfd = it[1] as String,
            exp = it[2] as String,
            batchNo = it[3] as String,
            monthYearOnly = it[4] as Boolean
        )
    }
)

/** Keeps a [Party] across rotation and process death. */
private val PartySaver = listSaver<Party, String>(
    save = { listOf(it.name, it.address, it.postcodeCity, it.country, it.phone) },
    restore = { Party(it[0], it[1], it[2], it[3], it[4]) }
)

/** Keeps an [Order] across rotation and process death. */
private val OrderSaver = listSaver<Order, String>(
    save = { listOf(it.number, it.date, it.items, it.weight, it.cod) },
    restore = { Order(it[0], it[1], it[2], it[3], it[4]) }
)

/** What the preview shows for a blank Address form: every block, so the layout is visible. */
private val SampleAddress = LabelContent.Address(
    receiver = Party(
        name = "Receiver name",
        address = "Street address",
        postcodeCity = "POSTCODE CITY",
        country = "COUNTRY",
        phone = "+00 000 000 000"
    ),
    sender = Party(
        name = "Sender name",
        address = "Street address",
        postcodeCity = "POSTCODE CITY",
        country = "COUNTRY"
    ),
    order = Order(number = "0001", date = "dd/mm/yyyy", items = "1", weight = "0.0 kg")
)

/** The order details of an Address label: what is sent and how it is paid for. All optional. */
@Composable
private fun OrderCard(order: Order, onChange: (Order) -> Unit, wide: Boolean) {
    AppCard {
        CardTitle("Order details")
        val numberField: @Composable (Modifier) -> Unit = { fieldModifier ->
            LabeledTextField(
                label = "Order no",
                value = order.number,
                onValueChange = { onChange(order.copy(number = it)) },
                modifier = fieldModifier,
                placeholder = "e.g. 10234"
            )
        }
        val dateField: @Composable (Modifier) -> Unit = { fieldModifier ->
            DatePickerField("Order date", order.date, { onChange(order.copy(date = it)) }, fieldModifier)
        }
        val itemsField: @Composable (Modifier) -> Unit = { fieldModifier ->
            NumberField("Items", order.items, { onChange(order.copy(items = it)) }, fieldModifier)
        }
        val weightField: @Composable (Modifier) -> Unit = { fieldModifier ->
            LabeledTextField(
                label = "Weight",
                value = order.weight,
                onValueChange = { onChange(order.copy(weight = it)) },
                modifier = fieldModifier,
                placeholder = "e.g. 1.5 kg"
            )
        }
        // Two to a row only when there is room; stacked on a phone.
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                numberField(Modifier.weight(1f))
                dateField(Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                itemsField(Modifier.weight(1f))
                weightField(Modifier.weight(1f))
            }
        } else {
            numberField(Modifier)
            dateField(Modifier)
            itemsField(Modifier)
            weightField(Modifier)
        }
        LabeledTextField(
            label = "Cash on delivery (COD)",
            value = order.cod,
            onValueChange = { onChange(order.copy(cod = it)) },
            placeholder = "e.g. Rs. 2,500.00",
            optional = true
        )
        NoteBox("Optional. Empty fields are not printed. COD is the amount the courier collects.")
    }
}

/** The fields of one end of a parcel, in the order of a postal address (UPU S42). */
@Composable
private fun PartyCard(
    title: String,
    party: Party,
    onChange: (Party) -> Unit,
    wide: Boolean,
    note: String,
    /** When given, a Clear button empties the card; it shows only while there is something to clear. */
    onClear: (() -> Unit)? = null
) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardTitle(title, Modifier.weight(1f))
            if (onClear != null && !party.isEmpty) {
                GhostButton("Clear", onClear)
            }
        }
        LabeledTextField(
            label = "Name",
            value = party.name,
            onValueChange = { onChange(party.copy(name = it)) },
            placeholder = "Person or company"
        )
        LabeledTextField(
            label = "Address",
            value = party.address,
            onValueChange = { onChange(party.copy(address = it)) },
            placeholder = "Street, number, apartment",
            multiline = true
        )
        val postcodeCityField: @Composable (Modifier) -> Unit = { fieldModifier ->
            LabeledTextField(
                label = "Postcode and city",
                value = party.postcodeCity,
                onValueChange = { onChange(party.copy(postcodeCity = it)) },
                modifier = fieldModifier,
                placeholder = "e.g. 22767 Hamburg"
            )
        }
        val countryField: @Composable (Modifier) -> Unit = { fieldModifier ->
            LabeledTextField(
                label = "Country",
                value = party.country,
                onValueChange = { onChange(party.copy(country = it)) },
                modifier = fieldModifier,
                placeholder = "e.g. Germany"
            )
        }
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                postcodeCityField(Modifier.weight(1f))
                countryField(Modifier.weight(1f))
            }
        } else {
            postcodeCityField(Modifier)
            countryField(Modifier)
        }
        LabeledTextField(
            label = "Phone",
            value = party.phone,
            onValueChange = { onChange(party.copy(phone = it)) },
            placeholder = "+94 77 123 4567",
            optional = true,
            keyboardType = KeyboardType.Phone
        )
        NoteBox(note)
    }
}

/** How copies are spread over the columns, e.g. "9 labels = 5 rows of 2 (last row has 1)". */
private fun columnSummary(copies: Int, columns: Int): String {
    val rows = (copies + columns - 1) / columns
    val lastRow = copies - (rows - 1) * columns
    val base = "$copies labels = $rows rows of $columns"
    return if (lastRow == columns) base else "$base (last row has $lastRow)"
}

/**
 * The exact bitmap that will be printed, shown at the label's physical size on
 * screen (1 mm of label = 1 mm of screen). Rows wider than the screen scroll.
 * With several [columns] the whole row is shown, as the printer receives it.
 */
@Composable
private fun PreviewCard(
    content: LabelContent,
    size: LabelSize?,
    columns: Int,
    columnGapMm: Int
) {
    val palette = MaterialTheme.palette
    val metrics = LocalContext.current.resources.displayMetrics
    val density = LocalDensity.current

    val job = remember(content, size, columns, columnGapMm) {
        size?.let {
            LabelJob(content, it.widthMm, it.heightMm, columns = columns, columnGapMm = columnGapMm)
        }
    }
    val image = remember(job) { job?.let { LabelBitmapRenderer.render(it).asImageBitmap() } }
    // The platform's own mm unit uses the screen's real pixel density.
    fun mmToDp(mm: Int): Dp = with(density) {
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, mm.toFloat(), metrics).toDp()
    }

    AppCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardTitle("Preview")
            if (job != null && size != null) {
                Text(
                    text = if (columns == 1) {
                        "Actual size: ${size.displayCm}"
                    } else {
                        "$columns across, ${LabelSize(job.rowWidthMm, size.heightMm).displayCm} per row"
                    },
                    color = palette.mute,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(palette.stage)
        ) {
            if (job == null || image == null) {
                Text(
                    "Enter a valid label size to see the preview.",
                    color = palette.mute,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(24.dp)
                )
            } else {
                val viewportWidth = maxWidth - 32.dp
                Box(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                ) {
                    // Centres a narrow row; a wide one grows past the viewport and scrolls.
                    Box(Modifier.widthIn(min = viewportWidth), contentAlignment = Alignment.Center) {
                        Image(
                            bitmap = image,
                            contentDescription = "Label preview",
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .size(mmToDp(job.rowWidthMm), mmToDp(job.heightMm))
                                .shadow(3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrinterCard(
    printers: List<UsbPrinterInfo>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onRefresh: () -> Unit,
    /** Null while a test print is not possible (no printer, busy, or no valid label size). */
    onTestPrint: (() -> Unit)?
) {
    AppCard(spacing = 10.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CardTitle("USB printer", Modifier.weight(1f))
            GhostButton("Test print", onClick = { onTestPrint?.invoke() }, enabled = onTestPrint != null)
            GhostButton("Refresh", onRefresh)
        }
        if (printers.isEmpty()) {
            Text(
                "No USB printer found. Connect the printer with an OTG cable, then refresh.",
                color = MaterialTheme.palette.mute,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
        }
        printers.forEach { printer ->
            PrinterRow(
                name = printer.name,
                detail = "USB ${printer.id}",
                selected = printer.id == selectedId,
                onClick = { onSelect(printer.id) }
            )
        }
    }
}

/**
 * A date field that opens a date picker. The date is reported as dd/MM/yyyy, or
 * as an empty string when the field is cleared.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    label: String,
    value: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** Shows the date as MM/yyyy; the value itself keeps the day. */
    monthYearOnly: Boolean = false
) {
    var showPicker by remember { mutableStateOf(false) }

    DateSelectField(
        label = label,
        value = formatLabelDate(value, monthYearOnly),
        placeholder = "Tap to choose",
        onClick = { showPicker = true },
        onClear = { onDateSelected("") },
        modifier = modifier
    )

    if (showPicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    enabled = pickerState.selectedDateMillis != null,
                    onClick = {
                        pickerState.selectedDateMillis?.let { onDateSelected(formatDate(it)) }
                        showPicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** The date picker reports midnight UTC, so format in UTC to avoid an off-by-one day. */
private fun formatDate(utcMillis: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date(utcMillis))

@Composable
private fun DecimalField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LabeledTextField(
        label = label,
        value = value,
        onValueChange = { input ->
            // Digits plus at most one decimal separator, e.g. "4.5".
            val filtered = input.filter { it.isDigit() || it == '.' || it == ',' }
            if (filtered.count { it == '.' || it == ',' } <= 1) onValueChange(filtered.take(5))
        },
        modifier = modifier,
        keyboardType = KeyboardType.Decimal
    )
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LabeledTextField(
        label = label,
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(4)) },
        modifier = modifier,
        keyboardType = KeyboardType.Number
    )
}

@Preview(showBackground = true)
@Composable
private fun LabelScreenPreview() {
    XPrinterLabelTheme {
        LabelScreen(
            printerState = PrinterUiState(
                printers = listOf(UsbPrinterInfo("1-2", "XP-365B")),
                selectedId = "1-2"
            ),
            onRefreshPrinters = {},
            onSelectPrinter = {},
            onPrint = {},
            onTestPrint = {},
            onMessageShown = {}
        )
    }
}
