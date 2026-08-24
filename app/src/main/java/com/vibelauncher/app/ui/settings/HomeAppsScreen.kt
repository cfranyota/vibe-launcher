package com.vibelauncher.app.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.vibelauncher.app.data.apps.AppInfo
import com.vibelauncher.app.model.BuiltInAction
import com.vibelauncher.app.model.TileTarget
import com.vibelauncher.app.ui.home.components.builtInIcon
import com.vibelauncher.app.ui.settings.components.SectionHeader
import com.vibelauncher.app.ui.theme.LauncherMutedGray
import com.vibelauncher.app.ui.theme.LauncherWhite
import com.vibelauncher.app.ui.theme.LocalAccentColor
import com.vibelauncher.app.ui.theme.settingsTypography
import com.vibelauncher.app.util.IntentDefaults

/**
 * A scrolling checkbox list of every installed app - check up to 8, those become the home
 * screen tiles, in the order checked. "Reset to Default" restores the original 8 built-ins.
 */
@Composable
fun HomeAppsScreen(viewModel: HomeAppsViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    MaterialTheme(typography = settingsTypography()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
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
                    text = "home screen apps",
                    color = LauncherWhite,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
                Text(
                    text = "reset to default",
                    color = LocalAccentColor.current,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .clickable { viewModel.resetToDefault() }
                        .padding(8.dp)
                )
            }
            Text(
                text = "Pick up to 8 apps for your home screen (${uiState.selectedKeys.size}/8 selected).",
                color = LauncherMutedGray,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
            )

            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text("Search apps") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )

            LazyColumn {
                item { SectionHeader("Vibe Launcher") }
                items(uiState.builtInActions) { action ->
                    val checked = action in uiState.selectedKeys
                    val enabled = checked || !uiState.atCap
                    BuiltInCheckRow(
                        action = action,
                        checked = checked,
                        enabled = enabled,
                        onToggle = { viewModel.toggleBuiltIn(action) }
                    )
                }
                item { SectionHeader("Installed Apps") }
                items(uiState.apps, key = { it.packageName to it.className }) { app ->
                    val key = app.packageName to app.className
                    val checked = key in uiState.selectedKeys
                    val enabled = checked || !uiState.atCap
                    HomeAppCheckRow(
                        app = app,
                        checked = checked,
                        enabled = enabled,
                        onToggle = { viewModel.toggleApp(app) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BuiltInCheckRow(action: BuiltInAction, checked: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    val label = remember(action) { IntentDefaults.defaultTiles().first { (it.target as TileTarget.BuiltIn).kind == action }.label }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = builtInIcon(action), contentDescription = label, tint = LauncherWhite, modifier = Modifier.size(32.dp))
            Text(
                text = label,
                color = LauncherWhite,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            enabled = enabled,
            colors = CheckboxDefaults.colors(checkedColor = LocalAccentColor.current)
        )
    }
}

@Composable
private fun HomeAppCheckRow(app: AppInfo, checked: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val painter = remember(app.packageName) { BitmapPainter(app.icon.toBitmap().asImageBitmap()) }
            Image(painter = painter, contentDescription = app.label, modifier = Modifier.size(32.dp))
            Text(
                text = app.label,
                color = LauncherWhite,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            enabled = enabled,
            colors = CheckboxDefaults.colors(checkedColor = LocalAccentColor.current)
        )
    }
}
