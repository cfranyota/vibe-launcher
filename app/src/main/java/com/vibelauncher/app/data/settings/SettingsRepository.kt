package com.vibelauncher.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings_prefs")
private val ZIP_CODE_KEY = stringPreferencesKey("zip_code")
private val ICON_THEME_PACKAGE_KEY = stringPreferencesKey("icon_theme_package")
private val EVENT_CARD_COLOR_KEY = intPreferencesKey("event_card_color")
private val EVENT_CARD_COLOR_ENABLED_KEY = booleanPreferencesKey("event_card_color_enabled")
private val TILE_BORDER_ENABLED_KEY = booleanPreferencesKey("tile_border_enabled")
private val TILE_BORDER_SIZE_STEP_KEY = intPreferencesKey("tile_border_size_step")
private val VIBE_BAR_ENABLED_KEY = booleanPreferencesKey("vibe_bar_enabled")
private val ICON_ACCENT_COLOR_KEY = intPreferencesKey("icon_accent_color")
private val ICON_ACCENT_COLOR_ENABLED_KEY = booleanPreferencesKey("icon_accent_color_enabled")
private val ACCENT_COLOR_KEY = intPreferencesKey("accent_color")
private val FONT_SCALE_KEY = floatPreferencesKey("font_scale")

/** Default event-card color, matching `LauncherCard` in ui/theme/Color.kt (0xFF1A1A1A) -
 *  duplicated as a raw constant here so this data-layer file doesn't need to depend on
 *  the UI theme package. */
private const val DEFAULT_EVENT_CARD_COLOR = 0xFF1A1A1A.toInt()

/** Default icon accent color, matching `LauncherRed` in ui/theme/Color.kt (0xFFEF4444) -
 *  same reasoning as DEFAULT_EVENT_CARD_COLOR above. */
private const val DEFAULT_ICON_ACCENT_COLOR = 0xFFEF4444.toInt()

/** Default border-size step on the 1-10 scale (see TileView.kt's resolveTileSizeDp) - the
 *  midpoint, not the max. */
private const val DEFAULT_TILE_BORDER_SIZE_STEP = 5

/** New app-wide accent default - Tailwind orange-500, chosen to sit at roughly the same
 *  lightness/chroma band as the outgoing LauncherRed (Tailwind red-500) so existing
 *  contrast assumptions against LauncherBlack/LauncherCard/LauncherWhite still hold. */
private const val DEFAULT_ACCENT_COLOR = 0xFFF97316.toInt()

/** 1.0 = no adjustment - matches Android's own fontScale semantics so it composes
 *  multiplicatively with the OS accessibility font scale instead of replacing it. */
private const val DEFAULT_FONT_SCALE = 1.0f

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

    /** On by default - Vibe Bar is a core interaction, not optional chrome; users who
     *  don't want it can turn it off here. */
    val vibeBarEnabled = context.settingsDataStore.data.map { it[VIBE_BAR_ENABLED_KEY] ?: true }

    suspend fun setVibeBarEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[VIBE_BAR_ENABLED_KEY] = enabled }
    }

    /** Packed ARGB for the calendar/checklist icons, the "5m"/"0m"/"•" badge circle, and
     *  the weather sun icon - one color for all of them together. */
    val iconAccentColor = context.settingsDataStore.data.map { it[ICON_ACCENT_COLOR_KEY] ?: DEFAULT_ICON_ACCENT_COLOR }

    suspend fun setIconAccentColor(argb: Int) {
        context.settingsDataStore.edit { it[ICON_ACCENT_COLOR_KEY] = argb }
    }

    /** Off by default - icons stay their fixed red until the user opts in. */
    val iconAccentColorEnabled = context.settingsDataStore.data.map { it[ICON_ACCENT_COLOR_ENABLED_KEY] ?: false }

    suspend fun setIconAccentColorEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[ICON_ACCENT_COLOR_ENABLED_KEY] = enabled }
    }

    /** Packed ARGB for the app-wide accent - replaces every screen's old fixed LauncherRed
     *  (see LocalAccentColor). */
    val accentColor = context.settingsDataStore.data.map { it[ACCENT_COLOR_KEY] ?: DEFAULT_ACCENT_COLOR }

    suspend fun setAccentColor(argb: Int) {
        context.settingsDataStore.edit { it[ACCENT_COLOR_KEY] = argb }
    }

    val fontScale = context.settingsDataStore.data.map { it[FONT_SCALE_KEY] ?: DEFAULT_FONT_SCALE }

    suspend fun setFontScale(scale: Float) {
        context.settingsDataStore.edit { it[FONT_SCALE_KEY] = scale }
    }
}
