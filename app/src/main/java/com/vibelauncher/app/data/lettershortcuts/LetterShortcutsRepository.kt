package com.vibelauncher.app.data.lettershortcuts

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.letterShortcutsDataStore by preferencesDataStore(name = "letter_shortcuts_prefs")
private val LETTER_SHORTCUTS_KEY = stringPreferencesKey("letter_shortcuts")

class LetterShortcutsRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(LetterShortcut.serializer())

    val shortcuts = context.letterShortcutsDataStore.data.map { prefs ->
        val stored = prefs[LETTER_SHORTCUTS_KEY]
        stored?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() } ?: emptyList()
    }

    suspend fun setShortcut(shortcut: LetterShortcut) {
        save { list -> list.filterNot { it.letter == shortcut.letter } + shortcut }
    }

    suspend fun clearShortcut(letter: Char) {
        save { list -> list.filterNot { it.letter == letter } }
    }

    private suspend fun save(transform: (List<LetterShortcut>) -> List<LetterShortcut>) {
        context.letterShortcutsDataStore.edit { prefs ->
            val current = prefs[LETTER_SHORTCUTS_KEY]
                ?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }
                ?: emptyList()
            prefs[LETTER_SHORTCUTS_KEY] = json.encodeToString(serializer, transform(current))
        }
    }
}
