@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.vibelauncher.app.ui.home.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.vibelauncher.app.R
import com.vibelauncher.app.model.Tile
import com.vibelauncher.app.ui.theme.LauncherWhite
import com.vibelauncher.app.util.IntentDefaults

@Composable
fun TileView(
    tile: Tile,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    hasNotification: Boolean = false,
    iconOverride: Drawable? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // The icon is fixed to the slot's category and never changes when the slot is
        // reassigned to a different app - only the label below updates. The notification
        // badge is anchored to this box (sized to the icon itself), not the whole tile,
        // so it sits right at the icon's corner instead of drifting off to the tile edge.
        Box(contentAlignment = Alignment.TopEnd) {
            if (iconOverride != null) {
                val painter = remember(iconOverride) { BitmapPainter(iconOverride.toBitmap().asImageBitmap()) }
                Image(
                    painter = painter,
                    contentDescription = tile.label,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Icon(
                    imageVector = builtInIcon(IntentDefaults.actionForSlot(tile.id)),
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
