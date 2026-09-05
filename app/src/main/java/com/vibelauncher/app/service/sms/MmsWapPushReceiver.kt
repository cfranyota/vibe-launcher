package com.vibelauncher.app.service.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Deliberately a no-op - declared only because the default-SMS-app role requires a
 *  WAP_PUSH_DELIVER receiver to exist. Full MMS support (WAP PDU parsing, MMSC transaction
 *  manager) is a separate, much larger effort, explicitly out of scope for this pass.
 *  Incoming MMS are silently ignored rather than crashing anything. */
class MmsWapPushReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Intentionally empty.
    }
}
