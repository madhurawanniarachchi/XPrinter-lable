package com.kmpgaraj.xprinter_label.printer

import android.app.Application
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

    fun print(job: LabelJob) {
        val printerId = _state.value.selectedId ?: return
        if (_state.value.isPrinting) return

        _state.update { it.copy(isPrinting = true) }
        viewModelScope.launch {
            val message = try {
                service.print(printerId, job)
                "Label sent to printer"
            } catch (e: PrinterException) {
                e.message ?: "Print failed"
            }
            _state.update { it.copy(isPrinting = false, message = message) }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}
