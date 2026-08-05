package com.vibelauncher.app.ui.home.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vibelauncher.app.model.Tile

/**
 * A plain, non-lazy 2-row grid (not LazyVerticalGrid) - the tile set is always exactly
 * 8 fixed slots, so there's no reason for a scrollable container here. That was making
 * the row scroll/shift under a drag even though all 8 tiles always fit on screen.
 */
@Composable
fun TileGrid(
    tiles: List<Tile>,
    onTileClick: (Tile) -> Unit,
    onTileLongPress: (Tile) -> Unit,
    hasNotification: (Tile) -> Boolean = { false },
    iconOverride: (Tile) -> Drawable? = { null },
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        tiles.chunked(4).forEach { rowTiles ->
            Row(
                modifier = Modifier,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowTiles.forEach { tile ->
                    TileView(
                        tile = tile,
                        onClick = { onTileClick(tile) },
                        onLongPress = { onTileLongPress(tile) },
                        hasNotification = hasNotification(tile),
                        iconOverride = iconOverride(tile),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
