package com.vibelauncher.app.data.icontheme

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import org.xmlpull.v1.XmlPullParser

data class IconPackInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable
)

/** The standard multi-launcher icon-pack contract - apps that want to be picked up by
 *  launchers (Nova, Apex, ADW, and now this one) declare an activity responding to one
 *  of these categories, purely as a discovery marker. */
private val ICON_PACK_CATEGORIES = listOf(
    "com.novalauncher.THEME",
    "com.anddoes.launcher.THEME",
    "com.gau.go.launcherex.theme",
    "org.adw.launcher.THEMES"
)

/**
 * Resolves themed drawer icons from an installed icon-pack app's own `appfilter.xml` -
 * only apps explicitly listed by the pack get a themed icon; anything the pack doesn't
 * cover falls back to the app's real icon rather than attempting the back/mask/upon
 * compositing packs use for their generic "unthemed" treatment.
 */
class IconThemeRepository(private val context: Context) {
    private val packageManager get() = context.packageManager

    /** componentKey -> drawable resource name, cached per pack since parsing appfilter.xml
     *  on every icon lookup would be wasteful - invalidated by simply picking a different pack. */
    private val mappingCache = mutableMapOf<String, Map<String, String>>()

    fun getInstalledIconPacks(): List<IconPackInfo> {
        val packageNames = ICON_PACK_CATEGORIES
            .flatMap { category ->
                val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
                runCatching { packageManager.queryIntentActivities(intent, 0) }.getOrDefault(emptyList())
            }
            .map { it.activityInfo.packageName }
            .distinct()

        return packageNames.mapNotNull { pkg ->
            runCatching {
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                IconPackInfo(
                    packageName = pkg,
                    label = packageManager.getApplicationLabel(appInfo).toString(),
                    icon = packageManager.getApplicationIcon(appInfo)
                )
            }.getOrNull()
        }.sortedBy { it.label.lowercase() }
    }

    fun getThemedIcon(componentName: ComponentName, iconPackPackage: String): Drawable? {
        if (iconPackPackage.isBlank()) return null
        val mapping = mappingCache.getOrPut(iconPackPackage) { parseAppFilter(iconPackPackage) }
        val componentKey = "ComponentInfo{${componentName.packageName}/${componentName.className}}"
        val drawableName = mapping[componentKey] ?: return null
        return runCatching {
            val packResources = packageManager.getResourcesForApplication(iconPackPackage)
            val resId = packResources.getIdentifier(drawableName, "drawable", iconPackPackage)
            if (resId == 0) null else ResourcesCompat.getDrawable(packResources, resId, null)
        }.getOrNull()
    }

    private fun parseAppFilter(iconPackPackage: String): Map<String, String> = runCatching {
        val packResources = packageManager.getResourcesForApplication(iconPackPackage)
        val xmlResId = packResources.getIdentifier("appfilter", "xml", iconPackPackage)
        if (xmlResId == 0) return@runCatching emptyMap()

        val parser = packResources.getXml(xmlResId)
        val result = mutableMapOf<String, String>()
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                val component = parser.getAttributeValue(null, "component")
                val drawable = parser.getAttributeValue(null, "drawable")
                if (component != null && drawable != null) {
                    result[component] = drawable
                }
            }
            eventType = parser.next()
        }
        result
    }.getOrDefault(emptyMap())
}
