package com.vibelauncher.app.data.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone
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

    /**
     * Writes an event straight into the user's own calendar - Vibe Bar's '*' command creates
     * events silently rather than handing off to the Calendar app's add-event form. Needs
     * WRITE_CALENDAR (plus READ_CALENDAR, to find a calendar to write into); returns false
     * if either is missing, no writable calendar exists, or the insert fails.
     *
     * All-day events must carry UTC-midnight timestamps and a UTC timezone - that's
     * CalendarContract's storage contract for them, not a display choice (see
     * [com.vibelauncher.app.features.vibebar.parseEventText], which builds them that way).
     */
    fun insertEvent(title: String, startMillis: Long, endMillis: Long, allDay: Boolean): Boolean {
        val calendarId = writableCalendarId() ?: return false
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.ALL_DAY, if (allDay) 1 else 0)
            put(
                CalendarContract.Events.EVENT_TIMEZONE,
                if (allDay) "UTC" else TimeZone.getDefault().id
            )
        }
        return runCatching {
            context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        }.getOrNull() != null
    }

    /** The calendar new events land in: the primary one where there is one, otherwise the
     *  first the user can actually write to (read-only subscribed calendars - holidays,
     *  shared feeds - report a lower access level and would reject the insert). */
    private fun writableCalendarId(): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.IS_PRIMARY)
        return runCatching {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?",
                arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
                "${CalendarContract.Calendars.IS_PRIMARY} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                if (cursor.moveToFirst()) cursor.getLong(idCol) else null
            }
        }.getOrNull()
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
