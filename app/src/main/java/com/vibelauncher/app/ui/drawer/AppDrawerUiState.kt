package com.vibelauncher.app.ui.drawer

import com.vibelauncher.app.data.apps.AppInfo

data class AppDrawerUiState(
    val apps: List<AppInfo> = emptyList(),
    val query: String = "",
    val iconThemePackage: String = "",
    val monkEssentialsOnlyEnabled: Boolean = false,
    val monkHideSocialBrowserEnabled: Boolean = false,
    val essentialsAllowlist: Set<String> = emptySet(),
    val socialBrowserPackages: Set<String> = emptySet()
) {
    val filteredApps: List<AppInfo>
        get() {
            var result = apps
            if (monkEssentialsOnlyEnabled) {
                result = result.filter { "${it.packageName}/${it.className}" in essentialsAllowlist }
            }
            if (monkHideSocialBrowserEnabled) {
                result = result.filter { it.packageName !in socialBrowserPackages }
            }
            if (query.isNotBlank()) {
                result = result.filter { it.label.contains(query, ignoreCase = true) }
            }
            return result
        }
}
