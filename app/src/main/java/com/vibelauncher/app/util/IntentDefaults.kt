package com.vibelauncher.app.util

import android.app.SearchManager
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Telephony
import android.telecom.TelecomManager
import android.telephony.SmsManager
import com.vibelauncher.app.model.BuiltInAction
import com.vibelauncher.app.model.Tile
import com.vibelauncher.app.model.TileTarget

/**
 * Note/To-Do have no standard system-wide intent to launch a "notes" or "tasks" app -
 * there's no equivalent to ACTION_DIAL for those categories. Rather than guess a
 * package, tapping those default tiles opens the app picker directly so the user
 * chooses their own app.
 */
object IntentDefaults {
    const val SLOT_COUNT = 8

    fun defaultTiles(): List<Tile> = listOf(
        Tile(0, "Note", "builtin:note", TileTarget.BuiltIn(BuiltInAction.NOTE)),
        Tile(1, "Event", "builtin:event", TileTarget.BuiltIn(BuiltInAction.EVENT)),
        Tile(2, "Timer", "builtin:timer", TileTarget.BuiltIn(BuiltInAction.TIMER)),
        Tile(3, "To-Do", "builtin:todo", TileTarget.BuiltIn(BuiltInAction.TODO)),
        Tile(4, "Call", "builtin:call", TileTarget.BuiltIn(BuiltInAction.CALL)),
        Tile(5, "Message", "builtin:message", TileTarget.BuiltIn(BuiltInAction.MESSAGE)),
        Tile(6, "Camera", "builtin:camera", TileTarget.BuiltIn(BuiltInAction.CAMERA)),
        Tile(7, "Memo", "builtin:memo", TileTarget.BuiltIn(BuiltInAction.MEMO))
    )

    /** Null return means "no standard intent - open the app picker instead." */
    fun intentFor(action: BuiltInAction, context: Context): Intent? = when (action) {
        BuiltInAction.NOTE -> null
        BuiltInAction.TODO -> null
        BuiltInAction.EVENT -> {
            // ACTION_VIEW on the "time" URI opens the calendar app's normal agenda/day
            // view (today), as opposed to ACTION_INSERT which opens the add-event form.
            val timeUri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()
            Intent(Intent.ACTION_VIEW).setData(ContentUris.withAppendedId(timeUri, System.currentTimeMillis()))
        }
        BuiltInAction.TIMER -> Intent(AlarmClock.ACTION_SET_TIMER)
        BuiltInAction.CALL -> Intent(Intent.ACTION_DIAL)
        BuiltInAction.MESSAGE -> {
            // Launch the default SMS app's own main screen (conversation list) rather
            // than ACTION_SENDTO, which always opens a blank compose screen.
            val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(context)
            defaultSmsPackage?.let { context.packageManager.getLaunchIntentForPackage(it) }
                ?: Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))
        }
        BuiltInAction.CAMERA -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        BuiltInAction.MEMO -> Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)
    }

    /** Best-effort package name for notification-badge lookups. Reassigned tiles resolve
     *  trivially; built-in actions with a fixed default handler (Message, Call) resolve
     *  via the platform API for that role; the rest fall back to whatever currently
     *  resolves the tile's own launch intent - the same app the tile actually opens. */
    fun packageForTile(tile: Tile, context: Context): String? = when (val target = tile.target) {
        is TileTarget.App -> target.packageName
        is TileTarget.BuiltIn -> when (target.kind) {
            BuiltInAction.MESSAGE -> Telephony.Sms.getDefaultSmsPackage(context)
            BuiltInAction.CALL -> {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                telecomManager?.defaultDialerPackage
            }
            BuiltInAction.NOTE, BuiltInAction.TODO -> null
            else -> intentFor(target.kind, context)
                ?.resolveActivity(context.packageManager)
                ?.packageName
        }
    }

    /** Full ComponentName per tile, needed for icon-pack appfilter.xml lookups (which map
     *  against an app's main launcher activity, not just its package). Sibling to
     *  [packageForTile], reusing the same per-action resolution. */
    fun componentForTile(tile: Tile, context: Context): ComponentName? = when (val target = tile.target) {
        is TileTarget.App -> ComponentName(target.packageName, target.className)
        is TileTarget.BuiltIn -> when (target.kind) {
            BuiltInAction.MESSAGE, BuiltInAction.CALL -> {
                packageForTile(tile, context)
                    ?.let { context.packageManager.getLaunchIntentForPackage(it) }
                    ?.component
            }
            BuiltInAction.NOTE, BuiltInAction.TODO -> null
            else -> intentFor(target.kind, context)?.resolveActivity(context.packageManager)
        }
    }

    // ─── Vibe Bar actions ───────────────────────────────────────────────────────
    // '-' (to-do) saves locally (see TodoRepository) and never reaches here. '/' (note)
    // is never saved anywhere - it's an ephemeral draft the user shares or copies
    // straight out of the NoteBubble, hence shareText() below. '@' and '#' execute
    // directly (SmsManager/TelecomManager) rather than handing off to another app. '+'
    // still hands off to the Calendar app - there's a real system calendar for events to
    // live in, unlike a quick to-do. Every one of these is guarded - this app is the Home
    // launcher, so an uncaught exception from any of these would crash the whole home screen.

    private fun start(context: Context, intent: Intent): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    /** '+' - opens the calendar app's own add-event form pre-filled with the title. */
    fun insertCalendarEvent(context: Context, title: String, allDay: Boolean): Boolean {
        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, title)
            .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, allDay)
        return start(context, intent)
    }

    /** '#' - places the call directly via TelecomManager (requires CALL_PHONE, checked/
     *  requested by the caller before this is invoked). */
    fun placeCallDirect(context: Context, phone: String): Boolean {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager ?: return false
        return runCatching { telecomManager.placeCall(Uri.parse("tel:" + Uri.encode(phone)), null) }.isSuccess
    }

    /** '@' - sends the SMS directly via SmsManager (requires SEND_SMS, checked/requested
     *  by the caller before this is invoked). getDefault(), not the API 31+
     *  getSystemService(SmsManager::class.java) overload, since minSdk is 26. */
    fun sendSmsDirect(context: Context, phone: String, body: String): Boolean =
        runCatching { SmsManager.getDefault().sendTextMessage(phone, null, body, null, null) }.isSuccess

    /** '/' - opens the system share sheet with the note's text (recent direct-share
     *  contacts row plus the full app list - Messages, Gmail, Quick Share, etc. - all
     *  handled by the OS chooser, nothing custom needed here). */
    fun shareText(context: Context, body: String): Boolean {
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, body)
        return start(context, Intent.createChooser(intent, null))
    }

    /** No prefix - runs a plain web search. Most devices resolve ACTION_WEB_SEARCH via
     *  their default search app; where nothing does, fall back to a browser search URL. */
    fun webSearch(context: Context, query: String): Boolean {
        val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)
        if (searchIntent.resolveActivity(context.packageManager) != null) {
            return start(context, searchIntent)
        }
        val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query)))
        return start(context, fallback)
    }
}
