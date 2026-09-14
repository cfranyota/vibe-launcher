package com.vibelauncher.app.ui.setup

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.vibelauncher.app.data.usage.HourState
import com.vibelauncher.app.service.NotificationBadgeListenerService
import com.vibelauncher.app.ui.home.components.ActivityBar
import com.vibelauncher.app.ui.home.components.ZipCodeDialog
import com.vibelauncher.app.util.HomeRoleUtils

/** Routes every step that asks for something to its own screen. */
@Composable
internal fun PermissionStep(
    step: SetupStep,
    label: String,
    access: SetupAccess,
    zipCode: String,
    onAccessChanged: () -> Unit,
    onSaveZipCode: (String) -> Unit
) {
    when (step) {
        SetupStep.MAKE_HOME -> MakeHomeStep(label, access.isDefaultHome, onAccessChanged)
        SetupStep.LETTER_KEYS -> LetterKeysStep(label)
        SetupStep.CALENDAR -> RuntimePermissionStep(
            label = label,
            title = "calendar",
            description = "puts today's events on the home screen, and lets * add an event straight to your calendar - *dentist mar 24 9a.",
            permissions = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
            granted = access.calendar,
            onResult = onAccessChanged
        )
        SetupStep.CONTACTS -> RuntimePermissionStep(
            label = label,
            title = "contacts",
            description = "lets @ and # find people as you type their name. your contacts stay on this phone.",
            permissions = arrayOf(Manifest.permission.READ_CONTACTS),
            granted = access.contacts,
            onResult = onAccessChanged
        )
        SetupStep.TEXT_AND_CALL -> RuntimePermissionStep(
            label = label,
            title = "texting & calling",
            description = "@ sends a text and # places a call right from home, without opening another app first.",
            permissions = arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.CALL_PHONE),
            granted = access.textAndCall,
            onResult = onAccessChanged
        )
        SetupStep.NOTIFICATIONS -> NotificationsStep(label, access.notifications)
        SetupStep.USAGE -> UsageStep(label, access.usage)
        SetupStep.WEATHER -> WeatherStep(label, zipCode, onSaveZipCode)
        else -> Unit
    }
}

@Composable
private fun MakeHomeStep(label: String, isDefaultHome: Boolean, onResult: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { onResult() }
    val homeSettings = Intent(Settings.ACTION_HOME_SETTINGS)
    val allSettings = Intent(Settings.ACTION_SETTINGS)

    StepLayout(label = label, title = "make it home") {
        StepText("android needs to be told which app is your home screen. until then, the home key takes you back to the one your phone came with.")
        StatusPill(isDefaultHome, grantedText = "vibe is your home screen")
        if (!isDefaultHome) {
            PrimaryButton("make vibe home") {
                val request = HomeRoleUtils.requestHomeRoleIntent(context)
                if (request != null) {
                    runCatching { launcher.launch(request) }
                } else {
                    context.startFirstAvailable(homeSettings, allSettings)
                }
            }
            // Android stops showing the role prompt after it's been turned down, so there's
            // always a way to pick it by hand as well.
            SecondaryButton("choose it in android settings") { context.startFirstAvailable(homeSettings, allSettings) }
        }
    }
}

@Composable
private fun LetterKeysStep(label: String) {
    val context = LocalContext.current
    StepLayout(label = label, title = "the letter keys") {
        StepText("this phone can map letter keys to apps. while that's switched on, the phone takes every letter before vibe sees it - so typing from home, and holding a letter for a shortcut, won't work.")
        StepText("1. open shortcut keys\n2. scroll down and tap letter key\n3. switch off keyboard shortcuts\n4. press back to come back here")
        PrimaryButton("open shortcut keys") { context.startFirstAvailable(titanShortcutKeysIntent()) }
        StepText("there's no way for vibe to check this one, so move on when it's done.", muted = true)
    }
}

