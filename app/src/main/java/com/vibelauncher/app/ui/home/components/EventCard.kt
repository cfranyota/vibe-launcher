package com.vibelauncher.app.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vibelauncher.app.data.calendar.CalendarEvent
import com.vibelauncher.app.data.calendar.timeUntilLabel
import com.vibelauncher.app.ui.theme.CardCornerShape
import com.vibelauncher.app.ui.theme.LauncherCard
import com.vibelauncher.app.ui.theme.LauncherMutedGray
import com.vibelauncher.app.ui.theme.LauncherRed
import com.vibelauncher.app.ui.theme.LauncherWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EventCard(
    event: CalendarEvent,
    nowMillis: Long,
    modifier: Modifier = Modifier,
    backgroundColor: Color = LauncherCard,
    onClick: (() -> Unit)? = null,
    badgeOverride: String? = null
) {
    val timeText = remember(event) {
        if (event.isAllDay) "all day" else SimpleDateFormat("h:mm a", Locale.getDefault())
            .format(Date(event.startMillis)).lowercase()
    }
    val badge = badgeOverride ?: remember(event, nowMillis) { event.timeUntilLabel(nowMillis) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardCornerShape)
            .background(backgroundColor)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Event,
                contentDescription = null,
                tint = LauncherRed,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column {
                Text(
                    text = "${event.title} · $timeText",
                    color = LauncherWhite,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 28.dp, minHeight = 28.dp)
                .clip(CircleShape)
                .background(LauncherRed)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badge,
                color = LauncherWhite,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun CalendarPermissionCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardCornerShape)
            .background(LauncherCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = LauncherMutedGray,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = "Enable calendar access to see upcoming events",
            color = LauncherMutedGray,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun NotificationAccessCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardCornerShape)
            .background(LauncherCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Notifications,
            contentDescription = null,
            tint = LauncherMutedGray,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = "Enable notification access to see badges on tiles",
            color = LauncherMutedGray,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
