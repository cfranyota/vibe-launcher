package com.caseyfrancis.vibelauncher.ui.settings

import com.caseyfrancis.vibelauncher.data.icontheme.IconPackInfo

data class SettingsUiState(
    val iconPacks: List<IconPackInfo> = emptyList(),
    val selectedIconThemePackage: String = "",
    val applyIconThemeToHomeTiles: Boolean = false
)
