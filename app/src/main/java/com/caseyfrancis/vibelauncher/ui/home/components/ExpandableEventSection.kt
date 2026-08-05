package com.caseyfrancis.vibelauncher.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.caseyfrancis.vibelauncher.data.calendar.CalendarEvent
import com.caseyfrancis.vibelauncher.ui.theme.LauncherCard

/**
 * Collapsed: just the single most relevant event. Expanded: every event for the day,
 * stacked directly underneath in place - no overlay, no own scroll container. The parent
 * (the middle section of HomeScreen) is what scrolls if this grows past the leftover
 * space, so this is a plain Column, not a LazyColumn.
 */
@Composable
fun ExpandableEventSection(
    events: List<CalendarEvent>,
    collapsedEvent: CalendarEvent?,
    expanded: Boolean,
    nowMillis: Long,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    cardColor: Color = LauncherCard
) {
    if (collapsedEvent == null) return

    if (!expanded) {
        EventCard(
            event = collapsedEvent,
            nowMillis = nowMillis,
            modifier = modifier.fillMaxWidth(),
            backgroundColor = cardColor,
            onClick = onToggle
        )
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            events.forEach { event ->
                EventCard(
                    event = event,
                    nowMillis = nowMillis,
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = cardColor,
                    onClick = onToggle
                )
            }
        }
    }
}
