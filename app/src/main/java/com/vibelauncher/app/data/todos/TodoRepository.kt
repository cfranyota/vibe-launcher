package com.vibelauncher.app.data.todos

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vibelauncher.app.model.TodoItem
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.todosDataStore by preferencesDataStore(name = "todos_prefs")
private val TODOS_KEY = stringPreferencesKey("todos")

class TodoRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(TodoItem.serializer())

    val todos = context.todosDataStore.data.map { prefs ->
        val stored = prefs[TODOS_KEY]
        stored?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() } ?: emptyList()
    }

    suspend fun add(text: String, dueAt: Long? = null, dueAllDay: Boolean = false) {
        if (text.isBlank()) return
        val now = System.currentTimeMillis()
        val todo = TodoItem(id = now, text = text.trim(), createdAt = now, dueAt = dueAt, dueAllDay = dueAllDay)
        save { it + todo }
    }

    /** Changes only the text - an edit that didn't mention a date keeps the due date it had. */
    suspend fun update(id: Long, text: String) {
        if (text.isBlank()) return
        save { list -> list.map { if (it.id == id) it.copy(text = text.trim()) else it } }
    }

    suspend fun updateWithDue(id: Long, text: String, dueAt: Long, dueAllDay: Boolean) {
        if (text.isBlank()) return
        save { list ->
            list.map { if (it.id == id) it.copy(text = text.trim(), dueAt = dueAt, dueAllDay = dueAllDay) else it }
        }
    }

    suspend fun clearDue(id: Long) {
        save { list -> list.map { if (it.id == id) it.copy(dueAt = null, dueAllDay = false) else it } }
    }

    suspend fun delete(id: Long) {
        save { list -> list.filterNot { it.id == id } }
    }

    suspend fun setDone(id: Long, done: Boolean) {
        save { list -> list.map { if (it.id == id) it.copy(done = done) else it } }
    }

    suspend fun setStarred(id: Long, starred: Boolean) {
        save { list -> list.map { if (it.id == id) it.copy(starred = starred) else it } }
    }

    /** Removes every finished to-do and returns what it removed, so the caller can offer
     *  them back through Undo. */
    suspend fun clearCompleted(): List<TodoItem> {
        var removed = emptyList<TodoItem>()
        save { list ->
            removed = list.filter { it.done }
            list.filterNot { it.done }
        }
        return removed
    }

    /** Puts removed to-dos back exactly as they were (same id/createdAt) - backs the To-Do
     *  screen's Undo for both a single delete and Clear completed. Ids are creation times,
     *  so sorting by id returns each one to its original place rather than the end. */
    suspend fun restoreAll(items: List<TodoItem>) {
        save { list ->
            val missing = items.filter { item -> list.none { it.id == item.id } }
            (list + missing).sortedBy { it.id }
        }
    }

    private suspend fun save(transform: (List<TodoItem>) -> List<TodoItem>) {
        context.todosDataStore.edit { prefs ->
            val current = prefs[TODOS_KEY]
                ?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }
                ?: emptyList()
            prefs[TODOS_KEY] = json.encodeToString(serializer, transform(current))
        }
    }
}
