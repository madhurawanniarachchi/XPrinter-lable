package com.kmpgaraj.xprinter_label.printer

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import android.util.Log
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
    suspend fun print(printerId: String, job: LabelJob) {
        Log.i(
            TAG,
            "print: printer=$printerId label=${job.widthMm}x${job.heightMm}mm columns=${job.columns} " +
                "columnGap=${job.columnGapMm}mm rowGap=${job.gapMm}mm copies=${job.copies} " +
                "-> rowWidth=${job.rowWidthMm}mm rows=${job.rows} lastRowLabels=${job.lastRowLabels} " +
                "content=${job.content}"
        )
        withPrinter(printerId, "print") { connection ->
            withContext(Dispatchers.IO) {
                val printer = newPrinter(connection, job)

                val hasShortLastRow = job.lastRowLabels < job.columns
                val fullRows = if (hasShortLastRow) job.rows - 1 else job.rows
                Log.d(TAG, "print: fullRows=$fullRows hasShortLastRow=$hasShortLastRow")
                if (fullRows > 0) {
                    printRows(printer, LabelBitmapRenderer.render(job), fullRows)
                }
                if (hasShortLastRow) {
                    printRows(printer, LabelBitmapRenderer.render(job, job.lastRowLabels), 1)
                }
            }
        }
    }

    /**
     * Prints three rows that tell a blank result apart from a faulty one:
     *  A: a border and text drawn by the printer itself, at its own darkness setting
     *  B: the same bitmap path real labels use
     *  C: the printer's own graphics again, at maximum darkness
     *
     * Row C changes the printer's darkness; it is put back to the TSPL default
     * (8) afterwards.
     */
    suspend fun printTest(printerId: String, job: LabelJob) {
        Log.i(TAG, "test print: printer=$printerId row=${job.rowWidthMm}x${job.heightMm}mm")
        withPrinter(printerId, "test print") { connection ->
            withContext(Dispatchers.IO) {
                val printer = newPrinter(connection, job)
                val width = job.rowWidthMm * LabelBitmapRenderer.DOTS_PER_MM
                val height = job.heightMm * LabelBitmapRenderer.DOTS_PER_MM

                Log.d(TAG, "test A: printer's own graphics, default darkness")
                printBuiltInPattern(printer, width, height, "A: DEFAULT")

                Log.d(TAG, "test B: label bitmap")
                val bitmapJob = job.copy(
                    content = LabelContent.Expiry(batchNo = "B: BITMAP", mrp = "TEST"),
                    copies = 1
                )
                printRows(printer, LabelBitmapRenderer.render(bitmapJob), 1)

                Log.d(TAG, "test C: printer's own graphics, darkness 15")
                printer.density(15)
                printBuiltInPattern(printer, width, height, "C: DENSITY 15")
                printer.density(DEFAULT_DENSITY)
            }
        }
    }

    /** Connects, runs [block], waits for the printer to drain, then checks its status and disconnects. */
    private suspend fun withPrinter(
        printerId: String,
        what: String,
        block: suspend (IDeviceConnection) -> Unit
    ) = printMutex.withLock {
        val startedAt = SystemClock.elapsedRealtime()
        val device = discovery.find(printerId)
        if (device == null) {
            Log.e(TAG, "$what: printer $printerId not found; attached printers=${discovery.listPrinters()}")
            throw PrinterException("Printer not found. Check the USB cable and refresh.")
        }
        Log.d(TAG, "$what: found ${device.deviceName} vid=${device.vendorId} pid=${device.productId}")
        if (!discovery.ensurePermission(device)) {
            Log.e(TAG, "$what: USB permission denied for ${device.deviceName}")
            throw PrinterException("USB permission denied.")
        }

        val connection = try {
            connect(device.deviceName)
        } catch (e: PrinterException) {
            Log.e(TAG, "$what: connect failed: ${e.message}", e)
            throw e
        }
        Log.d(TAG, "$what: connected in ${SystemClock.elapsedRealtime() - startedAt} ms")
        try {
            block(connection)
            // Give the printer time to drain its buffer before the link is closed.
            delay(FLUSH_DELAY_MS)
            checkPrinterStatus(connection)
            Log.i(TAG, "$what: done in ${SystemClock.elapsedRealtime() - startedAt} ms")
        } catch (e: PrinterException) {
            Log.e(TAG, "$what: failed: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "$what: failed with ${e.javaClass.simpleName}: ${e.message}", e)
            throw PrinterException(e.message ?: "Print failed")
        } finally {
            connection.close()
            Log.d(TAG, "$what: connection closed")
        }
    }

    /**
     * The printer sees one row across the whole roll, however many labels sit in
     * it, so SIZE is the full row width. Copies are spread over the columns; GAP
     * is only the gap between rows.
     */
    private fun newPrinter(connection: IDeviceConnection, job: LabelJob): TSPLPrinter {
        Log.d(TAG, "TSPL: SIZE ${job.rowWidthMm} mm,${job.heightMm} mm; GAP ${job.gapMm} mm,0 mm; DIRECTION forward")
        return TSPLPrinter(connection)
            .sizeMm(job.rowWidthMm.toDouble(), job.heightMm.toDouble())
            .gapMm(job.gapMm, 0.0)
            .direction(TSPLConst.DIRECTION_FORWARD)
    }

    /**
     * Asks the printer for its status and fails the job when it reports a problem,
     * so the user sees why nothing came out instead of "label sent". A printer that
     * does not answer is not treated as an error, because some models stay silent.
     */
    private suspend fun checkPrinterStatus(connection: IDeviceConnection) {
        val reply = queryRaw(connection, STATUS_QUERY)
        if (reply == null || reply.size != 1) {
            val what = if (reply == null || reply.isEmpty()) "no answer" else "unexpected reply ${hex(reply)}"
            Log.w(TAG, "printer status: $what")
            return
        }
        val code = reply[0].toInt() and 0xFF
        val problems = STATUS_FLAGS.filter { (bit, _) -> code and bit != 0 }.map { (_, text) -> text }
        val printing = if (code and STATUS_PRINTING != 0) " (printing)" else ""
        Log.i(TAG, "printer status: 0x%02x %s$printing".format(code, problems.ifEmpty { listOf("ok") }.joinToString()))
        if (problems.isNotEmpty()) {
            throw PrinterException(
                "Printer reports: ${problems.joinToString()}. Check the label roll, label sensor and cover."
            )
        }
    }

    private suspend fun queryRaw(connection: IDeviceConnection, command: ByteArray): ByteArray? =
        withContext(Dispatchers.IO) {
            connection.sendData(command)
            connection.readSync(REPLY_TIMEOUT_MS.toInt())
        }

    private fun hex(bytes: ByteArray) = bytes.joinToString(" ") { "%02x".format(it) }

    private fun darkPixelCount(bitmap: Bitmap): Int {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels.count { Color.luminance(it) < 0.5f }
    }

    private fun printRows(printer: TSPLPrinter, row: Bitmap, rows: Int) {
        val dark = darkPixelCount(row)
        Log.d(
            TAG,
            "TSPL: CLS; BITMAP ${row.width}x${row.height} dots " +
                "($dark dark pixels = ${dark * 100 / (row.width * row.height)}%); PRINT $rows"
        )
        printer.cls()
            .bitmap(0, 0, TSPLConst.BMP_MODE_OVERWRITE, row.width, row, AlgorithmType.Threshold)
            .print(rows)
    }

    /** A border, a title in the printer's own font and a solid bar, drawn by the printer instead of from a bitmap. */
    private fun printBuiltInPattern(printer: TSPLPrinter, width: Int, height: Int, title: String) {
        Log.d(TAG, "TSPL: CLS; BOX; TEXT \"$title\"; BAR; PRINT 1 (${width}x$height dots)")
        printer.cls()
            .box(2, 2, width - 3, height - 3, 4)
            .text(16, 16, "3", 0, 2, 2, title)
            .bar(16, height - 36, width - 32, 12)
            .print(1)
    }

    private suspend fun connect(target: String): IDeviceConnection =
        suspendCancellableCoroutine { continuation ->
            POSConnect.init(appContext)
            // In debug builds the SDK logs every command it sends (hex, tag "PosLog").
            POSConnect.openLog(appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
            val connection = POSConnect.createDevice(POSConnect.DEVICE_TYPE_USB)
            continuation.invokeOnCancellation { connection.close() }

            Log.d(TAG, "connect: opening $target")
            connection.connect(target, IConnectListener { code, _, message ->
                Log.d(TAG, "connect: listener code=$code message=$message")
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
        const val TAG = "XPrinterLabel"
        const val FLUSH_DELAY_MS = 1000L
        const val REPLY_TIMEOUT_MS = 2000L

        /** The TSPL default print darkness. */
        const val DEFAULT_DENSITY = 8

        /** TSPL real-time status request (ESC !?); the printer answers with one status byte. */
        val STATUS_QUERY = byteArrayOf(0x1B, 0x21, 0x3F)

        /** Status bits that mean the printer cannot print. */
        val STATUS_FLAGS = listOf(
            0x01 to "cover open",
            0x02 to "label jam or gap not found",
            0x04 to "out of paper",
            0x08 to "out of ribbon",
            0x10 to "paused",
            0x80 to "other error"
        )

        /** Set while the printer is busy printing; not a problem. */
        const val STATUS_PRINTING = 0x20
    }
}
