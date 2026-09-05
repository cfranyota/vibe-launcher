package com.vibelauncher.app.data.contacts

import android.content.Context
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils

private const val MAX_RESULTS = 5

/** Used only by Vibe Bar's '@'/'#' actions to search contacts as the user types a name
 *  after the prefix. Read-only - never writes to the contacts provider. */
class ContactsRepository(private val context: Context) {

    fun searchContacts(query: String): List<ContactResult> {
        if (query.isBlank()) return emptyList()
        return runCatching { queryContacts(query) }.getOrDefault(emptyList())
    }

    /** Reverse (number -> name) lookup, for Hub's message rows - the forward search above
     *  matches by name prefix, which is the opposite direction. */
    fun nameForNumber(phone: String): String? {
        if (phone.isBlank()) return null
        return runCatching {
            val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon()
                .appendPath(phone)
                .build()
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    private fun queryContacts(query: String): List<ContactResult> {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} LIKE ?"
        val selectionArgs = arrayOf("$query%")

        val results = mutableListOf<ContactResult>()
        val seen = mutableSetOf<String>()

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
            val numberCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val typeCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
            val labelCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)

            while (cursor.moveToNext() && results.size < MAX_RESULTS) {
                val contactId = cursor.getLong(idCol)
                val number = cursor.getString(numberCol) ?: continue
                val dedupeKey = "$contactId:${PhoneNumberUtils.normalizeNumber(number)}"
                if (!seen.add(dedupeKey)) continue

                val type = cursor.getInt(typeCol)
                val customLabel = cursor.getString(labelCol)
                val phoneLabel = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                    context.resources, type, customLabel
                ).toString()

                results += ContactResult(
                    contactId = contactId,
                    name = cursor.getString(nameCol) ?: number,
                    phone = number,
                    phoneLabel = phoneLabel
                )
            }
        }
        return results
    }
}
