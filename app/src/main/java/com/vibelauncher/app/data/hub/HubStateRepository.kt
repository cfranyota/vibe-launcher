package com.vibelauncher.app.data.hub

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.hubStateDataStore by preferencesDataStore(name = "hub_state_prefs")
private val HUB_STATE_KEY = stringPreferencesKey("hub_item_states")

class HubStateRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(HubItemFlagState.serializer())

    val states = context.hubStateDataStore.data.map { prefs ->
        val stored = prefs[HUB_STATE_KEY]
        stored?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() } ?: emptyList()
    }

    suspend fun setFlagged(itemId: String, flagged: Boolean) {
        save { list -> upsert(list, itemId) { it.copy(flagged = flagged) } }
    }

    suspend fun setCategory(itemId: String, category: HubCategory?) {
        save { list -> upsert(list, itemId) { it.copy(category = category) } }
    }

    private fun upsert(list: List<HubItemFlagState>, itemId: String, transform: (HubItemFlagState) -> HubItemFlagState): List<HubItemFlagState> {
        val existing = list.find { it.itemId == itemId }
        val updated = transform(existing ?: HubItemFlagState(itemId = itemId))
        return list.filterNot { it.itemId == itemId } + updated
    }

    private suspend fun save(transform: (List<HubItemFlagState>) -> List<HubItemFlagState>) {
        context.hubStateDataStore.edit { prefs ->
            val current = prefs[HUB_STATE_KEY]
                ?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }
                ?: emptyList()
            prefs[HUB_STATE_KEY] = json.encodeToString(serializer, transform(current))
        }
    }
}
