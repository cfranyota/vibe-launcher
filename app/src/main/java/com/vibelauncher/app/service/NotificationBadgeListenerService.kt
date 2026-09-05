package com.vibelauncher.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

private const val MAX_RETAINED_NOTIFICATIONS = 200

/** A notification's content, captured the moment it posts - used by Hub. Not the same as
 *  [StatusBarNotification] itself, which becomes unusable once the notification is
 *  dismissed/removed from the system. */
@Serializable
data class CapturedNotification(
    val key: String,
    val packageName: String,
    val title: String?,
    val text: String?,
    val postTimeMillis: Long
)

/**
 * The system controls this service's lifecycle (bound/unbound as notification access is
 * granted/revoked), so state lives in a companion object rather than being owned by a
 * ViewModel - there's nothing else to hold it between rebinds.
 *
 * [capturedNotifications] is in-memory/process-lifetime only, and `onListenerConnected`
 * only replays currently-active notifications, never historical/dismissed ones - a
 * NotificationListenerService platform limitation, not a shortcut taken here. Hub's own
 * repository is responsible for persisting each new item locally the moment it arrives if
 * it wants a durable history beyond this service's own lifetime.
 */
class NotificationBadgeListenerService : NotificationListenerService() {

    companion object {
        private val _activePackages = MutableStateFlow<Set<String>>(emptySet())
        val activePackages: StateFlow<Set<String>> = _activePackages.asStateFlow()

        private val _capturedNotifications = MutableStateFlow<List<CapturedNotification>>(emptyList())
        val capturedNotifications: StateFlow<List<CapturedNotification>> = _capturedNotifications.asStateFlow()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        publishBadges()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        publishBadges()
        if (!sbn.isOngoing) publishContent(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        publishBadges()
    }

    private fun publishBadges() {
        _activePackages.value = runCatching { activeNotifications }
            .getOrNull()
            ?.map { it.packageName }
            ?.toSet()
            ?: emptySet()
    }

    private fun publishContent(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val captured = CapturedNotification(
            key = sbn.key,
            packageName = sbn.packageName,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            postTimeMillis = sbn.postTime
        )
        _capturedNotifications.value = (_capturedNotifications.value.filterNot { it.key == sbn.key } + captured)
            .takeLast(MAX_RETAINED_NOTIFICATIONS)
    }
}
