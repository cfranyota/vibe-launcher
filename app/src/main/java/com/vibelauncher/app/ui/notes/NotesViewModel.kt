package com.vibelauncher.app.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vibelauncher.app.data.notes.NotesRepository
import com.vibelauncher.app.model.NoteItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(private val notesRepository: NotesRepository) : ViewModel() {

    private val editingItem = MutableStateFlow<NoteItem?>(null)
    private val lastDeleted = MutableStateFlow<NoteItem?>(null)

    val uiState: StateFlow<NotesUiState> = combine(
        notesRepository.notes,
        editingItem,
        lastDeleted
    ) { notes, editing, deleted ->
        NotesUiState(notes = notes.sortedByDescending { it.createdAt }, editingItem = editing, lastDeleted = deleted)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotesUiState())

    fun onEditTapped(item: NoteItem) {
        editingItem.value = item
    }

    fun onSaveEdit(text: String) {
        val item = editingItem.value ?: return
        viewModelScope.launch {
            notesRepository.update(item.id, text)
            editingItem.value = null
        }
    }

    fun dismissEdit() {
        editingItem.value = null
    }

    /** Deletes immediately but keeps the item around for one Undo - matches how a
     *  reversible action should behave (act now, offer a way back) rather than a
     *  confirm dialog for what's a low-stakes, easily-undone delete. */
    fun deleteNote(item: NoteItem) {
        viewModelScope.launch {
            notesRepository.delete(item.id)
            lastDeleted.value = item
        }
    }

    fun undoDelete() {
        val item = lastDeleted.value ?: return
        viewModelScope.launch {
            notesRepository.restore(item)
            lastDeleted.value = null
        }
    }

    fun dismissUndo() {
        lastDeleted.value = null
    }

    class Factory(private val notesRepository: NotesRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return NotesViewModel(notesRepository) as T
        }
    }
}
