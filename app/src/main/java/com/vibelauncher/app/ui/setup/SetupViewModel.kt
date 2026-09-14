package com.vibelauncher.app.ui.setup

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vibelauncher.app.data.notifications.NotificationBadgeRepository
import com.vibelauncher.app.data.settings.SettingsRepository
import com.vibelauncher.app.data.usage.UsageActivityRepository
import com.vibelauncher.app.util.HomeRoleUtils
import com.vibelauncher.app.util.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    val steps: List<SetupStep> = emptyList(),
    val index: Int = 0,
    val access: SetupAccess = SetupAccess(),
    val zipCode: String = ""
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
    private val settingsRepository: SettingsRepository,
    private val usageActivityRepository: UsageActivityRepository
) : ViewModel() {

    private val steps = buildList {
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
        add(SetupStep.DONE)
    }

    private val index = MutableStateFlow(0)
    private val access = MutableStateFlow(readAccess())

    val uiState: StateFlow<SetupUiState> = combine(index, access, settingsRepository.zipCode) { i, granted, zip ->
        SetupUiState(steps = steps, index = i, access = granted, zipCode = zip)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SetupUiState(steps = steps, access = access.value))

    fun next() {
        if (index.value < steps.lastIndex) index.value += 1
    }

    /** False when already on the first step, so the caller knows Back had nowhere to go. */
    fun back(): Boolean {
        if (index.value == 0) return false
        index.value -= 1
        return true
    }

    fun refreshAccess() {
        access.value = readAccess()
    }

    fun saveZipCode(zipCode: String) {
        viewModelScope.launch { settingsRepository.setZipCode(zipCode) }
    }

    /** Records that setup is done before leaving - leaving first would clear this ViewModel
     *  and cancel the write before it landed, and setup would be back on the next launch. */
    fun finish(onFinished: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.markSetupComplete()
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
        private val settingsRepository: SettingsRepository,
        private val usageActivityRepository: UsageActivityRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SetupViewModel(appContext, settingsRepository, usageActivityRepository) as T
        }
    }
}
