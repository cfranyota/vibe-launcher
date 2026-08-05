package com.caseyfrancis.vibelauncher.model

import kotlinx.serialization.Serializable

enum class BuiltInAction {
    NOTE, EVENT, TIMER, TODO, CALL, MESSAGE, CAMERA, MEMO
}

@Serializable
sealed class TileTarget {
    @Serializable
    data class BuiltIn(val kind: BuiltInAction) : TileTarget()

    @Serializable
    data class App(val packageName: String, val className: String) : TileTarget()
}
