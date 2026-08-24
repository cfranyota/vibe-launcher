package com.vibelauncher.app.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.vibelauncher.app.ui.settings.components.SectionHeader
import com.vibelauncher.app.ui.settings.components.ToggleRow
import com.vibelauncher.app.ui.theme.LauncherCard
import com.vibelauncher.app.ui.theme.LauncherMutedGray
import com.vibelauncher.app.ui.theme.LauncherWhite
import com.vibelauncher.app.ui.theme.LocalAccentColor
import com.vibelauncher.app.ui.theme.settingsTypography
import kotlin.math.roundToInt

@Composable
fun IconThemeScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    MaterialTheme(typography = settingsTypography()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
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
                    text = "icon theme",
                    color = LauncherWhite,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text(
                text = "Applies to the app drawer and the home screen, including Note/To-Do/etc.",
                color = LauncherMutedGray,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
            )

            SectionHeader("ICON SIZE")
            Text(
                text = "icon size: ${uiState.iconSizeStep}/10",
                color = LauncherMutedGray,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp)
            )
            Slider(
                value = uiState.iconSizeStep.toFloat(),
                onValueChange = { viewModel.setIconSizeStep(it.roundToInt()) },
                valueRange = 1f..10f,
                steps = 8,
                colors = SliderDefaults.colors(
                    thumbColor = LocalAccentColor.current,
                    activeTrackColor = LocalAccentColor.current
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            SectionHeader("HOME SCREEN")
            ToggleRow(
                title = "Don't change homescreen apps",
                subtitle = "keep home tiles on default icons; app drawer still themed",
                checked = uiState.homeIconsStayDefault,
                onCheckedChange = viewModel::setHomeIconsStayDefault
            )

            LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
                item {
                    IconThemeRow(
                        label = "Default",
                        icon = null,
                        selected = uiState.selectedIconThemePackage.isBlank(),
                        onClick = { viewModel.selectIconTheme("") }
                    )
                }
                items(uiState.iconPacks, key = { it.packageName }) { pack ->
                    IconThemeRow(
                        label = pack.label,
                        icon = pack.icon,
                        selected = uiState.selectedIconThemePackage == pack.packageName,
                        onClick = { viewModel.selectIconTheme(pack.packageName) }
                    )
                }
                if (uiState.iconPacks.isEmpty()) {
                    item {
                        Text(
                            text = "No icon pack apps found. Install one from the Play Store to theme the app drawer.",
                            color = LauncherMutedGray,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IconThemeRow(
    label: String,
    icon: android.graphics.drawable.Drawable?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                val painter = remember(label) { BitmapPainter(icon.toBitmap().asImageBitmap()) }
                Image(
                    painter = painter,
                    contentDescription = label,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(LauncherCard)
                )
            }
            Text(
                text = label,
                color = LauncherWhite,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        if (selected) {
            Icon(imageVector = Icons.Filled.Check, contentDescription = "Selected", tint = LocalAccentColor.current)
        }
    }
}
