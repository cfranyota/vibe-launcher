package com.vibelauncher.app.ui.settings.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.vibelauncher.app.ui.theme.LauncherMutedGray
import com.vibelauncher.app.ui.theme.LauncherWhite
import com.vibelauncher.app.ui.theme.LocalAccentColor
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

const val GLASS_OPACITY = 0.15f

/**
 * Hue (angle) x saturation (distance from center) wheel at fixed value=1 - a standard HSV
 * color-wheel picker. Owns its whole hit area with nothing else competing for gestures in
 * it, so a plain hand-rolled tap+drag loop is enough (no need for the Initial-pass tricks
 * used for gestures elsewhere in this app that have to coexist with sibling clickables).
 */
@Composable
fun ColorWheel(
    hue: Float,
    saturation: Float,
    onColorChange: (hue: Float, saturation: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.fillMaxWidth().aspectRatio(1f)) {
        val sizePx = remember(maxWidth) { with(density) { maxWidth.toPx().roundToInt() }.coerceAtLeast(1) }
        val bitmap = remember(sizePx) { generateColorWheelBitmap(sizePx) }
        val radius = sizePx / 2f

        fun updateFromOffset(offset: Offset) {
            val dx = offset.x - radius
            val dy = offset.y - radius
            val distance = sqrt(dx * dx + dy * dy).coerceAtMost(radius)
            val angleDeg = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat().let { if (it < 0f) it + 360f else it }
            val newSaturation = if (radius > 0f) (distance / radius).coerceIn(0f, 1f) else 0f
            onColorChange(angleDeg, newSaturation)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pointerInput(sizePx) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        updateFromOffset(down.position)
                        drag(down.id) { change ->
                            change.consume()
                            updateFromOffset(change.position)
                        }
                    }
                }
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = "Color wheel",
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            )

            val angleRad = Math.toRadians(hue.toDouble())
            val thumbDistance = saturation * radius
            val thumbX = radius + thumbDistance * cos(angleRad).toFloat()
            val thumbY = radius + thumbDistance * sin(angleRad).toFloat()
            val thumbSizePx = with(density) { 24.dp.toPx() }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (thumbX - thumbSizePx / 2f).roundToInt(),
                            (thumbY - thumbSizePx / 2f).roundToInt()
                        )
                    }
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.hsv(hue, saturation, 1f))
                    .border(2.dp, LauncherWhite, CircleShape)
            )
        }
    }
}

private fun generateColorWheelBitmap(sizePx: Int): ImageBitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val radius = sizePx / 2f
    for (y in 0 until sizePx) {
        for (x in 0 until sizePx) {
            val dx = x - radius
            val dy = y - radius
            val distance = sqrt(dx * dx + dy * dy)
            if (distance > radius) {
                bitmap.setPixel(x, y, android.graphics.Color.TRANSPARENT)
            } else {
                val angleDeg = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat().let { if (it < 0f) it + 360f else it }
                val sat = (distance / radius).coerceIn(0f, 1f)
                bitmap.setPixel(x, y, Color.hsv(angleDeg, sat, 1f).toArgb())
            }
        }
    }
    return bitmap.asImageBitmap()
}

@Composable
fun BrightnessSlider(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "Brightness: ${(value * 100).roundToInt()}%",
            color = LauncherMutedGray,
            style = MaterialTheme.typography.bodySmall
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(thumbColor = LocalAccentColor.current, activeTrackColor = LocalAccentColor.current)
        )
    }
}

@Composable
fun OpacitySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onGlassPreset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Opacity: ${(value * 100).roundToInt()}%",
                color = LauncherMutedGray,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Glass",
                color = LocalAccentColor.current,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(LocalAccentColor.current.copy(alpha = 0.15f))
                    .clickable(onClick = onGlassPreset)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(thumbColor = LocalAccentColor.current, activeTrackColor = LocalAccentColor.current)
        )
    }
}
