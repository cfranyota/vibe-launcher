package com.vibelauncher.app.data.tiles

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.tilesDataStore by preferencesDataStore(name = "tiles_prefs")

// Legacy per-slot keys - kept only so TileRepository can read an existing user's current
// setup as a one-time fallback; no longer written to.
fun tileKey(slot: Int) = stringPreferencesKey("tile_$slot")

val TILES_LIST_KEY = stringPreferencesKey("tiles_list")
