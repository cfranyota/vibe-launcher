package com.vibelauncher.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.vibelauncher.app.model.NoteBlock
import com.vibelauncher.app.model.NoteBlockType
import com.vibelauncher.app.model.NoteCategory
import com.vibelauncher.app.ui.theme.LauncherMutedGray
import com.vibelauncher.app.ui.theme.LauncherWhite
import com.vibelauncher.app.ui.theme.LocalAccentColor
import com.vibelauncher.app.ui.theme.settingsTypography

@Composable
fun NoteEditorScreen(viewModel: NoteEditorViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val accent = LocalAccentColor.current

    DisposableEffect(Unit) {
        onDispose { viewModel.saveAndExit() }
    }

    MaterialTheme(typography = settingsTypography()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LauncherWhite)
                }
                Box(modifier = Modifier.weight(1f))
                IconButton(onClick = viewModel::togglePinned) {
                    Icon(
                        imageVector = if (uiState.pinned) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "Pin",
                        tint = if (uiState.pinned) accent else LauncherMutedGray
                    )
                }
                if (!uiState.isNew) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = LauncherMutedGray)
                    }
                }
                Box {
                    Text(
                        text = noteCategoryLabel(uiState.category),
                        color = accent,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.clickable { showCategoryMenu = true }.padding(horizontal = 8.dp)
                    )
                    DropdownMenu(expanded = showCategoryMenu, onDismissRequest = { showCategoryMenu = false }) {
                        NoteCategory.entries.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(noteCategoryLabel(category)) },
                                onClick = { viewModel.setCategory(category); showCategoryMenu = false }
                            )
                        }
                    }
                }
            }

            TextField(
                value = uiState.title,
                onValueChange = viewModel::setTitle,
                placeholder = { Text("Title", color = LauncherMutedGray) },
                textStyle = MaterialTheme.typography.titleLarge,
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                items(uiState.blocks.size) { index ->
                    NoteBlockRow(
                        block = uiState.blocks[index],
                        numberInList = uiState.blocks.take(index + 1).count { it.type == NoteBlockType.NUMBERED },
                        accent = accent,
                        onFocus = { viewModel.setFocusedBlock(index) },
                        onTextChange = { viewModel.updateBlockText(index, it) },
                        onToggleChecked = { viewModel.toggleChecked(index) },
                        onEnter = { cursor -> viewModel.splitBlock(index, cursor) }
                    )
                }
            }

            NoteFormattingToolbar(
                canUndo = uiState.canUndo,
                canRedo = uiState.canRedo,
                onSetTextType = { viewModel.setBlockType(uiState.focusedBlockIndex, NoteBlockType.TEXT) },
                onBold = { start, end -> viewModel.toggleTokenAroundSelection(uiState.focusedBlockIndex, start, end, BOLD_TOKEN) },
                onItalic = { start, end -> viewModel.toggleTokenAroundSelection(uiState.focusedBlockIndex, start, end, ITALIC_TOKEN) },
                onUnderline = { start, end -> viewModel.toggleTokenAroundSelection(uiState.focusedBlockIndex, start, end, UNDERLINE_TOKEN) },
                onChecklist = { viewModel.setBlockType(uiState.focusedBlockIndex, NoteBlockType.CHECKLIST) },
                onBullet = { viewModel.setBlockType(uiState.focusedBlockIndex, NoteBlockType.BULLET) },
                onNumbered = { viewModel.setBlockType(uiState.focusedBlockIndex, NoteBlockType.NUMBERED) },
                onUndo = viewModel::undo,
                onRedo = viewModel::redo
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete note?") },
            text = { Text("This note will be permanently deleted.") },
            confirmButton = {
                Button(
                    onClick = { showDeleteConfirm = false; viewModel.delete(); onBack() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun NoteBlockRow(
    block: NoteBlock,
    numberInList: Int,
    accent: Color,
    onFocus: () -> Unit,
    onTextChange: (String) -> Unit,
    onToggleChecked: () -> Unit,
    onEnter: (cursorOffset: Int) -> Unit
) {
    val externalText = block.spans.firstOrNull()?.text.orEmpty()
    var fieldValue by remember { mutableStateOf(TextFieldValue(externalText)) }
    var lastPushedText by remember { mutableStateOf(externalText) }

    // Only resync from the ViewModel's block when the change didn't originate from this
    // field's own typing (undo/redo, or the async load of an existing note's saved text
    // arriving after first composition) - resyncing on every keystroke's own round-trip
    // would reset the cursor to the start of the text each time (TextFieldValue(text)'s
    // default selection is TextRange.Zero), making every subsequent character insert at
    // position 0 instead of at the cursor - the exact cause of the reversed-text bug.
    LaunchedEffect(externalText) {
        if (externalText != lastPushedText) {
            fieldValue = TextFieldValue(externalText, selection = TextRange(externalText.length))
            lastPushedText = externalText
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        when (block.type) {
            NoteBlockType.CHECKLIST -> Checkbox(
                checked = block.checked,
                onCheckedChange = { onToggleChecked() },
                colors = CheckboxDefaults.colors(checkedColor = accent),
                modifier = Modifier.padding(top = 8.dp)
            )
            NoteBlockType.BULLET -> Text("•", color = LauncherMutedGray, modifier = Modifier.padding(top = 16.dp, end = 8.dp, start = 12.dp))
            NoteBlockType.NUMBERED -> Text(
                "${numberInList}.",
                color = LauncherMutedGray,
                modifier = Modifier.padding(top = 16.dp, end = 8.dp, start = 8.dp)
            )
            NoteBlockType.TEXT -> {}
        }
        TextField(
            value = fieldValue,
            onValueChange = { newValue ->
                val enterPressed = newValue.text.length == fieldValue.text.length + 1 &&
                    newValue.text.getOrNull(newValue.selection.start - 1) == '\n'
                if (enterPressed) {
                    val cursor = newValue.selection.start - 1
                    onEnter(cursor)
                } else {
                    fieldValue = newValue
                    lastPushedText = newValue.text
                    onTextChange(newValue.text)
                }
            },
            visualTransformation = noteFormattingTransformation(LauncherMutedGray),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                textDecoration = if (block.type == NoteBlockType.CHECKLIST && block.checked) TextDecoration.LineThrough else null,
                color = if (block.type == NoteBlockType.CHECKLIST && block.checked) LauncherMutedGray else LauncherWhite
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Default),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = if (block.type == NoteBlockType.TEXT) 4.dp else 0.dp)
                .onFocusChanged { state -> if (state.isFocused) onFocus() }
        )
    }
}

@Composable
private fun NoteFormattingToolbar(
    canUndo: Boolean,
    canRedo: Boolean,
    onSetTextType: () -> Unit,
    onBold: (Int, Int) -> Unit,
    onItalic: (Int, Int) -> Unit,
    onUnderline: (Int, Int) -> Unit,
    onChecklist: () -> Unit,
    onBullet: () -> Unit,
    onNumbered: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onSetTextType) { Icon(Icons.Filled.TextFields, "Text", tint = LauncherMutedGray) }
        IconButton(onClick = { onBold(0, 0) }) { Icon(Icons.Filled.FormatBold, "Bold", tint = LauncherMutedGray) }
        IconButton(onClick = { onItalic(0, 0) }) { Icon(Icons.Filled.FormatItalic, "Italic", tint = LauncherMutedGray) }
        IconButton(onClick = { onUnderline(0, 0) }) { Icon(Icons.Filled.FormatUnderlined, "Underline", tint = LauncherMutedGray) }
        IconButton(onClick = onChecklist) { Icon(Icons.Filled.Checklist, "Checklist", tint = LauncherMutedGray) }
        IconButton(onClick = onBullet) { Icon(Icons.Filled.FormatListBulleted, "Bullet list", tint = LauncherMutedGray) }
        IconButton(onClick = onNumbered) { Icon(Icons.Filled.FormatListNumbered, "Numbered list", tint = LauncherMutedGray) }
        IconButton(onClick = onUndo, enabled = canUndo) {
            Icon(Icons.Filled.Undo, "Undo", tint = if (canUndo) LauncherWhite else LauncherMutedGray.copy(alpha = 0.4f))
        }
        IconButton(onClick = onRedo, enabled = canRedo) {
            Icon(Icons.Filled.Redo, "Redo", tint = if (canRedo) LauncherWhite else LauncherMutedGray.copy(alpha = 0.4f))
        }
    }
}
