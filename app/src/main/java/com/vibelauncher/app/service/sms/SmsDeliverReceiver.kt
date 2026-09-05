package com.vibelauncher.app.service.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.vibelauncher.app.data.sms.SmsRepository

/** Fires only while this app holds the default-SMS-app role - the platform delivers
 *  incoming messages here instead of writing them to the provider itself, so this app is
 *  responsible for persisting them (see SmsRepository.recordIncoming). No user-visible
 *  notification is posted here yet - a known gap for this pass, tracked as a follow-up. */
class SmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val address = messages.first().originatingAddress ?: return
        val body = messages.joinToString("") { it.messageBody ?: "" }
        val timestamp = messages.first().timestampMillis

        SmsRepository(context.applicationContext).recordIncoming(address, body, timestamp)
    }
}
