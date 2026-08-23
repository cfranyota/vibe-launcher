# Vibe Launcher

A custom Android home-screen launcher, built with Kotlin and Jetpack Compose. Vibe Launcher replaces the stock home screen with a fixed, distraction-light layout: today's date and weather up top, your calendar and tasks for the day, and 8 fully customizable quick-launch tiles - all sitting directly on your wallpaper.

This is a first prototype, built for and tested on a physical device (Unihertz Titan 2).

## Screenshots

| Home | App Drawer |
|---|---|
| ![Home screen](docs/screenshots/home.png) | ![App drawer](docs/screenshots/app_drawer.png) |

| Launcher Settings | Card Color picker |
|---|---|
| ![Launcher Settings](docs/screenshots/settings.png) | ![Card Color picker](docs/screenshots/card_color.png) |

## Features

- **Fixed header** - day of week, date, and weather (by zip code), pinned in place and never scrolling.
- **Calendar & Tasks** - today's next event and task shown collapsed; tap to expand in place into a scrollable list of everything else that day, starting from whichever event you're currently on.
- **8 customizable tiles** - reassign any of the 8 home-screen slots to any installed app, either by long-pressing the tile directly or from a dedicated "Home Screen Apps" screen in settings.
- **Icon borders** - an "Icon borders" toggle in Launcher Settings draws a thin white rounded-square outline around each home-screen tile, sized to the tile's full column width. Off by default.
- **Notification badges** - a small red badge appears on any tile whose app has an active notification (unread texts, missed calls, etc.), via Android's notification listener API.
- **App drawer** - swipe up (or tap the drag handle) to open a full searchable list of installed apps, with a slide-up/slide-down animation and swipe-down-to-dismiss.
- **Icon theming** - apply any installed icon pack to the app drawer, with an option to also apply it to the home-screen tiles.
- **Card Color** - a full HSV color wheel plus brightness and opacity sliders to customize the Calendar/Task card color, including a "Glass" mode that turns the cards transparent so the wallpaper shows through clearly.
- **Real wallpaper support** - the home screen renders directly over your system wallpaper rather than a solid background.
- **Vibe Bar** - a command input that's invisible until you start typing on a hardware keyboard, then slides up from the bottom of the screen (on by default, toggle it off in Launcher Settings); delete back to empty and it slides away again. The first character routes to a quick action: `@` texts a contact, `#` calls a contact, `-` adds a to-do, `/` opens a scratch note, `+` adds a calendar event, `?` searches and launches an installed app, and plain text runs a web search. `@`/`#` execute directly (no dialer/messaging app opens); `-` saves into Vibe Launcher's own local To-Do store; `+` still hands off to the Calendar app.
- **To-Do** - the home screen's built-in "To-Do" tile opens Vibe Launcher's own local list (add via Vibe Bar's `-`, edit or delete any entry with the pen/trash icons). Also shows in the Tasks bar alongside real calendar all-day events.
- **Note** - `/` in Vibe Bar or the home screen's "Note" tile opens a half-page scratchpad above the keyboard - type something, then Share it (the real Android share sheet: recent contacts plus Messages/Gmail/Quick Share/etc.) or Copy it. Nothing is ever saved - closing the sheet, or deleting the draft, clears it for good.

## Tech stack

- Kotlin, Jetpack Compose, Navigation Compose
- MVVM with `ViewModel` + `StateFlow`
- Jetpack DataStore (Preferences) for settings persistence
- `CalendarContract` for calendar/task data, `LauncherApps` for app enumeration, `NotificationListenerService` for notification badges

## Building

```bash
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`. Install it with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then set it as your default home app:

```bash
adb shell cmd role add-role-holder android.app.role.HOME com.vibelauncher.app
```

## Status

Early prototype - built iteratively through hands-on testing on a real device. Expect rough edges. Contributions and feedback welcome.

## Changelog

- **1.0.10** - Reworked `/` from a saved note into an ephemeral scratchpad, and fixed the software keyboard hiding its buttons:
  - **`/` no longer saves anything.** Removed the persisted Notes feature entirely (`NotesRepository`, the Notes list screen, `NoteItem` - all deleted). Typing `/` in Vibe Bar (or tapping the home screen's "Note" tile) now opens `NoteBubble`, a half-page sheet with no repository behind it at all - the draft lives only in memory and is gone the moment the sheet closes.
  - Multi-line text field where Enter inserts a newline (never submits). A trash icon confirms before clearing ("This draft will permanently be cleared" / Cancel / Delete). Copy puts the text on the clipboard; Share opens Android's native share sheet (`Intent.ACTION_SEND` + chooser) with the recent-contacts row and the full app list (Messages, Gmail, Quick Share, Chrome, etc.) - both stay available after use so the same note can go to more than one place.
  - Vibe Bar now hands off to `NoteBubble` immediately on `/` rather than rendering its own note editor - collapses cleanly with no visible flash.
  - Fixed the software keyboard covering `NoteBubble`'s Copy/Share buttons - added `android:windowSoftInputMode="adjustResize"` so the window (and the sheet) resizes above the keyboard instead of being covered by it.
- **1.0.9** - Vibe Bar polish pass, all in `VibeBar.kt`/`VibeBarComponents.kt` unless noted:
  - **Always-visible shortcuts legend** - the `HOT KEYS` reference grid (`@` Text, `#` Call, `-` To-Do, `/` Note, `+` Event, `?` App) now shows above the input every time Vibe Bar is open, not just in an empty state that could no longer actually occur once 1.0.8 made the bar hidden-until-typed (typing always arrives with a character already in it). Hidden only in `/` note mode, where the full-screen editor needs the space and the prefix is already fixed. Tap any entry to switch commands, same as before.
  - **Reworked the six per-action accent colors** (`ui/theme/Color.kt`) - `@`/`#`/`-`/`/`/`+`/`?` each still get their own color (used consistently across the legend, the input's send button, the contact chip, and result rows), but the six were previously stock Tailwind/Bootstrap hex values used as-is; retuned into a deliberately-spaced set (teal/green/amber/plum/terracotta/slate) so `-` To-Do and `+` Event - previously two adjacent ambers/browns - now read apart at a glance, and `?` App no longer shares a hue with the app's own red accent.
  - **Background is near-black, not pure black** (`LauncherBlack` in `Color.kt`, `#0A0A0A` instead of `#000000`) - affects Vibe Bar's dimming scrim behind the expanded bar, plus the Settings/Notes/To-Do screen backgrounds that share the same theme token.
  - **Deleting a note or to-do is reversible** - the Notes and To-Do screens (opened from Vibe Bar's `/` and `-`, or the home screen's Note/To-Do tiles) now show a "deleted" snackbar with an Undo action instead of deleting silently and permanently; Undo restores the exact item.
  - Tightened several off-grid paddings in the bar and its legend onto a consistent 8dp spacing rhythm.
- **1.0.8** - Vibe Bar is now hidden until you start typing on a hardware keyboard, then slides up from the bottom (was a tap-to-expand pill before). `@`/`#` now send the text/place the call directly instead of opening another app (adds `SEND_SMS`/`CALL_PHONE` permissions). `-`/`/` now save into new local To-Do and Notes stores instead of handing off to the Calendar app / a share sheet - reachable from the existing home-screen "To-Do"/"Note" tiles, with edit and delete. To-dos also show in the Tasks bar.
- **1.0.7** - Added Vibe Bar, a floating command input (on by default) above the tile grid: `@`/`#` text or call a contact, `-`/`+` add a to-do/event, `/` shares a note, `?` searches installed apps, plain text runs a web search. Adds a Contacts read permission, used only for the `@`/`#` search.
- **1.0.6** - Icon border tiles are now square (not wider-than-tall) and scale to fill the full column width, matching the reference design more closely.
- **1.0.5** - Added an "Icon borders" toggle in Launcher Settings that draws a thin white outline around each home-screen tile. Off by default.
- **1.0.4** - Added an on/off toggle for Card Color. Off by default - cards stay the fixed default color until explicitly enabled.
- **1.0.3** - Fixed notification badge size and positioning.
- **1.0.2** - Replaced the notification badge with a glossy 3D sphere icon.
- **1.0.1** - Switched to an improved sphere image for the notification badge.
- **1.0** - Initial release.
