package com.caseyfrancis.vibelauncher.ui.settings

import com.caseyfrancis.vibelauncher.model.Tile

data class HomeAppsUiState(
    val tiles: List<Tile> = emptyList(),
    val pickerForSlot: Int? = null
)
