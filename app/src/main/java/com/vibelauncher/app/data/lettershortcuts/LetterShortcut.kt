package com.vibelauncher.app.data.lettershortcuts

import kotlinx.serialization.Serializable

@Serializable
enum class LetterShortcutType { OPEN_APP, MESSAGE_CONTACT, CALL_CONTACT }

/** A user-assigned hold-a-letter shortcut - holding [letter] on a hardware keyboard from
 *  the collapsed home screen runs [type]'s action. Most letters have no entry (unassigned),
 *  in which case holding behaves exactly like a normal tap (opens Vibe Bar). */
@Serializable
data class LetterShortcut(
    val letter: Char,
    val type: LetterShortcutType,
    val label: String,
    val packageName: String? = null,
    val className: String? = null,
    val contactId: Long? = null,
    val phone: String? = null
)
