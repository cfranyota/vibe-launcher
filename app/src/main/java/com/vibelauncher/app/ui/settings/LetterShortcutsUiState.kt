package com.vibelauncher.app.ui.settings

import com.vibelauncher.app.data.apps.AppInfo
import com.vibelauncher.app.data.contacts.ContactResult
import com.vibelauncher.app.data.lettershortcuts.LetterShortcut

data class LetterShortcutsUiState(
    val shortcuts: Map<Char, LetterShortcut> = emptyMap(),
    val apps: List<AppInfo> = emptyList(),
    val appQuery: String = "",
    val contactQuery: String = "",
    val contactResults: List<ContactResult> = emptyList()
)
