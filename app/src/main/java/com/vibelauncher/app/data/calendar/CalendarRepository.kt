package com.vibelauncher.app.data.calendar

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Android has no unified system provider for "reminders/tasks" distinct from calendar
 * events - only CalendarContract. So an item like "renew my passport" is just an
 * ordinary (possibly all-day) calendar event the user created themselves. All-day
 * events are surfaced separately (see [DayEvents.allDayEvents]) and treated as "tasks"
 * in the UI. A real Tasks/Reminders integration would need a separate, app-specific
 * API and is out of scope.
 */
class CalendarRepository(private val context: Context) {

    /** Timed events and all-day events for the given day, both sorted by start time. */
    fun getEventsForDay(dayOffset: Int): DayEvents {
        val zone = ZoneId.systemDefault()
        val day = LocalDate.now(zone).plusDays(dayOffset.toLong())
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, start)
        ContentUris.appendId(builder, end)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY
        )

        val timed = mutableListOf<CalendarEvent>()
        val allDay = mutableListOf<CalendarEvent>()
        context.contentResolver.query(
            builder.build(),
            projection,
            null,
            null,
            "${CalendarContract.Instances.BEGIN} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
            val titleCol = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
            val beginCol = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
            val endCol = cursor.getColumnIndex(CalendarContract.Instances.END)
            val allDayCol = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)

            while (cursor.moveToNext()) {
                val isAllDay = cursor.getInt(allDayCol) != 0
                val event = CalendarEvent(
                    id = cursor.getLong(idCol),
                    title = cursor.getString(titleCol) ?: "(untitled)",
                    startMillis = cursor.getLong(beginCol),
                    endMillis = cursor.getLong(endCol),
                    isAllDay = isAllDay
                )
                if (isAllDay) allDay += event else timed += event
            }
        }
        return DayEvents(timedEvents = timed, allDayEvents = allDay)
    }
}

data class DayEvents(
    val timedEvents: List<CalendarEvent>,
    val allDayEvents: List<CalendarEvent>
)

/** Compact "time until start" label matching the reference design's bare-number badges. */
fun CalendarEvent.timeUntilLabel(nowMillis: Long = System.currentTimeMillis()): String {
    val diff = (startMillis - nowMillis).coerceAtLeast(0)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        else -> "${days}d"
    }
}
