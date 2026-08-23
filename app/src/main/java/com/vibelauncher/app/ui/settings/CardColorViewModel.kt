package com.vibelauncher.app.ui.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vibelauncher.app.data.settings.SettingsRepository
import com.vibelauncher.app.ui.settings.components.GLASS_OPACITY
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CardColorViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CardColorUiState())
    val uiState: StateFlow<CardColorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.eventCardColor.collectLatest { argb ->
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(argb, hsv)
                _uiState.value = _uiState.value.copy(
                    hue = hsv[0],
                    saturation = hsv[1],
                    brightness = hsv[2],
                    opacity = android.graphics.Color.alpha(argb) / 255f
                )
            }
        }
        viewModelScope.launch {
            settingsRepository.eventCardColorEnabled.collectLatest { enabled ->
                _uiState.value = _uiState.value.copy(enabled = enabled)
            }
        }
        viewModelScope.launch {
            settingsRepository.iconAccentColor.collectLatest { argb ->
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(argb, hsv)
                _uiState.value = _uiState.value.copy(
                    iconHue = hsv[0],
                    iconSaturation = hsv[1],
                    iconBrightness = hsv[2]
                )
            }
        }
        viewModelScope.launch {
            settingsRepository.iconAccentColorEnabled.collectLatest { enabled ->
                _uiState.value = _uiState.value.copy(iconEnabled = enabled)
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(enabled = enabled)
        viewModelScope.launch { settingsRepository.setEventCardColorEnabled(enabled) }
    }

    fun setHueSaturation(hue: Float, saturation: Float) {
        persist(_uiState.value.copy(hue = hue, saturation = saturation))
    }

    fun setBrightness(value: Float) {
        persist(_uiState.value.copy(brightness = value))
    }

    fun setOpacity(value: Float) {
        persist(_uiState.value.copy(opacity = value))
    }

    fun applyGlassPreset() {
        persist(_uiState.value.copy(opacity = GLASS_OPACITY))
    }

    private fun persist(newState: CardColorUiState) {
        _uiState.value = newState
        val argb = Color.hsv(newState.hue, newState.saturation, newState.brightness, newState.opacity).toArgb()
        viewModelScope.launch { settingsRepository.setEventCardColor(argb) }
    }

    fun setIconEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(iconEnabled = enabled)
        viewModelScope.launch { settingsRepository.setIconAccentColorEnabled(enabled) }
    }

    fun setIconHueSaturation(hue: Float, saturation: Float) {
        persistIcon(_uiState.value.copy(iconHue = hue, iconSaturation = saturation))
    }

    fun setIconBrightness(value: Float) {
        persistIcon(_uiState.value.copy(iconBrightness = value))
    }

    private fun persistIcon(newState: CardColorUiState) {
        _uiState.value = newState
        // Icons stay fully opaque - no glass/opacity mode for them like the card background has.
        val argb = Color.hsv(newState.iconHue, newState.iconSaturation, newState.iconBrightness, 1f).toArgb()
        viewModelScope.launch { settingsRepository.setIconAccentColor(argb) }
    }

    class Factory(private val settingsRepository: SettingsRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return CardColorViewModel(settingsRepository) as T
        }
    }
}
