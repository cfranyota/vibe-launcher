package com.vibelauncher.app.ui.settings

import com.vibelauncher.app.data.apps.AppInfo
import com.vibelauncher.app.model.BuiltInAction

data class HomeAppsUiState(
    val builtInActions: List<BuiltInAction> = emptyList(),
    val apps: List<AppInfo> = emptyList(),
    val selectedKeys: Set<Any> = emptySet(),
    val atCap: Boolean = false,
    val query: String = ""
)
