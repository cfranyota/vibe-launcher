@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.vibelauncher.app.ui.home.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.vibelauncher.app.R
import com.vibelauncher.app.model.Tile
import com.vibelauncher.app.model.TileTarget
import com.vibelauncher.app.ui.theme.LauncherWhite
import com.vibelauncher.app.ui.theme.TileCornerShape

/** Nominal bounds for a tile's size. The real ceiling on any given screen is enforced at
 *  render time by HomeScreen's runtime-measured `dynamicMaxSizeDp` (see TileGrid/HomeScreen),
 *  which can only ever be <= MAX_TILE_SIZE_DP - so it's safe for this nominal max to be
 *  generous; the dynamic clamp is what actually guarantees the Calendar/Task bars are never
 *  crowded, on any day or content combination. */
val MIN_TILE_SIZE_DP = 64.dp
val MAX_TILE_SIZE_DP = 128.dp

data class TileSize(val width: Dp, val height: Dp)

/**
 * How big a tile is right now. The dynamic (runtime-measured, always safe) cap always wins
 * over the nominal max, and with the border off tiles simply sit at that cap.
 *
 * With the border on, the 1-10 step (see the Settings slider) works in two halves:
 * - 1-5 grow a square from MIN_TILE_SIZE_DP up to the cap - the biggest tile that fits
 *   without crowding the event and to-do cards. 5 is the default.
 * - 6-10 keep that height and only widen the tile, until at 10 it's a quarter of the row
 *   ([fullRowTileWidthDp]) and neighbouring outlines touch. Height is what the cap protects,
 *   so this is the only direction there's room to keep growing.
 */
fun resolveTileSize(showBorder: Boolean, borderSizeStep: Int, dynamicMaxSizeDp: Dp, fullRowTileWidthDp: Dp): TileSize {
    val cap = dynamicMaxSizeDp.coerceAtMost(MAX_TILE_SIZE_DP)
    if (!showBorder) return TileSize(cap, cap)
    val step = borderSizeStep.coerceIn(1, 10)
    if (step <= 5) {
        val side = MIN_TILE_SIZE_DP + (cap - MIN_TILE_SIZE_DP) * ((step - 1) / 4f)
        return TileSize(side, side)
    }
    val widest = fullRowTileWidthDp.coerceAtLeast(cap)
    return TileSize(width = cap + (widest - cap) * ((step - 5) / 5f), height = cap)
}

/** Bounds for the icon glyph itself (independent of [resolveTileSizeDp], which sizes the
 *  whole tile box/border). Step 5 is the default users land on until they touch the
 *  slider - deliberately bigger than the platform-fixed 28dp icons used to be. */
val ICON_SIZE_MIN_DP = 20.dp
val ICON_SIZE_DEFAULT_DP = 52.dp
val ICON_SIZE_MAX_DP = 84.dp

/** Two-segment linear interpolation around step 5 (see bounds above), clamped against the
 *  tile's height - its smaller side, since the border slider's top steps only widen it - so
 *  a maxed-out icon can never overflow a tile that's been shrunk down by that slider. */
fun resolveIconSizeDp(iconSizeStep: Int, tileSizeDp: Dp): Dp {
    val step = iconSizeStep.coerceIn(1, 10)
    val raw = if (step <= 5) {
        ICON_SIZE_MIN_DP + (ICON_SIZE_DEFAULT_DP - ICON_SIZE_MIN_DP) * ((step - 1) / 4f)
    } else {
        ICON_SIZE_DEFAULT_DP + (ICON_SIZE_MAX_DP - ICON_SIZE_DEFAULT_DP) * ((step - 5) / 5f)
    }
    return raw.coerceAtMost(tileSizeDp - 24.dp)
}

@Composable
fun TileView(
    tile: Tile,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    hasNotification: Boolean = false,
    iconOverride: Drawable? = null,
    showBorder: Boolean = false,
    borderSizeStep: Int = 5,
    iconSizeStep: Int = 5,
    dynamicMaxSizeDp: Dp = MAX_TILE_SIZE_DP,
    fullRowTileWidthDp: Dp = MAX_TILE_SIZE_DP,
    modifier: Modifier = Modifier
) {
    val tileSize = resolveTileSize(showBorder, borderSizeStep, dynamicMaxSizeDp, fullRowTileWidthDp)
    val iconSize = resolveIconSizeDp(iconSizeStep, tileSize.height)
    Column(
        modifier = modifier
            .size(width = tileSize.width, height = tileSize.height)
            .then(
                if (showBorder) Modifier.border(1.dp, LauncherWhite, TileCornerShape) else Modifier
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // The notification badge is anchored to this box (sized to the icon itself), not
        // the whole tile, so it sits right at the icon's corner instead of drifting off
        // to the tile edge.
        Box(contentAlignment = Alignment.TopEnd) {
            if (iconOverride != null) {
                // App tiles always arrive here with a real Drawable from
                // HomeViewModel.iconFor; BuiltIn Note/To-Do only arrive here when a
                // manual pack icon has been assigned to them.
                val painter = remember(iconOverride) { BitmapPainter(iconOverride.toBitmap().asImageBitmap()) }
                Image(
                    painter = painter,
                    contentDescription = tile.label,
                    modifier = Modifier.size(iconSize)
                )
            } else {
                val kind = (tile.target as TileTarget.BuiltIn).kind
                Icon(
                    imageVector = builtInIcon(kind),
                    contentDescription = tile.label,
                    tint = LauncherWhite,
                    modifier = Modifier.size(iconSize)
                )
            }
            if (hasNotification) {
                // A standalone dot (this asset is now cropped tight to just the badge
                // graphic, no surrounding transparent margin), offset proportionally to
                // the icon's own size so it stays tucked at the icon's corner at any
                // iconSizeStep - reduces to the original fixed 22dp when iconSize == 28dp.
                Image(
                    painter = painterResource(R.drawable.notification_badge),
                    contentDescription = "Notification",
                    modifier = Modifier
                        .offset(x = iconSize - 6.dp, y = (-4).dp)
                        .size(20.dp)
                )
            }
        }
        Text(
            text = tile.label,
            color = LauncherWhite,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
