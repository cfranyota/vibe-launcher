package com.vibelauncher.app.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.vibelauncher.app.ui.theme.LauncherCard
import com.vibelauncher.app.ui.theme.LocalAccentColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibelauncher.app.data.apps.AppInfo
import com.vibelauncher.app.data.calendar.CalendarEvent
import com.vibelauncher.app.model.BuiltInAction
import com.vibelauncher.app.model.TileTarget
import com.vibelauncher.app.ui.home.components.ActivityBar
import com.vibelauncher.app.ui.home.components.CalendarPermissionCard
import com.vibelauncher.app.ui.home.components.DateWeatherHeader
import com.vibelauncher.app.ui.home.components.DrawerHandle
import com.vibelauncher.app.ui.home.components.ExpandableEventSection
import com.vibelauncher.app.ui.home.components.MAX_TILE_SIZE_DP
import com.vibelauncher.app.ui.home.components.MIN_TILE_SIZE_DP
import com.vibelauncher.app.ui.home.components.NotificationAccessCard
import com.vibelauncher.app.ui.home.components.UsageAccessCard
import com.vibelauncher.app.ui.home.components.TileGrid
import com.vibelauncher.app.ui.home.components.VibeBar
import com.vibelauncher.app.ui.home.components.ZipCodeDialog
import com.vibelauncher.app.ui.picker.AppPickerDialog
import com.vibelauncher.app.ui.picker.AppPickerViewModel
import com.vibelauncher.app.util.IntentDefaults
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

/** Down-events starting above this height are treated as a header day-swipe (horizontal).
 *  Covers day/date/weather. Opening the drawer is handled by the visible [DrawerHandle]
 *  at the bottom instead of a gesture zone here. */
private val HEADER_SWIPE_ZONE_DP = 180.dp
private const val SWIPE_THRESHOLD_PX = 120f

/** Upper-right rectangle (within the header's vertical band) reserved for the
 *  double-tap-to-open-Vibe-Bar gesture, for touchscreen-only phones with no hardware
 *  keyboard. Right-anchored so it never overlaps DateWeatherHeader's left-aligned,
 *  unwidened weather row (its own onWeatherClick clickable). Sits inside
 *  HEADER_SWIPE_ZONE_DP's y-range on purpose - see detectVibeBarDoubleTap's doc comment
 *  for why the two detectors don't conflict. */
private val VIBE_BAR_TAP_ZONE_HEIGHT_DP = 90.dp
private val VIBE_BAR_TAP_ZONE_WIDTH_DP = 120.dp

/** TileGrid's own vertical padding (12dp top + 6dp bottom) + the fixed gap between its two
 *  rows (TileGrid's ROW_GAP_DP = 12dp - deliberately not tied to the horizontal spacing, so
 *  this stays a true constant regardless of tile size or leftover row width) +
 *  DrawerHandle's height (24dp) - the non-tile vertical space the pinned bottom section
 *  always needs, regardless of tile size. */
private val GRID_CHROME_DP = 54.dp

/** Small safety buffer subtracted before sizing tiles, so the bare-minimum-safe-fit math
 *  has a little slack for rounding rather than sizing tiles right up to the exact edge. */
private val EXTRA_MARGIN_DP = 4.dp

/** TileGrid's own horizontal padding (12dp start + 12dp end) - must match TileGrid.kt's
 *  Column padding, or the width-derived tile-size cap below could still overflow/underflow. */
private val GRID_HORIZONTAL_PADDING_DP = 24.dp

/** Tile columns per row - always 4, never adaptive (tiles scale down by width instead). */
private const val COLUMNS_PER_ROW = 4

/** Conservative minimum gap reserved across the 5 SpaceEvenly slots (before/between/after
 *  the 4 tiles in a row), so tiles never render edge-to-edge once width-bound. */
private val MIN_INTER_TILE_GAP_DP = 8.dp

