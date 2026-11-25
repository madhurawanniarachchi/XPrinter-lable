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
import com.kmpgaraj.xprinter_label.printer.LabelJob
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

    var title by rememberSaveable { mutableStateOf("") }
    var subtitle by rememberSaveable { mutableStateOf("") }
    var width by rememberSaveable { mutableStateOf("40") }
    var height by rememberSaveable { mutableStateOf("30") }
    var copies by rememberSaveable { mutableStateOf("1") }

    val widthMm = width.toIntOrNull()
    val heightMm = height.toIntOrNull()
    val copyCount = copies.toIntOrNull()
    val canPrint = printerState.canPrint && title.isNotBlank() &&
        widthMm != null && widthMm > 0 &&
        heightMm != null && heightMm > 0 &&
        copyCount != null && copyCount > 0

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
            Text("Preview", style = MaterialTheme.typography.titleMedium)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = title.ifBlank { "Label title" },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        if (subtitle.isNotBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = subtitle,
                onValueChange = { subtitle = it },
                label = { Text("Subtitle (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField("Width (mm)", width, { width = it }, Modifier.weight(1f))
                NumberField("Height (mm)", height, { height = it }, Modifier.weight(1f))
                NumberField("Copies", copies, { copies = it }, Modifier.weight(1f))
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
                            title = title.trim(),
                            subtitle = subtitle.trim(),
                            widthMm = widthMm!!,
                            heightMm = heightMm!!,
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
