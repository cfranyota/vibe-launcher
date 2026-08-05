package com.vibelauncher.app.ui.drawer

import com.vibelauncher.app.data.apps.AppInfo

data class AppDrawerUiState(
    val apps: List<AppInfo> = emptyList(),
    val query: String = "",
    val iconThemePackage: String = ""
) {
    val filteredApps: List<AppInfo>
        get() = if (query.isBlank()) apps else apps.filter { it.label.contains(query, ignoreCase = true) }
}
