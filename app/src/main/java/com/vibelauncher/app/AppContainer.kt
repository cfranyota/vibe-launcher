package com.vibelauncher.app

import android.content.Context
import com.vibelauncher.app.data.apps.InstalledAppsRepository
import com.vibelauncher.app.data.calendar.CalendarRepository
import com.vibelauncher.app.data.calls.CallLogRepository
import com.vibelauncher.app.data.contacts.ContactsRepository
import com.vibelauncher.app.data.hub.EmailAppsRepository
import com.vibelauncher.app.data.hub.HubNotificationHistoryRepository
import com.vibelauncher.app.data.hub.HubRepository
import com.vibelauncher.app.data.hub.HubStateRepository
import com.vibelauncher.app.data.icontheme.IconThemeRepository
import com.vibelauncher.app.data.lettershortcuts.LetterShortcutsRepository
import com.vibelauncher.app.data.monkmode.EssentialsAllowlistRepository
import com.vibelauncher.app.data.notes.NoteRepository
import com.vibelauncher.app.data.settings.SettingsRepository
import com.vibelauncher.app.data.sms.SmsRepository
import com.vibelauncher.app.data.tiles.TileRepository
import com.vibelauncher.app.data.todos.TodoRepository
import com.vibelauncher.app.data.usage.UsageActivityRepository
import com.vibelauncher.app.data.weather.OpenMeteoWeatherRepository
import com.vibelauncher.app.data.weather.WeatherRepository

class AppContainer(context: Context) {
    val calendarRepository = CalendarRepository(context)
    val weatherRepository: WeatherRepository = OpenMeteoWeatherRepository()
    val tileRepository = TileRepository(context)
    val installedAppsRepository = InstalledAppsRepository(context)
    val settingsRepository = SettingsRepository(context)
    val iconThemeRepository = IconThemeRepository(context)
    val todoRepository = TodoRepository(context)
    val letterShortcutsRepository = LetterShortcutsRepository(context)
    val essentialsAllowlistRepository = EssentialsAllowlistRepository(context)
    val callLogRepository = CallLogRepository(context)
    val smsRepository = SmsRepository(context)
    val contactsRepository = ContactsRepository(context)
    val hubStateRepository = HubStateRepository(context)
    val hubNotificationHistoryRepository = HubNotificationHistoryRepository(context)
    val emailAppsRepository = EmailAppsRepository(context)
    val hubRepository = HubRepository(callLogRepository, smsRepository, contactsRepository, hubStateRepository, hubNotificationHistoryRepository, emailAppsRepository)
    val noteRepository = NoteRepository(context)
    val usageActivityRepository = UsageActivityRepository(context, installedAppsRepository)
}
