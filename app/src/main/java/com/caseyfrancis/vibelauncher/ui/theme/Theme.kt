package com.caseyfrancis.vibelauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LauncherColorScheme = darkColorScheme(
    primary = LauncherRed,
    onPrimary = LauncherWhite,
    background = LauncherBlack,
    onBackground = LauncherWhite,
    surface = LauncherCard,
    onSurface = LauncherWhite,
    secondary = LauncherMutedGray,
    onSecondary = LauncherBlack
)

@Composable
fun VibeLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LauncherColorScheme,
        typography = LauncherTypography,
        shapes = LauncherShapes,
        content = content
    )
}
