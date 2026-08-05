package com.caseyfrancis.vibelauncher.ui.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.caseyfrancis.vibelauncher.data.settings.SettingsRepository
import com.caseyfrancis.vibelauncher.ui.settings.components.GLASS_OPACITY
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
                _uiState.value = CardColorUiState(
                    hue = hsv[0],
                    saturation = hsv[1],
                    brightness = hsv[2],
                    opacity = android.graphics.Color.alpha(argb) / 255f
                )
            }
        }
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

    class Factory(private val settingsRepository: SettingsRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return CardColorViewModel(settingsRepository) as T
        }
    }
}
