package com.vibelauncher.app.ui.setup

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.SwipeUp
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.vibelauncher.app.features.vibebar.parseTodoText
import com.vibelauncher.app.features.vibebar.whenLabel
import com.vibelauncher.app.ui.theme.CardCornerShape
import com.vibelauncher.app.ui.theme.LauncherMutedGray
import com.vibelauncher.app.ui.theme.LauncherWhite
import com.vibelauncher.app.ui.theme.LocalAccentColor

@Composable
internal fun TourStep(step: SetupStep, label: String, triedTodo: String?, onSaveTodo: (String) -> Unit) {
    when (step) {
        SetupStep.VIBE_BAR -> VibeBarStep(label)
        SetupStep.TRY_IT -> TryItStep(label, triedTodo, onSaveTodo)
        SetupStep.HOME_SCREEN -> HomeScreenStep(label)
        SetupStep.TILES_AND_SHORTCUTS -> TilesStep(label)
        SetupStep.SETTINGS -> SettingsStep(label)
        else -> Unit
    }
}

@Composable
private fun VibeBarStep(label: String) {
    StepLayout(label = label, title = "vibe bar") {
        StepText("on home, just start typing - the command bar opens with whatever you typed. start the line with a symbol and it does something:")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CommandExample('@', "jason on my way", "text jason")
            CommandExample('#', "lauren", "call lauren")
            CommandExample('-', "buy milk tomorrow", "add a to-do")
            CommandExample('*', "dentist mar 24 9a", "add an event")
            CommandExample('!', "paint color swiss coffee", "save a note")
            CommandExample(null, "best tacos near me", "search the web")
        }
        StepText("enter runs the line, shift+enter adds a new line, and deleting everything closes the bar. for @ and #, tap the person once they show up.")
        StepText("no keyboard on your phone? double-tap the top-right corner of home to open the bar.", muted = true)
    }
}

/** One outlined row per command - the symbol in the accent color, what it does on the right,
 *  the same look Vibe Bar's own breadcrumb gives a typed line. */
@Composable
private fun CommandExample(symbol: Char?, text: String, action: String) {
    val accent = LocalAccentColor.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LauncherMutedGray.copy(alpha = 0.3f), CardCornerShape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = buildAnnotatedString {
                if (symbol != null) {
                    withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold)) { append(symbol) }
                }
                withStyle(SpanStyle(color = LauncherWhite)) { append(text) }
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text("▸ $action", color = LauncherMutedGray, style = MaterialTheme.typography.labelSmall)
    }
}

/** The one hands-on step: typing a to-do here saves a real one, dates and all, with the same
 *  preview line Vibe Bar shows while you type. */
@Composable
private fun TryItStep(label: String, triedTodo: String?, onSave: (String) -> Unit) {
    var typed by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val accent = LocalAccentColor.current
    val draft = remember(typed) { typed.trim().removePrefix("-").takeIf { it.isNotBlank() }?.let { parseTodoText(it) } }
    val preview = draft?.let { d -> d.dueAt?.let { "to-do → ${d.text} · ${whenLabel(it, d.dueAllDay)}" } ?: "to-do → ${d.text}" }

    fun save() {
        if (draft == null) return
        onSave(typed)
        typed = ""
    }

    // On a keyboard phone this means you can start typing the moment the step appears.
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    StepLayout(label = label, title = "try it") {
        StepText("add a to-do the way you would from home. give it a day or a time if you like - \"buy milk tomorrow\", \"call mom in 2 hours\".")
        Text(
            text = preview ?: " ",
            color = LauncherMutedGray,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 4.dp)
        )
        TextField(
            value = typed,
            onValueChange = { typed = it },
            placeholder = { Text("-buy milk tomorrow", color = LauncherMutedGray) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { save() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = LauncherWhite,
                unfocusedTextColor = LauncherWhite,
                cursorColor = accent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, LauncherMutedGray.copy(alpha = 0.4f), CardCornerShape)
                .focusRequester(focusRequester)
                // A hardware Enter should save, the same as Vibe Bar - swallow its KeyUp too so
                // it can't land as a stray keystroke afterwards.
                .onPreviewKeyEvent { event ->
                    val code = event.nativeKeyEvent.keyCode
                    if (code != android.view.KeyEvent.KEYCODE_ENTER && code != android.view.KeyEvent.KEYCODE_NUMPAD_ENTER) {
                        return@onPreviewKeyEvent false
                    }
                    if (event.type == KeyEventType.KeyDown) save()
                    true
                }
        )
        PrimaryButton("save") { save() }
        if (triedTodo != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Text("saved \"$triedTodo\" - it's in your to-do list", color = LauncherWhite, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun HomeScreenStep(label: String) {
    StepLayout(label = label, title = "around home") {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            GestureRow(Icons.Filled.SwipeUp, "swipe up", "see every app. pull down to close it again.")
            GestureRow(Icons.Filled.Swipe, "swipe the date left or right", "look at another day's events and activity.")
            GestureRow(Icons.Filled.TouchApp, "tap the event or to-do card", "open the full list for the day.")
            GestureRow(Icons.Filled.WbSunny, "tap the weather", "change your location.")
        }
        StepText("the dots under the date are your day, one per hour - white when it stayed intentional, your accent color when it slipped into feeds, dim for what's still to come.")
    }
}

@Composable
private fun TilesStep(label: String) {
    StepLayout(label = label, title = "tiles & shortcuts") {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            GestureRow(Icons.Filled.TouchApp, "tap a tile", "open it. note and to-do open vibe's own notes and list.")
            GestureRow(Icons.Filled.GridView, "long-press a tile", "swap it for any app, or put its default back.")
            GestureRow(Icons.Filled.Keyboard, "hold a letter key", "run a letter shortcut - open an app, text or call someone. set them up in settings.")
        }
    }
}

@Composable
private fun SettingsStep(label: String) {
    StepLayout(label = label, title = "settings") {
        GestureRow(Icons.Filled.Settings, "swipe up, then tap the gear", "it's beside the search box. long-pressing any app gets you there too.")
        StepText("in there:")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SettingsItem("appearance", "accent color and text size")
            SettingsItem("home screen apps", "choose your 8 tiles")
            SettingsItem("icon theme", "apply an icon pack")
            SettingsItem("letter shortcuts", "what each held letter does")
            SettingsItem("vibe mode", "essentials only, hide social apps, grayscale")
            SettingsItem("celsius", "temperature in °C")
            SettingsItem("how to use vibe", "this tour, any time")
            SettingsItem("run setup again", "permissions and defaults")
        }
    }
}

@Composable
private fun GestureRow(icon: ImageVector, title: String, detail: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, contentDescription = null, tint = LocalAccentColor.current, modifier = Modifier.size(22.dp))
        Column {
            Text(title, color = LauncherWhite, style = MaterialTheme.typography.bodyLarge)
            Text(detail, color = LauncherMutedGray, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SettingsItem(name: String, detail: String) {
    Text(
        text = AnnotatedString.Builder().apply {
            withStyle(SpanStyle(color = LauncherWhite)) { append(name) }
            withStyle(SpanStyle(color = LauncherMutedGray)) { append("  -  $detail") }
        }.toAnnotatedString(),
        style = MaterialTheme.typography.bodySmall
    )
}
