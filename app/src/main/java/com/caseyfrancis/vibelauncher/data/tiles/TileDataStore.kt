package com.caseyfrancis.vibelauncher.data.tiles

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.tilesDataStore by preferencesDataStore(name = "tiles_prefs")

fun tileKey(slot: Int) = stringPreferencesKey("tile_$slot")
