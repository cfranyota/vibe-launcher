package com.vibelauncher.app.ui.settings

import com.vibelauncher.app.model.Tile

data class HomeAppsUiState(
    val tiles: List<Tile> = emptyList(),
    val pickerForSlot: Int? = null
)
