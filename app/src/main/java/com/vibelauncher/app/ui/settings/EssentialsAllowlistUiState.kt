package com.vibelauncher.app.ui.settings

import com.vibelauncher.app.data.apps.AppInfo

data class EssentialsAllowlistUiState(
    val apps: List<AppInfo> = emptyList(),
    val selectedKeys: Set<String> = emptySet(),
    val query: String = ""
)
