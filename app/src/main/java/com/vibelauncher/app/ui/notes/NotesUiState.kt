package com.vibelauncher.app.ui.notes

import com.vibelauncher.app.model.NoteItem

data class NotesUiState(
    val notes: List<NoteItem> = emptyList(),
    val editingItem: NoteItem? = null,
    val lastDeleted: NoteItem? = null
)