/**
 * Calendar, contacts, texting & calling - ordinary runtime permissions with a system dialog.
 *
 * Android stops showing that dialog once it's been denied twice. The only way to tell is
 * after a request comes back denied with no rationale to show - so from then on the step
 * offers App info, where it can still be switched on, while still letting the user try the
 * dialog again (dismissing it without choosing reads the same way, and would work again).
 */
@Composable
private fun RuntimePermissionStep(
    label: String,
    title: String,
    description: String,
    permissions: Array<String>,
    granted: Boolean,
    onResult: () -> Unit
) {
    val context = LocalContext.current
    var blocked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        onResult()
        val denied = results.filterValues { !it }.keys
        val activity = context.findActivity()
        blocked = denied.isNotEmpty() && activity != null &&
            denied.none { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
    }

    StepLayout(label = label, title = title) {
        StepText(description)
        StatusPill(granted)
        if (!granted) {
            if (blocked) {
                StepText("android won't ask again from here. open app info, tap permissions, and switch it on there.", muted = true)
                PrimaryButton("open app info") { context.startFirstAvailable(appInfoIntent(context.packageName)) }
                SecondaryButton("try again") { launcher.launch(permissions) }
            } else {
                PrimaryButton("allow") { launcher.launch(permissions) }
            }
        }
    }
}

@Composable
private fun NotificationsStep(label: String, granted: Boolean) {
    val context = LocalContext.current
    StepLayout(label = label, title = "notifications") {
        StepText("lets your home tiles show a badge when an app has something unread. nothing leaves your phone.")
        StatusPill(granted)
        if (!granted) {
            PrimaryButton("open notification access") {
                // Android 11+ can open vibe's own switch directly; older versions get the list.
                val component = ComponentName(context, NotificationBadgeListenerService::class.java).flattenToString()
                val direct = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                        .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component)
                } else {
                    null
                }
                context.startFirstAvailable(*listOfNotNull(direct, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)).toTypedArray())
            }
            // Android 13+ greys this switch out for any app installed from a file rather than a
            // store, which is how Vibe is installed - so the way around it is part of the step.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Column(Modifier.padding(top = 4.dp)) {
                    StepText("switch greyed out? android holds this setting back for apps installed from a file. open app info, tap ⋮ in the top corner, choose allow restricted settings, then come back and try again.", muted = true)
                    SecondaryButton("open app info") { context.startFirstAvailable(appInfoIntent(context.packageName)) }
                }
            }
        }
    }
}

@Composable
private fun UsageStep(label: String, granted: Boolean) {
    val context = LocalContext.current
    // A made-up morning: intentional hours, one that slipped, and the rest still to come.
    val sampleDay = List(24) { hour ->
        when {
            hour == 10 -> HourState.DISTRACTED
            hour <= 12 -> HourState.INTENTIONAL
            else -> HourState.AHEAD
        }
    }
    StepLayout(label = label, title = "your day") {
        StepText("the row of dots under the date is today, one dot per hour.")
        ActivityBar(hours = sampleDay, modifier = Modifier.padding(vertical = 4.dp))
        StepText("white when the hour stayed intentional, your accent color when it slipped into feeds, social or video, dim for hours still to come. usage access is how vibe knows which apps you used when - it stays on this phone.")
        StatusPill(granted)
        if (!granted) {
            PrimaryButton("open usage access") {
                // Newer Android jumps straight to vibe's entry when given the package.
                context.startFirstAvailable(
                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:${context.packageName}")),
                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                )
            }
        }
    }
}

@Composable
private fun WeatherStep(label: String, zipCode: String, onSave: (String) -> Unit) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    StepLayout(label = label, title = "weather") {
        StepText("shows the temperature beside the date. set a US zip or Canadian postal code - you can change it any time by tapping the weather on home.")
        StatusPill(zipCode.isNotBlank(), grantedText = zipCode.ifBlank { "on" }, missingText = "not set")
        PrimaryButton(if (zipCode.isBlank()) "set location" else "change location") { showDialog = true }
    }
    if (showDialog) {
        ZipCodeDialog(
            currentZipCode = zipCode,
            onSave = {
                onSave(it)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

private fun appInfoIntent(packageName: String): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
