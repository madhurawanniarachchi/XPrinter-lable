package com.kmpgaraj.xprinter_label.printer

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.posprinter.IConnectListener
import net.posprinter.IDeviceConnection
import net.posprinter.POSConnect
import net.posprinter.TSPLConst
import net.posprinter.TSPLPrinter
import net.posprinter.model.AlgorithmType
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PrinterException(message: String) : IOException(message)

/**
 * Prints labels on an Xprinter TSPL label printer over USB.
 *
 * Each call connects, prints and disconnects. Jobs are serialised with a mutex,
 * so concurrent callers queue up instead of fighting over the USB endpoint.
 */
class XPrinterLabelService(context: Context) {

    private val appContext = context.applicationContext
    private val discovery = UsbPrinterDiscovery(appContext)
    private val printMutex = Mutex()

    fun listPrinters(): List<UsbPrinterInfo> = discovery.listPrinters()

    /** @throws PrinterException with a user-readable message when anything goes wrong. */
    suspend fun print(printerId: String, job: LabelJob) = printMutex.withLock {
        val device = discovery.find(printerId)
            ?: throw PrinterException("Printer not found. Check the USB cable and refresh.")
        if (!discovery.ensurePermission(device)) {
            throw PrinterException("USB permission denied.")
        }

        val connection = connect(device.deviceName)
        try {
            withContext(Dispatchers.IO) {
                val bitmap = LabelBitmapRenderer.render(job)
                TSPLPrinter(connection)
                    .sizeMm(job.widthMm.toDouble(), job.heightMm.toDouble())
                    .gapMm(job.gapMm, 0.0)
                    .direction(TSPLConst.DIRECTION_FORWARD)
                    .cls()
                    .bitmap(0, 0, TSPLConst.BMP_MODE_OVERWRITE, bitmap.width, bitmap, AlgorithmType.Threshold)
                    .print(job.copies)
            }
            // Give the printer time to drain its buffer before the link is closed.
            delay(FLUSH_DELAY_MS)
        } catch (e: PrinterException) {
            throw e
        } catch (e: Exception) {
            throw PrinterException(e.message ?: "Print failed")
        } finally {
            connection.close()
        }
    }

    private suspend fun connect(target: String): IDeviceConnection =
        suspendCancellableCoroutine { continuation ->
            POSConnect.init(appContext)
            val connection = POSConnect.createDevice(POSConnect.DEVICE_TYPE_USB)
            continuation.invokeOnCancellation { connection.close() }

            connection.connect(target, IConnectListener { code, _, message ->
                when (code) {
                    POSConnect.CONNECT_SUCCESS ->
                        if (continuation.isActive) continuation.resume(connection)

                    POSConnect.CONNECT_FAIL, POSConnect.CONNECT_INTERRUPT -> {
                        connection.close()
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                PrinterException(message ?: "Unable to connect to printer")
                            )
                        }
                    }
                }
            })
        }

    private companion object {
        const val FLUSH_DELAY_MS = 1000L
    }
}