/** Conservative fallback for the collapsed content's height (page indicator + 2 stacked
 *  cards, header NOT included - that's measured separately as headerHeightPx) - used only
 *  until a real 2-bar day has been measured this session (see lockedTwoBarContentHeightPx).
 *  Based on this session's empirical range (page indicator ~14dp + 2 cards with gaps
 *  ~130dp), rounded up for safety. */
private val FALLBACK_TWO_BAR_CONTENT_DP = 160.dp

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    pickerViewModelFactory: AppPickerViewModel.Factory,
    onOpenDrawer: () -> Unit,
    onOpenTodos: () -> Unit,
    onOpenHub: () -> Unit,
    onOpenNotes: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showZipDialog by remember { mutableStateOf(false) }
    var vibeBarOpenRequestToken by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    // Measured at runtime rather than hand-tuned, so the tile grid can never grow tall
    // enough to crowd the Calendar/Task bars above it. Each EventCard renders at a fixed
    // height regardless of its text (single line, ellipsized - see EventCard.kt), so "the
    // height of 2 stacked cards" is a constant for this device, not something that should
    // vary by day. lockedTwoBarContentHeightPx ratchets up to the tallest real 2-bar
    // measurement seen this session and never decreases - every day (0, 1, or 2 bars) then
    // sizes tiles against that same worst-case number, so tiles never resize as you swipe
    // between days and are never bigger than what a 2-bar day can safely fit.
    // Keyed on the window's own configured size so the grows-only measurement below can't
    // stay latched to a stale screen: a real size change (rotation, fold, split-screen)
    // changes the configuration and starts the measurement over, while the IME - which
    // resizes the window without any configuration change - cannot.
    val windowSizeKey = configuration.screenHeightDp to configuration.screenWidthDp
    var totalHeightPx by remember(windowSizeKey) { mutableIntStateOf(0) }
    var totalWidthPx by remember(windowSizeKey) { mutableIntStateOf(0) }
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val safeMaxTileSizeDp = with(density) {
        val totalDp = totalHeightPx.toDp()
        val twoBarContentDp = if (viewModel.lockedTwoBarContentHeightPx > 0) {
            viewModel.lockedTwoBarContentHeightPx.toDp()
        } else {
            FALLBACK_TWO_BAR_CONTENT_DP
        }
        val topContentDp = headerHeightPx.toDp() + twoBarContentDp
        val heightCapDp = (totalDp - topContentDp - GRID_CHROME_DP - EXTRA_MARGIN_DP) / 2

        // Screen width never entered this formula before - on tall/narrow screens the
        // height cap alone could exceed what 4 columns actually fit, clipping tiles off
        // the edge (TileGrid's Row has no wrap/scroll). Cap by width too so 4 tiles +
        // gaps always fit, by construction.
        val widthCapDp = (totalWidthPx.toDp() - GRID_HORIZONTAL_PADDING_DP -
            MIN_INTER_TILE_GAP_DP * (COLUMNS_PER_ROW + 1)) / COLUMNS_PER_ROW

        minOf(heightCapDp, widthCapDp).coerceIn(MIN_TILE_SIZE_DP, MAX_TILE_SIZE_DP)
    }

    // This composable is freshly (re)created every time Home becomes the visible screen
    // again (Navigation Compose disposes it while the drawer is on top), so this captures
    // the exact moment Home reappeared. Guards against the drawer immediately reopening
    // right after being dismissed - seen intermittently on-device (both via swipe and the
    // hardware back key) where something spuriously re-triggers onOpenDrawer a moment
    // after a successful close, making it look like the drawer "pops back up."
    val homeShownAtMillis = remember { System.currentTimeMillis() }
    val guardedOnOpenDrawer: () -> Unit = {
        if (System.currentTimeMillis() - homeShownAtMillis > 500) onOpenDrawer()
    }
    val guardedOnOpenHub: () -> Unit = {
        if (System.currentTimeMillis() - homeShownAtMillis > 500) onOpenHub()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshCalendarPermissionAndEvents() }

    BackHandler(enabled = true) { /* no-op: launcher home screen ignores back */ }

    // Notification access is granted via Settings, not a permission dialog, so there's
    // no activity-result callback to hook - re-check whenever the screen resumes instead.
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentViewModel = rememberUpdatedState(viewModel)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentViewModel.value.refreshNotificationAccess()
                currentViewModel.value.refreshUsageActivity()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Only ever grows (within one window configuration - see the remember keys on
            // totalHeightPx/totalWidthPx). The window is declared adjustResize, so opening
            // Vibe Bar's keyboard shrinks this Box by the height of the IME; feeding that
            // back into safeMaxTileSizeDp re-ran the tile-size formula against a screen
            // temporarily ~40% shorter and visibly shrank all 8 tiles (measured: 257px ->
            // 175px) for as long as the bar was open. Tile size is a property of the
            // screen, not of whether a keyboard happens to be up. Taking the tallest
            // measurement is what rejects the IME-shrunken ones without having to detect
            // the IME itself - WindowInsets.ime reads as zero here, since adjustResize has
            // already consumed the inset by resizing the window.
            .onGloballyPositioned {
                if (it.size.height > totalHeightPx) {
                    totalHeightPx = it.size.height
                    totalWidthPx = it.size.width
                }
            }
            // No opaque background here - the activity window itself renders the system
            // wallpaper behind this content (see Theme.VibeLauncher's windowShowWallpaper
            // + MainActivity's FLAG_SHOW_WALLPAPER); painting a solid color would hide it.
            .pointerInput(Unit) {
                val headerZonePx = with(density) { HEADER_SWIPE_ZONE_DP.toPx() }
                detectHeaderDaySwipe(
                    headerZonePx = headerZonePx,
                    onDaySwipe = { delta -> viewModel.onDayOffsetChange(delta) }
                )
            }
            // Independent pointerInput - Compose gives each its own copy of the event
            // stream, so this doesn't compete with detectHeaderDaySwipe above for the
            // same gesture (see detectVibeBarDoubleTap's doc comment).
            .pointerInput(Unit) {
                val zoneHeightPx = with(density) { VIBE_BAR_TAP_ZONE_HEIGHT_DP.toPx() }
                val zoneWidthPx = with(density) { VIBE_BAR_TAP_ZONE_WIDTH_DP.toPx() }
                detectVibeBarDoubleTap(
                    zoneHeightPx = zoneHeightPx,
                    zoneWidthPx = zoneWidthPx,
                    totalWidthPx = { totalWidthPx },
                    onDoubleTap = { vibeBarOpenRequestToken++ }
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Fixed - never scrolls, never shifts, regardless of expand state below it.
            DateWeatherHeader(
                selectedDayOffset = uiState.selectedDayOffset,
                weather = uiState.weather,
                weatherLoading = uiState.weatherLoading,
                onWeatherClick = { showZipDialog = true },
                sunTint = if (uiState.iconAccentColorEnabled) Color(uiState.iconAccentColorArgb) else LocalAccentColor.current,
                modifier = Modifier.onGloballyPositioned { headerHeightPx = it.size.height }
            )

            // Flexible middle: gets whatever space is left after the header and the
            // pinned tile row/handle below. An expanded event/task section grows in
            // place here; if it's taller than the leftover space, this scrolls to
            // reveal the rest rather than covering the tiles or pushing them off-screen.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                val currentTimedEvent = collapsedTimedEvent(uiState.timedEvents, uiState.selectedDayOffset, uiState.nowMillis)
                val collapsedEvent = currentTimedEvent ?: uiState.allDayEvents.firstOrNull()
                val bothBarsShowing = uiState.hasCalendarPermission &&
                    collapsedEvent != null &&
                    uiState.tasks.firstOrNull() != null

                // A plain (non-weighted, non-scrolling) wrapper around the same content, so
                // its measured height reflects the content's own natural size rather than
                // whatever the scrollable parent above was allotted. Ratchets
                // lockedTwoBarContentHeightPx (see safeMaxTileSizeDp above) up to the
                // tallest 2-bar measurement seen so far - never down - so a transient/
                // incomplete measurement (e.g. caught mid-swipe, before an async calendar
                // load settles) can never poison the session; it just gets superseded by
                // the next, fully-settled 2-bar render.
                Column(
                    modifier = Modifier.onGloballyPositioned {
                        if (bothBarsShowing && !uiState.eventsExpanded && !uiState.tasksExpanded) {
                            viewModel.observeTwoBarContentHeightPx(it.size.height)
                        }
                    }
                ) {
                    ActivityBar(
                        hours = uiState.activityHours,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )

                    if (!uiState.hasUsageAccess) {
                        UsageAccessCard(
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                }
                            },
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                        )
                    }

                    if (!uiState.hasNotificationAccess) {
                        NotificationAccessCard(
                            onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                        )
                    }

                    val cardColor = if (uiState.eventCardColorEnabled) Color(uiState.eventCardColorArgb) else LauncherCard
                    val iconTint = if (uiState.iconAccentColorEnabled) Color(uiState.iconAccentColorArgb) else LocalAccentColor.current

                    // Top card is 100% real calendar data (all-day events first, then timed
                    // events), so it stays fully gated behind calendar permission. Bottom
                    // card is 100% local to-dos, which never need calendar permission, so it
                    // renders independently of the top card's permission state.
                    if (!uiState.hasCalendarPermission) {
                        CalendarPermissionCard(
                            onClick = { permissionLauncher.launch(android.Manifest.permission.READ_CALENDAR) },
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    } else {
                        ExpandableEventSection(
                            events = uiState.allDayEvents + uiState.timedEvents,
                            collapsedEvent = collapsedEvent,
                            expanded = uiState.eventsExpanded,
                            nowMillis = uiState.nowMillis,
                            onToggle = viewModel::toggleEventsExpanded,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                            cardColor = cardColor,
                            iconTint = iconTint
                        )
                    }
                    ExpandableEventSection(
                        events = uiState.tasks,
                        collapsedEvent = uiState.tasks.firstOrNull(),
                        expanded = uiState.tasksExpanded,
                        nowMillis = uiState.nowMillis,
                        onToggle = viewModel::toggleTasksExpanded,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        cardColor = cardColor,
                        icon = Icons.Filled.Checklist,
                        iconTint = iconTint,
                        badgeFor = { "•" }
                    )
                }
            }

            // Pinned - always given its full needed size before the weighted middle
            // Column above gets whatever's left, so it can never be squeezed off-screen.
            // Scoped separately from the scrollable middle section above (rather than a
            // whole-root swipe zone), so a swipe up anywhere over the icons/handle opens
            // the drawer without ever stealing a scroll gesture meant for an expanded
            // event list higher up.
            Column(
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectSwipeUpToOpenDrawer(onOpenDrawer = guardedOnOpenDrawer)
                    }
                    // Swipe-left-to-open-Hub gesture disabled while Hub is still being
                    // finished - detectSwipeLeftToOpenHub/guardedOnOpenHub/onOpenHub stay
                    // wired below so this is a one-line revert once it's ready.
            ) {
                TileGrid(
                    tiles = uiState.tiles,
                    onTileClick = { tile ->
                        // The default Note/To-Do tiles have no external app to hand off
                        // to (see IntentDefaults.intentFor's NOTE/TODO -> null branches) -
                        // both open their own local list.
                        when ((tile.target as? TileTarget.BuiltIn)?.kind) {
                            BuiltInAction.NOTE -> onOpenNotes()
                            BuiltInAction.TODO -> onOpenTodos()
                            else -> viewModel.onTileClick(tile)
                        }
                    },
                    onTileLongPress = viewModel::onTileLongPress,
                    hasNotification = { tile ->
                        uiState.hasNotificationAccess &&
                            IntentDefaults.packageForTile(tile, context) in uiState.notificationPackages
                    },
                    iconOverride = { tile -> viewModel.iconFor(tile) },
                    showBorder = uiState.tileBorderEnabled,
                    borderSizeStep = uiState.tileBorderSizeStep,
                    iconSizeStep = uiState.iconSizeStep,
                    dynamicMaxSizeDp = safeMaxTileSizeDp
                )
                DrawerHandle(onOpenDrawer = guardedOnOpenDrawer)
            }
        }

        // Declared last (on top) so its scrim/expanded content draws over everything else
        // above. Invisible and inert when collapsed - see VibeBar's own doc comment.
        if (uiState.vibeBarEnabled) {
            VibeBar(
                keyboardInputEnabled = uiState.pickerForSlot == null && !showZipDialog,
                openRequestToken = vibeBarOpenRequestToken
            )
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

    if (showZipDialog) {
        ZipCodeDialog(
            currentZipCode = uiState.zipCode,
            onSave = { zip ->
                viewModel.setZipCode(zip)
                showZipDialog = false
            },
            onDismiss = { showZipDialog = false }
        )
    }
}

