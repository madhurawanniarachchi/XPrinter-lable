package com.kmpgaraj.xprinter_label

import android.content.Context
import androidx.core.content.edit
import com.kmpgaraj.xprinter_label.printer.Party

/**
 * Remembers the sender of Address labels between launches, so it does not have to be
 * typed again for every parcel. The details stay on this device, in the app's private
 * storage.
 */
class SenderStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** The sender saved last time, or an empty [Party] when none was saved. */
    fun load(): Party = Party(
        name = prefs.getString(KEY_NAME, "").orEmpty(),
        address = prefs.getString(KEY_ADDRESS, "").orEmpty(),
        postcodeCity = prefs.getString(KEY_POSTCODE_CITY, "").orEmpty(),
        country = prefs.getString(KEY_COUNTRY, "").orEmpty(),
        phone = prefs.getString(KEY_PHONE, "").orEmpty()
    )

    fun save(party: Party) {
        prefs.edit {
            putString(KEY_NAME, party.name)
            putString(KEY_ADDRESS, party.address)
            putString(KEY_POSTCODE_CITY, party.postcodeCity)
            putString(KEY_COUNTRY, party.country)
            putString(KEY_PHONE, party.phone)
        }
    }

    private companion object {
        const val FILE = "sender"
        const val KEY_NAME = "name"
        const val KEY_ADDRESS = "address"
        const val KEY_POSTCODE_CITY = "postcode_city"
        const val KEY_COUNTRY = "country"
        const val KEY_PHONE = "phone"
    }
}
