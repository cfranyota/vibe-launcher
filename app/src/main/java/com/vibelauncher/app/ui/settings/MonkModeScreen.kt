package com.vibelauncher.app.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vibelauncher.app.ui.settings.components.SectionHeader
import com.vibelauncher.app.ui.settings.components.SettingsNavRow
import com.vibelauncher.app.ui.settings.components.ToggleRow
import com.vibelauncher.app.ui.theme.LauncherMutedGray
import com.vibelauncher.app.ui.theme.LauncherWhite
import com.vibelauncher.app.ui.theme.settingsTypography
import androidx.compose.material.icons.filled.Contrast

/** Three independent focus tiers - essentials-only app allowlist, hiding social/browser
 *  apps entirely, and a grayscale shortcut. No install-blocking/lockdown tier - that's
 *  only realistically achievable via Device Owner (MDM) provisioning, far more invasive
 *  than a normal launcher should ask for. */
@Composable
fun MonkModeScreen(viewModel: SettingsViewModel, onBack: () -> Unit, onOpenEssentialsAllowlist: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    MaterialTheme(typography = settingsTypography()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back", tint = LauncherWhite)
                }
                Text(
                    text = "vibe mode",
                    color = LauncherWhite,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
            }
            Text(
                text = "Cut down what your phone shows you, in as many or as few ways as you want.",
                color = LauncherMutedGray,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
            )

            SectionHeader("APP DRAWER")
            ToggleRow(
                title = "Essentials only",
                subtitle = "app drawer only shows apps you've allowed",
                checked = uiState.monkEssentialsOnlyEnabled,
                onCheckedChange = viewModel::setMonkEssentialsOnlyEnabled
            )
            if (uiState.monkEssentialsOnlyEnabled) {
                SettingsNavRow(
                    icon = Icons.Filled.Checklist,
                    title = "choose essentials",
                    subtitle = "pick which apps stay visible",
                    onClick = onOpenEssentialsAllowlist
                )
            }
            ToggleRow(
                title = "Hide social & browsers",
                subtitle = "removes social apps and browsers from the drawer",
                checked = uiState.monkHideSocialBrowserEnabled,
                onCheckedChange = viewModel::setMonkHideSocialBrowserEnabled
            )

            SectionHeader("DISPLAY")
            SettingsNavRow(
                icon = Icons.Filled.Contrast,
                title = "grayscale always",
                subtitle = "opens Android's own Accessibility settings to turn it on",
                onClick = {
                    runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                }
            )
        }
    }
}
