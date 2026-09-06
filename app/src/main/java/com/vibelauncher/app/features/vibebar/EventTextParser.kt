package com.vibelauncher.app.features.vibebar

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** One line of typed text resolved into a real calendar event - "*dentist mar 24 9a"
 *  becomes title "dentist" starting Mar 24 at 9:00 AM. [allDay] events carry UTC-midnight
 *  timestamps, which is what CalendarContract requires for them. */
internal data class ParsedEvent(
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean
)

private const val DEFAULT_DURATION_MS = 60 * 60 * 1000L

private val MONTHS = mapOf(
    "jan" to 0, "january" to 0, "feb" to 1, "february" to 1, "mar" to 2, "march" to 2,
    "apr" to 3, "april" to 3, "may" to 4, "jun" to 5, "june" to 5, "jul" to 6, "july" to 6,
    "aug" to 7, "august" to 7, "sep" to 8, "sept" to 8, "september" to 8, "oct" to 9,
    "october" to 9, "nov" to 10, "november" to 10, "dec" to 11, "december" to 11
)

private val WEEKDAYS = mapOf(
    "sun" to Calendar.SUNDAY, "sunday" to Calendar.SUNDAY,
    "mon" to Calendar.MONDAY, "monday" to Calendar.MONDAY,
    "tue" to Calendar.TUESDAY, "tues" to Calendar.TUESDAY, "tuesday" to Calendar.TUESDAY,
    "wed" to Calendar.WEDNESDAY, "weds" to Calendar.WEDNESDAY, "wednesday" to Calendar.WEDNESDAY,
    "thu" to Calendar.THURSDAY, "thur" to Calendar.THURSDAY, "thurs" to Calendar.THURSDAY,
    "thursday" to Calendar.THURSDAY,
    "fri" to Calendar.FRIDAY, "friday" to Calendar.FRIDAY,
    "sat" to Calendar.SATURDAY, "saturday" to Calendar.SATURDAY
)

// "9a", "9am", "9:30 pm", "930pm" - a bare hour with no meridiem is only a time when it
// carries a colon ("14:00"), so a lone number in the title ("*call 5 people") isn't eaten.
private val MERIDIEM_TIME = Regex("""^(\d{1,2})(?::?(\d{2}))?(a|p|am|pm)$""", RegexOption.IGNORE_CASE)
private val CLOCK_TIME = Regex("""^(\d{1,2}):(\d{2})$""")
private val DAY_NUMBER = Regex("""^(\d{1,2})(?:st|nd|rd|th)?$""", RegexOption.IGNORE_CASE)
private val NUMERIC_DATE = Regex("""^(\d{1,2})[/-](\d{1,2})(?:[/-](\d{2,4}))?$""")

private class TimeOfDay(val hour: Int, val minute: Int)

private class DatePart(val year: Int?, val month: Int?, val day: Int?, val dayOffset: Int?)

/**
 * Pulls a date and/or time out of [text], wherever they appear, and treats whatever is left
 * as the title. Returns null when nothing is left to name the event.
 *
 * Recognized: month names with a day ("mar 24", "24 march"), numeric dates ("3/24"),
 * "today"/"tomorrow", weekday names (next occurrence, optionally prefixed with "next"),
 * and times ("9a", "9:30pm", "14:00"). A missing date means today; a missing time means an
 * all-day event. A month/day already past this year rolls to next year, and a time already
 * past today (with no date given) rolls to tomorrow - typing a time is always a request for
 * an event that hasn't happened yet.
 */
internal fun parseEventText(text: String, nowMillis: Long = System.currentTimeMillis()): ParsedEvent? {
    val tokens = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (tokens.isEmpty()) return null

    val consumed = BooleanArray(tokens.size)
    val time = extractTime(tokens, consumed)
    val date = extractDate(tokens, consumed)

    val title = tokens.filterIndexed { index, _ -> !consumed[index] }.joinToString(" ").trim()
    if (title.isEmpty()) return null

    val allDay = time == null
    return if (allDay) {
        // CalendarContract stores all-day events as UTC midnight, so the day is resolved in
        // local time (what the user meant by "mar 24") and then rebuilt in UTC.
        val local = resolveDate(date, nowMillis, time = null)
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }
        ParsedEvent(title, utc.timeInMillis, utc.timeInMillis + 24 * 60 * 60 * 1000L, allDay = true)
    } else {
        val start = resolveDate(date, nowMillis, time).timeInMillis
        ParsedEvent(title, start, start + DEFAULT_DURATION_MS, allDay = false)
    }
}

/** "Mar 24, 9:00 AM" for a timed event, "Mar 24 (all day)" otherwise - the breadcrumb text
 *  shown above the input while typing. */
internal fun eventPreviewLabel(event: ParsedEvent): String {
    val dayFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    if (event.allDay) {
        dayFormat.timeZone = TimeZone.getTimeZone("UTC")
        return "${dayFormat.format(event.startMillis)} (all day)"
    }
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    return "${dayFormat.format(event.startMillis)}, ${timeFormat.format(event.startMillis)}"
}

