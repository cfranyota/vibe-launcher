package com.vibelauncher.app.ui.home

import com.vibelauncher.app.data.calendar.CalendarEvent
import com.vibelauncher.app.data.weather.WeatherInfo
import com.vibelauncher.app.model.Tile
import com.vibelauncher.app.ui.theme.LauncherCard
import androidx.compose.ui.graphics.toArgb

data class HomeUiState(
    val nowMillis: Long = System.currentTimeMillis(),
    val weather: WeatherInfo? = null,
    val selectedDayOffset: Int = 0,
    val timedEvents: List<CalendarEvent> = emptyList(),
    val allDayEvents: List<CalendarEvent> = emptyList(),
    val eventsExpanded: Boolean = false,
    val tasksExpanded: Boolean = false,
    val hasCalendarPermission: Boolean = false,
    val tiles: List<Tile> = emptyList(),
    val pickerForSlot: Int? = null,
    val zipCode: String = "",
    val weatherLoading: Boolean = false,
    val hasNotificationAccess: Boolean = false,
    val notificationPackages: Set<String> = emptySet(),
    val iconThemePackage: String = "",
    val applyIconThemeToHomeTiles: Boolean = false,
    val eventCardColorArgb: Int = LauncherCard.toArgb(),
    val eventCardColorEnabled: Boolean = false,
    val tileBorderEnabled: Boolean = false
)
