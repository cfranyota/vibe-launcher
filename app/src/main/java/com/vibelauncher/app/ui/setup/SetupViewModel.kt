package com.vibelauncher.app.ui.setup

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vibelauncher.app.data.notifications.NotificationBadgeRepository
import com.vibelauncher.app.data.settings.SettingsRepository
import com.vibelauncher.app.data.todos.TodoRepository
import com.vibelauncher.app.data.usage.UsageActivityRepository
import com.vibelauncher.app.features.vibebar.parseTodoText
import com.vibelauncher.app.util.HomeRoleUtils
import com.vibelauncher.app.util.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** [FULL] is first-run setup (and "run setup again"): permissions, then the tour. [TOUR] is
 *  "how to use vibe" from Settings - the tour on its own, which never touches setup's
 *  completed flag. */
enum class SetupMode(val routeValue: String) {
    FULL("full"),
    TOUR("tour");

    companion object {
        fun fromRoute(value: String?): SetupMode = entries.firstOrNull { it.routeValue == value } ?: FULL
    }
}

enum class SetupStep {
    WELCOME,
    MAKE_HOME,
    LETTER_KEYS,
    CALENDAR,
    CONTACTS,
    TEXT_AND_CALL,
    NOTIFICATIONS,
    USAGE,
    WEATHER,
    VIBE_BAR,
    TRY_IT,
    HOME_SCREEN,
    TILES_AND_SHORTCUTS,
    SETTINGS,
    DONE
}

/** What's been granted so far, re-read whenever the screen resumes - most of these are
 *  switched on in system Settings, so there's no result callback to learn about them from. */
data class SetupAccess(
    val isDefaultHome: Boolean = false,
    val calendar: Boolean = false,
    val contacts: Boolean = false,
    val textAndCall: Boolean = false,
    val notifications: Boolean = false,
    val usage: Boolean = false
)

data class SetupUiState(
    val mode: SetupMode = SetupMode.FULL,
    val steps: List<SetupStep> = emptyList(),
    val index: Int = 0,
    val access: SetupAccess = SetupAccess(),
    val zipCode: String = "",
    /** The text of the to-do saved from the try-it step, for its confirmation. */
    val triedTodo: String? = null
) {
    val step: SetupStep get() = steps[index]
    val isFirst: Boolean get() = index == 0
}

internal const val TITAN_SHORTCUT_KEYS_PACKAGE = "com.agui.shortcutsettings"

/** The Titan's own "Shortcut keys" screen. It has no launcher entry, so it's opened by
 *  component - the manifest's <queries> makes the package visible to resolve it. */
internal fun titanShortcutKeysIntent(): Intent =
    Intent(Intent.ACTION_MAIN)
        .setComponent(ComponentName(TITAN_SHORTCUT_KEYS_PACKAGE, "$TITAN_SHORTCUT_KEYS_PACKAGE.ui.EntryAppActivity"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

class SetupViewModel(
    private val appContext: Context,
    private val mode: SetupMode,
    private val settingsRepository: SettingsRepository,
    private val todoRepository: TodoRepository,
    private val usageActivityRepository: UsageActivityRepository
) : ViewModel() {

    private val steps = buildList {
        if (mode == SetupMode.FULL) {
            add(SetupStep.WELCOME)
            add(SetupStep.MAKE_HOME)
            // Only Unihertz phones have the shortcut-keys feature that swallows letter keys.
            if (titanShortcutKeysIntent().resolveActivity(appContext.packageManager) != null) {
                add(SetupStep.LETTER_KEYS)
            }
            add(SetupStep.CALENDAR)
            add(SetupStep.CONTACTS)
            add(SetupStep.TEXT_AND_CALL)
            add(SetupStep.NOTIFICATIONS)
            add(SetupStep.USAGE)
            add(SetupStep.WEATHER)
        }
        add(SetupStep.VIBE_BAR)
        add(SetupStep.TRY_IT)
        add(SetupStep.HOME_SCREEN)
        add(SetupStep.TILES_AND_SHORTCUTS)
        add(SetupStep.SETTINGS)
        add(SetupStep.DONE)
    }

    private val index = MutableStateFlow(0)
    private val access = MutableStateFlow(readAccess())
    private val triedTodo = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SetupUiState> = combine(index, access, settingsRepository.zipCode, triedTodo) { i, granted, zip, tried ->
        SetupUiState(mode = mode, steps = steps, index = i, access = granted, zipCode = zip, triedTodo = tried)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SetupUiState(mode = mode, steps = steps, access = access.value))

    fun next() {
        if (index.value < steps.lastIndex) index.value += 1
    }

    /** Steps backwards. On the first step Back leaves - the tour, or setup someone already
     *  finished once, can always be closed - except during first-run setup, where leaving
     *  would just bring it back on the next launch. */
    fun back(onLeave: () -> Unit) {
        if (index.value > 0) {
            index.value -= 1
            return
        }
        viewModelScope.launch {
            if (mode == SetupMode.TOUR || !settingsRepository.needsFirstRunSetup.first()) onLeave()
        }
    }

    fun refreshAccess() {
        access.value = readAccess()
    }

    fun saveZipCode(zipCode: String) {
        viewModelScope.launch { settingsRepository.setZipCode(zipCode) }
    }

    /** The try-it step saves a real to-do, exactly as Vibe Bar's '-' would - dates included -
     *  so the first thing someone types is waiting in their list afterwards. The leading '-'
     *  is optional here, since the step is already about to-dos. */
    fun saveTryItTodo(typed: String) {
        val draft = parseTodoText(typed.trim().removePrefix("-"))
        if (draft.text.isBlank()) return
        viewModelScope.launch {
            todoRepository.add(draft.text, draft.dueAt, draft.dueAllDay)
            triedTodo.value = draft.text
        }
    }

    /** Records that setup is done before leaving - leaving first would clear this ViewModel
     *  and cancel the write before it landed, and setup would be back on the next launch. The
     *  tour on its own leaves that flag alone. */
    fun finish(onFinished: () -> Unit) {
        viewModelScope.launch {
            if (mode == SetupMode.FULL) settingsRepository.markSetupComplete()
            onFinished()
        }
    }

    private fun readAccess() = SetupAccess(
        isDefaultHome = HomeRoleUtils.isDefaultHome(appContext),
        calendar = PermissionUtils.hasCalendarPermission(appContext) &&
            PermissionUtils.hasWriteCalendarPermission(appContext),
        contacts = PermissionUtils.hasContactsPermission(appContext),
        textAndCall = PermissionUtils.hasSmsPermission(appContext) &&
            PermissionUtils.hasCallPermission(appContext),
        notifications = NotificationBadgeRepository.hasNotificationAccess(appContext),
        usage = usageActivityRepository.hasUsageAccess()
    )

    class Factory(
        private val appContext: Context,
        private val mode: SetupMode,
        private val settingsRepository: SettingsRepository,
        private val todoRepository: TodoRepository,
        private val usageActivityRepository: UsageActivityRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SetupViewModel(appContext, mode, settingsRepository, todoRepository, usageActivityRepository) as T
        }
    }
}
