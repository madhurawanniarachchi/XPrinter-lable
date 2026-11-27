package com.kmpgaraj.xprinter_label.printer

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PrinterUiState(
    val printers: List<UsbPrinterInfo> = emptyList(),
    val selectedId: String? = null,
    val isPrinting: Boolean = false,
    /** One-shot message for a snackbar; clear with [PrinterViewModel.messageShown]. */
    val message: String? = null
) {
    val canPrint: Boolean get() = selectedId != null && !isPrinting
}

class PrinterViewModel(application: Application) : AndroidViewModel(application) {

    private val service = XPrinterLabelService(application)

    private val _state = MutableStateFlow(PrinterUiState())
    val state: StateFlow<PrinterUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val printers = service.listPrinters()
        _state.update { current ->
            val selected = current.selectedId?.takeIf { id -> printers.any { it.id == id } }
                ?: printers.singleOrNull()?.id
            current.copy(printers = printers, selectedId = selected)
        }
    }

    fun select(id: String) = _state.update { it.copy(selectedId = id) }

    fun print(job: LabelJob) =
        launchJob("print", "Label sent to printer") { printerId -> service.print(printerId, job) }

    /** Prints the diagnostic rows A, B and C, to find out why labels come out blank. */
    fun printTest(job: LabelJob) =
        launchJob("test print", "Test rows A, B and C sent to printer") { printerId ->
            service.printTest(printerId, job)
        }

    private fun launchJob(what: String, successMessage: String, job: suspend (printerId: String) -> Unit) {
        val printerId = _state.value.selectedId
        if (printerId == null || _state.value.isPrinting) {
            Log.w(TAG, "$what: ignored (selectedId=$printerId, isPrinting=${_state.value.isPrinting})")
            return
        }

        _state.update { it.copy(isPrinting = true) }
        viewModelScope.launch {
            val message = try {
                job(printerId)
                Log.i(TAG, "$what: finished without error")
                successMessage
            } catch (e: PrinterException) {
                Log.w(TAG, "$what: finished with error: ${e.message}")
                e.message ?: "Print failed"
            }
            _state.update { it.copy(isPrinting = false, message = message) }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }

    private companion object {
        const val TAG = "XPrinterLabel"
    }
}
