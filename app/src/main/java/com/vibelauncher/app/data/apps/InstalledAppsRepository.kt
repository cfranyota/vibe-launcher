package com.vibelauncher.app.data.apps

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager

class InstalledAppsRepository(private val context: Context) {

    fun getLaunchableApps(): List<AppInfo> {
        return runCatching { getLaunchableAppsViaLauncherApps() }
            .getOrElse { getLaunchableAppsViaPackageManager() }
    }

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
