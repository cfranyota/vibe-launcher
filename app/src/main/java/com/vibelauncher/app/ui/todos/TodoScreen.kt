package com.vibelauncher.app.ui.todos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibelauncher.app.model.TodoItem
import com.vibelauncher.app.ui.theme.BadgeCornerShape
import com.vibelauncher.app.ui.theme.LauncherCard
import com.vibelauncher.app.ui.theme.LauncherMutedGray
import com.vibelauncher.app.ui.theme.LauncherRed
import com.vibelauncher.app.ui.theme.LauncherWhite
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
                    actionColor = LauncherRed
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TodoHeader(
                uiState = uiState,
                onBack = onBack,
                onFilterSelected = viewModel::setFilter,
                onDismissSelection = viewModel::onTaskDeselected,
                onEdit = { viewModel.onEditTapped(it) },
                onDelete = { viewModel.deleteTodo(it) },
                onMarkDone = { viewModel.markDone(it) },
                onToggleStarred = { viewModel.toggleStarred(it) }
            )

            AddTaskRow(onSubmit = viewModel::addTodo)

            if (uiState.visibleTodos.isEmpty()) {
                Text(
                    text = if (uiState.filter == TodoFilter.OPEN) {
                        "Nothing open - add a task above."
                    } else {
                        "No completed tasks yet."
                    },
                    color = LauncherMutedGray,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            } else {
                LazyColumn {
                    items(uiState.visibleTodos, key = { it.id }) { todo ->
                        TodoRow(
                            todo = todo,
                            selected = todo.id == uiState.selectedTaskId,
                            onSelect = { viewModel.onTaskSelected(todo.id) }
                        )
                    }
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
private fun TodoHeader(
    uiState: TodoUiState,
    onBack: () -> Unit,
    onFilterSelected: (TodoFilter) -> Unit,
    onDismissSelection: () -> Unit,
    onEdit: (TodoItem) -> Unit,
    onDelete: (TodoItem) -> Unit,
    onMarkDone: (TodoItem) -> Unit,
    onToggleStarred: (TodoItem) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back", tint = LauncherWhite)
        }
        Text(
            text = "to do",
            color = LauncherWhite,
            style = TitleTextStyle,
            modifier = Modifier.weight(1f).padding(start = 8.dp)
        )

        val selectedTask = uiState.selectedTask
        if (selectedTask != null) {
            Box {
                var menuExpanded by remember { mutableStateOf(true) }
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Task actions", tint = LauncherWhite)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = {
                        menuExpanded = false
                        onDismissSelection()
                    }
                ) {
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { menuExpanded = false; onEdit(selectedTask) })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menuExpanded = false; onDelete(selectedTask) })
                    if (!selectedTask.done) {
                        DropdownMenuItem(text = { Text("Mark done") }, onClick = { menuExpanded = false; onMarkDone(selectedTask) })
                    }
                    DropdownMenuItem(
                        text = { Text(if (selectedTask.starred) "Unstar" else "Star") },
                        onClick = { menuExpanded = false; onToggleStarred(selectedTask) }
                    )
                }
            }
        } else {
            FilterChip(
                label = "${uiState.openCount} open",
                active = uiState.filter == TodoFilter.OPEN,
                showDot = true,
                onClick = { onFilterSelected(TodoFilter.OPEN) }
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                label = "${uiState.doneCount} done",
                active = uiState.filter == TodoFilter.DONE,
                showDot = false,
                onClick = { onFilterSelected(TodoFilter.DONE) }
            )
        }
    }
}

@Composable
private fun FilterChip(label: String, active: Boolean, showDot: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable { onClick() }
            .border(1.dp, if (active) LauncherRed else LauncherMutedGray, BadgeCornerShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showDot) {
            Icon(
                imageVector = Icons.Filled.FiberManualRecord,
                contentDescription = null,
                tint = LauncherRed,
                modifier = Modifier.size(8.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            color = if (active) LauncherWhite else LauncherMutedGray,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
        )
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
        leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, tint = LauncherRed) },
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
            cursorColor = LauncherRed
        ),
        shape = BadgeCornerShape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .border(1.dp, LauncherMutedGray, BadgeCornerShape)
    )
}

@Composable
private fun TodoRow(todo: TodoItem, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .background(if (selected) LauncherCard else Color.Transparent, TileCornerShape)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Circle,
            contentDescription = null,
            tint = LauncherMutedGray,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = todo.text,
            color = LauncherWhite,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        )
        if (todo.starred) {
            Icon(imageVector = Icons.Filled.Star, contentDescription = null, tint = LauncherRed, modifier = Modifier.size(16.dp))
        } else {
            Icon(
                imageVector = Icons.Filled.FiberManualRecord,
                contentDescription = null,
                tint = LauncherRed,
                modifier = Modifier.size(8.dp)
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(text = todo.ageLabel(), color = LauncherMutedGray, style = MaterialTheme.typography.labelSmall)
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
