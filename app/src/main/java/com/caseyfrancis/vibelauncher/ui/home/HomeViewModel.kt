package com.caseyfrancis.vibelauncher.ui.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.caseyfrancis.vibelauncher.data.calendar.CalendarRepository
import com.caseyfrancis.vibelauncher.data.calendar.DayEvents
import com.caseyfrancis.vibelauncher.data.icontheme.IconThemeRepository
import com.caseyfrancis.vibelauncher.data.notifications.NotificationBadgeRepository
import com.caseyfrancis.vibelauncher.data.settings.SettingsRepository
import com.caseyfrancis.vibelauncher.data.tiles.TileRepository
import com.caseyfrancis.vibelauncher.data.weather.WeatherRepository
import com.caseyfrancis.vibelauncher.model.Tile
import com.caseyfrancis.vibelauncher.model.TileTarget
import com.caseyfrancis.vibelauncher.ui.theme.LauncherCard
import com.caseyfrancis.vibelauncher.util.IntentDefaults
import com.caseyfrancis.vibelauncher.util.PermissionUtils
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val MIN_DAY_OFFSET = -12
private const val MAX_DAY_OFFSET = 12

class HomeViewModel(
    private val appContext: Context,
    private val calendarRepository: CalendarRepository,
    private val weatherRepository: WeatherRepository,
    private val tileRepository: TileRepository,
    private val settingsRepository: SettingsRepository,
    private val iconThemeRepository: IconThemeRepository
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
    private val weather = MutableStateFlow<com.caseyfrancis.vibelauncher.data.weather.WeatherInfo?>(null)
    private val weatherLoading = MutableStateFlow(false)
    private val zipCode = MutableStateFlow("")
    private val pickerForSlot = MutableStateFlow<Int?>(null)
    private val hasNotificationAccess = MutableStateFlow(NotificationBadgeRepository.hasNotificationAccess(appContext))
    private val iconThemePackage = MutableStateFlow("")
    private val applyIconThemeToHomeTiles = MutableStateFlow(false)
    private val eventCardColorArgb = MutableStateFlow(LauncherCard.toArgb())

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
            settingsRepository.applyIconThemeToHomeTiles.collectLatest { applyIconThemeToHomeTiles.value = it }
        }
        viewModelScope.launch {
            settingsRepository.eventCardColor.collectLatest { eventCardColorArgb.value = it }
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
        applyIconThemeToHomeTiles,
        eventCardColorArgb
    ) { values ->
        val events = values[1] as DayEvents
        @Suppress("UNCHECKED_CAST")
        val notificationPackages = values[12] as Set<String>
        HomeUiState(
            nowMillis = values[0] as Long,
            timedEvents = events.timedEvents,
            allDayEvents = events.allDayEvents,
            hasCalendarPermission = values[2] as Boolean,
            weather = values[3] as com.caseyfrancis.vibelauncher.data.weather.WeatherInfo?,
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
            applyIconThemeToHomeTiles = values[14] as Boolean,
            eventCardColorArgb = values[15] as Int
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

    /** Null unless the opt-in is on, a theme is selected, and the tile resolves to an
     *  app the pack actually covers - callers fall back to the fixed glyph otherwise. */
    fun themedIconFor(tile: Tile): Drawable? {
        if (!applyIconThemeToHomeTiles.value) return null
        val themePackage = iconThemePackage.value
        if (themePackage.isBlank()) return null
        return IntentDefaults.componentForTile(tile, appContext)
            ?.let { iconThemeRepository.getThemedIcon(it, themePackage) }
    }

    fun dismissPicker() {
        pickerForSlot.value = null
    }

    fun assignTile(slot: Int, label: String, iconKey: String, target: TileTarget) {
        viewModelScope.launch {
            tileRepository.setTile(slot, label, iconKey, target)
            pickerForSlot.value = null
        }
    }

    fun resetTile(slot: Int) {
        viewModelScope.launch {
            tileRepository.resetTile(slot)
            pickerForSlot.value = null
        }
    }

    class Factory(
        private val appContext: Context,
        private val calendarRepository: CalendarRepository,
        private val weatherRepository: WeatherRepository,
        private val tileRepository: TileRepository,
        private val settingsRepository: SettingsRepository,
        private val iconThemeRepository: IconThemeRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(appContext, calendarRepository, weatherRepository, tileRepository, settingsRepository, iconThemeRepository) as T
        }
    }
}
