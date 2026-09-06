package com.vibelauncher.app.data.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri

/** Package names Vibe Mode's "hide social/browser" tier removes even when
 *  [ApplicationInfo.category] isn't declared - many apps, especially social ones, never set
 *  a Play Store category, so [InstalledAppsRepository.isSocialOrBrowser]'s category check
 *  alone under-matches in practice. Best-effort, not exhaustive. */
private val KNOWN_SOCIAL_PACKAGES = setOf(
    "com.instagram.android",
    "com.zhiliaoapp.musically", // TikTok
    "com.ss.android.ugc.trill", // TikTok (alt package seen in some regions)
    "com.twitter.android",
    "com.facebook.katana",
    "com.snapchat.android",
    "com.reddit.frontpage",
    "com.pinterest",
    "com.linkedin.android",
    "com.whatsapp"
)

/** Video/streaming apps the activity bar counts as distracting on top of the social set -
 *  same best-effort reasoning as [KNOWN_SOCIAL_PACKAGES], since plenty of them never
 *  declare [ApplicationInfo.CATEGORY_VIDEO] either. */
private val KNOWN_MEDIA_PACKAGES = setOf(
    "com.google.android.youtube",
    "com.google.android.apps.youtube.music",
    "com.netflix.mediaclient",
    "com.hulu.plus",
    "com.disney.disneyplus",
    "com.hbo.hbonow", // Max
    "com.wbd.stream", // Max (current package)
    "com.amazon.avod.thirdpartyclient", // Prime Video
    "tv.twitch.android.app",
    "com.google.android.videos" // Google TV / Play Movies
)

class InstalledAppsRepository(private val context: Context) {

    fun getLaunchableApps(): List<AppInfo> {
        return runCatching { getLaunchableAppsViaLauncherApps() }
            .getOrElse { getLaunchableAppsViaPackageManager() }
    }

    /** Used by Vibe Mode's "hide social/browser" tier. Category detection is best-effort
     *  (see [KNOWN_SOCIAL_PACKAGES]); browser detection is exact - a browser is, by
     *  definition, an app that resolves a plain https:// view intent. */
    fun isSocialOrBrowser(packageName: String): Boolean {
        if (packageName in KNOWN_SOCIAL_PACKAGES) return true
        val appInfo = runCatching { context.packageManager.getApplicationInfo(packageName, 0) }.getOrNull()
        if (appInfo?.category == ApplicationInfo.CATEGORY_SOCIAL) return true
        val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).setPackage(packageName)
        return viewIntent.resolveActivity(context.packageManager) != null
    }

    /** What the home screen's activity bar counts as an hour "slipping into feeds, social
     *  apps or media" - Vibe Mode's social/browser set plus video/streaming, which is its
     *  own kind of drift and isn't covered by either of those. Music isn't included: it
     *  usually plays alongside real work rather than replacing it. */
    fun isDistracting(packageName: String): Boolean {
        if (packageName in KNOWN_MEDIA_PACKAGES) return true
        val appInfo = runCatching { context.packageManager.getApplicationInfo(packageName, 0) }.getOrNull()
        if (appInfo?.category == ApplicationInfo.CATEGORY_VIDEO) return true
        return isSocialOrBrowser(packageName)
    }

    /** Best-effort real-icon lookup by component, for tiles not already covered by a
     *  loaded AppInfo list. Wrapped in runCatching since the app may have been
     *  uninstalled since the tile was assigned. */
    fun iconFor(packageName: String, className: String): Drawable? =
        runCatching { context.packageManager.getActivityIcon(ComponentName(packageName, className)) }.getOrNull()

    /** The API built specifically for launcher apps - more complete/reliable than a
     *  generic package query (e.g. surfaces apps a plain queryIntentActivities can miss).
     *  Requires the caller to be the current default Home app, which this app is. */
    private fun getLaunchableAppsViaLauncherApps(): List<AppInfo> {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val ownPackage = context.packageName

        return launcherApps.profiles
            .flatMap { profile -> launcherApps.getActivityList(null, profile) }
            .filter { it.applicationInfo.packageName != ownPackage }
            .map { info ->
                AppInfo(
                    label = info.label.toString(),
                    packageName = info.componentName.packageName,
                    className = info.componentName.className,
                    icon = info.getIcon(0)
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    private fun getLaunchableAppsViaPackageManager(): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val ownPackage = context.packageName

        return pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .filter { it.activityInfo.packageName != ownPackage }
            .map { resolveInfo ->
                AppInfo(
                    label = resolveInfo.loadLabel(pm).toString(),
                    packageName = resolveInfo.activityInfo.packageName,
                    className = resolveInfo.activityInfo.name,
                    icon = resolveInfo.loadIcon(pm)
                )
            }
            .sortedBy { it.label.lowercase() }
    }
}
