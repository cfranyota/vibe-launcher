package com.vibelauncher.app.features.vibebar

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

/** Converts a raw hardware-keyboard unicode code point into an appendable string, or null
 *  for non-printable/control input (backspace, arrows, etc). Used by the always-present
 *  hidden focus target that reveals Vibe Bar on the first physical keystroke. */
internal fun printableHardwareText(unicodeCodePoint: Int): String? {
    if (
        unicodeCodePoint == 0 ||
        !Character.isValidCodePoint(unicodeCodePoint) ||
        Character.isISOControl(unicodeCodePoint)
    ) {
        return null
    }
    return String(Character.toChars(unicodeCodePoint))
}
