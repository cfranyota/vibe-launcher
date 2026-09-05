package com.vibelauncher.app.data.hub

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vibelauncher.app.service.CapturedNotification
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.hubNotificationHistoryDataStore by preferencesDataStore(name = "hub_notification_history_prefs")
private val HISTORY_KEY = stringPreferencesKey("captured_notifications")
private const val MAX_RETAINED = 300

/** Durable notification history - [com.vibelauncher.app.service.NotificationBadgeListenerService]'s
 *  own captured-notifications flow is in-memory/process-lifetime only, so Hub needs to
 *  persist each new item locally the moment it arrives to survive process death and to
 *  keep notifications the user has since dismissed from the shade. Can't backfill anything
 *  posted before this repository started observing the service - a hard platform limit of
 *  NotificationListenerService, not a shortcut taken here. */
class HubNotificationHistoryRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(CapturedNotification.serializer())

    val history = context.hubNotificationHistoryDataStore.data.map { prefs ->
        val stored = prefs[HISTORY_KEY]
        stored?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() } ?: emptyList()
    }

    suspend fun record(notification: CapturedNotification) {
        context.hubNotificationHistoryDataStore.edit { prefs ->
            val current = prefs[HISTORY_KEY]
                ?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }
                ?: emptyList()
            if (current.any { it.key == notification.key && it.postTimeMillis == notification.postTimeMillis }) return@edit
            val next = (current + notification).takeLast(MAX_RETAINED)
            prefs[HISTORY_KEY] = json.encodeToString(serializer, next)
        }
    }
}
