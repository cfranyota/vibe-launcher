package com.vibelauncher.app

import android.content.Context
import com.vibelauncher.app.data.apps.InstalledAppsRepository
import com.vibelauncher.app.data.calendar.CalendarRepository
import com.vibelauncher.app.data.icontheme.IconThemeRepository
import com.vibelauncher.app.data.settings.SettingsRepository
import com.vibelauncher.app.data.tiles.TileRepository
import com.vibelauncher.app.data.weather.OpenMeteoWeatherRepository
import com.vibelauncher.app.data.weather.WeatherRepository

class AppContainer(context: Context) {
    val calendarRepository = CalendarRepository(context)
    val weatherRepository: WeatherRepository = OpenMeteoWeatherRepository()
    val tileRepository = TileRepository(context)
    val installedAppsRepository = InstalledAppsRepository(context)
    val settingsRepository = SettingsRepository(context)
    val iconThemeRepository = IconThemeRepository(context)
}