/**
 * Intercepts touches at [PointerEventPass.Initial] (root-to-leaf, parent-first) rather
 * than the default Main pass (leaf-to-root), so this sees every touch before any card/tile's
 * own `clickable`/`combinedClickable` can claim it - those only run at the Main pass. A
 * plain tap or long-press never crosses touch slop, so it's never consumed here and reaches
 * children completely normally; only once real dragging is confirmed do we start consuming.
 * Only handles the header's horizontal day-swipe - opening the drawer is handled by the
 * dedicated [DrawerHandle] instead.
 */
private suspend fun PointerInputScope.detectHeaderDaySwipe(
    headerZonePx: Float,
    onDaySwipe: (Int) -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val pointerId = down.id
        val startedInHeader = down.position.y < headerZonePx
        if (!startedInHeader) return@awaitEachGesture

        var totalX = 0f
        var totalY = 0f
        var dragging = false

        while (true) {
            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
            if (!change.pressed) break

            totalX += change.position.x - change.previousPosition.x
            totalY += change.position.y - change.previousPosition.y

            if (!dragging && (abs(totalX) > viewConfiguration.touchSlop || abs(totalY) > viewConfiguration.touchSlop)) {
                dragging = true
            }
            if (dragging) {
                change.consume()
            }
        }

        if (dragging && abs(totalX) > abs(totalY)) {
            when {
                totalX <= -SWIPE_THRESHOLD_PX -> onDaySwipe(1)
                totalX >= SWIPE_THRESHOLD_PX -> onDaySwipe(-1)
            }
        }
    }
}

