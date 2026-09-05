package com.vibelauncher.app.model

import kotlinx.serialization.Serializable

enum class NoteCategory { PERSONAL, WORK, IDEAS, JOURNAL }

enum class NoteBlockType { TEXT, CHECKLIST, BULLET, NUMBERED }

/** A single run of text with its own bold/italic/underline flags - reserved for a future
 *  per-span storage model. Today's editor stores formatting as inline markdown-style
 *  tokens directly in [NoteBlock]'s single-span text instead (see NoteEditorScreen's
 *  VisualTransformation) - this type exists so the storage shape doesn't need to change if
 *  per-span storage is ever needed. */
@Serializable
data class NoteSpan(val text: String, val bold: Boolean = false, val italic: Boolean = false, val underline: Boolean = false)

/** One line/row of a note's body. TEXT blocks can hold multi-line wrapped paragraph text;
 *  CHECKLIST/BULLET/NUMBERED are always single logical items - Enter creates a new block of
 *  the same type, matching normal checklist-app behavior. */
@Serializable
data class NoteBlock(
    val type: NoteBlockType = NoteBlockType.TEXT,
    val checked: Boolean = false, // meaningful only for CHECKLIST
    val spans: List<NoteSpan> = listOf(NoteSpan(""))
)

@Serializable
data class NoteItem(
    val id: Long,
    val title: String,
    val category: NoteCategory,
    val blocks: List<NoteBlock>,
    val createdAt: Long,
    val updatedAt: Long,
    val pinned: Boolean = false
)
