package com.vibelauncher.app.ui.settings

data class CardColorUiState(
    val hue: Float = 0f,
    val saturation: Float = 0f,
    val brightness: Float = 0.1f,
    val opacity: Float = 1f,
    val enabled: Boolean = false,
    val iconHue: Float = 0f,
    val iconSaturation: Float = 0f,
    val iconBrightness: Float = 1f,
    val iconEnabled: Boolean = false
)
