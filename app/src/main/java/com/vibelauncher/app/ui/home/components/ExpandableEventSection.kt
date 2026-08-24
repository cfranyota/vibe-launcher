package com.vibelauncher.app.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.vibelauncher.app.data.calendar.CalendarEvent
import com.vibelauncher.app.ui.theme.LauncherCard
import com.vibelauncher.app.ui.theme.LocalAccentColor

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
    cardColor: Color = LauncherCard,
    icon: ImageVector = Icons.Filled.Event,
    iconTint: Color = LocalAccentColor.current,
    badgeFor: (CalendarEvent) -> String? = { null }
) {
    if (collapsedEvent == null) return

    if (!expanded) {
        EventCard(
            event = collapsedEvent,
            nowMillis = nowMillis,
            modifier = modifier.fillMaxWidth(),
            backgroundColor = cardColor,
            onClick = onToggle,
            badgeOverride = badgeFor(collapsedEvent),
            icon = icon,
            iconTint = iconTint
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
                    onClick = onToggle,
                    badgeOverride = badgeFor(event),
                    icon = icon,
                    iconTint = iconTint
                )
            }
        }
    }
}
