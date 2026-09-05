package com.vibelauncher.app.data.calls

import android.content.Context
import android.provider.CallLog
import android.util.Log

enum class CallLogType { INCOMING, OUTGOING, MISSED, OTHER }

data class CallLogItem(
    val id: String,
    val number: String,
    val cachedName: String?,
    val type: CallLogType,
    val durationSeconds: Int,
    val timestampMillis: Long
)

/** Read-only - Hub's call feed. Requires READ_CALL_LOG (requested at runtime, first time
 *  Hub is opened). */
class CallLogRepository(private val context: Context) {

    fun recentCalls(limit: Int = 200): List<CallLogItem> {
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DURATION,
            CallLog.Calls.DATE
        )
        val results = mutableListOf<CallLogItem>()
        runCatching {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(CallLog.Calls._ID)
                val numberCol = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val nameCol = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeCol = cursor.getColumnIndex(CallLog.Calls.TYPE)
                val durationCol = cursor.getColumnIndex(CallLog.Calls.DURATION)
                val dateCol = cursor.getColumnIndex(CallLog.Calls.DATE)
                while (cursor.moveToNext()) {
                    results += CallLogItem(
                        id = cursor.getString(idCol) ?: continue,
                        number = cursor.getString(numberCol) ?: "",
                        cachedName = cursor.getString(nameCol),
                        type = when (cursor.getInt(typeCol)) {
                            CallLog.Calls.INCOMING_TYPE -> CallLogType.INCOMING
                            CallLog.Calls.OUTGOING_TYPE -> CallLogType.OUTGOING
                            CallLog.Calls.MISSED_TYPE -> CallLogType.MISSED
                            else -> CallLogType.OTHER
                        },
                        durationSeconds = cursor.getInt(durationCol),
                        timestampMillis = cursor.getLong(dateCol)
                    )
                }
            }
        }.onFailure { Log.w("CallLogRepository", "recentCalls query failed", it) }
        return results.take(limit)
    }
}
