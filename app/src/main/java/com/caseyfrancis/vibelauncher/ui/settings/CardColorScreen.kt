package com.caseyfrancis.vibelauncher.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.caseyfrancis.vibelauncher.data.calendar.CalendarEvent
import com.caseyfrancis.vibelauncher.ui.home.components.EventCard
import com.caseyfrancis.vibelauncher.ui.settings.components.BrightnessSlider
import com.caseyfrancis.vibelauncher.ui.settings.components.ColorWheel
import com.caseyfrancis.vibelauncher.ui.settings.components.OpacitySlider
import com.caseyfrancis.vibelauncher.ui.theme.LauncherMutedGray
import com.caseyfrancis.vibelauncher.ui.theme.LauncherWhite

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
    val color = Color.hsv(uiState.hue, uiState.saturation, uiState.brightness, uiState.opacity)

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
                text = "Card Color",
                color = LauncherWhite,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Text(
            text = "Applies to the Calendar and Task cards on the home screen.",
            color = LauncherMutedGray,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
        )

        EventCard(
            event = PREVIEW_EVENT,
            nowMillis = System.currentTimeMillis(),
            backgroundColor = color,
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
    }
}
