package com.vibelauncher.app.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.vibelauncher.app.data.apps.AppInfo
import com.vibelauncher.app.data.lettershortcuts.LetterShortcut
import com.vibelauncher.app.data.lettershortcuts.LetterShortcutType
import com.vibelauncher.app.ui.home.components.ContactSuggestionRow
import com.vibelauncher.app.ui.theme.LauncherBlack
import com.vibelauncher.app.ui.theme.LauncherMutedGray
import com.vibelauncher.app.ui.theme.LauncherWhite
import com.vibelauncher.app.ui.theme.LocalAccentColor
import com.vibelauncher.app.ui.theme.settingsTypography

/** A-Z list - hold any assigned letter on the hardware keyboard from the collapsed home
 *  screen to run its action; unassigned letters (the default) behave exactly like a tap. */
@Composable
fun LetterShortcutsScreen(viewModel: LetterShortcutsViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    var editingLetter by remember { mutableStateOf<Char?>(null) }
    var editingType by remember { mutableStateOf<LetterShortcutType?>(null) }

    fun closePicker() {
        editingLetter = null
        editingType = null
        viewModel.resetPickerQueries()
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
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back", tint = LauncherWhite)
                }
                Text(
                    text = "letter shortcuts",
                    color = LauncherWhite,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
            }
            Text(
                text = "Hold a letter on your keyboard from the home screen to run its action. Unassigned letters open Vibe Bar as usual.",
                color = LauncherMutedGray,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
            )
            LazyColumn {
                items(('A'..'Z').toList()) { letter ->
                    LetterRow(
                        letter = letter,
                        shortcut = uiState.shortcuts[letter],
                        onClick = { editingLetter = letter; editingType = null }
                    )
                }
            }
        }
    }

    editingLetter?.let { letter ->
        Dialog(onDismissRequest = { closePicker() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            MaterialTheme(typography = settingsTypography()) {
                Column(Modifier.fillMaxSize().background(LauncherBlack)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (editingType != null) editingType = null else closePicker()
                        }) {
                            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back", tint = LauncherWhite)
                        }
                        Text(
                            text = "hold '$letter'",
                            color = LauncherWhite,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f).padding(start = 8.dp)
                        )
                        IconButton(onClick = { closePicker() }) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = LauncherWhite)
                        }
                    }
                    when (val type = editingType) {
                        null -> TypeChoiceStep(
                            hasExistingShortcut = uiState.shortcuts.containsKey(letter),
                            onChooseType = { editingType = it },
                            onClear = { viewModel.clear(letter); closePicker() }
                        )
                        LetterShortcutType.OPEN_APP -> AppPickerStep(
                            uiState = uiState,
                            onQueryChange = viewModel::onAppQueryChange,
                            onPick = { app -> viewModel.assignApp(letter, app); closePicker() }
                        )
                        LetterShortcutType.MESSAGE_CONTACT, LetterShortcutType.CALL_CONTACT -> ContactPickerStep(
                            uiState = uiState,
                            onQueryChange = viewModel::onContactQueryChange,
                            onPick = { contact -> viewModel.assignContact(letter, type, contact); closePicker() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LetterRow(letter: Char, shortcut: LetterShortcut?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = letter.toString(), color = LauncherWhite, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = shortcut?.let { "${typeLabel(it.type)}: ${it.label}" } ?: "unassigned",
            color = if (shortcut != null) LocalAccentColor.current else LauncherMutedGray,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun typeLabel(type: LetterShortcutType): String = when (type) {
    LetterShortcutType.OPEN_APP -> "open"
    LetterShortcutType.MESSAGE_CONTACT -> "message"
    LetterShortcutType.CALL_CONTACT -> "call"
}

@Composable
private fun TypeChoiceStep(hasExistingShortcut: Boolean, onChooseType: (LetterShortcutType) -> Unit, onClear: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TypeChoiceRow("Open an app", Icons.Filled.OpenInNew) { onChooseType(LetterShortcutType.OPEN_APP) }
        TypeChoiceRow("Message a contact", Icons.Filled.Message) { onChooseType(LetterShortcutType.MESSAGE_CONTACT) }
        TypeChoiceRow("Call a contact", Icons.Filled.Call) { onChooseType(LetterShortcutType.CALL_CONTACT) }
        if (hasExistingShortcut) {
            Text(
                text = "clear shortcut",
                color = LauncherMutedGray,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.clickable(onClick = onClear).padding(vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun TypeChoiceRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = LauncherWhite, modifier = Modifier.size(24.dp))
        Text(text = label, color = LauncherWhite, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
    }
}

@Composable
private fun AppPickerStep(uiState: LetterShortcutsUiState, onQueryChange: (String) -> Unit, onPick: (AppInfo) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = uiState.appQuery,
            onValueChange = onQueryChange,
            placeholder = { Text("Search apps") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyColumn {
            items(uiState.apps, key = { it.packageName to it.className }) { app ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onPick(app) }.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val painter = remember(app.packageName) { BitmapPainter(app.icon.toBitmap().asImageBitmap()) }
                    Image(painter = painter, contentDescription = app.label, modifier = Modifier.size(32.dp))
                    Text(
                        text = app.label,
                        color = LauncherWhite,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactPickerStep(
    uiState: LetterShortcutsUiState,
    onQueryChange: (String) -> Unit,
    onPick: (com.vibelauncher.app.data.contacts.ContactResult) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = uiState.contactQuery,
            onValueChange = onQueryChange,
            placeholder = { Text("Search contacts") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.contactResults, key = { it.contactId }) { contact ->
                ContactSuggestionRow(contact = contact, emphasized = false, onClick = { onPick(contact) })
            }
        }
    }
}
