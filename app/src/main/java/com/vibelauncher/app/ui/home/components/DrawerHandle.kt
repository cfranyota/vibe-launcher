package com.vibelauncher.app.ui.home.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private const val SWIPE_THRESHOLD_PX = 60f

/**
 * An invisible drag handle/hot zone above the bottom edge - tap it, or swipe up anywhere in
 * it, to open the app drawer. No visible affordance is drawn - swiping up to reveal all apps
 * is expected to read as an obvious gesture on its own. Sits with bottom padding rather than
 * flush against the literal edge, since touches right at the physical bottom of this device
 * landed in an unreachable inset found during earlier testing. Responds to a plain tap as
 * well as a swipe, since a precise swipe on a thin zone proved unreliable - a tap is a much
 * easier target to hit.
 */
@Composable
fun DrawerHandle(onOpenDrawer: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .padding(bottom = 7.dp)
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
            }
    )
}
