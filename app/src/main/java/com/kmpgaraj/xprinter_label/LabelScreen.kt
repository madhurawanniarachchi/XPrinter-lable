package com.kmpgaraj.xprinter_label

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.foundation.clickable
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.rememberDatePickerState
import com.kmpgaraj.xprinter_label.printer.LabelContent
import com.kmpgaraj.xprinter_label.printer.LabelJob
import com.kmpgaraj.xprinter_label.printer.LabelType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.kmpgaraj.xprinter_label.printer.LabelSize
import com.kmpgaraj.xprinter_label.printer.PrinterUiState
import com.kmpgaraj.xprinter_label.printer.UsbPrinterInfo
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kmpgaraj.xprinter_label.ui.theme.XPrinterLabelTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelScreen(
    printerState: PrinterUiState,
    onRefreshPrinters: () -> Unit,
    onSelectPrinter: (String) -> Unit,
    onPrint: (LabelJob) -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(printerState.message) {
        printerState.message?.let {
            snackbarHostState.showSnackbar(it)
            onMessageShown()
        }
    }

    var typeIndex by rememberSaveable { mutableStateOf(0) }
    val type = LabelType.entries[typeIndex]
    var productName by rememberSaveable { mutableStateOf("") }
    var expiryDate by rememberSaveable { mutableStateOf("") }
    var batch by rememberSaveable { mutableStateOf("") }
    var recipient by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    // Index into LabelSize.standard, or CUSTOM_SIZE for the typed-in size below.
    var sizeIndex by rememberSaveable {
        mutableStateOf(LabelSize.standard.indexOf(LabelSize.default))
    }
    var customWidthCm by rememberSaveable { mutableStateOf("4") }
    var customHeightCm by rememberSaveable { mutableStateOf("3") }
    var copies by rememberSaveable { mutableStateOf("1") }

    val isCustom = sizeIndex == CUSTOM_SIZE
    val labelSize: LabelSize? = if (isCustom) {
        val w = LabelSize.cmToMm(customWidthCm)
        val h = LabelSize.cmToMm(customHeightCm)
        if (w != null && h != null) LabelSize(w, h) else null
    } else {
        LabelSize.standard[sizeIndex]
    }
    val copyCount = copies.toIntOrNull()
    // Null until the required fields of the chosen label type are filled in.
    val content: LabelContent? = when (type) {
        LabelType.EXPIRY ->
            if (productName.isNotBlank() && expiryDate.isNotBlank()) {
                LabelContent.Expiry(productName.trim(), expiryDate, batch.trim())
            } else null

        LabelType.ADDRESS ->
            if (recipient.isNotBlank() && address.isNotBlank()) {
                LabelContent.Address(recipient.trim(), address.trim(), phone.trim())
            } else null
    }
    val canPrint = printerState.canPrint && content != null &&
        labelSize != null && copyCount != null && copyCount > 0

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar ={ TopAppBar(title = { Text("XPrinter Label") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LabelTypeSelector(
                selected = type,
                onSelect = { typeIndex = it.ordinal }
            )

            Text("Preview", style = MaterialTheme.typography.titleMedium)
            LabelPreview(
                content = content ?: when (type) {
                    LabelType.EXPIRY -> LabelContent.Expiry(
                        productName.ifBlank { "Product name" },
                        expiryDate.ifBlank { "dd/mm/yyyy" },
                        batch.trim()
                    )

                    LabelType.ADDRESS -> LabelContent.Address(
                        recipient.ifBlank { "Recipient name" },
                        address.ifBlank { "Address" },
                        phone.trim()
                    )
                }
            )

            when (type) {
                LabelType.EXPIRY -> {
                    OutlinedTextField(
                        value = productName,
                        onValueChange = { productName = it },
                        label = { Text("Product name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    DateField(
                        label = "Expiry date",
                        value = expiryDate,
                        onDateSelected = { expiryDate = it }
                    )
                    OutlinedTextField(
                        value = batch,
                        onValueChange = { batch = it },
                        label = { Text("Batch (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                LabelType.ADDRESS -> {
                    OutlinedTextField(
                        value = recipient,
                        onValueChange = { recipient = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone (optional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            SizeDropdown(
                selectedIndex = sizeIndex,
                onSelect = { sizeIndex = it }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isCustom) {
                    DecimalField("Width (cm)", customWidthCm, { customWidthCm = it }, Modifier.weight(1f))
                    DecimalField("Height (cm)", customHeightCm, { customHeightCm = it }, Modifier.weight(1f))
                }
                NumberField("Copies", copies, { copies = it }, Modifier.weight(1f))
            }
            if (isCustom && labelSize == null) {
                Text(
                    "Enter a size between ${LabelSize.MIN_MM / 10} and ${LabelSize.MAX_MM / 10} cm.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            PrinterPicker(
                printers = printerState.printers,
                selectedId = printerState.selectedId,
                onSelect = onSelectPrinter,
                onRefresh = onRefreshPrinters
            )

            Button(
                onClick = {
                    onPrint(
                        LabelJob(
                            content = content!!,
                            widthMm = labelSize!!.widthMm,
                            heightMm = labelSize.heightMm,
                            copies = copyCount!!
                        )
                    )
                },
                enabled = canPrint,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (printerState.isPrinting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (printerState.isPrinting) "Printing..." else "Print")
            }
        }
    }
}

@Composable
private fun PrinterPicker(
    printers: List<UsbPrinterInfo>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "USB printer",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onRefresh) { Text("Refresh") }
            }
            if (printers.isEmpty()) {
                Text(
                    "No USB printer found. Connect the printer with an OTG cable, then refresh.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            printers.forEach { printer ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = printer.id == selectedId,
                            role = Role.RadioButton,
                            onClick = { onSelect(printer.id) }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = printer.id == selectedId, onClick = null)
                    Text(printer.name, modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp))
                }
            }
        }
    }
}

private const val CUSTOM_SIZE = -1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabelTypeSelector(selected: LabelType, onSelect: (LabelType) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        LabelType.entries.forEachIndexed { index, type ->
            SegmentedButton(
                selected = type == selected,
                onClick = { onSelect(type) },
                shape = SegmentedButtonDefaults.itemShape(index, LabelType.entries.size)
            ) {
                Text(type.displayName)
            }
        }
    }
}

/** On-screen approximation of the printed label. */
@Composable
private fun LabelPreview(content: LabelContent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = when (content) {
                is LabelContent.Expiry -> Alignment.CenterHorizontally
                is LabelContent.Address -> Alignment.Start
            },
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            when (content) {
                is LabelContent.Expiry -> {
                    Text(
                        content.productName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text("EXPIRY DATE", style = MaterialTheme.typography.labelMedium)
                    Text(
                        content.expiryDate,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (content.batch.isNotBlank()) {
                        Text("Batch: ${content.batch}", style = MaterialTheme.typography.labelMedium)
                    }
                }

                is LabelContent.Address -> {
                    Text(
                        content.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(content.address, style = MaterialTheme.typography.bodyMedium)
                    if (content.phone.isNotBlank()) {
                        Text(
                            "Tel: ${content.phone}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/** Read-only field that opens a date picker; reports the date as dd/MM/yyyy. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, value: String, onDateSelected: (String) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text("Tap to choose") },
            modifier = Modifier.fillMaxWidth()
        )
        // A read-only text field swallows taps, so catch them on a layer above it.
        Box(
            Modifier
                .matchParentSize()
                .clickable { showPicker = true }
        )
    }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SizeDropdown(selectedIndex: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText = if (selectedIndex == CUSTOM_SIZE) {
        "Custom size"
    } else {
        LabelSize.standard[selectedIndex].displayCm
    }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Label size (width x height)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            LabelSize.standard.forEachIndexed { index, size ->
                DropdownMenuItem(
                    text = { Text(size.displayCm) },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Custom size") },
                onClick = {
                    onSelect(CUSTOM_SIZE)
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun DecimalField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            // Digits plus at most one decimal separator, e.g. "4.5".
            val filtered = input.filter { it.isDigit() || it == '.' || it == ',' }
            if (filtered.count { it == '.' || it == ',' } <= 1) onValueChange(filtered.take(5))
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(4)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
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
            onMessageShown = {}
        )
    }
}
