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

class NotesRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(NoteItem.serializer())

    val notes = context.notesDataStore.data.map { prefs ->
        val stored = prefs[NOTES_KEY]
        stored?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() } ?: emptyList()
    }

    suspend fun add(text: String) {
        if (text.isBlank()) return
        val note = NoteItem(id = System.currentTimeMillis(), text = text.trim(), createdAt = System.currentTimeMillis())
        save { it + note }
    }

    suspend fun update(id: Long, text: String) {
        if (text.isBlank()) return
        save { list -> list.map { if (it.id == id) it.copy(text = text.trim()) else it } }
    }

    suspend fun delete(id: Long) {
        save { list -> list.filterNot { it.id == id } }
    }

    /** Puts a deleted note back exactly as it was (same id/createdAt) - backs the
     *  Notes screen's delete-with-Undo snackbar. */
    suspend fun restore(note: NoteItem) {
        save { list -> if (list.any { it.id == note.id }) list else list + note }
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
