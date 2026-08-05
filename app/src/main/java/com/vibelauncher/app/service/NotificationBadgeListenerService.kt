package com.vibelauncher.app.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The system controls this service's lifecycle (bound/unbound as notification access is
 * granted/revoked), so the currently-active-notification-packages state lives in a
 * companion object rather than being owned by a ViewModel - there's nothing else to hold
 * it between rebinds.
 */
class NotificationBadgeListenerService : NotificationListenerService() {

    companion object {
        private val _activePackages = MutableStateFlow<Set<String>>(emptySet())
        val activePackages: StateFlow<Set<String>> = _activePackages.asStateFlow()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        publish()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        publish()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        publish()
    }

    private fun publish() {
        _activePackages.value = runCatching { activeNotifications }
            .getOrNull()
            ?.map { it.packageName }
            ?.toSet()
            ?: emptySet()
    }
}
