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

/** A typed line split into its title and whatever date/time it mentioned. [startMillis] is
 *  null when the line named no date or time at all - events default that to today, but a
 *  to-do with no date simply has no due date, so the difference has to survive parsing.
 *  All-day values use the same UTC-midnight convention as [ParsedEvent]. */
internal data class ParsedWhen(
    val title: String,
    val startMillis: Long?,
    val allDay: Boolean
)

/** A typed to-do line: "ring vet tomorrow 4pm" → text "ring vet", due tomorrow at 4pm. */
internal data class TodoDraft(
    val text: String,
    val dueAt: Long?,
    val dueAllDay: Boolean
)

private const val DEFAULT_DURATION_MS = 60 * 60 * 1000L
private const val MINUTE_MS = 60 * 1000L
private const val HOUR_MS = 60 * MINUTE_MS
private const val DAY_MS = 24 * HOUR_MS

// A cap well past any real "in N units" request, so an absurd number can't overflow the
// millisecond arithmetic.
private const val MAX_RELATIVE_AMOUNT = 10_000

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

private enum class OffsetUnit(val words: Set<String>) {
    MINUTE(setOf("m", "min", "mins", "minute", "minutes")),
    HOUR(setOf("h", "hr", "hrs", "hour", "hours")),
    DAY(setOf("d", "day", "days")),
    WEEK(setOf("w", "wk", "wks", "week", "weeks"));

    companion object {
        fun of(word: String): OffsetUnit? = entries.firstOrNull { word.lowercase() in it.words }
    }
}

// "9a", "9am", "9:30 pm", "930pm" - a bare hour with no meridiem is only a time when it
// carries a colon ("14:00"), so a lone number in the title ("*call 5 people") isn't eaten.
private val MERIDIEM_TIME = Regex("""^(\d{1,2})(?::?(\d{2}))?(a|p|am|pm)$""", RegexOption.IGNORE_CASE)
private val CLOCK_TIME = Regex("""^(\d{1,2}):(\d{2})$""")
private val DAY_NUMBER = Regex("""^(\d{1,2})(?:st|nd|rd|th)?$""", RegexOption.IGNORE_CASE)
private val NUMERIC_DATE = Regex("""^(\d{1,2})[/-](\d{1,2})(?:[/-](\d{2,4}))?$""")
// "in 2h", "in 30m", "in 3d", "in 2w" - the amount and unit typed as one token.
private val COMPACT_OFFSET = Regex("""^(\d+)([a-z]+)$""", RegexOption.IGNORE_CASE)

private class TimeOfDay(val hour: Int, val minute: Int)

private class DatePart(val year: Int?, val month: Int?, val day: Int?, val dayOffset: Int?)

/** An "in N units" phrase. Minutes and hours pin an exact moment; days and weeks only move
 *  the date, so a time can still be added ("in 3 days 9a"). */
private sealed class RelativeOffset {
    class Moment(val millis: Long) : RelativeOffset()
    class Days(val days: Int) : RelativeOffset()
}

/**
 * Pulls a date and/or time out of [text], wherever they appear, and treats whatever is left
 * as the title. Returns null when nothing is left to name the thing being scheduled.
 *
 * Recognized: month names with a day ("mar 24", "24 march"), numeric dates ("3/24"),
 * "today"/"tonight", "tomorrow"/"tmrw"/"tmr", weekday names (next occurrence, optionally
 * prefixed with "next"), times ("9a", "9:30pm", "14:00"), and offsets that always start with
 * "in" ("in 2 hours", "in 30 min", "in an hour", "in 3 days", "in 2 weeks", "in 2h"). The
 * leading "in" is what keeps a title like "buy 2 hats" from being read as a date.
 *
 * A time with no date lands today, rolling to tomorrow if it has already passed today; a
 * date with no time is all day. A month/day already past this year rolls to next year. An
 * "in N minutes/hours" offset fixes the exact moment on its own, so no other date or time
 * is read from the same line.
 */
internal fun parseWhenText(text: String, nowMillis: Long = System.currentTimeMillis()): ParsedWhen? {
    val tokens = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (tokens.isEmpty()) return null

    val consumed = BooleanArray(tokens.size)
    val relative = extractRelativeOffset(tokens, consumed, nowMillis)
    val time: TimeOfDay?
    val date: DatePart?
    when (relative) {
        is RelativeOffset.Moment -> {
            time = null
            date = null
        }
        is RelativeOffset.Days -> {
            time = extractTime(tokens, consumed)
            date = DatePart(null, null, null, dayOffset = relative.days)
        }
        null -> {
            time = extractTime(tokens, consumed)
            date = extractDate(tokens, consumed)
        }
    }

    val title = tokens.filterIndexed { index, _ -> !consumed[index] }.joinToString(" ").trim()
    if (title.isEmpty()) return null

    return when {
        relative is RelativeOffset.Moment -> ParsedWhen(title, relative.millis, allDay = false)
        time == null && date == null -> ParsedWhen(title, startMillis = null, allDay = true)
        time == null -> ParsedWhen(title, utcMidnightOf(resolveDate(date, nowMillis, time = null)), allDay = true)
        else -> ParsedWhen(title, resolveDate(date, nowMillis, time).timeInMillis, allDay = false)
    }
}

