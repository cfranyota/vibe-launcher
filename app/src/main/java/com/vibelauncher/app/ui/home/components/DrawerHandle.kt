package com.vibelauncher.app.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.vibelauncher.app.ui.theme.LauncherWhite
import kotlin.math.abs

private const val SWIPE_THRESHOLD_PX = 60f

/**
 * A small, visible drag handle above the bottom edge - tap it, or grab it and swipe up, to
 * open the app drawer. Sits with bottom padding rather than flush against the literal edge,
 * since touches right at the physical bottom of this device landed in an unreachable inset
 * found during earlier testing. Responds to a plain tap as well as a swipe, since a precise
 * swipe on a thin bar proved unreliable - a tap is a much easier target to hit.
 */
@Composable
fun DrawerHandle(onOpenDrawer: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(bottom = 14.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val pointerId = down.id
                    var totalY = 0f
                    var dragging = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed) break

                        totalY += change.position.y - change.previousPosition.y
                        if (!dragging && abs(totalY) > viewConfiguration.touchSlop) {
                            dragging = true
                        }
                        if (dragging) {
                            change.consume()
                        }
                    }

                    // Either a plain tap (never crossed slop) or a confirmed upward swipe
                    // past the threshold opens the drawer.
                    if (!dragging || totalY < -SWIPE_THRESHOLD_PX) {
                        onOpenDrawer()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 100.dp, height = 4.dp)
                .clip(RoundedCornerShape(50))
                .background(LauncherWhite.copy(alpha = 0.6f))
        )
    }
}
