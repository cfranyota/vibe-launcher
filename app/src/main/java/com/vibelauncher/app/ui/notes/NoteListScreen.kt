package com.vibelauncher.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vibelauncher.app.model.NoteItem
import com.vibelauncher.app.ui.theme.CardCornerShape
import com.vibelauncher.app.ui.theme.LauncherMutedGray
import com.vibelauncher.app.ui.theme.LauncherWhite
import com.vibelauncher.app.ui.theme.LocalAccentColor
import com.vibelauncher.app.ui.theme.settingsTypography

@Composable
fun NoteListScreen(viewModel: NoteListViewModel, onBack: () -> Unit, onOpenNote: (Long) -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val accent = LocalAccentColor.current

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
                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    Text("notes", color = LauncherWhite, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${uiState.notes.size} notes",
                        color = LauncherMutedGray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                IconButton(onClick = viewModel::toggleSearchVisible) {
                    Icon(Icons.Filled.Search, contentDescription = "Search", tint = LauncherWhite)
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(accent)
                        .clickable { onOpenNote(-1L) }
                        .padding(10.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "New note", tint = LauncherWhite)
                }
            }

            if (uiState.searchVisible) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::setQuery,
                    placeholder = { Text("Search notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NoteFilterTab.entries.forEach { tab ->
                    Column {
                        Text(
                            text = tab.name.lowercase(),
                            color = if (tab == uiState.filter) accent else LauncherMutedGray,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.clickable { viewModel.setFilter(tab) }
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.filteredNotes, key = { it.id }) { note ->
                    NoteCard(note = note, accent = accent, onClick = { onOpenNote(note.id) })
                }
            }
        }
    }
}

@Composable
private fun NoteCard(note: NoteItem, accent: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardCornerShape)
            .border(1.dp, LauncherMutedGray.copy(alpha = 0.35f), CardCornerShape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = noteCategoryIcon(note.category),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = note.title.ifBlank { "Untitled" },
                    color = LauncherWhite,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${noteCategoryLabel(note.category).uppercase()} ${relativeNoteTime(note.updatedAt)}",
                    color = LauncherMutedGray,
                    style = MaterialTheme.typography.labelSmall
                )
                if (note.pinned) {
                    Box(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                }
            }
            val preview = plainTextOf(note.blocks)
            if (preview.isNotBlank()) {
                Text(
                    text = preview,
                    color = LauncherMutedGray,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
