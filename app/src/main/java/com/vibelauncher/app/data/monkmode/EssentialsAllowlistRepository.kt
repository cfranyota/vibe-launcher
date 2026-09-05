package com.vibelauncher.app.data.monkmode

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.essentialsDataStore by preferencesDataStore(name = "essentials_allowlist_prefs")
private val ESSENTIALS_KEY = stringSetPreferencesKey("essentials_apps")

private fun keyOf(packageName: String, className: String) = "$packageName/$className"

/** The app-drawer allowlist for Vibe Mode's "essentials only" tier - unbounded (unlike the
 *  home screen's capped-at-8 tile selection), off (empty) by default. */
class EssentialsAllowlistRepository(private val context: Context) {

    val allowlist = context.essentialsDataStore.data.map { it[ESSENTIALS_KEY] ?: emptySet() }

    suspend fun toggle(packageName: String, className: String) {
        val key = keyOf(packageName, className)
        context.essentialsDataStore.edit { prefs ->
            val current = prefs[ESSENTIALS_KEY] ?: emptySet()
            prefs[ESSENTIALS_KEY] = if (key in current) current - key else current + key
        }
    }
}