/** [parseWhenText] for a '*' event, which always needs a day to land on: a line that names
 *  no date or time becomes an all-day event today. */
internal fun parseEventText(text: String, nowMillis: Long = System.currentTimeMillis()): ParsedEvent? {
    val parsed = parseWhenText(text, nowMillis) ?: return null
    val start = parsed.startMillis
        ?: utcMidnightOf(Calendar.getInstance().apply { timeInMillis = nowMillis })
    return if (parsed.allDay || parsed.startMillis == null) {
        ParsedEvent(parsed.title, start, start + DAY_MS, allDay = true)
    } else {
        ParsedEvent(parsed.title, start, start + DEFAULT_DURATION_MS, allDay = false)
    }
}

/** [parseWhenText] for a '-' to-do. A line that is nothing but a date ("tomorrow") has no
 *  title left once the date is taken out, so it's kept as the to-do's text with no due
 *  date - typing it was a to-do called "tomorrow", not a date with no to-do attached. */
internal fun parseTodoText(text: String, nowMillis: Long = System.currentTimeMillis()): TodoDraft {
    val parsed = parseWhenText(text, nowMillis)
    return if (parsed?.startMillis != null) {
        TodoDraft(parsed.title, parsed.startMillis, parsed.allDay)
    } else {
        TodoDraft(text.trim(), dueAt = null, dueAllDay = false)
    }
}

/** "Mar 24, 9:00 AM" for a timed moment, "Mar 24 (all day)" otherwise - the breadcrumb text
 *  shown above the input while typing. All-day values are UTC midnight, so they're
 *  formatted in UTC or they'd show the previous day west of Greenwich. */
internal fun whenLabel(startMillis: Long, allDay: Boolean): String {
    val dayFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    if (allDay) {
        dayFormat.timeZone = TimeZone.getTimeZone("UTC")
        return "${dayFormat.format(startMillis)} (all day)"
    }
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    return "${dayFormat.format(startMillis)}, ${timeFormat.format(startMillis)}"
}

internal fun eventPreviewLabel(event: ParsedEvent): String = whenLabel(event.startMillis, event.allDay)

private fun extractRelativeOffset(tokens: List<String>, consumed: BooleanArray, nowMillis: Long): RelativeOffset? {
    for (index in tokens.indices) {
        if (consumed[index] || !tokens[index].equals("in", ignoreCase = true)) continue
        val next = tokens.getOrNull(index + 1) ?: continue

        // "in 2h" - amount and unit in a single token.
        COMPACT_OFFSET.matchEntire(next)?.let { match ->
            val amount = match.groupValues[1].toIntOrNull()
            val unit = OffsetUnit.of(match.groupValues[2])
            if (amount != null && unit != null && amount in 1..MAX_RELATIVE_AMOUNT) {
                consumed[index] = true
                consumed[index + 1] = true
                return offsetFor(amount, unit, nowMillis)
            }
        }

        // "in 2 hours", "in an hour".
        val amount = amountOf(next) ?: continue
        val unit = tokens.getOrNull(index + 2)?.let { OffsetUnit.of(it) } ?: continue
        consumed[index] = true
        consumed[index + 1] = true
        consumed[index + 2] = true
        return offsetFor(amount, unit, nowMillis)
    }
    return null
}

private fun amountOf(token: String): Int? = when (token.lowercase()) {
    "a", "an", "one" -> 1
    else -> token.toIntOrNull()?.takeIf { it in 1..MAX_RELATIVE_AMOUNT }
}

private fun offsetFor(amount: Int, unit: OffsetUnit, nowMillis: Long): RelativeOffset = when (unit) {
    OffsetUnit.MINUTE -> RelativeOffset.Moment(truncateToMinute(nowMillis + amount * MINUTE_MS))
    OffsetUnit.HOUR -> RelativeOffset.Moment(truncateToMinute(nowMillis + amount * HOUR_MS))
    OffsetUnit.DAY -> RelativeOffset.Days(amount)
    OffsetUnit.WEEK -> RelativeOffset.Days(amount * 7)
}

private fun truncateToMinute(millis: Long): Long = millis - millis % MINUTE_MS

private fun utcMidnightOf(local: Calendar): Long =
    Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis

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
            "today", "tonight" -> {
                consumed[index] = true
                return DatePart(null, null, null, dayOffset = 0)
            }
            "tomorrow", "tmrw", "tmr" -> {
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
            } else if (calendar.timeInMillis < nowMillis - DAY_MS) {
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
