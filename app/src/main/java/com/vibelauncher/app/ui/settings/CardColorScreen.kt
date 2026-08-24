package com.vibelauncher.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vibelauncher.app.data.calendar.CalendarEvent
import com.vibelauncher.app.ui.home.components.EventCard
import com.vibelauncher.app.ui.settings.components.BrightnessSlider
import com.vibelauncher.app.ui.settings.components.ColorWheel
import com.vibelauncher.app.ui.settings.components.OpacitySlider
import com.vibelauncher.app.ui.settings.components.PillToggle
import com.vibelauncher.app.ui.settings.components.SectionHeader
import com.vibelauncher.app.ui.theme.LauncherCard
import com.vibelauncher.app.ui.theme.LauncherMutedGray
import com.vibelauncher.app.ui.theme.LauncherWhite
import com.vibelauncher.app.ui.theme.LocalAccentColor
import com.vibelauncher.app.ui.theme.settingsTypography

private val PREVIEW_EVENT = CalendarEvent(
    id = -1,
    title = "Prospecting text & email",
    startMillis = System.currentTimeMillis() + 30 * 60_000,
    endMillis = System.currentTimeMillis() + 90 * 60_000,
    isAllDay = false
)

/**
 * No opaque background on this screen's root - same as HomeScreen.kt - so the real system
 * wallpaper shows through behind the live preview card exactly as it will on Home, making
 * the glass/opacity effect immediately and accurately visible while adjusting it.
 */
@Composable
fun CardColorScreen(viewModel: CardColorViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val customColor = Color.hsv(uiState.hue, uiState.saturation, uiState.brightness, uiState.opacity)
    val previewColor = if (uiState.enabled) customColor else LauncherCard

    MaterialTheme(typography = settingsTypography()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
                text = "card & icon color",
                color = LauncherWhite,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Text(
            text = "Applies to the Calendar and To-Do cards on the home screen.",
            color = LauncherMutedGray,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
        )

        SectionHeader("Card Color")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Custom color", color = LauncherWhite, style = MaterialTheme.typography.bodyLarge)
            PillToggle(checked = uiState.enabled, onCheckedChange = viewModel::setEnabled)
        }
        Text(
            text = if (uiState.enabled) "On - cards use the color below." else "Off - cards stay the default color.",
            color = LauncherMutedGray,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
        )

        EventCard(
            event = PREVIEW_EVENT,
            nowMillis = System.currentTimeMillis(),
            backgroundColor = previewColor,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        ColorWheel(
            hue = uiState.hue,
            saturation = uiState.saturation,
            onColorChange = viewModel::setHueSaturation,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 240.dp)
                .padding(vertical = 24.dp)
        )

        BrightnessSlider(
            value = uiState.brightness,
            onValueChange = viewModel::setBrightness,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        OpacitySlider(
            value = uiState.opacity,
            onValueChange = viewModel::setOpacity,
            onGlassPreset = viewModel::applyGlassPreset,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        SectionHeader("Icon Color", modifier = Modifier.padding(top = 24.dp))
        Text(
            text = "Applies to the calendar and to-do icons, the badge circle, and the weather sun icon.",
            color = LauncherMutedGray,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Custom color", color = LauncherWhite, style = MaterialTheme.typography.bodyLarge)
            PillToggle(checked = uiState.iconEnabled, onCheckedChange = viewModel::setIconEnabled)
        }
        Text(
            text = if (uiState.iconEnabled) "On - icons use the color below." else "Off - icons stay the app accent.",
            color = LauncherMutedGray,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
        )

        val iconPreviewColor = if (uiState.iconEnabled) {
            Color.hsv(uiState.iconHue, uiState.iconSaturation, uiState.iconBrightness)
        } else {
            LocalAccentColor.current
        }
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Event, contentDescription = null, tint = iconPreviewColor, modifier = Modifier.size(28.dp))
            Icon(Icons.Filled.Checklist, contentDescription = null, tint = iconPreviewColor, modifier = Modifier.size(28.dp))
            Icon(Icons.Filled.WbSunny, contentDescription = null, tint = iconPreviewColor, modifier = Modifier.size(28.dp))
        }

        ColorWheel(
            hue = uiState.iconHue,
            saturation = uiState.iconSaturation,
            onColorChange = viewModel::setIconHueSaturation,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 240.dp)
                .padding(vertical = 24.dp)
        )

        BrightnessSlider(
            value = uiState.iconBrightness,
            onValueChange = viewModel::setIconBrightness,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
    }
    }
}
