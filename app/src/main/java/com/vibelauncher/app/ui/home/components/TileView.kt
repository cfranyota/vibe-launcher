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

/** The single source of truth for "how big is a tile right now" - shared by [TileView]
 *  itself and by [TileGrid], so the two can never disagree. The dynamic (runtime-measured,
 *  always safe) cap always wins over the nominal max. The bordered size is otherwise
 *  user-adjustable via a discrete 1-10 step (see the Launcher Settings slider): step 1 maps
 *  to MIN_TILE_SIZE_DP, step 10 to the safe cap, with a straight linear interpolation in
 *  between. With the border off, tiles stay at the safe max regardless of the step. */
fun resolveTileSizeDp(showBorder: Boolean, borderSizeStep: Int, dynamicMaxSizeDp: Dp): Dp {
    val cap = dynamicMaxSizeDp.coerceAtMost(MAX_TILE_SIZE_DP)
    if (!showBorder) return cap
    val fraction = (borderSizeStep.coerceIn(1, 10) - 1) / 9f
    return MIN_TILE_SIZE_DP + (cap - MIN_TILE_SIZE_DP) * fraction
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
    dynamicMaxSizeDp: Dp = MAX_TILE_SIZE_DP,
    modifier: Modifier = Modifier
) {
    val tileSize = resolveTileSizeDp(showBorder, borderSizeStep, dynamicMaxSizeDp)
    Column(
        modifier = modifier
            .size(tileSize)
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
                    modifier = Modifier.size(28.dp)
                )
            } else {
                val kind = (tile.target as TileTarget.BuiltIn).kind
                Icon(
                    imageVector = builtInIcon(kind),
                    contentDescription = tile.label,
                    tint = LauncherWhite,
                    modifier = Modifier.size(28.dp)
                )
            }
            if (hasNotification) {
                // A standalone dot (this asset is now cropped tight to just the badge
                // graphic, no surrounding transparent margin) offset clear of the 28dp
                // icon's corner so it never overlaps the icon itself.
                Image(
                    painter = painterResource(R.drawable.notification_badge),
                    contentDescription = "Notification",
                    modifier = Modifier
                        .offset(x = 22.dp, y = (-4).dp)
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
