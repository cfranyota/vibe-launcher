package com.vibelauncher.app.model

import kotlinx.serialization.Serializable

@Serializable
data class Tile(
    val id: Int,
    val label: String,
    val iconKey: String,
    val target: TileTarget
)
