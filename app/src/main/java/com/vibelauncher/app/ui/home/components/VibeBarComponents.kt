package com.vibelauncher.app.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibelauncher.app.ui.theme.LauncherMutedGray
import com.vibelauncher.app.ui.theme.LauncherWhite
import com.vibelauncher.app.ui.theme.VibeAppColor
import com.vibelauncher.app.ui.theme.VibeCallColor
import com.vibelauncher.app.ui.theme.VibeEventColor
import com.vibelauncher.app.ui.theme.VibeNoteColor
import com.vibelauncher.app.ui.theme.VibeTextColor
import com.vibelauncher.app.ui.theme.VibeTodoColor
import com.vibelauncher.app.features.vibebar.VIBE_BAR_NOTE_PREFIX

private data class VibeBarCommand(val key: Char, val label: String, val color: Color)

/** Shown when the bar is expanded and empty - a grid of the 6 hotkeys, tap one to
 *  pre-fill that prefix. */
@Composable
internal fun VibeBarLegend(activePrefix: Char?, onSelect: (Char) -> Unit) {
    val commands = listOf(
        VibeBarCommand('@', "Text", VibeTextColor),
        VibeBarCommand('#', "Call", VibeCallColor),
        VibeBarCommand('-', "To-Do", VibeTodoColor),
        VibeBarCommand(VIBE_BAR_NOTE_PREFIX, "Note", VibeNoteColor),
        VibeBarCommand('+', "Event", VibeEventColor),
        VibeBarCommand('?', "App", VibeAppColor)
    )
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .96f),
        shadowElevation = 8.dp
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "HOT KEYS",
                    Modifier.weight(1f),
                    color = LauncherMutedGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text("just type to search", color = LauncherMutedGray, fontSize = 11.sp)
            }
            commands.chunked(3).forEach { rowCommands ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowCommands.forEach { command ->
                        val active = activePrefix == command.key
                        Surface(
                            onClick = { onSelect(command.key) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (active) command.color else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (active) LauncherWhite else MaterialTheme.colorScheme.onSurface
                        ) {
                            Row(
                                Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    command.key.toString(),
                                    fontWeight = FontWeight.Black,
                                    color = if (active) LauncherWhite else command.color
                                )
                                Text(
                                    command.label,
                                    Modifier.padding(start = 8.dp),
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The locked-in-contact chip shown in the input row after selecting a contact for '@'. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CommandChip(label: String, color: Color, contentColor: Color, onClear: () -> Unit) {
    InputChip(
        selected = true,
        onClick = onClear,
        label = { Text(label, maxLines = 1) },
        trailingIcon = { Icon(Icons.Default.Close, "Clear", Modifier.padding(0.dp)) },
        colors = InputChipDefaults.inputChipColors(
            selectedContainerColor = color,
            selectedLabelColor = contentColor,
            selectedTrailingIconColor = contentColor
        ),
        modifier = Modifier.padding(end = 4.dp)
    )
}

@Composable
internal fun SuggestionRow(
    text: String,
    icon: ImageVector? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingContent != null) {
            leadingContent()
        } else if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp), tint = contentColor)
        }
        Text(text, Modifier.padding(start = 12.dp), color = contentColor, maxLines = 1, fontSize = 14.sp)
    }
}
