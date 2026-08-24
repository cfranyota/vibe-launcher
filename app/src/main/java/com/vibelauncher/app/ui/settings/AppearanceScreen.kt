package com.vibelauncher.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.vibelauncher.app.ui.settings.components.SectionHeader
import com.vibelauncher.app.ui.theme.LauncherCard
import com.vibelauncher.app.ui.theme.LauncherMutedGray
import com.vibelauncher.app.ui.theme.LauncherRed
import com.vibelauncher.app.ui.theme.LauncherWhite
import com.vibelauncher.app.ui.theme.LocalAccentColor
import com.vibelauncher.app.ui.theme.VibeAppColor
import com.vibelauncher.app.ui.theme.VibeCallColor
import com.vibelauncher.app.ui.theme.VibeEventColor
import com.vibelauncher.app.ui.theme.VibeNoteColor
import com.vibelauncher.app.ui.theme.VibeTextColor
import com.vibelauncher.app.ui.theme.VibeTodoColor
import com.vibelauncher.app.ui.theme.settingsTypography

private val ACCENT_PRESETS = listOf(
    Color(0xFFF97316), // default orange
    LauncherRed,
    VibeTextColor,
    VibeCallColor,
    VibeTodoColor,
    VibeNoteColor,
    VibeEventColor,
    VibeAppColor
)

@Composable
fun AppearanceScreen(
    viewModel: AppearanceViewModel,
    onBack: () -> Unit,
    onOpenCustomAccent: () -> Unit
) {
    val uiState = viewModel.uiState.collectAsState().value ?: AppearanceUiState()

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
                    text = "appearance",
                    color = LauncherWhite,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            SectionHeader("Text Size")
            Text(
                text = "font size",
                color = LauncherWhite,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp)
            )
            Slider(
                value = uiState.fontScale,
                onValueChange = viewModel::setFontScale,
                valueRange = 0.85f..1.3f,
                colors = SliderDefaults.colors(
                    thumbColor = LocalAccentColor.current,
                    activeTrackColor = LocalAccentColor.current
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            SectionHeader("Accent", modifier = Modifier.padding(top = 12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ACCENT_PRESETS.forEach { preset ->
                    AccentSwatch(
                        color = preset,
                        selected = preset.toArgb() == uiState.accentColorArgb,
                        onClick = { viewModel.setAccentColor(preset.toArgb()) }
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.dp, LauncherMutedGray, CircleShape)
                        .clickable(onClick = onOpenCustomAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Custom accent", tint = LauncherMutedGray)
                }
            }
            Text(
                text = "Accent color affects toggles, sliders, and highlights across every screen.",
                color = LauncherMutedGray,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun AccentSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (selected) LauncherCard else Color.Transparent)
            .then(if (selected) Modifier.border(2.dp, LocalAccentColor.current, CircleShape) else Modifier)
            .padding(4.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick)
    )
}
