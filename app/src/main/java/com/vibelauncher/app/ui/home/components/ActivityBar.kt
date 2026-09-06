package com.vibelauncher.app.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.vibelauncher.app.data.usage.HourState
import com.vibelauncher.app.ui.theme.CardCornerShape
import com.vibelauncher.app.ui.theme.LauncherMutedGray
import com.vibelauncher.app.ui.theme.LauncherWhite
import com.vibelauncher.app.ui.theme.LocalAccentColor

private val HOUR_LABELS = listOf("12a", "6a", "noon", "6p", "12a")

/**
 * The day at a glance: one dot per hour, white when the hour stayed intentional, accent
 * when it slipped into feeds/social/media, dim when it hasn't happened yet (see
 * [HourState]). Replaces the old day-position dots - the date in the header above already
 * says which day is being browsed, and swiping still moves between days, so these dots
 * describe whichever day is on screen rather than where you are in the week.
 */
@Composable
fun ActivityBar(hours: List<HourState>, modifier: Modifier = Modifier) {
    val accent = LocalAccentColor.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, LauncherMutedGray.copy(alpha = 0.25f), CardCornerShape)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            hours.forEach { state ->
                val color = when (state) {
                    HourState.INTENTIONAL -> LauncherWhite
                    HourState.DISTRACTED -> accent
                    HourState.AHEAD -> LauncherMutedGray.copy(alpha = 0.45f)
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HOUR_LABELS.forEach { label ->
                Text(
                    text = label,
                    color = LauncherMutedGray,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
