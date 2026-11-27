package com.kmpgaraj.xprinter_label

import android.content.Context
import androidx.core.content.edit
import com.kmpgaraj.xprinter_label.printer.LabelContent

/**
 * Remembers the last Expiry label record (price, dates, batch number and the
 * month/year option) between launches, so the next label starts from it. The
 * record stays on this device, in the app's private storage.
 */
class ExpiryStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** The record saved last time, or an empty one when none was saved. */
    fun load(): LabelContent.Expiry = LabelContent.Expiry(
        mrp = prefs.getString(KEY_MRP, "").orEmpty(),
        mfd = prefs.getString(KEY_MFD, "").orEmpty(),
        exp = prefs.getString(KEY_EXP, "").orEmpty(),
        batchNo = prefs.getString(KEY_BATCH_NO, "").orEmpty(),
        monthYearOnly = prefs.getBoolean(KEY_MONTH_YEAR_ONLY, false)
    )

    fun save(record: LabelContent.Expiry) {
        prefs.edit {
            putString(KEY_MRP, record.mrp)
            putString(KEY_MFD, record.mfd)
            putString(KEY_EXP, record.exp)
            putString(KEY_BATCH_NO, record.batchNo)
            putBoolean(KEY_MONTH_YEAR_ONLY, record.monthYearOnly)
        }
    }

    private companion object {
        const val FILE = "expiry"
        const val KEY_MRP = "mrp"
        const val KEY_MFD = "mfd"
        const val KEY_EXP = "exp"
        const val KEY_BATCH_NO = "batch_no"
        const val KEY_MONTH_YEAR_ONLY = "month_year_only"
    }
}
