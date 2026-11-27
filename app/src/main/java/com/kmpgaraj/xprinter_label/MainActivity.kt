package com.kmpgaraj.xprinter_label

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmpgaraj.xprinter_label.printer.PrinterViewModel
import com.kmpgaraj.xprinter_label.ui.theme.XPrinterLabelTheme

class MainActivity : ComponentActivity() {

    private val printerViewModel: PrinterViewModel by viewModels()
    private val senderStore by lazy { SenderStore(this) }
    private val expiryStore by lazy { ExpiryStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val printerState by printerViewModel.state.collectAsStateWithLifecycle()
            XPrinterLabelTheme {
                LabelScreen(
                    printerState = printerState,
                    onRefreshPrinters = printerViewModel::refresh,
                    onSelectPrinter = printerViewModel::select,
                    onPrint = printerViewModel::print,
                    onTestPrint = printerViewModel::printTest,
                    initialSender = remember { senderStore.load() },
                    onSenderChanged = senderStore::save,
                    initialExpiry = remember { expiryStore.load() },
                    onExpiryChanged = expiryStore::save,
                    onMessageShown = printerViewModel::messageShown
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // A cable may have been plugged in while the app was in the background.
        printerViewModel.refresh()
    }
}
