package com.vibelauncher.app.ui.home.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.vibelauncher.app.ui.theme.LauncherBlack
import com.vibelauncher.app.ui.theme.LauncherMutedGray
import com.vibelauncher.app.ui.theme.LauncherWhite
import com.vibelauncher.app.ui.theme.VibeNoteColor
import com.vibelauncher.app.util.IntentDefaults

/**
 * A half-page scratchpad, not a saved note: type something, share it or copy it, and it's
 * gone the moment this closes - no repository, no list, nothing written to disk. Opened
 * either by typing '/' in Vibe Bar or by tapping the home screen's "Note" tile directly.
 */
@Composable
fun NoteBubble(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var text by remember { mutableStateOf(TextFieldValue()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
        keyboard?.show()
    }

    BackHandler(enabled = true) { onDismiss() }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(LauncherBlack.copy(alpha = .55f))
                .clickable(onClick = onDismiss)
        )

        AnimatedVisibility(
            visible = true,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(animationSpec = tween(220), initialOffsetY = { it }) + fadeIn(tween(220)),
            exit = slideOutVertically(animationSpec = tween(200), targetOffsetY = { it }) + fadeOut(tween(200))
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.5f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp
            ) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Note", color = LauncherMutedGray, style = MaterialTheme.typography.labelLarge)
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.DeleteOutline, "Delete draft", tint = LauncherMutedGray)
                        }
                    }
                    NoteTextField(
                        value = text,
                        onValueChange = { text = it },
                        focusRequester = focusRequester,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(text.text))
                            },
                            enabled = text.text.isNotBlank()
                        ) {
                            Icon(Icons.Filled.ContentCopy, "Copy", tint = LauncherMutedGray)
                        }
                        FilledIconButton(
                            onClick = { IntentDefaults.shareText(context, text.text) },
                            enabled = text.text.isNotBlank(),
                            modifier = Modifier.padding(start = 8.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = VibeNoteColor,
                                contentColor = LauncherWhite
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, "Share")
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete draft?") },
            text = { Text("This draft will permanently be cleared.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        text = TextFieldValue()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Type a note…", color = LauncherMutedGray) },
        modifier = modifier.focusRequester(focusRequester),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        singleLine = false,
        // Enter always inserts a newline - this is a scratchpad, not a submit field.
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
        keyboardActions = KeyboardActions()
    )
}
