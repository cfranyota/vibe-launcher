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

    suspend fun add(text: String) {
        if (text.isBlank()) return
        val todo = TodoItem(id = System.currentTimeMillis(), text = text.trim(), createdAt = System.currentTimeMillis())
        save { it + todo }
    }

    suspend fun update(id: Long, text: String) {
        if (text.isBlank()) return
        save { list -> list.map { if (it.id == id) it.copy(text = text.trim()) else it } }
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

    /** Puts a deleted to-do back exactly as it was (same id/createdAt) - backs the
     *  To-Do screen's delete-with-Undo snackbar. */
    suspend fun restore(todo: TodoItem) {
        save { list -> if (list.any { it.id == todo.id }) list else list + todo }
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
