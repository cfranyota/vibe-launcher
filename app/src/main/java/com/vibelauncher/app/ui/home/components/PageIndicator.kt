package com.vibelauncher.app.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.vibelauncher.app.ui.theme.LocalAccentColor

/** Tracks the header's day-browsing position: [activeIndex] = selectedDayOffset + 12. */
@Composable
fun PageIndicator(dotCount: Int = 25, activeIndex: Int = 12, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(dotCount) { index ->
            val color = if (index == activeIndex) LocalAccentColor.current else MaterialTheme.colorScheme.secondary
            Row(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            ) {}
        }
    }
}