private fun extractTime(tokens: List<String>, consumed: BooleanArray): TimeOfDay? {
    tokens.forEachIndexed { index, token ->
        if (consumed[index]) return@forEachIndexed
        MERIDIEM_TIME.matchEntire(token)?.let { match ->
            val rawHour = match.groupValues[1].toInt()
            if (rawHour in 1..12) {
                val minute = match.groupValues[2].toIntOrNull() ?: 0
                if (minute in 0..59) {
                    val isPm = match.groupValues[3].lowercase().startsWith("p")
                    val hour = when {
                        isPm && rawHour < 12 -> rawHour + 12
                        !isPm && rawHour == 12 -> 0
                        else -> rawHour
                    }
                    consumed[index] = true
                    return TimeOfDay(hour, minute)
                }
            }
        }
        CLOCK_TIME.matchEntire(token)?.let { match ->
            val hour = match.groupValues[1].toInt()
            val minute = match.groupValues[2].toInt()
            if (hour in 0..23 && minute in 0..59) {
                consumed[index] = true
                return TimeOfDay(hour, minute)
            }
        }
    }
    // "9 am" typed with a space is two tokens - stitch an adjacent bare hour and meridiem.
    for (index in 0 until tokens.size - 1) {
        if (consumed[index] || consumed[index + 1]) continue
        val hour = tokens[index].toIntOrNull() ?: continue
        val meridiem = tokens[index + 1].lowercase()
        if (hour in 1..12 && (meridiem == "am" || meridiem == "pm" || meridiem == "a" || meridiem == "p")) {
            consumed[index] = true
            consumed[index + 1] = true
            val isPm = meridiem.startsWith("p")
            return TimeOfDay(if (isPm && hour < 12) hour + 12 else if (!isPm && hour == 12) 0 else hour, 0)
        }
    }
    return null
}

private fun extractDate(tokens: List<String>, consumed: BooleanArray): DatePart? {
    tokens.forEachIndexed { index, token ->
        if (consumed[index]) return@forEachIndexed
        val lower = token.lowercase()

        MONTHS[lower]?.let { month ->
            // The day can sit on either side ("mar 24" or "24 march"); without one, the
            // month alone isn't enough to place an event, so it stays part of the title.
            val after = tokens.getOrNull(index + 1)?.takeIf { !consumed[index + 1] }
            val before = tokens.getOrNull(index - 1)?.takeIf { index > 0 && !consumed[index - 1] }
            val afterDay = after?.let { DAY_NUMBER.matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() }
            val beforeDay = before?.let { DAY_NUMBER.matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() }
            val day = afterDay?.takeIf { it in 1..31 } ?: beforeDay?.takeIf { it in 1..31 }
            if (day != null) {
                consumed[index] = true
                if (afterDay != null && afterDay in 1..31) consumed[index + 1] = true else consumed[index - 1] = true
                return DatePart(year = null, month = month, day = day, dayOffset = null)
            }
        }

        NUMERIC_DATE.matchEntire(token)?.let { match ->
            val month = match.groupValues[1].toInt()
            val day = match.groupValues[2].toInt()
            if (month in 1..12 && day in 1..31) {
                val rawYear = match.groupValues[3].toIntOrNull()
                val year = when {
                    rawYear == null -> null
                    rawYear < 100 -> 2000 + rawYear
                    else -> rawYear
                }
                consumed[index] = true
                return DatePart(year = year, month = month - 1, day = day, dayOffset = null)
            }
        }

        when (lower) {
            "today" -> {
                consumed[index] = true
                return DatePart(null, null, null, dayOffset = 0)
            }
            "tomorrow", "tmrw" -> {
                consumed[index] = true
                return DatePart(null, null, null, dayOffset = 1)
            }
        }

        WEEKDAYS[lower]?.let { weekday ->
            consumed[index] = true
            val nextWeek = index > 0 && !consumed[index - 1] && tokens[index - 1].equals("next", ignoreCase = true)
            if (nextWeek) consumed[index - 1] = true
            return DatePart(null, null, null, dayOffset = daysUntilWeekday(weekday, nextWeek))
        }
    }
    return null
}

/** Days from today to the next [weekday]; today itself counts as 7 days out, since "*lunch
 *  friday" typed on a Friday reads as the coming Friday, not the one already underway.
 *  [nextWeek] ("next friday") pushes it a further week. */
private fun daysUntilWeekday(weekday: Int, nextWeek: Boolean): Int {
    val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    var delta = weekday - today
    if (delta <= 0) delta += 7
    return if (nextWeek) delta + 7 else delta
}

private fun resolveDate(date: DatePart?, nowMillis: Long, time: TimeOfDay?): Calendar {
    val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    when {
        date?.dayOffset != null -> calendar.add(Calendar.DAY_OF_YEAR, date.dayOffset)
        date?.month != null && date.day != null -> {
            calendar.set(Calendar.MONTH, date.month)
            calendar.set(Calendar.DAY_OF_MONTH, date.day)
            if (date.year != null) {
                calendar.set(Calendar.YEAR, date.year)
            } else if (calendar.timeInMillis < nowMillis - 24 * 60 * 60 * 1000L) {
                // A month/day already behind us means next year's occurrence.
                calendar.add(Calendar.YEAR, 1)
            }
        }
    }

    if (time != null) {
        calendar.set(Calendar.HOUR_OF_DAY, time.hour)
        calendar.set(Calendar.MINUTE, time.minute)
        // A time with no date at all that's already passed belongs to tomorrow.
        if (date == null && calendar.timeInMillis <= nowMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
    } else {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
    }
    return calendar
}
