package com.vibelauncher.app.ui.home.components

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.vibelauncher.app.data.apps.AppInfo
import com.vibelauncher.app.data.apps.InstalledAppsRepository
import com.vibelauncher.app.data.contacts.ContactResult
import com.vibelauncher.app.data.contacts.ContactsRepository
import com.vibelauncher.app.data.todos.TodoRepository
import com.vibelauncher.app.features.vibebar.VIBE_BAR_COMMAND_PREFIXES
import com.vibelauncher.app.features.vibebar.VIBE_BAR_NOTE_PREFIX
import com.vibelauncher.app.features.vibebar.parseVibeBarInput
import com.vibelauncher.app.features.vibebar.printableHardwareText
import com.vibelauncher.app.ui.theme.LauncherBlack
import com.vibelauncher.app.ui.theme.LauncherMutedGray
import com.vibelauncher.app.ui.theme.LauncherWhite
import com.vibelauncher.app.ui.theme.VibeAppColor
import com.vibelauncher.app.ui.theme.VibeCallColor
import com.vibelauncher.app.ui.theme.VibeEventColor
import com.vibelauncher.app.ui.theme.VibeNoteColor
import com.vibelauncher.app.ui.theme.VibeTextColor
import com.vibelauncher.app.ui.theme.VibeTodoColor
import com.vibelauncher.app.util.IntentDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class VibeBarActionVisuals(val color: Color, val icon: ImageVector)
private const val VIBE_BAR_MIN_HEIGHT_DP = 56

/**
 * A command input that's invisible until you start typing on a hardware keyboard, then
 * slides up from the bottom of the screen; deleting back to empty slides it away again.
 * The first typed character is a command prefix routing to a quick action: '@' text a
 * contact, '#' call a contact, '-' add a to-do, '/' open a scratch note (NoteBubble -
 * never saved by Vibe Bar itself), '+' add a calendar event, '?' search/launch an
 * installed app, no prefix runs a web search. '@'/'#' execute directly
 * (SmsManager/TelecomManager); '-' saves into Vibe Launcher's own local to-do store;
 * '+' hands off to the Calendar app, same as ever.
 */
