package com.vibelauncher.app.ui.todos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vibelauncher.app.data.todos.TodoRepository
import com.vibelauncher.app.features.vibebar.parseTodoText
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
    private val sort = MutableStateFlow(TodoSort.NEWEST)
    private val menuForTaskId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<TodoUiState> = combine(
        todoRepository.todos, editingItem, lastDeleted, sort, menuForTaskId
    ) { todos, editing, deleted, sort, menuId ->
        TodoUiState(
            todos = todos,
            editingItem = editing,
            lastDeleted = deleted,
            sort = sort,
            menuForTaskId = menuId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodoUiState())

    /** Same one-line syntax as Vibe Bar's '-' - a date typed with the to-do becomes its due
     *  date ("ring vet tomorrow 4pm"). */
    fun addTodo(text: String) {
        val draft = parseTodoText(text)
        viewModelScope.launch { todoRepository.add(draft.text, draft.dueAt, draft.dueAllDay) }
    }

    fun setSort(newSort: TodoSort) {
        sort.value = newSort
    }

    fun onTaskLongPressed(id: Long) {
        menuForTaskId.value = if (menuForTaskId.value == id) null else id
    }

    fun onDismissMenu() {
        menuForTaskId.value = null
    }

    fun toggleDone(item: TodoItem) {
        viewModelScope.launch {
            todoRepository.setDone(item.id, !item.done)
            menuForTaskId.value = null
        }
    }

    fun toggleStarred(item: TodoItem) {
        viewModelScope.launch {
            todoRepository.setStarred(item.id, !item.starred)
            menuForTaskId.value = null
        }
    }

    fun onEditTapped(item: TodoItem) {
        editingItem.value = item
        menuForTaskId.value = null
    }

    /** A date in the edited text replaces the due date; an edit with no date in it only
     *  changes the text, so fixing a typo never silently drops a due date. */
    fun onSaveEdit(text: String) {
        val item = editingItem.value ?: return
        val draft = parseTodoText(text)
        viewModelScope.launch {
            if (draft.dueAt != null) {
                todoRepository.updateWithDue(item.id, draft.text, draft.dueAt, draft.dueAllDay)
            } else {
                todoRepository.update(item.id, draft.text)
            }
            editingItem.value = null
        }
    }

    fun clearDue(item: TodoItem) {
        viewModelScope.launch {
            todoRepository.clearDue(item.id)
            menuForTaskId.value = null
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
            menuForTaskId.value = null
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
