package com.vibelauncher.app.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vibelauncher.app.data.notes.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class NoteListViewModel(private val noteRepository: NoteRepository) : ViewModel() {

    private val filter = MutableStateFlow(NoteFilterTab.ALL)
    private val query = MutableStateFlow("")
    private val searchVisible = MutableStateFlow(false)

    val uiState: StateFlow<NoteListUiState> = combine(
        noteRepository.notes, filter, query, searchVisible
    ) { notes, f, q, visible ->
        NoteListUiState(notes = notes, filter = f, query = q, searchVisible = visible)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NoteListUiState())

    fun setFilter(value: NoteFilterTab) {
        filter.value = value
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun toggleSearchVisible() {
        searchVisible.value = !searchVisible.value
        if (!searchVisible.value) query.value = ""
    }

    class Factory(private val noteRepository: NoteRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return NoteListViewModel(noteRepository) as T
        }
    }
}
