package com.vibelauncher.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** The user's chosen app-wide accent color (see SettingsRepository.accentColor). Provided
 *  once at the root by VibeLauncherTheme - static because the value changes at most once
 *  per user interaction, not per-frame, so there's no benefit to compositionLocalOf's
 *  scoped-recomposition tracking, and every read site should recompose on change anyway
 *  (that's the point of a live, app-wide recolor). */
val LocalAccentColor = staticCompositionLocalOf<Color> {
    error("No LocalAccentColor provided - wrap content in VibeLauncherTheme")
}
