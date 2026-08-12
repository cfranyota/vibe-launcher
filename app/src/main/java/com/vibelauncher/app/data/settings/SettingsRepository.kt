package com.vibelauncher.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings_prefs")
private val ZIP_CODE_KEY = stringPreferencesKey("zip_code")
private val ICON_THEME_PACKAGE_KEY = stringPreferencesKey("icon_theme_package")
private val APPLY_ICON_THEME_TO_HOME_TILES_KEY = booleanPreferencesKey("apply_icon_theme_to_home_tiles")
private val EVENT_CARD_COLOR_KEY = intPreferencesKey("event_card_color")
private val EVENT_CARD_COLOR_ENABLED_KEY = booleanPreferencesKey("event_card_color_enabled")
private val TILE_BORDER_ENABLED_KEY = booleanPreferencesKey("tile_border_enabled")
private val TILE_BORDER_SIZE_STEP_KEY = intPreferencesKey("tile_border_size_step")

/** Default event-card color, matching `LauncherCard` in ui/theme/Color.kt (0xFF1A1A1A) -
 *  duplicated as a raw constant here so this data-layer file doesn't need to depend on
 *  the UI theme package. */
private const val DEFAULT_EVENT_CARD_COLOR = 0xFF1A1A1A.toInt()

/** Default border-size step on the 1-10 scale (see TileView.kt's resolveTileSizeDp) - the
 *  midpoint, not the max. */
private const val DEFAULT_TILE_BORDER_SIZE_STEP = 5

class SettingsRepository(private val context: Context) {

    val zipCode = context.settingsDataStore.data.map { it[ZIP_CODE_KEY] ?: "" }

    suspend fun setZipCode(zipCode: String) {
        context.settingsDataStore.edit { it[ZIP_CODE_KEY] = zipCode.trim() }
    }

    /** Empty string means "no icon pack selected - show real app icons." Always applies
     *  to the app drawer; whether it also applies to home-screen tiles is a separate
     *  opt-in below. */
    val iconThemePackage = context.settingsDataStore.data.map { it[ICON_THEME_PACKAGE_KEY] ?: "" }

    suspend fun setIconThemePackage(packageName: String) {
        context.settingsDataStore.edit { it[ICON_THEME_PACKAGE_KEY] = packageName }
    }

    /** Off by default - home tiles keep their fixed glyphs unless explicitly opted in. */
    val applyIconThemeToHomeTiles = context.settingsDataStore.data.map { it[APPLY_ICON_THEME_TO_HOME_TILES_KEY] ?: false }

    suspend fun setApplyIconThemeToHomeTiles(enabled: Boolean) {
        context.settingsDataStore.edit { it[APPLY_ICON_THEME_TO_HOME_TILES_KEY] = enabled }
    }

    /** Packed ARGB for the home-screen Calendar/Task cards. Low alpha is the "glass" look -
     *  there's no separate flag for it, opacity is just part of the color. */
    val eventCardColor = context.settingsDataStore.data.map { it[EVENT_CARD_COLOR_KEY] ?: DEFAULT_EVENT_CARD_COLOR }

    suspend fun setEventCardColor(argb: Int) {
        context.settingsDataStore.edit { it[EVENT_CARD_COLOR_KEY] = argb }
    }

    /** Off by default - cards stay the fixed default color until the user opts in. */
    val eventCardColorEnabled = context.settingsDataStore.data.map { it[EVENT_CARD_COLOR_ENABLED_KEY] ?: false }

    suspend fun setEventCardColorEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[EVENT_CARD_COLOR_ENABLED_KEY] = enabled }
    }

    /** Off by default - home tiles have no border until the user opts in. */
    val tileBorderEnabled = context.settingsDataStore.data.map { it[TILE_BORDER_ENABLED_KEY] ?: false }

    suspend fun setTileBorderEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[TILE_BORDER_ENABLED_KEY] = enabled }
    }

    /** 1-10 scale, defaults to the midpoint. */
    val tileBorderSizeStep = context.settingsDataStore.data.map { it[TILE_BORDER_SIZE_STEP_KEY] ?: DEFAULT_TILE_BORDER_SIZE_STEP }

    suspend fun setTileBorderSizeStep(step: Int) {
        context.settingsDataStore.edit { it[TILE_BORDER_SIZE_STEP_KEY] = step }
    }
}
