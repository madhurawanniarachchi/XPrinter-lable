package com.kmpgaraj.xprinter_label.printer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Finds attached USB printers and obtains the runtime permission to talk to them. */
class UsbPrinterDiscovery(context: Context) {

    private val appContext = context.applicationContext
    private val usbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager

    /** Attached devices that expose a printer interface (USB class 7). */
    fun listPrinters(): List<UsbPrinterInfo> {
        val devices = usbManager.deviceList.values
        // Log every attached USB device, so a printer that is missing from the list can be told apart
        // from one that is not detected at all.
        Log.d(
            TAG,
            "listPrinters: ${devices.size} USB device(s): " + devices.joinToString { device ->
                "${device.deviceName} vid=${device.vendorId} pid=${device.productId} " +
                    "class=${device.deviceClass} interfaces=" +
                    (0 until device.interfaceCount).map { device.getInterface(it).interfaceClass } +
                    " permission=${usbManager.hasPermission(device)}"
            }
        )
        return devices
            .filter(::isPrinter)
            .map { UsbPrinterInfo(id = idOf(it), name = displayName(it)) }
            .also { Log.d(TAG, "listPrinters: printers=$it") }
    }

    fun find(id: String): UsbDevice? =
        usbManager.deviceList.values.firstOrNull { isPrinter(it) && idOf(it) == id }

    /** Returns true when permission is already held or the user grants it. */
    suspend fun ensurePermission(device: UsbDevice): Boolean {
        if (usbManager.hasPermission(device)) return true
        Log.d(TAG, "ensurePermission: asking the user for access to ${device.deviceName}")

        return suspendCancellableCoroutine { continuation ->
            val action = "${appContext.packageName}.USB_PERMISSION"
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action != action) return
                    runCatching { appContext.unregisterReceiver(this) }
                    if (continuation.isActive) continuation.resume(usbManager.hasPermission(device))
                }
            }
            ContextCompat.registerReceiver(
                appContext, receiver, IntentFilter(action), ContextCompat.RECEIVER_NOT_EXPORTED
            )
            continuation.invokeOnCancellation {
                runCatching { appContext.unregisterReceiver(receiver) }
            }

            // The system fills in the result extras, so the intent must be mutable (API 31+).
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val pendingIntent = PendingIntent.getBroadcast(
                appContext, 0, Intent(action).setPackage(appContext.packageName), flags
            )
            usbManager.requestPermission(device, pendingIntent)
        }
    }

    private companion object {
        const val TAG = "XPrinterLabel"
    }

    private fun isPrinter(device: UsbDevice): Boolean =
        device.deviceClass == UsbConstants.USB_CLASS_PRINTER ||
            (0 until device.interfaceCount).any {
                device.getInterface(it).interfaceClass == UsbConstants.USB_CLASS_PRINTER
            }

    private fun idOf(device: UsbDevice) = "${device.productId}-${device.vendorId}"

    private fun displayName(device: UsbDevice): String {
        val product = runCatching { device.productName }.getOrNull()
        return product?.takeIf { it.isNotBlank() } ?: "USB printer (${idOf(device)})"
    }
}
