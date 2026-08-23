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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.vibelauncher.app.ui.theme.LauncherCard
import com.vibelauncher.app.ui.theme.LauncherMutedGray
import com.vibelauncher.app.ui.theme.LauncherRed
import com.vibelauncher.app.ui.theme.LauncherWhite
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenHomeApps: () -> Unit,
    onOpenCardColor: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

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
                text = "Launcher Settings",
                color = LauncherWhite,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenHomeApps)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Home Screen Apps", color = LauncherWhite, style = MaterialTheme.typography.bodyLarge)
            Icon(imageVector = Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = LauncherMutedGray)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenCardColor)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Card & Icon Color", color = LauncherWhite, style = MaterialTheme.typography.bodyLarge)
            Icon(imageVector = Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = LauncherMutedGray)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { viewModel.setVibeBarEnabled(!uiState.vibeBarEnabled) })
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.vibeBarEnabled,
                onCheckedChange = viewModel::setVibeBarEnabled,
                colors = CheckboxDefaults.colors(checkedColor = LauncherRed)
            )
            Text(
                text = "Vibe Bar",
                color = LauncherWhite,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { viewModel.setTileBorderEnabled(!uiState.tileBorderEnabled) })
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.tileBorderEnabled,
                onCheckedChange = viewModel::setTileBorderEnabled,
                colors = CheckboxDefaults.colors(checkedColor = LauncherRed)
            )
            Text(
                text = "Icon borders",
                color = LauncherWhite,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        if (uiState.tileBorderEnabled) {
            Text(
                text = "Icon size: ${uiState.tileBorderSizeStep}/10",
                color = LauncherMutedGray,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Slider(
                value = uiState.tileBorderSizeStep.toFloat(),
                onValueChange = { viewModel.setTileBorderSizeStep(it.roundToInt()) },
                valueRange = 1f..10f,
                steps = 8,
                colors = SliderDefaults.colors(thumbColor = LauncherRed, activeTrackColor = LauncherRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }

        Text(
            text = "Icon Theme",
            color = LauncherMutedGray,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )
        Text(
            text = "Applies to the app drawer, and optionally the home screen below.",
            color = LauncherMutedGray,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { viewModel.setApplyIconThemeToHomeTiles(!uiState.applyIconThemeToHomeTiles) })
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.applyIconThemeToHomeTiles,
                onCheckedChange = viewModel::setApplyIconThemeToHomeTiles,
                colors = CheckboxDefaults.colors(checkedColor = LauncherRed)
            )
            Text(
                text = "Also apply to home screen icons",
                color = LauncherWhite,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
            item {
                IconThemeRow(
                    label = "Default",
                    icon = null,
                    selected = uiState.selectedIconThemePackage.isBlank(),
                    onClick = { viewModel.selectIconTheme("") }
                )
            }
            items(uiState.iconPacks, key = { it.packageName }) { pack ->
                IconThemeRow(
                    label = pack.label,
                    icon = pack.icon,
                    selected = uiState.selectedIconThemePackage == pack.packageName,
                    onClick = { viewModel.selectIconTheme(pack.packageName) }
                )
            }
            if (uiState.iconPacks.isEmpty()) {
                item {
                    Text(
                        text = "No icon pack apps found. Install one from the Play Store to theme the app drawer.",
                        color = LauncherMutedGray,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun IconThemeRow(
    label: String,
    icon: android.graphics.drawable.Drawable?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                val painter = remember(label) { BitmapPainter(icon.toBitmap().asImageBitmap()) }
                Image(
                    painter = painter,
                    contentDescription = label,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                )
            } else {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(LauncherCard)
                )
            }
            Text(
                text = label,
                color = LauncherWhite,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        if (selected) {
            Icon(imageVector = Icons.Filled.Check, contentDescription = "Selected", tint = LauncherRed)
        }
    }
}
