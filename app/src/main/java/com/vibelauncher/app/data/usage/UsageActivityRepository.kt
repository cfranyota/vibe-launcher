package com.vibelauncher.app.data.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import com.vibelauncher.app.data.apps.InstalledAppsRepository
import java.time.LocalDate
import java.time.ZoneId

/** One hour of the day's screen time, split into "was this time spent in feeds, social
 *  apps or media" and everything else. Both zero means the phone wasn't used that hour. */
data class HourUsage(val totalMs: Long, val distractingMs: Long)

/** What a single dot in the home screen's activity bar is saying. */
enum class HourState {
    /** White - the hour was either unused or spent outside the distracting set. */
    INTENTIONAL,

    /** Accent - more than half of that hour's screen time went to feeds/social/media. */
    DISTRACTED,

    /** Dim - the hour hasn't happened yet, or nothing is known about it (usage access
     *  off, or a day older than Android's own event retention). */
    AHEAD
}

private const val HOURS_PER_DAY = 24
private val HOUR_MS = 60 * 60 * 1000L

/**
 * Hour-by-hour screen time for the home screen's activity bar, read from
 * [UsageStatsManager]. Needs the Usage Access special permission, which is granted from
 * system Settings rather than a runtime dialog (see [hasUsageAccess]).
 *
 * Only `queryEvents` can answer "which hour was this app in front" - `queryUsageStats`
 * reports daily totals with no timing inside the day - so foreground spans are rebuilt
 * here from the raw resumed/paused event stream and split across hour boundaries.
 */
class UsageActivityRepository(
    private val context: Context,
    private val installedAppsRepository: InstalledAppsRepository
) {

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = runCatching {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }.getOrElse { return false }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** 24 entries, midnight-first, for the day [dayOffset] days from today. Hours that
     *  haven't happened yet come back empty, as does everything if usage access is off. */
    fun hourlyUsage(dayOffset: Int): List<HourUsage> {
        val empty = List(HOURS_PER_DAY) { HourUsage(0L, 0L) }
        if (!hasUsageAccess()) return empty

        val zone = ZoneId.systemDefault()
        val day = LocalDate.now(zone).plusDays(dayOffset.toLong())
        val dayStart = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        val dayEnd = minOf(day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(), now)
        if (dayEnd <= dayStart) return empty

        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return empty
        val events = runCatching { manager.queryEvents(dayStart, dayEnd) }.getOrNull() ?: return empty

        val totals = LongArray(HOURS_PER_DAY)
        val distracting = LongArray(HOURS_PER_DAY)
        // Classification is the expensive part (PackageManager lookups, and a browser probe
        // per package), so each package is judged once per query no matter how many times
        // it comes and goes.
        val distractingByPackage = mutableMapOf<String, Boolean>()

        var foregroundPackage: String? = null
        var foregroundSince = 0L
        val event = UsageEvents.Event()

        fun close(endMillis: Long) {
            val pkg = foregroundPackage ?: return
            val start = foregroundSince.coerceAtLeast(dayStart)
            val end = endMillis.coerceAtMost(dayEnd)
            foregroundPackage = null
            if (end <= start) return
            // The launcher itself is foreground the whole time the home screen is showing.
            // Counting that as screen time would swamp the "more than half the hour" test
            // with time spent looking at this very bar.
            if (pkg == context.packageName) return
            val isDistracting = distractingByPackage.getOrPut(pkg) {
                installedAppsRepository.isDistracting(pkg)
            }
            addSpan(start, end, dayStart, totals, if (isDistracting) distracting else null)
        }

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    // Some devices drop the matching PAUSED, so an unclosed span is closed
                    // here rather than being allowed to run to the end of the day.
                    if (foregroundPackage != null && foregroundPackage != event.packageName) {
                        close(event.timeStamp)
                    }
                    if (foregroundPackage == null) {
                        foregroundPackage = event.packageName
                        foregroundSince = event.timeStamp
                    }
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    if (foregroundPackage == event.packageName) close(event.timeStamp)
                }
            }
        }
        close(dayEnd)

        return List(HOURS_PER_DAY) { hour -> HourUsage(totals[hour], distracting[hour]) }
    }

    /** Adds a foreground span to its hour buckets, splitting it wherever it crosses an
     *  hour boundary (a 20-minute span from 8:50 gives 10 minutes to 8am and 10 to 9am). */
    private fun addSpan(
        startMillis: Long,
        endMillis: Long,
        dayStart: Long,
        totals: LongArray,
        distracting: LongArray?
    ) {
        var cursor = startMillis
        while (cursor < endMillis) {
            val hour = ((cursor - dayStart) / HOUR_MS).toInt()
            if (hour !in 0 until HOURS_PER_DAY) return
            val hourEnd = dayStart + (hour + 1) * HOUR_MS
            val sliceEnd = minOf(endMillis, hourEnd)
            val slice = sliceEnd - cursor
            totals[hour] += slice
            distracting?.let { it[hour] += slice }
            cursor = sliceEnd
        }
    }
}

/**
 * Turns a day's raw usage into the 24 dots the activity bar draws.
 *
 * An hour with no screen time reads as intentional - being off the phone is the point -
 * but that would also paint a day we simply have no records for (usage access off, or
 * older than Android keeps events for) as a perfect all-white day. So a *past* day with
 * no recorded usage at all is reported as unknown instead.
 */
fun hourStatesFor(
    usage: List<HourUsage>,
    dayOffset: Int,
    nowMillis: Long,
    hasUsageAccess: Boolean
): List<HourState> {
    val zone = ZoneId.systemDefault()
    val day = LocalDate.now(zone).plusDays(dayOffset.toLong())
    val dayStart = day.atStartOfDay(zone).toInstant().toEpochMilli()
    val dayHasRecords = usage.any { it.totalMs > 0L }

    return List(HOURS_PER_DAY) { hour ->
        val hourStart = dayStart + hour * HOUR_MS
        when {
            hourStart > nowMillis -> HourState.AHEAD
            !hasUsageAccess || !dayHasRecords -> HourState.AHEAD
            usage[hour].distractingMs * 2 > usage[hour].totalMs -> HourState.DISTRACTED
            else -> HourState.INTENTIONAL
        }
    }
}
