package com.vibelauncher.app.ui.home.components

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.vibelauncher.app.data.calendar.CalendarRepository
import com.vibelauncher.app.data.contacts.ContactResult
import com.vibelauncher.app.data.contacts.ContactsRepository
import com.vibelauncher.app.data.lettershortcuts.LetterShortcut
import com.vibelauncher.app.data.lettershortcuts.LetterShortcutType
import com.vibelauncher.app.data.lettershortcuts.LetterShortcutsRepository
import com.vibelauncher.app.data.notes.NoteRepository
import com.vibelauncher.app.data.todos.TodoRepository
import com.vibelauncher.app.features.vibebar.ParsedEvent
import com.vibelauncher.app.features.vibebar.VIBE_BAR_COMMAND_PREFIXES
import com.vibelauncher.app.features.vibebar.VIBE_BAR_EVENT_PREFIX
import com.vibelauncher.app.features.vibebar.VIBE_BAR_NOTE_PREFIX
import com.vibelauncher.app.features.vibebar.eventPreviewLabel
import com.vibelauncher.app.features.vibebar.parseEventText
import com.vibelauncher.app.features.vibebar.parseVibeBarInput
import com.vibelauncher.app.features.vibebar.previewTextFor
import com.vibelauncher.app.features.vibebar.printableHardwareText
import com.vibelauncher.app.model.NoteBlock
import com.vibelauncher.app.model.NoteCategory
import com.vibelauncher.app.model.NoteItem
import com.vibelauncher.app.model.NoteSpan
import com.vibelauncher.app.ui.theme.BadgeCornerShape
import com.vibelauncher.app.ui.theme.CardCornerShape
import com.vibelauncher.app.ui.theme.LauncherBlack
import com.vibelauncher.app.ui.theme.LauncherCard
import com.vibelauncher.app.ui.theme.LauncherMutedGray
import com.vibelauncher.app.ui.theme.LauncherWhite
import com.vibelauncher.app.ui.theme.LocalAccentColor
import com.vibelauncher.app.ui.theme.settingsTypography
import com.vibelauncher.app.util.IntentDefaults
import com.vibelauncher.app.util.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val VIBE_BAR_MIN_HEIGHT_DP = 56
private const val CONFIRMATION_DISPLAY_MS = 1100L
private const val LETTER_HOLD_THRESHOLD_MS = 500L

/**
 * A command input that's invisible until you start typing on a hardware keyboard, then
 * slides up from the bottom of the screen; deleting back to empty slides it away again.
 * One line, one action: the first typed character is a command prefix routing the rest of
 * the line to a quick action - '@' text a contact, '#' call a contact, '-' add a to-do,
 * '!' keep a note, '*' add a calendar event - and no prefix runs a web search.
 *
 * Every command completes without leaving the home screen: '@'/'#' execute directly
 * (SmsManager/TelecomManager), '-' saves into the local to-do store, '!' saves into the
 * Notes inbox, and '*' parses the typed date/time (see features/vibebar/EventTextParser)
 * and writes the event straight into the user's calendar.
 */
