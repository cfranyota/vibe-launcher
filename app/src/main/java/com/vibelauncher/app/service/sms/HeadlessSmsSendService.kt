package com.vibelauncher.app.service.sms

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.telephony.SmsManager
import com.vibelauncher.app.data.sms.SmsRepository

/** Handles RESPOND_VIA_MESSAGE - other apps' "reply via message" quick-action (e.g. from a
 *  missed-call notification) routes here once this app holds the default-SMS-app role.
 *  Required for role eligibility even if never actually invoked. */
class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val recipient = intent?.data?.schemeSpecificPart
        val body = intent?.getStringExtra(Intent.EXTRA_TEXT)
        if (recipient != null && !body.isNullOrBlank()) {
            val sent = runCatching { SmsManager.getDefault().sendTextMessage(recipient, null, body, null, null) }.isSuccess
            if (sent) SmsRepository(applicationContext).recordSent(recipient, body)
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
