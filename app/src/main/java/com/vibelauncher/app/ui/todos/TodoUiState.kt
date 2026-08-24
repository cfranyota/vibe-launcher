package com.vibelauncher.app.ui.todos

import com.vibelauncher.app.model.TodoItem

enum class TodoFilter { OPEN, DONE }

data class TodoUiState(
    val todos: List<TodoItem> = emptyList(),
    val editingItem: TodoItem? = null,
    val lastDeleted: TodoItem? = null,
    val filter: TodoFilter = TodoFilter.OPEN,
    val selectedTaskId: Long? = null
) {
    val openCount: Int get() = todos.count { !it.done }
    val doneCount: Int get() = todos.count { it.done }
    val visibleTodos: List<TodoItem> get() = todos.filter { if (filter == TodoFilter.OPEN) !it.done else it.done }
    val selectedTask: TodoItem? get() = todos.find { it.id == selectedTaskId }
}
