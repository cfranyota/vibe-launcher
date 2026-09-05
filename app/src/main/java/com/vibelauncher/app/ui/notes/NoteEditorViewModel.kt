package com.vibelauncher.app.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vibelauncher.app.data.notes.NoteRepository
import com.vibelauncher.app.model.NoteBlock
import com.vibelauncher.app.model.NoteBlockType
import com.vibelauncher.app.model.NoteCategory
import com.vibelauncher.app.model.NoteItem
import com.vibelauncher.app.model.NoteSpan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val MAX_HISTORY = 50

/** Backs the note editor. Autosaves on every change (debounce-free - block edits are
 *  already coarse, one commit per field-change/toolbar-action, not per keystroke, so
 *  writing straight through to DataStore on each is fine). Undo/redo is a coarse
 *  snapshot stack over the whole block list, pushed before each discrete edit-committing
 *  action - not per-keystroke undo. */
class NoteEditorViewModel(
    private val noteRepository: NoteRepository,
    initialNoteId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditorUiState(noteId = initialNoteId))
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    private var createdAt: Long = System.currentTimeMillis()
    private val undoStack = ArrayDeque<List<NoteBlock>>()
    private val redoStack = ArrayDeque<List<NoteBlock>>()

    init {
        if (initialNoteId >= 0) {
            viewModelScope.launch {
                val existing = noteRepository.notes.first().find { it.id == initialNoteId }
                if (existing != null) {
                    createdAt = existing.createdAt
                    _uiState.value = _uiState.value.copy(
                        title = existing.title,
                        category = existing.category,
                        blocks = existing.blocks,
                        pinned = existing.pinned,
                        isNew = false
                    )
                }
            }
        }
    }

    fun setTitle(value: String) {
        _uiState.value = _uiState.value.copy(title = value)
        persist()
    }

    fun setCategory(value: NoteCategory) {
        _uiState.value = _uiState.value.copy(category = value)
        persist()
    }

    fun setFocusedBlock(index: Int) {
        _uiState.value = _uiState.value.copy(focusedBlockIndex = index)
    }

    fun togglePinned() {
        _uiState.value = _uiState.value.copy(pinned = !_uiState.value.pinned)
        persist()
    }

    /** Replaces a single block's text (the raw, markdown-token-embedded string) - called as
     *  the focused block's text field changes. Pushes undo history first. */
    fun updateBlockText(index: Int, rawText: String) {
        pushUndo()
        mutateBlocks { blocks ->
            blocks.mapIndexed { i, block ->
                if (i == index) block.copy(spans = listOf(NoteSpan(rawText))) else block
            }
        }
    }

    fun setBlockType(index: Int, type: NoteBlockType) {
        pushUndo()
        mutateBlocks { blocks -> blocks.mapIndexed { i, block -> if (i == index) block.copy(type = type) else block } }
    }

    fun toggleChecked(index: Int) {
        pushUndo()
        mutateBlocks { blocks -> blocks.mapIndexed { i, block -> if (i == index) block.copy(checked = !block.checked) else block } }
    }

    /** Enter within a block - splits its text at [cursorOffset] into two blocks of the same
     *  type, focus moves to the new (second) block. */
    fun splitBlock(index: Int, cursorOffset: Int) {
        pushUndo()
        mutateBlocks { blocks ->
            val block = blocks.getOrNull(index) ?: return@mutateBlocks blocks
            val text = block.spans.firstOrNull()?.text.orEmpty()
            val before = text.take(cursorOffset)
            val after = text.drop(cursorOffset)
            val result = blocks.toMutableList()
            result[index] = block.copy(spans = listOf(NoteSpan(before)))
            result.add(index + 1, NoteBlock(type = block.type, spans = listOf(NoteSpan(after))))
            result
        }
        _uiState.value = _uiState.value.copy(focusedBlockIndex = index + 1)
    }

    /** Wraps the given [selectionStart]/[selectionEnd] range of the focused block's text in
     *  [token] (or removes it, if that exact range is already wrapped) - the toggle
     *  behavior backing the Bold/Italic/Underline toolbar buttons. */
    fun toggleTokenAroundSelection(index: Int, selectionStart: Int, selectionEnd: Int, token: String) {
        pushUndo()
        mutateBlocks { blocks ->
            val block = blocks.getOrNull(index) ?: return@mutateBlocks blocks
            val text = block.spans.firstOrNull()?.text.orEmpty()
            val start = selectionStart.coerceIn(0, text.length)
            val end = selectionEnd.coerceIn(0, text.length)
            val lo = minOf(start, end)
            val hi = maxOf(start, end)
            val already = text.regionMatches(lo - token.length, token, 0, token.length) &&
                text.regionMatches(hi, token, 0, token.length) &&
                lo >= token.length
            val newText = if (already) {
                text.removeRange(hi, hi + token.length).removeRange(lo - token.length, lo)
            } else {
                val selected = text.substring(lo, hi)
                text.substring(0, lo) + token + selected + token + text.substring(hi)
            }
            blocks.mapIndexed { i, b -> if (i == index) b.copy(spans = listOf(NoteSpan(newText))) else b }
        }
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(_uiState.value.blocks)
        _uiState.value = _uiState.value.copy(blocks = previous, canUndo = undoStack.isNotEmpty(), canRedo = true)
        persist()
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(_uiState.value.blocks)
        _uiState.value = _uiState.value.copy(blocks = next, canUndo = true, canRedo = redoStack.isNotEmpty())
        persist()
    }

    fun delete() {
        val id = _uiState.value.noteId
        if (id >= 0) viewModelScope.launch { noteRepository.delete(id) }
    }

    /** Called on back/navigate-away, in addition to the per-change persist() below - belt
     *  and suspenders so a note is never lost even if a mutation's own persist() call is
     *  still in flight. */
    fun saveAndExit() {
        persist()
    }

    private fun pushUndo() {
        undoStack.addLast(_uiState.value.blocks)
        if (undoStack.size > MAX_HISTORY) undoStack.removeFirst()
        redoStack.clear()
        _uiState.value = _uiState.value.copy(canUndo = true, canRedo = false)
    }

    private fun mutateBlocks(transform: (List<NoteBlock>) -> List<NoteBlock>) {
        val next = transform(_uiState.value.blocks).ifEmpty { listOf(NoteBlock(type = NoteBlockType.TEXT)) }
        _uiState.value = _uiState.value.copy(blocks = next)
        persist()
    }

    private fun persist() {
        val state = _uiState.value
        // An untouched brand-new note (blank title, single empty block) is never written -
        // avoids littering the list with empty notes just from opening the editor and
        // backing straight out.
        val isBlankNew = state.isNew && state.title.isBlank() &&
            state.blocks.size == 1 && state.blocks.first().spans.firstOrNull()?.text.isNullOrBlank()
        if (isBlankNew) return

        val id = if (state.noteId >= 0) state.noteId else System.currentTimeMillis().also {
            _uiState.value = _uiState.value.copy(noteId = it, isNew = false)
        }
        viewModelScope.launch {
            noteRepository.save(
                NoteItem(
                    id = id,
                    title = state.title,
                    category = state.category,
                    blocks = state.blocks,
                    createdAt = createdAt,
                    updatedAt = System.currentTimeMillis(),
                    pinned = state.pinned
                )
            )
        }
    }

    class Factory(
        private val noteRepository: NoteRepository,
        private val noteId: Long
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return NoteEditorViewModel(noteRepository, noteId) as T
        }
    }
}
