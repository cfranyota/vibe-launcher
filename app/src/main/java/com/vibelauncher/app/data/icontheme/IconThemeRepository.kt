package com.vibelauncher.app.data.icontheme

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import com.vibelauncher.app.model.BuiltInAction
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

/** Keywords to search a pack's drawable names for, per built-in action - since Note/To-Do/
 *  etc. have no real installed-app component an appfilter.xml could ever match, this is
 *  the whole "theming" story for them: pick a pack once, and every built-in action gets
 *  auto-matched by name with zero extra taps. Falls back to the fixed glyph if nothing
 *  matches - see HomeViewModel.iconFor. */
private val BUILT_IN_KEYWORDS: Map<BuiltInAction, List<String>> = mapOf(
    BuiltInAction.NOTE to listOf("note"),
    BuiltInAction.EVENT to listOf("event", "calendar"),
    BuiltInAction.TIMER to listOf("timer", "alarm", "clock"),
    BuiltInAction.TODO to listOf("todo", "checklist", "task"),
    BuiltInAction.CALL to listOf("call", "phone", "dialer"),
    BuiltInAction.MESSAGE to listOf("message", "sms", "chat"),
    BuiltInAction.CAMERA to listOf("camera"),
    BuiltInAction.MEMO to listOf("memo", "record", "voice", "mic")
)

/**
 * Resolves themed drawer/tile icons from an installed icon-pack app's own `appfilter.xml` -
 * real apps get matched by component; the 8 built-in Vibe Launcher actions (no real
 * component to match) get auto-matched by searching the pack's drawable names for a
 * category keyword instead. Anything a pack doesn't cover falls back to the app's real
 * icon (for real apps) or the fixed glyph (for built-ins) rather than attempting the
 * back/mask/upon compositing packs use for their generic "unthemed" treatment.
 */
class IconThemeRepository(private val context: Context) {
    private val packageManager get() = context.packageManager

    /** componentKey -> drawable resource name, cached per pack since parsing appfilter.xml
     *  on every icon lookup would be wasteful - invalidated by simply picking a different pack. */
    private val mappingCache = mutableMapOf<String, Map<String, String>>()

    /** BuiltInAction -> matched drawable name (or null if no keyword hit), cached per pack. */
    private val autoMatchCache = mutableMapOf<String, Map<BuiltInAction, String?>>()

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
        return resolveDrawable(iconPackPackage, drawableName)
    }

    fun getAutoMatchedIcon(iconPackPackage: String, action: BuiltInAction): Drawable? {
        if (iconPackPackage.isBlank()) return null
        val matches = autoMatchCache.getOrPut(iconPackPackage) { computeAutoMatches(iconPackPackage) }
        val drawableName = matches[action] ?: return null
        return resolveDrawable(iconPackPackage, drawableName)
    }

    private fun computeAutoMatches(iconPackPackage: String): Map<BuiltInAction, String?> {
        val names = mappingCache.getOrPut(iconPackPackage) { parseAppFilter(iconPackPackage) }.values.distinct()
        return BuiltInAction.entries.associateWith { action ->
            BUILT_IN_KEYWORDS.getValue(action).firstNotNullOfOrNull { keyword ->
                names.firstOrNull { it.contains(keyword, ignoreCase = true) }
            }
        }
    }

    private fun resolveDrawable(iconPackPackage: String, drawableName: String): Drawable? = runCatching {
        val packResources = packageManager.getResourcesForApplication(iconPackPackage)
        val resId = packResources.getIdentifier(drawableName, "drawable", iconPackPackage)
        if (resId == 0) null else ResourcesCompat.getDrawable(packResources, resId, null)
    }.getOrNull()

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
