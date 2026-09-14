package com.vibelauncher.app.model

import kotlinx.serialization.Serializable

@Serializable
data class TodoItem(
    val id: Long,
    val text: String,
    val createdAt: Long,
    val done: Boolean = false,
    val starred: Boolean = false,
    /** When it's due, if a date was typed with it ("ring vet tomorrow 4pm"). Defaults keep
     *  to-dos saved before due dates existed loading unchanged. */
    val dueAt: Long? = null,
    /** [dueAt] is a whole day rather than a moment. All-day values are UTC midnight, the
     *  same convention Vibe Bar's date parser and CalendarContract use. */
    val dueAllDay: Boolean = false
)
