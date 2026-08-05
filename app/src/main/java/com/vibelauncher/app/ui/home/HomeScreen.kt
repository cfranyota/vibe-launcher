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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibelauncher.app.data.apps.AppInfo
import com.vibelauncher.app.data.calendar.CalendarEvent
import com.vibelauncher.app.model.TileTarget
import com.vibelauncher.app.ui.home.components.CalendarPermissionCard
import com.vibelauncher.app.ui.home.components.DateWeatherHeader
import com.vibelauncher.app.ui.home.components.DrawerHandle
import com.vibelauncher.app.ui.home.components.ExpandableEventSection
import com.vibelauncher.app.ui.home.components.NotificationAccessCard
import com.vibelauncher.app.ui.home.components.PageIndicator
import com.vibelauncher.app.ui.home.components.TileGrid
import com.vibelauncher.app.ui.home.components.ZipCodeDialog
import com.vibelauncher.app.ui.picker.AppPickerDialog
import com.vibelauncher.app.ui.picker.AppPickerViewModel
import com.vibelauncher.app.util.IntentDefaults
import kotlin.math.abs

/** Down-events starting above this height are treated as a header day-swipe (horizontal).
 *  Covers day/date/weather. Opening the drawer is handled by the visible [DrawerHandle]
 *  at the bottom instead of a gesture zone here. */
private val HEADER_SWIPE_ZONE_DP = 180.dp
private const val SWIPE_THRESHOLD_PX = 120f

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    pickerViewModelFactory: AppPickerViewModel.Factory,
    onOpenDrawer: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showZipDialog by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val context = LocalContext.current

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
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
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
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Fixed - never scrolls, never shifts, regardless of expand state below it.
            DateWeatherHeader(
                selectedDayOffset = uiState.selectedDayOffset,
                weather = uiState.weather,
                weatherLoading = uiState.weatherLoading,
                onWeatherClick = { showZipDialog = true }
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
                PageIndicator(activeIndex = uiState.selectedDayOffset + 12)

                if (!uiState.hasNotificationAccess) {
                    NotificationAccessCard(
                        onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                }

                if (!uiState.hasCalendarPermission) {
                    CalendarPermissionCard(
                        onClick = { permissionLauncher.launch(android.Manifest.permission.READ_CALENDAR) },
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                } else {
                    val currentEvent = collapsedTimedEvent(uiState.timedEvents, uiState.selectedDayOffset, uiState.nowMillis)
                    val expandedTimedEvents = if (currentEvent == null) {
                        uiState.timedEvents
                    } else {
                        uiState.timedEvents.dropWhile { it.id != currentEvent.id }
                    }
                    ExpandableEventSection(
                        events = expandedTimedEvents,
                        collapsedEvent = currentEvent,
                        expanded = uiState.eventsExpanded,
                        nowMillis = uiState.nowMillis,
                        onToggle = viewModel::toggleEventsExpanded,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        cardColor = Color(uiState.eventCardColorArgb)
                    )
                    ExpandableEventSection(
                        events = uiState.allDayEvents,
                        collapsedEvent = uiState.allDayEvents.firstOrNull(),
                        expanded = uiState.tasksExpanded,
                        nowMillis = uiState.nowMillis,
                        onToggle = viewModel::toggleTasksExpanded,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        cardColor = Color(uiState.eventCardColorArgb)
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
            ) {
                TileGrid(
                    tiles = uiState.tiles,
                    onTileClick = viewModel::onTileClick,
                    onTileLongPress = viewModel::onTileLongPress,
                    hasNotification = { tile ->
                        uiState.hasNotificationAccess &&
                            IntentDefaults.packageForTile(tile, context) in uiState.notificationPackages
                    },
                    iconOverride = { tile -> viewModel.themedIconFor(tile) }
                )
                DrawerHandle(onOpenDrawer = guardedOnOpenDrawer)
            }
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

/** On today, the soonest event that hasn't ended yet (falls back to the day's last event
 *  if everything's already passed); on any other day, simply the first event. */
private fun collapsedTimedEvent(events: List<CalendarEvent>, dayOffset: Int, nowMillis: Long): CalendarEvent? {
    if (events.isEmpty()) return null
    if (dayOffset != 0) return events.first()
    return events.firstOrNull { it.endMillis > nowMillis } ?: events.last()
}
