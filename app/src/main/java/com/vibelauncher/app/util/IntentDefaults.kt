package com.vibelauncher.app.util

import android.app.SearchManager
import android.content.ComponentName
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

    /** Once Vibe Launcher itself holds the default-SMS-app role (see Hub's SmsRepository/
     *  SmsDeliverReceiver), Telephony.Sms.getDefaultSmsPackage(context) returns THIS app's
     *  own package - so the "Messages" tile must target Google Messages by name instead of
     *  "whatever app currently holds the SMS role," or it becomes circular. */
    const val GOOGLE_MESSAGES_PACKAGE = "com.google.android.apps.messaging"

    /** Requests the default-SMS-app role for a *named* package, not the caller itself -
     *  the counterpart to RoleManager.createRequestRoleIntent() (which can only request
     *  the role for the calling app). This legacy action still works as a compatibility
     *  shim on modern Android and always shows a system-owned confirmation dialog naming
     *  the target app, so it's safe for a third-party app like this one to trigger on
     *  another app's behalf - it can't silently reassign the role. */
    fun requestSmsRoleFor(packageName: String): Intent =
        Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            .putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)

    /** Whether this app currently holds the default-SMS-app role. RoleManager.isRoleHeld()
     *  is the source of truth on API 29+ - Telephony.Sms.getDefaultSmsPackage() is NOT a
     *  reliable read on every device (observed returning null/stale here even right after
     *  RoleManager confirms the role switched), so don't use it for state checks, only for
     *  the legacy API 26-28 fallback where RoleManager doesn't exist yet. */
    fun hasSmsRole(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            context.getSystemService(android.app.role.RoleManager::class.java)?.isRoleHeld(android.app.role.RoleManager.ROLE_SMS) == true
        } else {
            Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
        }
    }

    fun defaultTiles(): List<Tile> = listOf(
        Tile(0, "Note", "builtin:note", TileTarget.BuiltIn(BuiltInAction.NOTE)),
        Tile(1, "Calendar", "builtin:event", TileTarget.BuiltIn(BuiltInAction.EVENT)),
        Tile(2, "Clock", "builtin:timer", TileTarget.BuiltIn(BuiltInAction.TIMER)),
        Tile(3, "To-Do", "builtin:todo", TileTarget.BuiltIn(BuiltInAction.TODO)),
        Tile(4, "Call", "builtin:call", TileTarget.BuiltIn(BuiltInAction.CALL)),
        Tile(5, "Messages", "builtin:message", TileTarget.BuiltIn(BuiltInAction.MESSAGE)),
        Tile(6, "Camera", "builtin:camera", TileTarget.BuiltIn(BuiltInAction.CAMERA)),
        Tile(7, "Memo", "builtin:memo", TileTarget.BuiltIn(BuiltInAction.MEMO))
    )

    /** Null return means "no standard intent - open the app picker instead." */
    fun intentFor(action: BuiltInAction, context: Context): Intent? = when (action) {
        BuiltInAction.NOTE -> null
        BuiltInAction.TODO -> null
        // ACTION_INSERT on the events URI opens the calendar app's add-event form directly,
        // the same intent the Vibe Bar's '+' command already hands off to (see
        // insertCalendarEvent below) - no title/time prefilled here since this is the tile
        // tap path, not a Vibe Bar submission.
        BuiltInAction.EVENT -> Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
        BuiltInAction.TIMER -> {
            // Jump straight to the Clock app's Timer tab where supported (API 30+ AOSP/Google
            // Clock and most derivatives); ACTION_SET_TIMER is the next-best fallback (opens the
            // Timer tab via a "set a timer" flow on older or non-standard Clock apps); if neither
            // resolves, fall back to just opening the Clock app's own main screen, identified via
            // ACTION_SHOW_ALARMS (a system intent every Clock app has supported since API 1) so we
            // land in the same app instead of guessing a package name.
            val showTimers = Intent(AlarmClock.ACTION_SHOW_TIMERS)
            val setTimer = Intent(AlarmClock.ACTION_SET_TIMER)
            when {
                showTimers.resolveActivity(context.packageManager) != null -> showTimers
                setTimer.resolveActivity(context.packageManager) != null -> setTimer
                else -> Intent(AlarmClock.ACTION_SHOW_ALARMS)
                    .resolveActivity(context.packageManager)?.packageName
                    ?.let { context.packageManager.getLaunchIntentForPackage(it) }
            }
        }
        BuiltInAction.CALL -> Intent(Intent.ACTION_DIAL)
        BuiltInAction.MESSAGE -> {
            // Always opens Google Messages specifically, not "whatever holds the default
            // SMS role" - now that this app itself can hold that role (Hub), the old
            // getDefaultSmsPackage lookup would circularly resolve to Vibe Launcher's own
            // package. Falls back to the old role-based lookup (for the rare case this app
            // doesn't hold the role and Messages isn't installed either), then a blank
            // compose intent as the final fallback.
            context.packageManager.getLaunchIntentForPackage(GOOGLE_MESSAGES_PACKAGE)
                ?: Telephony.Sms.getDefaultSmsPackage(context)
                    ?.takeIf { it != context.packageName }
                    ?.let { context.packageManager.getLaunchIntentForPackage(it) }
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
            BuiltInAction.MESSAGE -> context.packageManager.getLaunchIntentForPackage(GOOGLE_MESSAGES_PACKAGE)
                ?.let { GOOGLE_MESSAGES_PACKAGE }
                ?: Telephony.Sms.getDefaultSmsPackage(context)?.takeIf { it != context.packageName }
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
