package com.vibelauncher.app.features.vibebar

import com.vibelauncher.app.data.contacts.ContactResult

internal const val VIBE_BAR_NOTE_PREFIX = '/'
internal val VIBE_BAR_COMMAND_PREFIXES = setOf('@', '#', '-', VIBE_BAR_NOTE_PREFIX, '+', '?')

internal data class VibeBarInput(
    val prefix: Char?,
    val searchTerm: String,
    val plainQuery: String
)

internal fun parseVibeBarInput(text: String, lockedPrefix: Char? = null): VibeBarInput {
    val prefix = lockedPrefix ?: text.firstOrNull()
    val searchTerm = if (lockedPrefix == null) {
        text.drop(1).substringBefore(' ').trim()
    } else {
        ""
    }
    val plainQuery = text.trim().takeIf { prefix !in VIBE_BAR_COMMAND_PREFIXES }.orEmpty()
    return VibeBarInput(prefix, searchTerm, plainQuery)
}

/** Short breadcrumb line describing what submitting will do, shown above the input box.
 *  Null when there's nothing meaningful to preview yet - '#' (acts immediately on a tapped
 *  contact, never has a typed body) and '/' (hands off to NoteBubble immediately) never
 *  preview. */
internal fun previewTextFor(prefix: Char?, payload: String, selectedContact: ContactResult?): String? = when {
    prefix == '-' && payload.isNotBlank() -> "to-do → $payload"
    prefix == '+' && payload.isNotBlank() -> "event → $payload"
    prefix == '@' && selectedContact != null -> "text → ${selectedContact.name}"
    prefix == '?' && payload.isNotBlank() -> "open → $payload"
    prefix == null && payload.isNotBlank() -> "search → $payload"
    else -> null
}

/** Converts a raw hardware-keyboard key event into an appendable string, or null for
 *  non-printable/control input (backspace, arrows, etc). Used by the always-present hidden
 *  focus target that reveals Vibe Bar on the first physical keystroke. */
internal fun printableHardwareText(event: android.view.KeyEvent): String? {
    val unicodeChar = event.unicodeChar
    if (unicodeChar != 0 && Character.isValidCodePoint(unicodeChar) && !Character.isISOControl(unicodeChar)) {
        return String(Character.toChars(unicodeChar))
    }
    // Some hardware keyboards report a valid A-Z keycode but a zero/unmapped unicodeChar
    // under certain meta-states - fall back to the keycode's own identity so a plain letter
    // press is never silently swallowed.
    val code = event.keyCode
    if (code in android.view.KeyEvent.KEYCODE_A..android.view.KeyEvent.KEYCODE_Z) {
        val letter = 'a' + (code - android.view.KeyEvent.KEYCODE_A)
        return letter.toString()
    }
    return null
}
