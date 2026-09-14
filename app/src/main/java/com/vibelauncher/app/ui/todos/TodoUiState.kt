package com.vibelauncher.app.ui.todos

import com.vibelauncher.app.model.TodoItem

enum class TodoSort { NEWEST, OLDEST, STARRED_FIRST, DUE_SOONEST }

/** What the Undo snackbar would put back, and what it says - one delete or a whole Clear
 *  completed share the same single Undo. */
data class TodoUndo(val message: String, val items: List<TodoItem>)

data class TodoUiState(
    val todos: List<TodoItem> = emptyList(),
    val editingItem: TodoItem? = null,
    val pendingUndo: TodoUndo? = null,
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
            // Open to-dos with a date, soonest first; then open ones without a date, newest
            // first; finished ones last, whatever they were due.
            TodoSort.DUE_SOONEST -> todos.sortedWith(
                compareBy<TodoItem>({ it.done }, { it.dueAt == null }, { it.dueAt ?: 0L })
                    .thenByDescending { it.createdAt }
            )
        }

    val menuTask: TodoItem? get() = todos.find { it.id == menuForTaskId }
}
