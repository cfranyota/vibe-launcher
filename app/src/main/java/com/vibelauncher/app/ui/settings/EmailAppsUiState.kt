package com.vibelauncher.app.ui.settings

import com.vibelauncher.app.data.apps.AppInfo

data class EmailAppsUiState(
    val apps: List<AppInfo> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val query: String = ""
)
