package com.vibelauncher.app.ui.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vibelauncher.app.data.apps.InstalledAppsRepository
import com.vibelauncher.app.data.calendar.CalendarEvent
import com.vibelauncher.app.data.calendar.CalendarRepository
import com.vibelauncher.app.data.calendar.DayEvents
import com.vibelauncher.app.data.icontheme.IconThemeRepository
import com.vibelauncher.app.data.notifications.NotificationBadgeRepository
import com.vibelauncher.app.data.settings.SettingsRepository
import com.vibelauncher.app.data.tiles.TileRepository
import com.vibelauncher.app.data.todos.TodoRepository
import com.vibelauncher.app.data.usage.HourUsage
import com.vibelauncher.app.data.usage.UsageActivityRepository
import com.vibelauncher.app.data.usage.hourStatesFor
import com.vibelauncher.app.data.weather.WeatherRepository
import com.vibelauncher.app.model.Tile
import com.vibelauncher.app.model.TileTarget
import com.vibelauncher.app.model.TodoItem
import com.vibelauncher.app.ui.theme.LauncherCard
import com.vibelauncher.app.util.IntentDefaults
import com.vibelauncher.app.util.PermissionUtils
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MIN_DAY_OFFSET = -12
private const val MAX_DAY_OFFSET = 12

