package com.vibelauncher.app.ui.settings.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vibelauncher.app.ui.theme.LauncherMutedGray

@Composable
fun SectionHeader(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(),
            color = LauncherMutedGray,
            style = MaterialTheme.typography.labelSmall
        )
        HorizontalDivider(
            color = LauncherMutedGray.copy(alpha = 0.3f),
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        )
    }
}
