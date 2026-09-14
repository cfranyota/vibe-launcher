package com.vibelauncher.app.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
private val WEEKDAY_FORMAT = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
private val DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

/** The calendar day a to-do is due. All-day due dates are UTC midnight, so they're read in
 *  UTC - reading them in local time would shift them to the previous day west of Greenwich. */
private fun TodoItem.dueDate(): LocalDate? {
    val due = dueAt ?: return null
    val zone = if (dueAllDay) ZoneOffset.UTC else ZoneId.systemDefault()
    return Instant.ofEpochMilli(due).atZone(zone).toLocalDate()
}

private fun today(nowMillis: Long): LocalDate =
    Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).toLocalDate()

/** Past due and still open. A timed to-do is late the moment its time passes; an all-day one
 *  only once its whole day is over. */
fun TodoItem.isOverdue(nowMillis: Long = System.currentTimeMillis()): Boolean {
    if (done) return false
    val due = dueAt ?: return false
    return if (dueAllDay) dueDate()!!.isBefore(today(nowMillis)) else due < nowMillis
}

/** The To-Do row's due label: "4:00 pm" when it's due later today, "today" for an all-day
 *  to-do today, "tomorrow", a weekday within the week ahead, otherwise "mar 24". Null when
 *  there's no due date. */
fun TodoItem.dueLabel(nowMillis: Long = System.currentTimeMillis()): String? {
    val date = dueDate() ?: return null
    val days = ChronoUnit.DAYS.between(today(nowMillis), date)
    val zonedDue = Instant.ofEpochMilli(dueAt!!).atZone(ZoneId.systemDefault())
    return when {
        days == 0L -> if (dueAllDay) "today" else zonedDue.format(TIME_FORMAT).lowercase()
        days == 1L -> "tomorrow"
        days == -1L -> "yesterday"
        days in 2L..6L -> date.format(WEEKDAY_FORMAT).lowercase()
        else -> date.format(DATE_FORMAT).lowercase()
    }
}

/** A short badge for the home to-do card, which only has room for a few characters: "late",
 *  "45m"/"3h" for later today, "today", "tmr", a weekday, otherwise "mar 24". Null when
 *  there's no due date or the to-do is done. */
fun TodoItem.dueBadge(nowMillis: Long = System.currentTimeMillis()): String? {
    if (done) return null
    val date = dueDate() ?: return null
    if (isOverdue(nowMillis)) return "late"
    val days = ChronoUnit.DAYS.between(today(nowMillis), date)
    return when {
        days == 0L && dueAllDay -> "today"
        days == 0L -> {
            val minutes = (dueAt!! - nowMillis) / 60_000L
            if (minutes < 60) "${minutes}m" else "${minutes / 60}h"
        }
        days == 1L -> "tmr"
        days in 2L..6L -> date.format(WEEKDAY_FORMAT).lowercase()
        else -> date.format(DATE_FORMAT).lowercase()
    }
}
