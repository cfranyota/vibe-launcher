package com.vibelauncher.app.ui.todos

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibelauncher.app.model.TodoItem
import com.vibelauncher.app.ui.theme.LauncherCard
import com.vibelauncher.app.ui.theme.LauncherMutedGray
import com.vibelauncher.app.ui.theme.LauncherWhite
import com.vibelauncher.app.ui.theme.LocalAccentColor
import com.vibelauncher.app.ui.theme.TileCornerShape
import java.util.concurrent.TimeUnit

private val TitleTextStyle = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold)

@Composable
fun TodoScreen(viewModel: TodoViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.lastDeleted) {
        if (uiState.lastDeleted != null) {
            val result = snackbarHostState.showSnackbar(message = "To-do deleted", actionLabel = "Undo")
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete()
            } else {
                viewModel.dismissUndo()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = LauncherCard,
                    contentColor = LauncherWhite,
                    actionColor = LocalAccentColor.current
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TodoHeader(uiState = uiState, onBack = onBack, onSortSelected = viewModel::setSort)

            if (uiState.todos.isEmpty()) {
                Text(
                    text = "Nothing here yet - add a task below.",
                    color = LauncherMutedGray,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
            ) {
                items(uiState.sortedTodos, key = { it.id }) { todo ->
                    TodoRow(
                        todo = todo,
                        menuOpen = todo.id == uiState.menuForTaskId,
                        onToggleDone = { viewModel.toggleDone(todo) },
                        onLongPress = { viewModel.onTaskLongPressed(todo.id) },
                        onDismissMenu = viewModel::onDismissMenu,
                        onEdit = { viewModel.onEditTapped(todo) },
                        onDelete = { viewModel.deleteTodo(todo) },
                        onToggleStarred = { viewModel.toggleStarred(todo) }
                    )
                }
                item {
                    AddTaskRow(onSubmit = viewModel::addTodo)
                }
            }
        }
    }

    val editingItem = uiState.editingItem
    if (editingItem != null) {
        EditTodoDialog(
            initialText = editingItem.text,
            onSave = { viewModel.onSaveEdit(it) },
            onDismiss = { viewModel.dismissEdit() }
        )
    }
}

@Composable
private fun TodoHeader(uiState: TodoUiState, onBack: () -> Unit, onSortSelected: (TodoSort) -> Unit) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back", tint = LauncherWhite)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text(text = "to do", color = LauncherWhite, style = TitleTextStyle)
            Text(
                text = "${uiState.openCount} open · ${uiState.doneCount} done",
                color = LauncherMutedGray,
                style = MaterialTheme.typography.labelSmall
            )
        }
        Box {
            IconButton(onClick = { sortMenuExpanded = true }) {
                Icon(imageVector = Icons.Filled.Tune, contentDescription = "Sort", tint = LauncherWhite)
            }
            DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                DropdownMenuItem(text = { Text("Newest first") }, onClick = { sortMenuExpanded = false; onSortSelected(TodoSort.NEWEST) })
                DropdownMenuItem(text = { Text("Oldest first") }, onClick = { sortMenuExpanded = false; onSortSelected(TodoSort.OLDEST) })
                DropdownMenuItem(text = { Text("Starred first") }, onClick = { sortMenuExpanded = false; onSortSelected(TodoSort.STARRED_FIRST) })
            }
        }
    }
}

@Composable
private fun AddTaskRow(onSubmit: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    fun submit() {
        if (text.isNotBlank()) {
            onSubmit(text)
            text = ""
        }
        focusManager.clearFocus()
    }

    TextField(
        value = text,
        onValueChange = { text = it },
        placeholder = { Text("add a task", color = LauncherMutedGray) },
        leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, tint = LocalAccentColor.current) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = LauncherWhite,
            unfocusedTextColor = LauncherWhite,
            cursorColor = LocalAccentColor.current
        ),
        shape = TileCornerShape,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LauncherMutedGray.copy(alpha = 0.3f), TileCornerShape)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TodoRow(
    todo: TodoItem,
    menuOpen: Boolean,
    onToggleDone: () -> Unit,
    onLongPress: () -> Unit,
    onDismissMenu: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStarred: () -> Unit
) {
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, LauncherMutedGray.copy(alpha = 0.3f), TileCornerShape)
                .combinedClickable(onClick = onToggleDone, onLongClick = onLongPress)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (todo.done) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(LocalAccentColor.current, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = LauncherWhite, modifier = Modifier.size(14.dp))
                }
            } else {
                Icon(
                    imageVector = Icons.Outlined.Circle,
                    contentDescription = null,
                    tint = LauncherMutedGray,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = todo.text,
                color = if (todo.done) LauncherMutedGray else LauncherWhite,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (todo.done) TextDecoration.LineThrough else null,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            )
            if (todo.starred) {
                Icon(imageVector = Icons.Filled.Star, contentDescription = null, tint = LocalAccentColor.current, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(text = todo.ageLabel(), color = LauncherMutedGray, style = MaterialTheme.typography.labelSmall)
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = onDismissMenu) {
            DropdownMenuItem(text = { Text("Edit") }, onClick = { onDismissMenu(); onEdit() })
            DropdownMenuItem(text = { Text("Delete") }, onClick = { onDismissMenu(); onDelete() })
            DropdownMenuItem(
                text = { Text(if (todo.starred) "Unstar" else "Star") },
                onClick = { onDismissMenu(); onToggleStarred() }
            )
        }
    }
}

private fun TodoItem.ageLabel(nowMillis: Long = System.currentTimeMillis()): String {
    val diff = (nowMillis - createdAt).coerceAtLeast(0)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        else -> "${days}d"
    }
}

@Composable
private fun EditTodoDialog(initialText: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = false
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(value) }, enabled = value.isNotBlank()) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