@Composable
fun VibeBar(
    keyboardInputEnabled: Boolean,
    onOpenNote: () -> Unit,
    openRequestToken: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val contactsRepository = remember { ContactsRepository(context) }
    val installedAppsRepository = remember { InstalledAppsRepository(context) }
    val todoRepository = remember { TodoRepository(context) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val armedFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    var text by remember { mutableStateOf(TextFieldValue()) }
    var expanded by remember { mutableStateOf(false) }
    var lockedPrefix by remember { mutableStateOf<Char?>(null) }
    var selectedContact by remember { mutableStateOf<ContactResult?>(null) }
    var callToConfirm by remember { mutableStateOf<ContactResult?>(null) }
    var hasContactsPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }
    var hasSmsPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED)
    }
    var hasCallPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED)
    }
    var pendingSms by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pendingCall by remember { mutableStateOf<String?>(null) }
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        installedApps = withContext(Dispatchers.IO) { installedAppsRepository.getLaunchableApps() }
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasContactsPermission = granted }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasSmsPermission = granted
        val draft = pendingSms
        pendingSms = null
        if (granted && draft != null) {
            val sent = IntentDefaults.sendSmsDirect(context, draft.first, draft.second)
            Toast.makeText(context, if (sent) "Message sent" else "Couldn't send message", Toast.LENGTH_SHORT).show()
        }
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCallPermission = granted
        val phone = pendingCall
        pendingCall = null
        if (granted && phone != null) {
            val called = IntentDefaults.placeCallDirect(context, phone)
            if (!called) Toast.makeText(context, "Couldn't place call", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearCommand() {
        text = TextFieldValue()
        selectedContact = null
        lockedPrefix = null
    }

    fun dismiss() {
        clearCommand()
        keyboard?.hide()
        focusManager.clearFocus(force = true)
        expanded = false
    }

    val parsed = parseVibeBarInput(text.text, lockedPrefix)
    val prefix = parsed.prefix
    val searchTerm = parsed.searchTerm

    // '/' never renders inside Vibe Bar - it hands off to the standalone NoteBubble
    // immediately. The common path (typing '/' as the very first hardware keystroke) is
    // intercepted earlier, in the armed box below, so the bar never actually animates
    // open for it; this covers the rarer path of reaching '/' while already expanded
    // (e.g. tapping the legend's Note entry).
    LaunchedEffect(prefix, expanded) {
        if (expanded && prefix == VIBE_BAR_NOTE_PREFIX) {
            onOpenNote()
            dismiss()
        }
    }

    val contactResults = remember(prefix, searchTerm, hasContactsPermission, selectedContact) {
        if (prefix in listOf('@', '#') && hasContactsPermission && selectedContact == null && searchTerm.isNotBlank()) {
            contactsRepository.searchContacts(searchTerm)
        } else {
            emptyList()
        }
    }
    val appResults = remember(prefix, searchTerm, installedApps) {
        if (prefix == '?' && searchTerm.isNotBlank()) {
            installedApps.filter { it.label.startsWith(searchTerm, ignoreCase = true) }.take(5)
        } else {
            emptyList()
        }
    }

    val actionVisuals = when (prefix) {
        '@' -> VibeBarActionVisuals(VibeTextColor, Icons.AutoMirrored.Filled.Send)
        '#' -> VibeBarActionVisuals(VibeCallColor, Icons.Filled.Phone)
        '-' -> VibeBarActionVisuals(VibeTodoColor, Icons.Filled.Checklist)
        VIBE_BAR_NOTE_PREFIX -> VibeBarActionVisuals(VibeNoteColor, Icons.AutoMirrored.Filled.NoteAdd)
        '+' -> VibeBarActionVisuals(VibeEventColor, Icons.Filled.Event)
        '?' -> VibeBarActionVisuals(VibeAppColor, Icons.Filled.Apps)
        else -> VibeBarActionVisuals(MaterialTheme.colorScheme.primary, Icons.Filled.Search)
    }

    fun sendDirectOrRequestAccess(phone: String, body: String) {
        if (hasSmsPermission) {
            val sent = IntentDefaults.sendSmsDirect(context, phone, body)
            Toast.makeText(context, if (sent) "Message sent" else "Couldn't send message", Toast.LENGTH_SHORT).show()
        } else {
            pendingSms = phone to body
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }
    }

    fun callDirectOrRequestAccess(phone: String) {
        if (hasCallPermission) {
            val called = IntentDefaults.placeCallDirect(context, phone)
            if (!called) Toast.makeText(context, "Couldn't place call", Toast.LENGTH_SHORT).show()
        } else {
            pendingCall = phone
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }
    }

    fun submit() {
        val payload = if (lockedPrefix != null) text.text.trim() else text.text.drop(1).trim()
        when (prefix) {
            '-' -> if (payload.isNotBlank()) {
                coroutineScope.launch { todoRepository.add(payload) }
                dismiss()
            }
            VIBE_BAR_NOTE_PREFIX -> {} // '/' hands off to NoteBubble immediately, never reaches submit()
            '+' -> if (payload.isNotBlank() && IntentDefaults.insertCalendarEvent(context, payload, allDay = false)) {
                dismiss()
            }
            '@' -> if (selectedContact != null && payload.isNotBlank()) {
                sendDirectOrRequestAccess(selectedContact!!.phone, payload)
                dismiss()
            }
            '#' -> {} // '#' acts directly on result tap, never reaches submit()
            '?' -> {} // '?' acts directly on result tap, never reaches submit()
            else -> if (text.text.isNotBlank() && IntentDefaults.webSearch(context, text.text)) {
                dismiss()
            }
        }
    }

    LaunchedEffect(expanded, keyboardInputEnabled) {
        if (!keyboardInputEnabled) return@LaunchedEffect
        if (expanded) {
            runCatching { focusRequester.requestFocus() }
            // No-op when a hardware keyboard is attached; on a touch-only phone this is
            // what actually pops the IME up after the double-tap trigger below focuses
            // the field - focus alone doesn't reliably show the keyboard on its own.
            keyboard?.show()
        } else {
            runCatching { armedFocusRequester.requestFocus() }
        }
    }

    // External trigger for touch-only phones with no hardware keyboard to open Vibe Bar
    // (see HomeScreen's detectVibeBarDoubleTap) - mirrors what the hardware key handler
    // below does when arming, minus seeding a character, since typing here comes from the
    // software keyboard rather than a keystroke.
    LaunchedEffect(openRequestToken) {
        if (openRequestToken > 0 && !expanded) {
            clearCommand()
            expanded = true
        }
    }

    Box(modifier.fillMaxSize()) {
        // Always-present, essentially untouchable (1dp) focus target that listens for the
        // first hardware keystroke while collapsed - reveals the bar with that character
        // already typed. Never intercepts touches meant for DrawerHandle/TileGrid beneath
        // it, unlike a full-width invisible surface would.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .size(1.dp)
                .focusRequester(armedFocusRequester)
                .onPreviewKeyEvent { event ->
                    if (expanded || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val typed = printableHardwareText(event.nativeKeyEvent.unicodeChar) ?: return@onPreviewKeyEvent false
                    if (typed == VIBE_BAR_NOTE_PREFIX.toString()) {
                        onOpenNote()
                        return@onPreviewKeyEvent true
                    }
                    clearCommand()
                    text = TextFieldValue(typed, selection = TextRange(typed.length))
                    expanded = true
                    true
                }
                .focusable()
        )

        if (expanded) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(LauncherBlack.copy(alpha = .55f))
                    .clickable(onClick = { dismiss() })
            )
        }

        AnimatedVisibility(
            visible = expanded,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(animationSpec = tween(220), initialOffsetY = { it }) + fadeIn(tween(220)),
            exit = slideOutVertically(animationSpec = tween(200), targetOffsetY = { it }) + fadeOut(tween(200))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val hasResults = contactResults.isNotEmpty() || appResults.isNotEmpty() ||
                    (prefix in listOf('@', '#') && !hasContactsPermission)
                if (hasResults) {
                    Column(
                        Modifier.fillMaxWidth().heightIn(max = 260.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        contactResults.forEach { contact ->
                            SuggestionRow(
                                text = "${contact.name} (${contact.phoneLabel})",
                                icon = Icons.Filled.Person
                            ) {
                                if (prefix == '#') {
                                    callToConfirm = contact
                                    dismiss()
                                } else {
                                    lockedPrefix = prefix
                                    selectedContact = contact
                                    text = TextFieldValue()
                                }
                            }
                        }
                        appResults.forEach { app ->
                            SuggestionRow(text = app.label, icon = Icons.Filled.Apps) {
                                val launched = runCatching {
                                    context.startActivity(
                                        Intent()
                                            .setComponent(ComponentName(app.packageName, app.className))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }.isSuccess
                                if (launched) dismiss()
                            }
                        }
                        if (prefix in listOf('@', '#') && !hasContactsPermission) {
                            SuggestionRow(text = "Allow contacts access to search people", icon = Icons.Filled.Person) {
                                contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        }
                    }
                }

                if (expanded) {
                    VibeBarLegend(prefix) { key ->
                        clearCommand()
                        text = TextFieldValue(key.toString(), selection = TextRange(1))
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(min = VIBE_BAR_MIN_HEIGHT_DP.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 12.dp
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        selectedContact?.let { contact ->
                            CommandChip(
                                "${contact.name} (${contact.phoneLabel})",
                                actionVisuals.color,
                                LauncherWhite
                            ) { clearCommand() }
                        }
                        VibeBarInputField(
                            value = text,
                            onValueChange = { value ->
                                text = value
                                if (lockedPrefix == null && value.text.firstOrNull() != prefix) {
                                    selectedContact = null
                                }
                                if (value.text.isBlank() && lockedPrefix == null && selectedContact == null) {
                                    expanded = false
                                }
                            },
                            prefix = prefix,
                            focusRequester = focusRequester,
                            onSubmit = { submit() },
                            modifier = Modifier.weight(1f)
                        )
                        FilledIconButton(
                            onClick = { submit() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = actionVisuals.color,
                                contentColor = LauncherWhite
                            )
                        ) { Icon(actionVisuals.icon, "Run command") }
                    }
                }
            }
        }
    }

    callToConfirm?.let { contact ->
        AlertDialog(
            onDismissRequest = { callToConfirm = null },
            icon = { Icon(Icons.Filled.Phone, null, tint = VibeCallColor) },
            title = { Text("Call ${contact.name}") },
            text = { Text("${contact.phoneLabel}: ${contact.phone}") },
            confirmButton = {
                Button(
                    onClick = {
                        callToConfirm = null
                        callDirectOrRequestAccess(contact.phone)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VibeCallColor, contentColor = LauncherWhite)
                ) { Text("Call now") }
            },
            dismissButton = {
                TextButton(onClick = { callToConfirm = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun VibeBarInputField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    prefix: Char?,
    focusRequester: FocusRequester,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    maxLines: Int = 5
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Type, or use @ # - / + ?", color = LauncherMutedGray) },
        modifier = modifier.focusRequester(focusRequester),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(
            imeAction = when {
                prefix == VIBE_BAR_NOTE_PREFIX -> ImeAction.Default
                prefix in VIBE_BAR_COMMAND_PREFIXES -> ImeAction.Send
                else -> ImeAction.Search
            }
        ),
        keyboardActions = KeyboardActions(
            onSearch = { onSubmit() },
            onSend = { onSubmit() }
        )
    )
}
