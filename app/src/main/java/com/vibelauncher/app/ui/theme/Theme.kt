package com.vibelauncher.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

@Composable
fun VibeLauncherTheme(accentColor: Color, content: @Composable () -> Unit) {
    val colorScheme = remember(accentColor) {
        darkColorScheme(
            primary = accentColor,
            onPrimary = LauncherWhite,
            background = LauncherBlack,
            onBackground = LauncherWhite,
            surface = LauncherCard,
            onSurface = LauncherWhite,
            secondary = LauncherMutedGray,
            onSecondary = LauncherBlack
        )
    }
    CompositionLocalProvider(LocalAccentColor provides accentColor) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LauncherTypography,
            shapes = LauncherShapes,
            content = content
        )
    }
}
