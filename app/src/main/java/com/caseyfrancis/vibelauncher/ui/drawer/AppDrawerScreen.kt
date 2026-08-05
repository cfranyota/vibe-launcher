@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.caseyfrancis.vibelauncher.ui.drawer

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.caseyfrancis.vibelauncher.data.apps.AppInfo
import com.caseyfrancis.vibelauncher.ui.theme.LauncherCard
import com.caseyfrancis.vibelauncher.ui.theme.LauncherMutedGray
import com.caseyfrancis.vibelauncher.ui.theme.LauncherWhite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** Drag past this fraction of the screen height to commit to closing; released short of
 *  it springs back open. */
private const val DISMISS_COMMIT_FRACTION = 0.5f

@Composable
fun AppDrawerScreen(
    viewModel: AppDrawerViewModel,
    onLaunchApp: (AppInfo) -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    var showQuickMenu by remember { mutableStateOf(false) }

    // Shared across *every* way this screen can be dismissed (swipe-down commit, hardware
    // back key) - once true, the swipe-down gesture detector permanently stops reacting to
    // touches. Without this, a stray touch landing on this composable during the ~200ms
    // popBackStack() exit transition (which this same view is still alive and fully
    // touch-active for) could start a brand-new drag and visually reverse the close before
    // the screen was actually gone - reproduced on-device via screen recording, and it
    // happened both after a swipe-close AND after a plain back-key press, so both paths
    // need to feed the same flag rather than just the swipe path guarding itself.
    var dismissed by remember { mutableStateOf(false) }
    val dismissOnce: () -> Unit = {
        if (!dismissed) {
            dismissed = true
            onDismiss()
        }
    }
    BackHandler(enabled = !dismissed, onBack = dismissOnce)

    // Tracks the drawer's live vertical offset while being dragged down - 0 is fully
    // open (resting), drawerHeightPx is fully closed. Driven directly by the gesture
    // below (snapTo per finger movement) so the content tracks the finger in real time,
    // then eased the rest of the way via animateTo on release.
    val dragOffsetY = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    // Known synchronously from the configuration rather than measured via onSizeChanged,
    // which only reports a real value after the first layout pass completes - swiping
    // down right as the drawer opens (its own slide-up animation still in flight) could
    // otherwise read a stale/zero height, wrongly fail the commit-to-close threshold, and
    // spring back open on the first attempt even though the drag went far enough.
    val drawerHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, dragOffsetY.value.roundToInt()) }
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(Unit) {
                    detectSwipeDownToDismiss(
                        isGridAtTop = { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 },
                        isDismissed = { dismissed },
                        dragOffsetY = dragOffsetY,
                        drawerHeightPx = drawerHeightPx,
                        coroutineScope = coroutineScope,
                        onDismiss = dismissOnce
                    )
                }
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                label = { Text("Search apps") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                items(uiState.filteredApps, key = { it.packageName + it.className }) { app ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onLaunchApp(app) },
                                onLongClick = { showQuickMenu = true }
                            )
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Only the drawer's own icons ever go through the icon-theme
                        // lookup - home-screen tiles always keep their fixed glyphs.
                        val painter = remember(app.packageName, uiState.iconThemePackage) {
                            BitmapPainter(viewModel.iconFor(app).toBitmap().asImageBitmap())
                        }
                        Image(painter = painter, contentDescription = app.label, modifier = Modifier.size(48.dp))
                        Text(
                            text = app.label,
                            color = LauncherWhite,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        if (showQuickMenu) {
            DrawerQuickMenu(
                onDismiss = { showQuickMenu = false },
                onOpenSettings = {
                    showQuickMenu = false
                    onOpenSettings()
                },
                onChooseWallpaper = {
                    showQuickMenu = false
                    runCatching { context.startActivity(Intent(Intent.ACTION_SET_WALLPAPER)) }
                }
            )
        }
    }
}

/** A small global quick-menu, not tied to whichever icon was long-pressed - long-pressing
 *  any app in the drawer surfaces the same two shortcuts: Launcher Settings (where the
 *  icon-theme picker lives) and the system wallpaper picker. */
@Composable
private fun DrawerQuickMenu(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onChooseWallpaper: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(LauncherCard)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            QuickMenuAction(icon = Icons.Filled.Settings, label = "Settings", onClick = onOpenSettings)
            QuickMenuAction(icon = Icons.Filled.PhotoLibrary, label = "Wallpaper", onClick = onChooseWallpaper)
        }
    }
}

