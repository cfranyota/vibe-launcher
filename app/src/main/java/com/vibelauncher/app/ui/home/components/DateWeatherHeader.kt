package com.vibelauncher.app.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vibelauncher.app.data.weather.WeatherInfo
import com.vibelauncher.app.ui.theme.DateWeatherTextStyle
import com.vibelauncher.app.ui.theme.DayTextStyle
import com.vibelauncher.app.ui.theme.LauncherMutedGray
import com.vibelauncher.app.ui.theme.LauncherRed
import com.vibelauncher.app.ui.theme.LauncherWhite
import com.vibelauncher.app.util.ordinalSuffix
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Purely presentational - day-browsing swipe is handled by the root gesture detector
 *  in HomeScreen (see HEADER_SWIPE_ZONE_DP), since a pointerInput nested this deep
 *  inside the Column hierarchy was found to never receive gesture callbacks on-device. */
@Composable
fun DateWeatherHeader(
    selectedDayOffset: Int,
    weather: WeatherInfo?,
    weatherLoading: Boolean,
    onWeatherClick: () -> Unit,
    modifier: Modifier = Modifier,
    sunTint: Color = LauncherRed
) {
    val date = remember(selectedDayOffset) { LocalDate.now().plusDays(selectedDayOffset.toLong()) }
    val dayOfWeekText = remember(date) {
        date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()).lowercase()
    }
    val monthDateText = remember(date) {
        val month = date.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).lowercase()
        "$month ${ordinalSuffix(date.dayOfMonth)}"
    }

    Column(modifier = modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text(text = dayOfWeekText, style = DayTextStyle, color = LauncherWhite)
        Text(text = monthDateText, style = DateWeatherTextStyle, color = LauncherWhite)
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable(onClick = onWeatherClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                weatherLoading -> {
                    CircularProgressIndicator(
                        color = LauncherRed,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(16.dp)
                    )
                    Text(
                        text = "loading weather…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LauncherMutedGray
                    )
                }
                weather != null -> {
                    Icon(
                        imageVector = Icons.Filled.WbSunny,
                        contentDescription = null,
                        tint = sunTint,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = "${weather.condition}, ${weather.tempF}°",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LauncherMutedGray
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Filled.WbSunny,
                        contentDescription = null,
                        tint = LauncherMutedGray,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = "tap to set your zip code for weather",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LauncherMutedGray
                    )
                }
            }
        }
    }
}
