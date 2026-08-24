package com.vibelauncher.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.vibelauncher.app.ui.theme.CardCornerShape
import com.vibelauncher.app.ui.theme.LauncherMutedGray
import com.vibelauncher.app.ui.theme.LauncherWhite
import com.vibelauncher.app.ui.theme.settingsTypography

@Composable
fun CustomAccentScreen(viewModel: AppearanceViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    var initialized by remember { mutableStateOf(false) }
    var hue by remember { mutableFloatStateOf(0f) }
    var brightness by remember { mutableFloatStateOf(1f) }

    // uiState is null until the real persisted color loads from DataStore - only seed
    // hue/brightness once we have that real value, never from a placeholder default, then
    // leave local drag state as the source of truth for the rest of this screen's life.
    LaunchedEffect(uiState) {
        val loaded = uiState
        if (!initialized && loaded != null) {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(loaded.accentColorArgb, hsv)
            hue = hsv[0]
            brightness = hsv[2].coerceAtLeast(0.15f)
            initialized = true
        }
    }
    val previewColor = Color.hsv(hue, 1f, brightness)

    fun commit(newHue: Float = hue, newBrightness: Float = brightness) {
        hue = newHue
        brightness = newBrightness
        viewModel.setAccentColor(Color.hsv(newHue, 1f, newBrightness).toArgb())
    }

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
                    text = "custom accent",
                    color = LauncherWhite,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(horizontal = 24.dp)
                    .clip(CardCornerShape)
                    .background(previewColor)
            )

            Text(
                text = "hue",
                color = LauncherMutedGray,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp)
            )
            GradientSlider(
                value = hue,
                valueRange = 0f..360f,
                onValueChange = { commit(newHue = it) },
                gradient = Brush.horizontalGradient(
                    (0..12).map { Color.hsv(it * 30f, 1f, 1f) }
                )
            )

            Text(
                text = "brightness",
                color = LauncherMutedGray,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp)
            )
            GradientSlider(
                value = brightness,
                valueRange = 0.15f..1f,
                onValueChange = { commit(newBrightness = it) },
                gradient = Brush.horizontalGradient(listOf(Color.Black, Color.hsv(hue, 1f, 1f)))
            )

            Row(
                modifier = Modifier
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp)
                    .clip(CardCornerShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(previewColor)
                )
                Text(
                    text = String.format("#%06X", 0xFFFFFF and previewColor.toArgb()),
                    color = LauncherWhite,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun GradientSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    gradient: Brush
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .align(Alignment.Center)
                .clip(CardCornerShape)
                .background(gradient)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = LauncherWhite,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            )
        )
    }
}
