package com.vibelauncher.app.ui.todos

import com.vibelauncher.app.model.TodoItem

enum class TodoSort { NEWEST, OLDEST, STARRED_FIRST }

data class TodoUiState(
    val todos: List<TodoItem> = emptyList(),
    val editingItem: TodoItem? = null,
    val lastDeleted: TodoItem? = null,
    val sort: TodoSort = TodoSort.NEWEST,
    val menuForTaskId: Long? = null
) {
    val openCount: Int get() = todos.count { !it.done }
    val doneCount: Int get() = todos.count { it.done }

    val sortedTodos: List<TodoItem>
        get() = when (sort) {
            TodoSort.NEWEST -> todos.sortedByDescending { it.createdAt }
            TodoSort.OLDEST -> todos.sortedBy { it.createdAt }
            TodoSort.STARRED_FIRST -> todos.sortedWith(compareByDescending<TodoItem> { it.starred }.thenByDescending { it.createdAt })
        }

    val menuTask: TodoItem? get() = todos.find { it.id == menuForTaskId }
}
