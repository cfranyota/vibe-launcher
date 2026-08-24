package com.vibelauncher.app.ui.home.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vibelauncher.app.model.Tile

/** Fixed gap between the 2 tile rows - deliberately NOT tied to the horizontal spacing
 *  below, so the total grid height stays a known, safe constant (see HomeScreen.kt's
 *  GRID_CHROME_DP) regardless of how much leftover width there is to fill. */
private val ROW_GAP_DP = 12.dp

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
    showBorder: Boolean = false,
    borderSizeStep: Int = 5,
    iconSizeStep: Int = 5,
    dynamicMaxSizeDp: Dp = MAX_TILE_SIZE_DP,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(ROW_GAP_DP)
    ) {
        tiles.chunked(4).forEach { rowTiles ->
            // SpaceEvenly puts equal space before the first tile, between each pair, and
            // after the last - so leftover row width becomes genuinely even edge-to-edge
            // spacing (matching the fixed ROW_GAP_DP feel) instead of a big dead margin on
            // the outer edges or an ever-growing gap that would eat into the vertical
            // safety margin (that's exactly why this is decoupled from ROW_GAP_DP).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rowTiles.forEach { tile ->
                    TileView(
                        tile = tile,
                        onClick = { onTileClick(tile) },
                        onLongPress = { onTileLongPress(tile) },
                        hasNotification = hasNotification(tile),
                        iconOverride = iconOverride(tile),
                        showBorder = showBorder,
                        borderSizeStep = borderSizeStep,
                        iconSizeStep = iconSizeStep,
                        dynamicMaxSizeDp = dynamicMaxSizeDp
                    )
                }
            }
        }
    }
}
