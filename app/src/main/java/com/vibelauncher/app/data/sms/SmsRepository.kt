package com.vibelauncher.app.data.sms

import android.content.ContentValues
import android.content.Context
import android.provider.Telephony

data class SmsMessageItem(
    val id: String,
    val address: String,
    val body: String,
    val isIncoming: Boolean,
    val timestampMillis: Long
)

/** Read/write SMS access - only meaningful once this app holds the default-SMS-app role
 *  (RoleManager.ROLE_SMS). As the default handler, the OS delivers incoming messages via
 *  SMS_DELIVER (see SmsDeliverReceiver) rather than writing them to the provider itself -
 *  this app is responsible for persisting them, which [recordIncoming] does. */
class SmsRepository(private val context: Context) {

    fun recentMessages(limit: Int = 200): List<SmsMessageItem> {
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.TYPE,
            Telephony.Sms.DATE
        )
        val results = mutableListOf<SmsMessageItem>()
        runCatching {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT $limit"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(Telephony.Sms._ID)
                val addressCol = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyCol = cursor.getColumnIndex(Telephony.Sms.BODY)
                val typeCol = cursor.getColumnIndex(Telephony.Sms.TYPE)
                val dateCol = cursor.getColumnIndex(Telephony.Sms.DATE)
                while (cursor.moveToNext()) {
                    results += SmsMessageItem(
                        id = cursor.getString(idCol) ?: continue,
                        address = cursor.getString(addressCol) ?: "",
                        body = cursor.getString(bodyCol) ?: "",
                        isIncoming = cursor.getInt(typeCol) == Telephony.Sms.MESSAGE_TYPE_INBOX,
                        timestampMillis = cursor.getLong(dateCol)
                    )
                }
            }
        }
        return results
    }

    /** Most recent messages exchanged with a single [address] - used for the Hub reply
     *  bubble's read-only "recent context" preview. */
    fun recentMessagesFor(address: String, limit: Int = 3): List<SmsMessageItem> {
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.TYPE,
            Telephony.Sms.DATE
        )
        val results = mutableListOf<SmsMessageItem>()
        runCatching {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                "${Telephony.Sms.ADDRESS} = ?",
                arrayOf(address),
                "${Telephony.Sms.DATE} DESC LIMIT $limit"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(Telephony.Sms._ID)
                val addressCol = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyCol = cursor.getColumnIndex(Telephony.Sms.BODY)
                val typeCol = cursor.getColumnIndex(Telephony.Sms.TYPE)
                val dateCol = cursor.getColumnIndex(Telephony.Sms.DATE)
                while (cursor.moveToNext()) {
                    results += SmsMessageItem(
                        id = cursor.getString(idCol) ?: continue,
                        address = cursor.getString(addressCol) ?: "",
                        body = cursor.getString(bodyCol) ?: "",
                        isIncoming = cursor.getInt(typeCol) == Telephony.Sms.MESSAGE_TYPE_INBOX,
                        timestampMillis = cursor.getLong(dateCol)
                    )
                }
            }
        }
        return results.sortedBy { it.timestampMillis }
    }

    /** Called from [com.vibelauncher.app.service.sms.SmsDeliverReceiver] for each incoming
     *  message - as the default SMS app, the platform does NOT write this to the provider
     *  automatically, this app must. */
    fun recordIncoming(address: String, body: String, timestampMillis: Long) {
        runCatching {
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, timestampMillis)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
                put(Telephony.Sms.READ, 0)
            }
            context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
        }
    }

    /** Called after a successful outgoing send (e.g. from ComposeSmsActivity or Vibe Bar's
     *  '@' flow) so sent messages also show up in Hub/the provider, matching what any real
     *  default SMS app does. */
    fun recordSent(address: String, body: String, timestampMillis: Long = System.currentTimeMillis()) {
        runCatching {
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, timestampMillis)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
                put(Telephony.Sms.READ, 1)
            }
            context.contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
        }
    }
}
