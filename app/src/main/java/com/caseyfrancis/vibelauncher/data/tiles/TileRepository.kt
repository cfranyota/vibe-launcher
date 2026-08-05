package com.caseyfrancis.vibelauncher.data.tiles

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.caseyfrancis.vibelauncher.model.Tile
import com.caseyfrancis.vibelauncher.model.TileTarget
import com.caseyfrancis.vibelauncher.util.IntentDefaults
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class TileRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    val tiles = context.tilesDataStore.data.map { prefs ->
        (0 until IntentDefaults.SLOT_COUNT).map { slot ->
            val stored = prefs[tileKey(slot)]
            val decoded = stored?.let { runCatching { json.decodeFromString<Tile>(it) }.getOrNull() }
            decoded ?: IntentDefaults.defaultTiles()[slot]
        }
    }

    suspend fun setTile(slot: Int, label: String, iconKey: String, target: TileTarget) {
        val tile = Tile(id = slot, label = label, iconKey = iconKey, target = target)
        context.tilesDataStore.edit { prefs ->
            prefs[tileKey(slot)] = json.encodeToString(Tile.serializer(), tile)
        }
    }

    suspend fun resetTile(slot: Int) {
        context.tilesDataStore.edit { prefs ->
            prefs.remove(tileKey(slot))
        }
    }
}
