package com.vibelauncher.app.data.notes

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vibelauncher.app.model.NoteItem
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.notesDataStore by preferencesDataStore(name = "notes_prefs")
private val NOTES_KEY = stringPreferencesKey("notes")

class NoteRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(NoteItem.serializer())

    val notes = context.notesDataStore.data.map { prefs ->
        val stored = prefs[NOTES_KEY]
        stored?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() } ?: emptyList()
    }

    /** Upsert by id - callers building a new note should stamp it with a fresh id first
     *  (e.g. System.currentTimeMillis(), same convention as TodoRepository.add). */
    suspend fun save(note: NoteItem) {
        save { list -> list.filterNot { it.id == note.id } + note }
    }

    suspend fun delete(id: Long) {
        save { list -> list.filterNot { it.id == id } }
    }

    suspend fun setPinned(id: Long, pinned: Boolean) {
        save { list -> list.map { if (it.id == id) it.copy(pinned = pinned) else it } }
    }

    private suspend fun save(transform: (List<NoteItem>) -> List<NoteItem>) {
        context.notesDataStore.edit { prefs ->
            val current = prefs[NOTES_KEY]
                ?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }
                ?: emptyList()
            prefs[NOTES_KEY] = json.encodeToString(serializer, transform(current))
        }
    }
}
