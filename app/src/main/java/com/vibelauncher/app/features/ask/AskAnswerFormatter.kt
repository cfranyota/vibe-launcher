package com.vibelauncher.app.features.ask

import com.vibelauncher.app.data.calendar.CalendarEvent
import com.vibelauncher.app.data.calendar.DayEvents
import java.text.SimpleDateFormat
import java.util.Locale

private val TIME_FORMAT = SimpleDateFormat("h:mm a", Locale.getDefault())

/** Formats a plain-English answer for [questionType] from the day's already-fetched
 *  [events] - pure/testable, no Android Context or I/O of its own. */
internal fun formatAskAnswer(events: DayEvents, questionType: AskQuestionType, nowMillis: Long): String = when (questionType) {
    AskQuestionType.TODAY_EVENTS -> {
        val items = events.allDayEvents + events.timedEvents
        when {
            items.isEmpty() -> "Nothing on your calendar today."
            items.size == 1 -> "Today: ${describe(items.first())}."
            else -> "Today: " + items.joinToString(", ") { describe(it) } + "."
        }
    }
    AskQuestionType.NEXT_EVENT -> {
        val next = events.timedEvents.firstOrNull { it.startMillis > nowMillis }
        if (next == null) "No more events today." else "Next: ${describe(next)}."
    }
}

private fun describe(event: CalendarEvent): String =
    if (event.isAllDay) event.title else "${event.title} at ${TIME_FORMAT.format(event.startMillis)}"
