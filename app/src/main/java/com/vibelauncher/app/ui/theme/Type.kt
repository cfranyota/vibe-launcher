package com.vibelauncher.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val DayTextStyle = TextStyle(
    fontSize = 46.sp,
    fontWeight = FontWeight.Light
)

val DateWeatherTextStyle = TextStyle(
    fontSize = 20.sp,
    fontWeight = FontWeight.Normal
)

val LauncherTypography = Typography(
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    labelSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
)

/** Monospace typography scoped to Settings screens and Vibe Bar only (see each screen's
 *  `MaterialTheme(typography = settingsTypography())` wrap) - the rest of the app keeps
 *  LauncherTypography untouched. Only overrides the TextStyle slots those screens
 *  actually use; titleLarge/labelLarge/bodySmall have no explicit override in
 *  LauncherTypography (they fall back to Material3 defaults) but Settings uses all three,
 *  so they're set here explicitly rather than silently inheriting the non-mono default. */
@Composable
fun settingsTypography(): Typography = MaterialTheme.typography.copy(
    bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
    bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
    bodySmall = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
    labelSmall = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
    labelLarge = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
    titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace)
)
