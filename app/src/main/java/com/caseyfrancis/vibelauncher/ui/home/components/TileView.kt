@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.caseyfrancis.vibelauncher.ui.home.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.caseyfrancis.vibelauncher.model.Tile
import com.caseyfrancis.vibelauncher.ui.theme.LauncherRed
import com.caseyfrancis.vibelauncher.ui.theme.LauncherWhite
import com.caseyfrancis.vibelauncher.util.IntentDefaults

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
                Box(
                    modifier = Modifier
                        .offset(x = 22.dp, y = (-4).dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(LauncherRed),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "*",
                        color = LauncherWhite,
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            fontSize = 17.sp,
                            lineHeight = 17.sp,
                            textAlign = TextAlign.Center,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
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