/**
 * Touch-only trigger for Vibe Bar, for phones with no hardware keyboard (Vibe Bar otherwise
 * only opens via a physical keystroke - see VibeBar.kt's onPreviewKeyEvent). Watches for two
 * taps, both landing in the upper-right [VIBE_BAR_TAP_ZONE_WIDTH_DP] x
 * [VIBE_BAR_TAP_ZONE_HEIGHT_DP] rectangle, within the platform's double-tap timing window.
 *
 * Runs as an independent `pointerInput` from [detectHeaderDaySwipe] (Compose gives each
 * `pointerInput` modifier its own copy of the event stream), and never consumes a single tap,
 * a drag, or a second tap outside the zone - only the second down of a *confirmed* double-tap
 * is consumed. That's what lets this coexist with detectHeaderDaySwipe (which only consumes
 * once real dragging is confirmed, so taps never reach it anyway) and with
 * DateWeatherHeader's own onWeatherClick further down the tree (whose left-aligned, unwidened
 * weather row never falls inside this right-anchored zone).
 */
private suspend fun PointerInputScope.detectVibeBarDoubleTap(
    zoneHeightPx: Float,
    zoneWidthPx: Float,
    totalWidthPx: () -> Int,
    onDoubleTap: () -> Unit
) {
    fun inZone(x: Float, y: Float): Boolean {
        val rightEdgePx = totalWidthPx().toFloat()
        return y < zoneHeightPx && x > (rightEdgePx - zoneWidthPx)
    }

    awaitEachGesture {
        val firstDown = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (!inZone(firstDown.position.x, firstDown.position.y)) return@awaitEachGesture

        // Let the first tap release normally (not a drag) without consuming it.
        val firstPointerId = firstDown.id
        while (true) {
            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == firstPointerId } ?: return@awaitEachGesture
            if (!change.pressed) break
        }

        val secondDown = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        } ?: return@awaitEachGesture

        if (inZone(secondDown.position.x, secondDown.position.y)) {
            secondDown.consume()
            onDoubleTap()
        }
    }
}

