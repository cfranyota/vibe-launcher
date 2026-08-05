package com.vibelauncher.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibelauncher.app.data.apps.AppInfo
import com.vibelauncher.app.model.Tile
import com.vibelauncher.app.model.TileTarget
import com.vibelauncher.app.ui.home.components.builtInIcon
import com.vibelauncher.app.ui.picker.AppPickerDialog
import com.vibelauncher.app.ui.picker.AppPickerViewModel
import com.vibelauncher.app.ui.theme.LauncherMutedGray
import com.vibelauncher.app.ui.theme.LauncherWhite
import com.vibelauncher.app.util.IntentDefaults

/**
 * Manages all 8 home-screen slots from one place - reuses the exact same [AppPickerDialog]
 * and [TileRepository][com.vibelauncher.app.data.tiles.TileRepository] flow that
 * long-pressing a tile on the home screen already uses, just reachable from Settings too.
 */
@Composable
fun HomeAppsScreen(
    viewModel: HomeAppsViewModel,
    pickerViewModelFactory: AppPickerViewModel.Factory,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

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
                text = "Home Screen Apps",
                color = LauncherWhite,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Text(
            text = "Tap a slot to change which app it opens.",
            color = LauncherMutedGray,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
        )

        LazyColumn {
            items(uiState.tiles, key = { it.id }) { tile ->
                HomeAppRow(tile = tile, onClick = { viewModel.onTileTapped(tile.id) })
            }
        }
    }

    val pickerSlot = uiState.pickerForSlot
    if (pickerSlot != null) {
        val pickerViewModel: AppPickerViewModel = viewModel(factory = pickerViewModelFactory)
        AppPickerDialog(
            viewModel = pickerViewModel,
            onAppSelected = { app: AppInfo ->
                viewModel.assignTile(
                    slot = pickerSlot,
                    label = app.label,
                    iconKey = "app:${app.packageName}",
                    target = TileTarget.App(app.packageName, app.className)
                )
            },
            onResetToDefault = { viewModel.resetTile(pickerSlot) },
            onDismiss = { viewModel.dismissPicker() }
        )
    }
}

@Composable
private fun HomeAppRow(tile: Tile, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = builtInIcon(IntentDefaults.actionForSlot(tile.id)),
                contentDescription = tile.label,
                tint = LauncherWhite,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = tile.label,
                color = LauncherWhite,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
