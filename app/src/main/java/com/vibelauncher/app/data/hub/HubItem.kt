package com.vibelauncher.app.data.hub

import java.security.MessageDigest

/** Notification-listener items have no stable long-term ID once dismissed, so their itemId
 *  is a best-effort content hash instead of a real provider ID (unlike SMS/call items,
 *  which use their real, permanent content-provider _ID). Collisions between legitimately
 *  different notifications with identical package/title/text on the same day are an
 *  accepted tradeoff - flagging one flags all visually-identical instances that day. */
fun notificationItemId(packageName: String, title: String?, text: String?, postTimeMillis: Long): String {
    val dayBucket = postTimeMillis / (24 * 60 * 60 * 1000)
    val normalized = "$packageName|${title.orEmpty().trim().lowercase()}|${text.orEmpty().trim().lowercase()}|$dayBucket"
    val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray())
    return "notif:" + digest.joinToString("") { "%02x".format(it) }
}

/** Package names whose notifications count as "email" for Hub's EMAIL filter tab -
 *  small and hardcoded since there's no reliable way to detect "this is an email app"
 *  from PackageManager alone. */
val EMAIL_PACKAGES = setOf(
    "com.google.android.gm",
    "com.microsoft.office.outlook",
    "com.samsung.android.email.provider",
    "com.yahoo.mobile.client.android.mail",
    "ru.yandex.mail"
)

enum class HubFilter { ALL, MESSAGES, CALLS, EMAIL, APPS, FLAGGED }

sealed interface HubItem {
    val id: String
    val timestampMillis: Long
    val flagged: Boolean
    val category: HubCategory?

    data class Message(
        override val id: String,
        val address: String,
        val contactName: String?,
        val body: String,
        val isIncoming: Boolean,
        override val timestampMillis: Long,
        override val flagged: Boolean,
        override val category: HubCategory?
    ) : HubItem

    data class Call(
        override val id: String,
        val number: String,
        val contactName: String?,
        val callType: com.vibelauncher.app.data.calls.CallLogType,
        val durationSeconds: Int,
        override val timestampMillis: Long,
        override val flagged: Boolean,
        override val category: HubCategory?
    ) : HubItem

    data class NotificationItem(
        override val id: String,
        val packageName: String,
        val title: String?,
        val text: String?,
        val isEmail: Boolean,
        override val timestampMillis: Long,
        override val flagged: Boolean,
        override val category: HubCategory?
    ) : HubItem
}

fun HubItem.matchesFilter(filter: HubFilter): Boolean = when (filter) {
    HubFilter.ALL -> true
    HubFilter.FLAGGED -> flagged
    HubFilter.MESSAGES -> this is HubItem.Message
    HubFilter.CALLS -> this is HubItem.Call
    HubFilter.EMAIL -> this is HubItem.NotificationItem && isEmail
    HubFilter.APPS -> this is HubItem.NotificationItem && !isEmail
}
