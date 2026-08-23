package com.vibelauncher.app.ui.theme

import androidx.compose.ui.graphics.Color

// Near-black, not pure #000 - matches how real dark-surface products (Linear #08090A,
// Vercel #171717) avoid true black, which crushes contrast against overlaid content
// (the wallpaper showing through, EventCard shadows, the Vibe Bar scrim).
val LauncherBlack = Color(0xFF0A0A0A)
val LauncherWhite = Color(0xFFFFFFFF)
val LauncherRed = Color(0xFFEF4444)
val LauncherCard = Color(0xFF1A1A1A)
val LauncherMutedGray = Color(0xFF9E9E9E)

// Vibe Bar per-action accent colors - one hue per command, held apart on the wheel so
// they read at a glance (was previously stock Tailwind/Bootstrap swatches copied as-is:
// blue-600, Bootstrap success green, violet-600 - genuine but generic; these are tuned
// off those exact stops, and Event/To-Do (previously two adjacent warm ambers/browns)
// are pushed further apart). App search no longer reuses LauncherRed - that's the app's
// own brand accent, not a fifth thing to compete for the same hue.
val VibeTextColor = Color(0xFF3AA0A0)   // teal
val VibeCallColor = Color(0xFF4C9A5B)   // green
val VibeTodoColor = Color(0xFFC99A3D)   // amber
val VibeNoteColor = Color(0xFFA8558F)   // plum
val VibeEventColor = Color(0xFFB0562E)  // terracotta
val VibeAppColor = Color(0xFF6B7280)    // slate
