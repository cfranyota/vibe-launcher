package com.vibelauncher.app.ui.notes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.ui.graphics.vector.ImageVector
import com.vibelauncher.app.model.NoteBlock
import com.vibelauncher.app.model.NoteCategory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

internal fun noteCategoryIcon(category: NoteCategory): ImageVector = when (category) {
    NoteCategory.PERSONAL -> Icons.Filled.Home
    NoteCategory.WORK -> Icons.Filled.Work
    NoteCategory.IDEAS -> Icons.Filled.LocationOn
    NoteCategory.JOURNAL -> Icons.AutoMirrored.Filled.MenuBook
}

internal fun noteCategoryLabel(category: NoteCategory): String = category.name.lowercase()
    .replaceFirstChar { it.uppercase() }

/** Strips markdown tokens for a plain preview line. */
internal fun plainTextOf(blocks: List<NoteBlock>): String = blocks
    .mapNotNull { it.spans.firstOrNull()?.text?.takeIf { text -> text.isNotBlank() } }
    .firstOrNull()
    ?.replace("**", "")?.replace("__", "")?.replace("*", "")
    .orEmpty()

private val TIME_FORMAT = SimpleDateFormat("h:mm a", Locale.getDefault())
private val DATE_FORMAT = SimpleDateFormat("MMM d", Locale.getDefault())

internal fun relativeNoteTime(millis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val today = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val then = Calendar.getInstance().apply { timeInMillis = millis }
    val sameDay = today.get(Calendar.YEAR) == then.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    if (sameDay) return TIME_FORMAT.format(millis).lowercase()

    val yesterday = Calendar.getInstance().apply { timeInMillis = nowMillis; add(Calendar.DAY_OF_YEAR, -1) }
    val wasYesterday = yesterday.get(Calendar.YEAR) == then.get(Calendar.YEAR) && yesterday.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    if (wasYesterday) return "yesterday"

    return DATE_FORMAT.format(millis)
}
