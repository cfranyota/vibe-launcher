package com.vibelauncher.app.ui.notes

import com.vibelauncher.app.model.NoteBlock
import com.vibelauncher.app.model.NoteBlockType
import com.vibelauncher.app.model.NoteCategory

data class NoteEditorUiState(
    val noteId: Long = -1L,
    val title: String = "",
    val category: NoteCategory = NoteCategory.PERSONAL,
    val blocks: List<NoteBlock> = listOf(NoteBlock(type = NoteBlockType.TEXT)),
    val pinned: Boolean = false,
    val focusedBlockIndex: Int = 0,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isNew: Boolean = true
)
