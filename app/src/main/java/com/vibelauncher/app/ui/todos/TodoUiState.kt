package com.vibelauncher.app.ui.todos

import com.vibelauncher.app.model.TodoItem

data class TodoUiState(
    val todos: List<TodoItem> = emptyList(),
    val editingItem: TodoItem? = null,
    val lastDeleted: TodoItem? = null
)
