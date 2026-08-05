package com.vibelauncher.app.ui.settings

import com.vibelauncher.app.data.icontheme.IconPackInfo

data class SettingsUiState(
    val iconPacks: List<IconPackInfo> = emptyList(),
    val selectedIconThemePackage: String = "",
    val applyIconThemeToHomeTiles: Boolean = false
)