class HomeViewModel(
    private val appContext: Context,
    private val calendarRepository: CalendarRepository,
    private val weatherRepository: WeatherRepository,
    private val tileRepository: TileRepository,
    private val settingsRepository: SettingsRepository,
    private val iconThemeRepository: IconThemeRepository,
    private val todoRepository: TodoRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val usageActivityRepository: UsageActivityRepository
) : ViewModel() {

    private val clockTicker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000)
        }
    }

    private val dayEvents = MutableStateFlow(DayEvents(emptyList(), emptyList()))
    private val selectedDayOffset = MutableStateFlow(0)
    private val eventsExpanded = MutableStateFlow(false)
    private val tasksExpanded = MutableStateFlow(false)
    private val hasCalendarPermission = MutableStateFlow(PermissionUtils.hasCalendarPermission(appContext))
    private val weather = MutableStateFlow<com.vibelauncher.app.data.weather.WeatherInfo?>(null)
    private val weatherLoading = MutableStateFlow(false)
    private val zipCode = MutableStateFlow("")
    private val pickerForSlot = MutableStateFlow<Int?>(null)
    private val hasNotificationAccess = MutableStateFlow(NotificationBadgeRepository.hasNotificationAccess(appContext))
    private val hasUsageAccess = MutableStateFlow(usageActivityRepository.hasUsageAccess())
    private val hourlyUsage = MutableStateFlow(List(24) { HourUsage(0L, 0L) })
    private val iconThemePackage = MutableStateFlow("")
    private val eventCardColorArgb = MutableStateFlow(LauncherCard.toArgb())
    private val eventCardColorEnabled = MutableStateFlow(false)
    private val iconAccentColorArgb = MutableStateFlow(0xFFF97316.toInt())
    private val iconAccentColorEnabled = MutableStateFlow(false)
    private val tileBorderEnabled = MutableStateFlow(false)
    private val tileBorderSizeStep = MutableStateFlow(5)
    private val vibeBarEnabled = MutableStateFlow(true)
    private val iconSizeStep = MutableStateFlow(5)
    private val homeIconsStayDefault = MutableStateFlow(false)
    private val todos = MutableStateFlow<List<TodoItem>>(emptyList())

    // Owned by the ViewModel (not composable `remember` state) because Navigation Compose
    // disposes and recreates HomeScreen's composition every time the app drawer is opened
    // and dismissed - `remember` state would reset on every trip to the drawer and back,
    // silently reverting to the (larger, less safe) fallback tile size. The ViewModel
    // survives that navigation, so this ratchets up once per session and stays put. See
    // HomeScreen's safeMaxTileSizeDp for how it's used.
    // A Compose-observable state (not a plain var) - HomeScreen reads this while computing
    // safeMaxTileSizeDp, and needs to recompose with the corrected (usually smaller, less
    // conservative) value the moment the real measurement lands, not just on whatever next
    // unrelated recomposition happens to occur.
    var lockedTwoBarContentHeightPx: Int by mutableIntStateOf(0)
        private set

    fun observeTwoBarContentHeightPx(px: Int) {
        if (px > lockedTwoBarContentHeightPx) lockedTwoBarContentHeightPx = px
    }

    init {
        refreshCalendarPermissionAndEvents()
        viewModelScope.launch {
            settingsRepository.zipCode.collectLatest { zip ->
                zipCode.value = zip
                if (zip.isBlank()) {
                    weather.value = null
                } else {
                    weatherLoading.value = true
                    weather.value = runCatching { weatherRepository.current(zip) }.getOrNull()
                    weatherLoading.value = false
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.iconThemePackage.collectLatest { iconThemePackage.value = it }
        }
        viewModelScope.launch {
            settingsRepository.eventCardColor.collectLatest { eventCardColorArgb.value = it }
        }
        viewModelScope.launch {
            settingsRepository.eventCardColorEnabled.collectLatest { eventCardColorEnabled.value = it }
        }
        viewModelScope.launch {
            settingsRepository.iconAccentColor.collectLatest { iconAccentColorArgb.value = it }
        }
        viewModelScope.launch {
            settingsRepository.iconAccentColorEnabled.collectLatest { iconAccentColorEnabled.value = it }
        }
        viewModelScope.launch {
            settingsRepository.tileBorderEnabled.collectLatest { tileBorderEnabled.value = it }
        }
        viewModelScope.launch {
            settingsRepository.tileBorderSizeStep.collectLatest { tileBorderSizeStep.value = it }
        }
        viewModelScope.launch {
            settingsRepository.vibeBarEnabled.collectLatest { vibeBarEnabled.value = it }
        }
        viewModelScope.launch {
            settingsRepository.iconSizeStep.collectLatest { iconSizeStep.value = it }
        }
        viewModelScope.launch {
            settingsRepository.homeIconsStayDefault.collectLatest { homeIconsStayDefault.value = it }
        }
        viewModelScope.launch {
            todoRepository.todos.collectLatest { todos.value = it }
        }
        refreshActivity()
    }

    /** Reloads the activity bar's hour buckets for whichever day the header is showing.
     *  Reading them walks a day of raw usage events, so it stays off the main thread and
     *  only runs on the events that can change the answer - resuming the screen, or moving
     *  to another day. The ahead/past boundary itself needs no reload: it's derived from
     *  the clock tick in [uiState]. */
    private fun refreshActivity() {
        val offset = selectedDayOffset.value
        viewModelScope.launch {
            val access = withContext(Dispatchers.IO) { usageActivityRepository.hasUsageAccess() }
            hasUsageAccess.value = access
            hourlyUsage.value = withContext(Dispatchers.IO) { usageActivityRepository.hourlyUsage(offset) }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        clockTicker,
        dayEvents,
        hasCalendarPermission,
        weather,
        tileRepository.tiles,
        pickerForSlot,
        zipCode,
        weatherLoading,
        selectedDayOffset,
        eventsExpanded,
        tasksExpanded,
        hasNotificationAccess,
        NotificationBadgeRepository.activePackages,
        iconThemePackage,
        eventCardColorArgb,
        eventCardColorEnabled,
        tileBorderEnabled,
        tileBorderSizeStep,
        vibeBarEnabled,
        todos,
        iconAccentColorArgb,
        iconAccentColorEnabled,
        iconSizeStep,
        hourlyUsage,
        hasUsageAccess
    ) { values ->
        val events = values[1] as DayEvents
        @Suppress("UNCHECKED_CAST")
        val notificationPackages = values[12] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val todoItems = values[19] as List<TodoItem>
        // The Tasks bar is local to-dos only now - real calendar all-day events render in
        // the top (calendar) card instead (see HomeScreen). To-dos are a running list, not
        // tied to a calendar day, so they show every day, regardless of selectedDayOffset.
        // Negative ids keep them out of the way of real event ids.
        val tasks = todoItems.map {
            CalendarEvent(id = -it.id, title = it.text, startMillis = it.createdAt, endMillis = it.createdAt, isAllDay = true)
        }
        val nowMillis = values[0] as Long
        @Suppress("UNCHECKED_CAST")
        val usage = values[23] as List<HourUsage>
        val usageAccess = values[24] as Boolean
        HomeUiState(
            nowMillis = nowMillis,
            timedEvents = events.timedEvents,
            allDayEvents = events.allDayEvents,
            tasks = tasks,
            hasCalendarPermission = values[2] as Boolean,
            weather = values[3] as com.vibelauncher.app.data.weather.WeatherInfo?,
            tiles = values[4] as List<Tile>,
            pickerForSlot = values[5] as Int?,
            zipCode = values[6] as String,
            weatherLoading = values[7] as Boolean,
            selectedDayOffset = values[8] as Int,
            eventsExpanded = values[9] as Boolean,
            tasksExpanded = values[10] as Boolean,
            hasNotificationAccess = values[11] as Boolean,
            notificationPackages = notificationPackages,
            iconThemePackage = values[13] as String,
            eventCardColorArgb = values[14] as Int,
            eventCardColorEnabled = values[15] as Boolean,
            tileBorderEnabled = values[16] as Boolean,
            tileBorderSizeStep = values[17] as Int,
            vibeBarEnabled = values[18] as Boolean,
            iconAccentColorArgb = values[20] as Int,
            iconAccentColorEnabled = values[21] as Boolean,
            iconSizeStep = values[22] as Int,
            activityHours = hourStatesFor(usage, values[8] as Int, nowMillis, usageAccess),
            hasUsageAccess = usageAccess
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun refreshCalendarPermissionAndEvents() {
        val granted = PermissionUtils.hasCalendarPermission(appContext)
        hasCalendarPermission.value = granted
        if (granted) {
            viewModelScope.launch {
                dayEvents.value = calendarRepository.getEventsForDay(selectedDayOffset.value)
            }
        }
    }

    /** Notification access is granted via system Settings, not a runtime permission
     *  dialog, so there's no activity-result callback - the screen re-checks this
     *  whenever it resumes instead. */
    fun refreshNotificationAccess() {
        hasNotificationAccess.value = NotificationBadgeRepository.hasNotificationAccess(appContext)
    }

    /** Usage access is granted from system Settings like notification access is, so the
     *  screen re-checks on resume - which is also the moment the activity bar's hours are
     *  worth reloading, since the user has just come back from using something. */
    fun refreshUsageActivity() {
        refreshActivity()
    }

    fun onDayOffsetChange(delta: Int) {
        val newOffset = (selectedDayOffset.value + delta).coerceIn(MIN_DAY_OFFSET, MAX_DAY_OFFSET)
        if (newOffset == selectedDayOffset.value) return
        selectedDayOffset.value = newOffset
        eventsExpanded.value = false
        tasksExpanded.value = false
        if (hasCalendarPermission.value) {
            viewModelScope.launch {
                dayEvents.value = calendarRepository.getEventsForDay(newOffset)
            }
        }
        refreshActivity()
    }

    fun toggleEventsExpanded() {
        val expanding = !eventsExpanded.value
        eventsExpanded.value = expanding
        if (expanding) tasksExpanded.value = false
    }

    fun toggleTasksExpanded() {
        val expanding = !tasksExpanded.value
        tasksExpanded.value = expanding
        if (expanding) eventsExpanded.value = false
    }

    fun setZipCode(value: String) {
        viewModelScope.launch { settingsRepository.setZipCode(value) }
    }

    fun onTileClick(tile: Tile) {
        val intent = when (val target = tile.target) {
            is TileTarget.BuiltIn -> IntentDefaults.intentFor(target.kind, appContext)
            is TileTarget.App -> Intent().setComponent(ComponentName(target.packageName, target.className))
        }
        val resolvable = intent != null && intent.resolveActivity(appContext.packageManager) != null
        // resolveActivity only checks component resolution, not runtime permissions - some
        // OEM components (e.g. this device's deskclock timer handler) resolve fine but then
        // throw SecurityException on startActivity. Since this app is the Home launcher, an
        // uncaught crash here takes down the whole home screen, so never let this throw.
        if (resolvable) {
            intent!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val started = runCatching { appContext.startActivity(intent) }.isSuccess
            if (!started) {
                pickerForSlot.value = tile.id
            }
        } else {
            pickerForSlot.value = tile.id
        }
    }

    fun onTileLongPress(tile: Tile) {
        pickerForSlot.value = tile.id
    }

    /** Resolves the real Drawable a tile should render, or null when TileView should fall
     *  back to the fixed builtInIcon glyph. Theming is always attempted once a pack is
     *  selected, no opt-in toggle - TileTarget.App tiles get the app's own real icon, or a
     *  themed substitute when the pack covers that app; TileTarget.BuiltIn tiles get an
     *  auto-matched pack icon by keyword (see IconThemeRepository.getAutoMatchedIcon), or
     *  null (the fixed glyph) if the pack has nothing matching that action. */
    fun iconFor(tile: Tile): Drawable? {
        val target = tile.target
        if (homeIconsStayDefault.value) {
            return if (target is TileTarget.BuiltIn) {
                null // TileView falls back to the fixed Material glyph, same as "no pack selected"
            } else {
                target as TileTarget.App
                installedAppsRepository.iconFor(target.packageName, target.className)
            }
        }
        val themePackage = iconThemePackage.value
        if (target is TileTarget.BuiltIn) {
            if (themePackage.isBlank()) return null
            return iconThemeRepository.getAutoMatchedIcon(themePackage, target.kind)
        }
        target as TileTarget.App
        if (themePackage.isNotBlank()) {
            iconThemeRepository.getThemedIcon(ComponentName(target.packageName, target.className), themePackage)?.let { return it }
        }
        return installedAppsRepository.iconFor(target.packageName, target.className)
    }

    fun dismissPicker() {
        pickerForSlot.value = null
    }

    fun assignTile(slot: Int, label: String, iconKey: String, target: TileTarget) {
        viewModelScope.launch {
            tileRepository.setTileAt(slot, Tile(slot, label, iconKey, target))
            pickerForSlot.value = null
        }
    }

    fun resetTile(slot: Int) {
        viewModelScope.launch {
            tileRepository.setTileAt(slot, IntentDefaults.defaultTiles()[slot])
            pickerForSlot.value = null
        }
    }

    class Factory(
        private val appContext: Context,
        private val calendarRepository: CalendarRepository,
        private val weatherRepository: WeatherRepository,
        private val tileRepository: TileRepository,
        private val settingsRepository: SettingsRepository,
        private val iconThemeRepository: IconThemeRepository,
        private val todoRepository: TodoRepository,
        private val installedAppsRepository: InstalledAppsRepository,
        private val usageActivityRepository: UsageActivityRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(appContext, calendarRepository, weatherRepository, tileRepository, settingsRepository, iconThemeRepository, todoRepository, installedAppsRepository, usageActivityRepository) as T
        }
    }
}
