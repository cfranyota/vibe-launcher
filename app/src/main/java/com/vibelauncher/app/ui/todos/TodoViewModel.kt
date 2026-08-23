package com.vibelauncher.app.ui.todos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vibelauncher.app.data.todos.TodoRepository
import com.vibelauncher.app.model.TodoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoViewModel(private val todoRepository: TodoRepository) : ViewModel() {

    private val editingItem = MutableStateFlow<TodoItem?>(null)
    private val lastDeleted = MutableStateFlow<TodoItem?>(null)

    val uiState: StateFlow<TodoUiState> = combine(
        todoRepository.todos,
        editingItem,
        lastDeleted
    ) { todos, editing, deleted ->
        TodoUiState(todos = todos.sortedByDescending { it.createdAt }, editingItem = editing, lastDeleted = deleted)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodoUiState())

    fun onEditTapped(item: TodoItem) {
        editingItem.value = item
    }

    fun onSaveEdit(text: String) {
        val item = editingItem.value ?: return
        viewModelScope.launch {
            todoRepository.update(item.id, text)
            editingItem.value = null
        }
    }

    fun dismissEdit() {
        editingItem.value = null
    }

    /** Deletes immediately but keeps the item around for one Undo - matches how a
     *  reversible action should behave (act now, offer a way back) rather than a
     *  confirm dialog for what's a low-stakes, easily-undone delete. */
    fun deleteTodo(item: TodoItem) {
        viewModelScope.launch {
            todoRepository.delete(item.id)
            lastDeleted.value = item
        }
    }

    fun undoDelete() {
        val item = lastDeleted.value ?: return
        viewModelScope.launch {
            todoRepository.restore(item)
            lastDeleted.value = null
        }
    }

    fun dismissUndo() {
        lastDeleted.value = null
    }

    class Factory(private val todoRepository: TodoRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return TodoViewModel(todoRepository) as T
        }
    }
}
