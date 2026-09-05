package com.vibelauncher.app.ui.settings

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vibelauncher.app.util.IntentDefaults
import com.vibelauncher.app.ui.settings.components.SectionHeader
import com.vibelauncher.app.ui.settings.components.SettingsNavRow
import com.vibelauncher.app.ui.settings.components.ToggleRow
import com.vibelauncher.app.ui.theme.LauncherMutedGray
import com.vibelauncher.app.ui.theme.LauncherWhite
import com.vibelauncher.app.ui.theme.LocalAccentColor
import com.vibelauncher.app.ui.theme.settingsTypography
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenHomeApps: () -> Unit,
    onOpenCardColor: () -> Unit,
    onOpenIconTheme: () -> Unit,
    onOpenLetterShortcuts: () -> Unit,
    onOpenMonkMode: () -> Unit,
    onOpenHub: () -> Unit,
    onOpenEmailApps: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var isVibeDefaultSms by remember { mutableStateOf(IntentDefaults.hasSmsRole(context)) }
    val smsRoleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { isVibeDefaultSms = IntentDefaults.hasSmsRole(context) }

    MaterialTheme(typography = settingsTypography()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back", tint = LauncherWhite)
                }
                Text(
                    text = "settings",
                    color = LauncherWhite,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            SectionHeader("Appearance")
            SettingsNavRow(
                icon = Icons.Filled.Palette,
                title = "appearance",
                subtitle = "text size and accent color",
                onClick = onOpenAppearance
            )
            SettingsNavRow(
                icon = Icons.Filled.Widgets,
                title = "card & icon color",
                subtitle = "home screen card and icon tint",
                onClick = onOpenCardColor
            )

            SectionHeader("Home Screen", modifier = Modifier.padding(top = 12.dp))
            SettingsNavRow(
                icon = Icons.Filled.Inbox,
                title = "hub",
                subtitle = "calls and notifications in one feed",
                onClick = onOpenHub
            )
            SettingsNavRow(
                icon = Icons.Filled.Email,
                title = "email apps",
                subtitle = "choose which apps count as email in Hub",
                onClick = onOpenEmailApps
            )
            SettingsNavRow(
                icon = Icons.Filled.SwapHoriz,
                title = "default messaging app",
                subtitle = if (isVibeDefaultSms) {
                    "Vibe (Hub) - tap to switch to Google Messages"
                } else {
                    "Google Messages - tap to switch back to Vibe (Hub)"
                },
                onClick = {
                    val intent = if (isVibeDefaultSms) {
                        val request = IntentDefaults.requestSmsRoleFor(IntentDefaults.GOOGLE_MESSAGES_PACKAGE)
                        if (request.resolveActivity(context.packageManager) != null) {
                            request
                        } else {
                            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                        }
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        context.getSystemService(RoleManager::class.java)?.createRequestRoleIntent(RoleManager.ROLE_SMS)
                    } else {
                        Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                            .putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
                    }
                    if (intent != null) runCatching { smsRoleLauncher.launch(intent) }
                }
            )
            SettingsNavRow(
                icon = Icons.Filled.GridView,
                title = "home screen apps",
                subtitle = "choose your 8 home tiles",
                onClick = onOpenHomeApps
            )
            SettingsNavRow(
                icon = Icons.Filled.Checklist,
                title = "icon theme",
                subtitle = "apply an icon pack to the app drawer",
                onClick = onOpenIconTheme
            )

            SectionHeader("Shortcuts", modifier = Modifier.padding(top = 12.dp))
            SettingsNavRow(
                icon = Icons.Filled.Keyboard,
                title = "letter shortcuts",
                subtitle = "hold a letter to open an app, message, or call",
                onClick = onOpenLetterShortcuts
            )
            SettingsNavRow(
                icon = Icons.Filled.SelfImprovement,
                title = "vibe mode",
                subtitle = "essentials only, hide social & browsers, grayscale",
                onClick = onOpenMonkMode
            )

            ToggleRow(
                title = "Vibe Bar",
                subtitle = "the '/' bar for quick actions",
                checked = uiState.vibeBarEnabled,
                onCheckedChange = viewModel::setVibeBarEnabled
            )
            ToggleRow(
                title = "Icon borders",
                subtitle = "outline home screen icons",
                checked = uiState.tileBorderEnabled,
                onCheckedChange = viewModel::setTileBorderEnabled
            )
            if (uiState.tileBorderEnabled) {
                Text(
                    text = "icon size: ${uiState.tileBorderSizeStep}/10",
                    color = LauncherMutedGray,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Slider(
                    value = uiState.tileBorderSizeStep.toFloat(),
                    onValueChange = { viewModel.setTileBorderSizeStep(it.roundToInt()) },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = LocalAccentColor.current,
                        activeTrackColor = LocalAccentColor.current
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}