/**
 * Same [PointerEventPass.Initial] + slop-deferred-consume shape as HomeScreen's gesture
 * detectors, but scoped by scroll position rather than a fixed screen zone: a downward
 * drag is only claimed as a dismiss when [isGridAtTop] is true (checked once at the start
 * of each gesture), which lets it work from anywhere on screen - the search bar, empty
 * space, or directly on top of an app icon - while an upward drag (scrolling further into
 * the list) or any drag while already scrolled down is left completely alone for the grid's
 * own scrolling to handle. A tap never crosses touch slop, so it's never consumed here.
 *
 * `awaitEachGesture`/`awaitPointerEvent` run in a restricted suspend scope that can't call
 * arbitrary suspend functions like `Animatable.snapTo`/`animateTo` directly, so those are
 * dispatched via [coroutineScope] instead - `snapTo` on every frame of movement (so the
 * drawer content, which reads `dragOffsetY.value` in its own `offset` modifier, tracks the
 * finger in real time), then `animateTo` once on release to settle open or closed.
 *
 * Uses a manual `while (!isDismissed())` loop around a single [awaitPointerEventScope]
 * cycle instead of the usual infinite-looping `awaitEachGesture`, so that once a dismiss
 * is committed - via *this* gesture or via [isDismissed] flipping true some other way
 * (the hardware back key, handled by a sibling `BackHandler` in `AppDrawerScreen`) - this
 * stops watching for gestures *entirely* for the rest of this composable's lifetime. A
 * screen recording of the reported "closes then pops back open" bug showed the close
 * animation getting hijacked mid-flight: `navController.popBackStack()` removes this
 * composable via an animated transition that takes ~200ms, and during that window the
 * drawer (with this same gesture detector still attached) was still fully alive - a stray
 * re-touch in that gap (a finger settling unevenly on release, or just brushing the screen
 * while reaching for a hardware key, are both known touchscreen artifacts) started a *new*
 * gesture cycle that dragged `dragOffsetY` back toward 0, visually reversing the close
 * before the screen was actually gone. This reproduced after both swipe-closes and plain
 * back-key presses, which is why [isDismissed] is a shared flag rather than a local one.
 */
private suspend fun PointerInputScope.detectSwipeDownToDismiss(
    isGridAtTop: () -> Boolean,
    isDismissed: () -> Boolean,
    dragOffsetY: Animatable<Float, *>,
    drawerHeightPx: Float,
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit
) {
    while (!isDismissed()) {
        awaitPointerEventScope {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val pointerId = down.id
            if (isDismissed() || !isGridAtTop()) return@awaitPointerEventScope

            // totalY is the single source of truth for how far the finger has moved - purely
            // local, synchronous arithmetic with no dependency on dragOffsetY.value, which
            // would otherwise lag behind due to the async `launch` dispatch below and cause
            // the accumulated drag to be computed from a stale snapshot.
            var totalY = 0f
            var dragging = false

            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                if (!change.pressed) break

                totalY += change.position.y - change.previousPosition.y

                if (!dragging) {
                    if (totalY > viewConfiguration.touchSlop) {
                        dragging = true
                    } else if (totalY < -viewConfiguration.touchSlop) {
                        // Confirmed upward drag while at the top of the list - that's a normal
                        // scroll into the list, not a dismiss. Back off without consuming
                        // anything so the grid's own scroll handling takes over untouched.
                        return@awaitPointerEventScope
                    }
                }
                if (dragging) {
                    change.consume()
                    val clamped = totalY.coerceIn(0f, drawerHeightPx)
                    coroutineScope.launch { dragOffsetY.snapTo(clamped) }
                }
            }

            val finalOffset = totalY.coerceIn(0f, drawerHeightPx)
            if (dragging && finalOffset >= drawerHeightPx * DISMISS_COMMIT_FRACTION) {
                // onDismiss() here is the shared dismissOnce() from AppDrawerScreen - it
                // flips the isDismissed() flag immediately (stopping this loop for good,
                // see the function doc for why that matters) and calls the real navigation
                // callback. Called directly rather than after the settle animation
                // completes: Animatable cancels an in-flight animation if a new mutation
                // preempts it, which was silently dropping the pending dismiss. The
                // animation below is now purely cosmetic and can't block the real dismissal.
                onDismiss()
                coroutineScope.launch { dragOffsetY.animateTo(drawerHeightPx, tween(durationMillis = 200)) }
            } else if (dragging) {
                coroutineScope.launch { dragOffsetY.animateTo(0f, spring()) }
            }
        }
    }
}

@Composable
private fun QuickMenuAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = LauncherWhite, modifier = Modifier.size(32.dp))
        Text(
            text = label,
            color = LauncherMutedGray,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
