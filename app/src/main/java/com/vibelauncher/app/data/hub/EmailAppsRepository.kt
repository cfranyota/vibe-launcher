package com.vibelauncher.app.data.hub

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.emailAppsDataStore by preferencesDataStore(name = "email_apps_prefs")
private val EMAIL_APPS_KEY = stringSetPreferencesKey("email_app_packages")

/** User-configured packages that count as "email" for Hub's EMAIL filter tab, on top of the
 *  small hardcoded [EMAIL_PACKAGES] set - lets the user add a mail app that isn't in that
 *  built-in list. Package-level (not activity-level, unlike EssentialsAllowlistRepository),
 *  since a notification's packageName is all Hub has to classify it. */
class EmailAppsRepository(private val context: Context) {

    val selectedPackages = context.emailAppsDataStore.data.map { it[EMAIL_APPS_KEY] ?: emptySet() }

    suspend fun toggle(packageName: String) {
        context.emailAppsDataStore.edit { prefs ->
            val current = prefs[EMAIL_APPS_KEY] ?: emptySet()
            prefs[EMAIL_APPS_KEY] = if (packageName in current) current - packageName else current + packageName
        }
    }
}
