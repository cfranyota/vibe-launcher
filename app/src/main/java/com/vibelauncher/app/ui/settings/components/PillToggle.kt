package com.vibelauncher.app.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.vibelauncher.app.ui.theme.BadgeCornerShape
import com.vibelauncher.app.ui.theme.LauncherCard
import com.vibelauncher.app.ui.theme.LauncherMutedGray
import com.vibelauncher.app.ui.theme.LauncherWhite
import com.vibelauncher.app.ui.theme.LocalAccentColor

@Composable
fun PillToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(BadgeCornerShape)
            .background(if (checked) LocalAccentColor.current else LauncherCard)
            .clickable { onCheckedChange(!checked) }
            .widthIn(min = 56.dp)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = if (checked) "on" else "off",
            color = if (checked) LauncherWhite else LauncherMutedGray,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
