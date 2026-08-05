package com.caseyfrancis.vibelauncher.data.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.caseyfrancis.vibelauncher.service.NotificationBadgeListenerService
import kotlinx.coroutines.flow.StateFlow

object NotificationBadgeRepository {
    val activePackages: StateFlow<Set<String>> = NotificationBadgeListenerService.activePackages

    fun hasNotificationAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}
