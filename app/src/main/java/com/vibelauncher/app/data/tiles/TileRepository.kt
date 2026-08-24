package com.vibelauncher.app.data.tiles

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.vibelauncher.app.model.Tile
import com.vibelauncher.app.util.IntentDefaults
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class TileRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val listSerializer = ListSerializer(Tile.serializer())

    val tiles: Flow<List<Tile>> = context.tilesDataStore.data.map { prefs ->
        prefs[TILES_LIST_KEY]?.let { runCatching { json.decodeFromString(listSerializer, it) }.getOrNull() }
            ?: legacyOrDefault(prefs)
    }

    /** Reads the OLD per-slot keys if any are present (an existing user's current setup),
     *  else the canonical defaults. Read-only fallback, no write-back - the very next
     *  setTiles() call (any checkbox toggle, or Reset to Default) naturally populates
     *  TILES_LIST_KEY and this path stops being hit. */
    private fun legacyOrDefault(prefs: Preferences): List<Tile> =
        (0 until IntentDefaults.SLOT_COUNT).map { slot ->
            prefs[tileKey(slot)]?.let { runCatching { json.decodeFromString<Tile>(it) }.getOrNull() }
                ?: IntentDefaults.defaultTiles()[slot]
        }

    suspend fun setTiles(newTiles: List<Tile>) {
        val reindexed = newTiles.mapIndexed { i, t -> t.copy(id = i) }
        context.tilesDataStore.edit { it[TILES_LIST_KEY] = json.encodeToString(listSerializer, reindexed) }
    }

    suspend fun setTileAt(index: Int, tile: Tile) {
        val current = tiles.first().toMutableList()
        if (index < current.size) current[index] = tile.copy(id = index) else current.add(tile.copy(id = current.size))
        setTiles(current)
    }

    suspend fun resetToDefault() {
        setTiles(IntentDefaults.defaultTiles())
    }
}