@Composable
fun VibeBar(
    keyboardInputEnabled: Boolean,
    openRequestToken: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val contactsRepository = remember { ContactsRepository(context) }
    val todoRepository = remember { TodoRepository(context) }
    val noteRepository = remember { NoteRepository(context) }
    val calendarRepository = remember { CalendarRepository(context) }
    val letterShortcutsRepository = remember { LetterShortcutsRepository(context) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val armedFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val accent = LocalAccentColor.current

    var text by remember { mutableStateOf(TextFieldValue()) }
    var expanded by remember { mutableStateOf(false) }
    var lockedPrefix by remember { mutableStateOf<Char?>(null) }
    var selectedContact by remember { mutableStateOf<ContactResult?>(null) }
    var callToConfirm by remember { mutableStateOf<ContactResult?>(null) }
    var confirmationMessage by remember { mutableStateOf<String?>(null) }
    var hasContactsPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }
    var hasSmsPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED)
    }
    var hasCallPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED)
    }
    var hasWriteCalendarPermission by remember { mutableStateOf(PermissionUtils.hasWriteCalendarPermission(context)) }
    var pendingSms by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pendingCall by remember { mutableStateOf<String?>(null) }
    var pendingEvent by remember { mutableStateOf<ParsedEvent?>(null) }
    // Hold-a-letter shortcut tracking: which letter's KeyDown is still pending a decision
    // (either the hold threshold fires, or KeyUp arrives first and it's treated as a tap),
    // and whether the hold action already fired for it (so KeyUp knows to swallow, not
    // also open Vibe Bar).
    var pendingHoldLetter by remember { mutableStateOf<Char?>(null) }
    var pendingHoldFired by remember { mutableStateOf(false) }
    var pendingHoldJob by remember { mutableStateOf<Job?>(null) }

    val letterShortcuts by letterShortcutsRepository.shortcuts.collectAsState(initial = emptyList())
    val letterShortcutsByLetter = remember(letterShortcuts) { letterShortcuts.associateBy { it.letter } }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasContactsPermission = granted }

    fun persistEvent(event: ParsedEvent) {
        coroutineScope.launch {
            val saved = withContext(Dispatchers.IO) {
                calendarRepository.insertEvent(event.title, event.startMillis, event.endMillis, event.allDay)
            }
            if (saved) confirmationMessage = "saved to calendar"
            else Toast.makeText(context, "Couldn't save event", Toast.LENGTH_SHORT).show()
        }
    }

    // Inserting needs WRITE_CALENDAR, and finding a calendar to insert into needs
    // READ_CALENDAR - requested together so a '*' command only ever prompts once.
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasWriteCalendarPermission = grants.values.all { it } ||
            PermissionUtils.hasWriteCalendarPermission(context)
        val draft = pendingEvent
        pendingEvent = null
        if (hasWriteCalendarPermission && draft != null) persistEvent(draft)
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasSmsPermission = granted
        val draft = pendingSms
        pendingSms = null
        if (granted && draft != null) {
            val sent = IntentDefaults.sendSmsDirect(context, draft.first, draft.second)
            if (sent) confirmationMessage = "sent to ${selectedContact?.name ?: "contact"}"
            else Toast.makeText(context, "Couldn't send message", Toast.LENGTH_SHORT).show()
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

    LaunchedEffect(confirmationMessage) {
        if (confirmationMessage != null) {
            delay(CONFIRMATION_DISPLAY_MS)
            confirmationMessage = null
            dismiss()
        }
    }

    // The first Back press while expanded just dismisses the IME (default Android
    // behavior, nothing to handle here). This handles the second Back press - HomeScreen's
    // own BackHandler is an intentional no-op (launcher home screen ignores Back), so
    // without this, Vibe Bar would stay open forever once the keyboard was gone.
    BackHandler(enabled = expanded) { dismiss() }

    val parsed = parseVibeBarInput(text.text, lockedPrefix)
    val prefix = parsed.prefix
    val searchTerm = parsed.searchTerm
    // parseVibeBarInput's prefix is just text.firstOrNull() when unlocked - true even for
    // plain search text with no real command character - so payload/breadcrumb need their
    // own "is this actually a known command" check rather than blindly dropping char 1.
    val isKnownPrefixCommand = lockedPrefix != null || prefix in VIBE_BAR_COMMAND_PREFIXES
    val payload = when {
        lockedPrefix != null -> text.text.trim()
        isKnownPrefixCommand -> text.text.drop(1).trim()
        else -> text.text.trim()
    }
    val previewPrefix = if (isKnownPrefixCommand) prefix else null

    val contactResults = remember(prefix, searchTerm, hasContactsPermission, selectedContact) {
        if (prefix in listOf('@', '#') && hasContactsPermission && selectedContact == null && searchTerm.isNotBlank()) {
            contactsRepository.searchContacts(searchTerm)
        } else {
            emptyList()
        }
    }

    // Re-parsed on every keystroke so the breadcrumb can show the resolved date the moment
    // enough of it has been typed ("*dentist mar 24 9a" → "event → Mar 24, 9:00 AM").
    val parsedEvent = remember(prefix, payload) {
        if (prefix == VIBE_BAR_EVENT_PREFIX && payload.isNotBlank()) parseEventText(payload) else null
    }

    fun sendDirectOrRequestAccess(phone: String, body: String, contactName: String) {
        if (hasSmsPermission) {
            val sent = IntentDefaults.sendSmsDirect(context, phone, body)
            if (sent) confirmationMessage = "sent to $contactName"
            else Toast.makeText(context, "Couldn't send message", Toast.LENGTH_SHORT).show()
        } else {
            pendingSms = phone to body
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }
    }

    fun saveEventOrRequestAccess(event: ParsedEvent) {
        if (hasWriteCalendarPermission) {
            persistEvent(event)
        } else {
            pendingEvent = event
            calendarPermissionLauncher.launch(
                arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
            )
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

    fun runLetterShortcut(shortcut: LetterShortcut) {
        when (shortcut.type) {
            LetterShortcutType.OPEN_APP -> {
                val pkg = shortcut.packageName
                val cls = shortcut.className
                if (pkg != null && cls != null) {
                    runCatching {
                        context.startActivity(
                            Intent().setComponent(ComponentName(pkg, cls)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            }
            LetterShortcutType.CALL_CONTACT -> {
                val phone = shortcut.phone ?: return
                callToConfirm = ContactResult(shortcut.contactId ?: 0L, shortcut.label, phone, "")
            }
            LetterShortcutType.MESSAGE_CONTACT -> {
                // Lands in the same '@'-locked-contact state a tapped contact suggestion
                // would, so the user types the message themselves rather than a shortcut
                // silently sending a blank text.
                val phone = shortcut.phone ?: return
                clearCommand()
                lockedPrefix = '@'
                selectedContact = ContactResult(shortcut.contactId ?: 0L, shortcut.label, phone, "")
                expanded = true
            }
        }
    }

    fun submit() {
        when (prefix) {
            '-' -> if (payload.isNotBlank()) {
                coroutineScope.launch { todoRepository.add(payload) }
                confirmationMessage = "saved to to-do"
            }
            VIBE_BAR_NOTE_PREFIX -> if (payload.isNotBlank()) {
                // Straight into the Notes inbox - one line, one action. The first line
                // becomes the title, matching how the Notes app titles a new note; the
                // category can be changed later in the editor.
                val now = System.currentTimeMillis()
                val note = NoteItem(
                    id = now,
                    title = payload.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
                        .ifBlank { "Untitled" },
                    category = NoteCategory.PERSONAL,
                    blocks = listOf(NoteBlock(spans = listOf(NoteSpan(payload)))),
                    createdAt = now,
                    updatedAt = now
                )
                coroutineScope.launch { noteRepository.save(note) }
                confirmationMessage = "saved to notes"
            }
            VIBE_BAR_EVENT_PREFIX -> parsedEvent?.let { saveEventOrRequestAccess(it) }
            '@' -> if (selectedContact != null && payload.isNotBlank()) {
                sendDirectOrRequestAccess(selectedContact!!.phone, payload, selectedContact!!.name)
            }
            '#' -> {} // '#' acts directly on result tap, never reaches submit()
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
                    if (expanded) return@onPreviewKeyEvent false
                    val nativeEvent = event.nativeKeyEvent
                    val keyCode = nativeEvent.keyCode
                    val letter = if (keyCode in android.view.KeyEvent.KEYCODE_A..android.view.KeyEvent.KEYCODE_Z) {
                        'A' + (keyCode - android.view.KeyEvent.KEYCODE_A)
                    } else {
                        null
                    }
                    val shortcut = letter?.let { letterShortcutsByLetter[it] }

                    fun openWithTypedChar(): Boolean {
                        val typed = printableHardwareText(nativeEvent) ?: return false
                        clearCommand()
                        text = TextFieldValue(typed, selection = TextRange(typed.length))
                        expanded = true
                        return true
                    }

                    when (event.type) {
                        KeyEventType.KeyDown -> {
                            // Letters with an assigned shortcut can't open Vibe Bar instantly
                            // on KeyDown - holding vs. tapping can only be told apart once the
                            // key is released (or the hold timer fires), so the open-trigger
                            // moves to KeyUp for exactly these letters. Every other key
                            // (including unassigned letters, the default/majority case) keeps
                            // today's zero-latency instant-open behavior, unchanged.
                            if (shortcut != null) {
                                if (nativeEvent.repeatCount == 0) {
                                    pendingHoldLetter = letter
                                    pendingHoldFired = false
                                    pendingHoldJob?.cancel()
                                    pendingHoldJob = coroutineScope.launch {
                                        delay(LETTER_HOLD_THRESHOLD_MS)
                                        if (pendingHoldLetter == letter) {
                                            pendingHoldFired = true
                                            runLetterShortcut(shortcut)
                                        }
                                    }
                                }
                                return@onPreviewKeyEvent true
                            }
                            openWithTypedChar()
                        }
                        KeyEventType.KeyUp -> {
                            if (letter != null && letter == pendingHoldLetter) {
                                pendingHoldJob?.cancel()
                                pendingHoldJob = null
                                val fired = pendingHoldFired
                                pendingHoldLetter = null
                                pendingHoldFired = false
                                if (!fired) openWithTypedChar()
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
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
            MaterialTheme(typography = settingsTypography()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (confirmationMessage != null) {
                        ConfirmationPill(confirmationMessage!!)
                    } else {
                        val hasResults = contactResults.isNotEmpty() ||
                            (prefix in listOf('@', '#') && !hasContactsPermission)
                        if (hasResults) {
                            Column(
                                Modifier.fillMaxWidth().heightIn(max = 260.dp).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                contactResults.forEachIndexed { index, contact ->
                                    ContactSuggestionRow(contact = contact, emphasized = index == 0) {
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
                                if (prefix in listOf('@', '#') && !hasContactsPermission) {
                                    SuggestionRow(text = "Allow contacts access to search people", icon = Icons.Filled.Person) {
                                        contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                    }
                                }
                            }
                        }

                        val eventPreview = parsedEvent?.let { eventPreviewLabel(it) }
                        previewTextFor(previewPrefix, payload, selectedContact, eventPreview)?.let { preview ->
                            Text(
                                text = preview,
                                color = LauncherMutedGray,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth().heightIn(min = VIBE_BAR_MIN_HEIGHT_DP.dp),
                            shape = CardCornerShape,
                            color = LauncherBlack,
                            border = BorderStroke(1.dp, LauncherMutedGray.copy(alpha = 0.4f))
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                selectedContact?.let { contact ->
                                    CommandChip(
                                        "${contact.name} (${contact.phoneLabel})",
                                        accent,
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
                                    lockedPrefix = lockedPrefix,
                                    accent = accent,
                                    focusRequester = focusRequester,
                                    onSubmit = { submit() },
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { submit() }) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardReturn, contentDescription = "Run command", tint = accent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    callToConfirm?.let { contact ->
        AlertDialog(
            onDismissRequest = { callToConfirm = null },
            icon = { Icon(Icons.Filled.Phone, null, tint = accent) },
            title = { Text("Call ${contact.name}") },
            text = { Text("${contact.phoneLabel}: ${contact.phone}") },
            confirmButton = {
                Button(
                    onClick = {
                        callToConfirm = null
                        callDirectOrRequestAccess(contact.phone)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = LauncherWhite)
                ) { Text("Call now") }
            },
            dismissButton = {
                TextButton(onClick = { callToConfirm = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ConfirmationPill(message: String) {
    val accent = LocalAccentColor.current
    Surface(
        shape = BadgeCornerShape,
        color = LauncherCard,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = accent, modifier = Modifier.padding(end = 8.dp))
            Text(message, color = LauncherWhite)
        }
    }
}

/** Colors just the leading command-prefix character (e.g. '@', '-', '+') in the input's
 *  displayed text - purely cosmetic, doesn't change the underlying text/offsets. */
private fun commandPrefixTransformation(accent: Color): VisualTransformation = VisualTransformation { text ->
    val raw = text.text
    val firstChar = raw.firstOrNull()
    if (firstChar == null || firstChar !in VIBE_BAR_COMMAND_PREFIXES) {
        return@VisualTransformation androidx.compose.ui.text.input.TransformedText(text, OffsetMapping.Identity)
    }
    val annotated = AnnotatedString.Builder().apply {
        withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold)) { append(firstChar) }
        append(raw.drop(1))
    }.toAnnotatedString()
    androidx.compose.ui.text.input.TransformedText(annotated, OffsetMapping.Identity)
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun VibeBarInputField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    prefix: Char?,
    lockedPrefix: Char?,
    accent: Color,
    focusRequester: FocusRequester,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    maxLines: Int = 5
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Type, or use @ # - * !", color = LauncherMutedGray) },
        // The field is multi-line (a note can wrap), so a hardware Enter would otherwise
        // just insert a newline and no typed line could ever be run from the keyboard -
        // the whole point on a keyboard phone. Enter runs the command; Shift+Enter still
        // breaks the line. The matching KeyUp is swallowed too, or it lands in the field
        // as a stray newline after the command has already been submitted.
        modifier = modifier
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                val nativeEvent = event.nativeKeyEvent
                val isEnter = nativeEvent.keyCode == android.view.KeyEvent.KEYCODE_ENTER ||
                    nativeEvent.keyCode == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER
                if (!isEnter || nativeEvent.isShiftPressed) return@onPreviewKeyEvent false
                if (event.type == KeyEventType.KeyDown) onSubmit()
                true
            },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        visualTransformation = if (lockedPrefix == null) commandPrefixTransformation(accent) else VisualTransformation.None,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(
            imeAction = if (prefix in VIBE_BAR_COMMAND_PREFIXES) ImeAction.Send else ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = { onSubmit() },
            onSend = { onSubmit() }
        )
    )
}
