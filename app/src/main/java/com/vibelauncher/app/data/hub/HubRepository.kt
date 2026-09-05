package com.vibelauncher.app.data.hub

import com.vibelauncher.app.data.calls.CallLogRepository
import com.vibelauncher.app.data.contacts.ContactsRepository
import com.vibelauncher.app.data.sms.SmsRepository
import com.vibelauncher.app.service.NotificationBadgeListenerService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/** Combines messages, calls, captured notifications, and their persisted flag/category
 *  state into one time-sorted feed. Messages only populate once this app holds the
 *  default-SMS-app role (see SmsRepository) - the query itself is always safe to call, it
 *  just returns nothing until then. */
class HubRepository(
    private val callLogRepository: CallLogRepository,
    private val smsRepository: SmsRepository,
    private val contactsRepository: ContactsRepository,
    private val hubStateRepository: HubStateRepository,
    private val hubNotificationHistoryRepository: HubNotificationHistoryRepository,
    private val emailAppsRepository: EmailAppsRepository
) {
    private val callsRefreshToken = MutableStateFlow(0)

    /** Call log has no observable content-provider flow wired up here, so the UI calls
     *  this (e.g. on screen resume) to pick up calls made/received since the feed was
     *  first loaded. */
    fun refreshCalls() {
        callsRefreshToken.value++
    }

    /** Long-lived side effect: persists each newly-captured notification into durable
     *  storage the moment it arrives. Call once from a scope that outlives the Hub screen
     *  (e.g. the ViewModel's own scope is fine, since this only needs to run while some
     *  part of the app is alive to observe it - see NotificationBadgeListenerService's own
     *  doc comment for why this can't be done any more passively). */
    suspend fun observeAndPersistNotifications() {
        NotificationBadgeListenerService.capturedNotifications.collect { list ->
            list.forEach { hubNotificationHistoryRepository.record(it) }
        }
    }

    fun items(): Flow<List<HubItem>> = combine(
        hubNotificationHistoryRepository.history,
        hubStateRepository.states,
        callsRefreshToken,
        emailAppsRepository.selectedPackages
    ) { notifications, states, _, userEmailPackages ->
        val stateMap = states.associateBy { it.itemId }

        val messages = smsRepository.recentMessages().map { sms ->
            val id = "sms:${sms.id}"
            val state = stateMap[id]
            HubItem.Message(
                id = id,
                address = sms.address,
                contactName = contactsRepository.nameForNumber(sms.address),
                body = sms.body,
                isIncoming = sms.isIncoming,
                timestampMillis = sms.timestampMillis,
                flagged = state?.flagged ?: false,
                category = state?.category
            )
        }

        val calls = callLogRepository.recentCalls().map { call ->
            val id = "call:${call.id}"
            val state = stateMap[id]
            HubItem.Call(
                id = id,
                number = call.number,
                contactName = call.cachedName,
                callType = call.type,
                durationSeconds = call.durationSeconds,
                timestampMillis = call.timestampMillis,
                flagged = state?.flagged ?: false,
                category = state?.category
            )
        }

        // notificationItemId() is a deliberately coarse hash (package+title+text+day, see
        // its own doc comment) - by design, two distinct captured-notification history
        // entries can land on the same id. That's fine for flag/category state (a shared
        // id is the point), but LazyColumn's key={it.id} in HubScreen requires strict
        // uniqueness within the list - passing every raw history entry through would crash
        // on the first same-day repeat. Collapse to one row per id here, keeping the most
        // recent instance's text/timestamp.
        val notifItems = notifications
            .sortedByDescending { it.postTimeMillis }
            .distinctBy { n -> notificationItemId(n.packageName, n.title, n.text, n.postTimeMillis) }
            .map { n ->
                val id = notificationItemId(n.packageName, n.title, n.text, n.postTimeMillis)
                val state = stateMap[id]
                HubItem.NotificationItem(
                    id = id,
                    packageName = n.packageName,
                    title = n.title,
                    text = n.text,
                    isEmail = n.packageName in EMAIL_PACKAGES || n.packageName in userEmailPackages,
                    timestampMillis = n.postTimeMillis,
                    flagged = state?.flagged ?: false,
                    category = state?.category
                )
            }

        (messages + calls + notifItems).sortedByDescending { it.timestampMillis }
    }
}
