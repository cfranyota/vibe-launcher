package com.caseyfrancis.vibelauncher.data.calendar

data class CalendarEvent(
    val id: Long,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val isAllDay: Boolean
)
