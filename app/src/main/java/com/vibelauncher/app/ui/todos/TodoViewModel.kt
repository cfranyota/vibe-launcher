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
    private val filter = MutableStateFlow(TodoFilter.OPEN)
    private val selectedTaskId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<TodoUiState> = combine(
        todoRepository.todos, editingItem, lastDeleted, filter, selectedTaskId
    ) { todos, editing, deleted, filter, selectedId ->
        TodoUiState(
            todos = todos.sortedByDescending { it.createdAt },
            editingItem = editing,
            lastDeleted = deleted,
            filter = filter,
            selectedTaskId = selectedId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodoUiState())

    fun addTodo(text: String) {
        viewModelScope.launch { todoRepository.add(text) }
    }

    fun setFilter(newFilter: TodoFilter) {
        filter.value = newFilter
        selectedTaskId.value = null
    }

    fun onTaskSelected(id: Long) {
        selectedTaskId.value = if (selectedTaskId.value == id) null else id
    }

    fun onTaskDeselected() {
        selectedTaskId.value = null
    }

    fun markDone(item: TodoItem) {
        viewModelScope.launch {
            todoRepository.setDone(item.id, true)
            selectedTaskId.value = null
        }
    }

    fun toggleStarred(item: TodoItem) {
        viewModelScope.launch {
            todoRepository.setStarred(item.id, !item.starred)
            selectedTaskId.value = null
        }
    }

    fun onEditTapped(item: TodoItem) {
        editingItem.value = item
        selectedTaskId.value = null
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
            selectedTaskId.value = null
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
