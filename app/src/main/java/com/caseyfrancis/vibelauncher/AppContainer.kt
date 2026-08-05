package com.caseyfrancis.vibelauncher

import android.content.Context
import com.caseyfrancis.vibelauncher.data.apps.InstalledAppsRepository
import com.caseyfrancis.vibelauncher.data.calendar.CalendarRepository
import com.caseyfrancis.vibelauncher.data.icontheme.IconThemeRepository
import com.caseyfrancis.vibelauncher.data.settings.SettingsRepository
import com.caseyfrancis.vibelauncher.data.tiles.TileRepository
import com.caseyfrancis.vibelauncher.data.weather.OpenMeteoWeatherRepository
import com.caseyfrancis.vibelauncher.data.weather.WeatherRepository

class AppContainer(context: Context) {
    val calendarRepository = CalendarRepository(context)
    val weatherRepository: WeatherRepository = OpenMeteoWeatherRepository()
    val tileRepository = TileRepository(context)
    val installedAppsRepository = InstalledAppsRepository(context)
    val settingsRepository = SettingsRepository(context)
    val iconThemeRepository = IconThemeRepository(context)
}
