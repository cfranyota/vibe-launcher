package com.caseyfrancis.vibelauncher.ui.home

import com.caseyfrancis.vibelauncher.data.calendar.CalendarEvent
import com.caseyfrancis.vibelauncher.data.weather.WeatherInfo
import com.caseyfrancis.vibelauncher.model.Tile
import com.caseyfrancis.vibelauncher.ui.theme.LauncherCard
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
    val eventCardColorArgb: Int = LauncherCard.toArgb()
)
