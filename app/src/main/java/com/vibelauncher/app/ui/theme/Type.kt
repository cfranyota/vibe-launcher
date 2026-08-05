package com.vibelauncher.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
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