/**
 * Same [PointerEventPass.Initial] + slop-deferred-consume approach as [detectHeaderDaySwipe],
 * scoped to the tile grid + drag handle area instead of the header. A swipe up starting
 * anywhere in that area - not just precisely on the handle bar - opens the drawer; a plain
 * tap or long-press never crosses slop, so it's never consumed here and still reaches
 * each tile's own `combinedClickable` normally.
 */
private suspend fun PointerInputScope.detectSwipeUpToOpenDrawer(onOpenDrawer: () -> Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val pointerId = down.id

        var totalX = 0f
        var totalY = 0f
        var dragging = false

        while (true) {
            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
            if (!change.pressed) break

            totalX += change.position.x - change.previousPosition.x
            totalY += change.position.y - change.previousPosition.y

            if (!dragging && (abs(totalX) > viewConfiguration.touchSlop || abs(totalY) > viewConfiguration.touchSlop)) {
                dragging = true
            }
            if (dragging) {
                change.consume()
            }
        }

        if (dragging && abs(totalY) > abs(totalX) && totalY <= -SWIPE_THRESHOLD_PX) {
            onOpenDrawer()
        }
    }
}

/** Sibling to [detectSwipeUpToOpenDrawer] - same skeleton/threshold, horizontal-left
 *  instead of vertical-up. Scoped to the tile-grid/DrawerHandle Column (not the header
 *  day-swipe band above it), as its own independent pointerInput, so it can't collide with
 *  detectHeaderDaySwipe's left-swipe-changes-day gesture or steal the drawer's up-swipe. */
private suspend fun PointerInputScope.detectSwipeLeftToOpenHub(onOpenHub: () -> Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val pointerId = down.id

        var totalX = 0f
        var totalY = 0f
        var dragging = false

        while (true) {
            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
            if (!change.pressed) break

            totalX += change.position.x - change.previousPosition.x
            totalY += change.position.y - change.previousPosition.y

            if (!dragging && (abs(totalX) > viewConfiguration.touchSlop || abs(totalY) > viewConfiguration.touchSlop)) {
                dragging = true
            }
            if (dragging) {
                change.consume()
            }
        }

        if (dragging && abs(totalX) > abs(totalY) && totalX <= -SWIPE_THRESHOLD_PX) {
            onOpenHub()
        }
    }
}

/** On today, the soonest event that hasn't ended yet (falls back to the day's last event
 *  if everything's already passed); on any other day, simply the first event. */
private fun collapsedTimedEvent(events: List<CalendarEvent>, dayOffset: Int, nowMillis: Long): CalendarEvent? {
    if (events.isEmpty()) return null
    if (dayOffset != 0) return events.first()
    return events.firstOrNull { it.endMillis > nowMillis } ?: events.last()
}
