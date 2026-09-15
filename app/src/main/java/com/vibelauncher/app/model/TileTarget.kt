package com.vibelauncher.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class BuiltInAction {
    NOTE,
    EVENT,
    /** Opens the phone's default AI assistant. Saved as "TIMER" - this slot was the Clock
     *  tile before it became AI, and keeping the stored name means tiles people already set
     *  up still decode. */
    @SerialName("TIMER") ASSISTANT,
    TODO,
    CALL,
    MESSAGE,
    CAMERA,
    MEMO
}

@Serializable
sealed class TileTarget {
    @Serializable
    data class BuiltIn(val kind: BuiltInAction) : TileTarget()

    @Serializable
    data class App(val packageName: String, val className: String) : TileTarget()
}
