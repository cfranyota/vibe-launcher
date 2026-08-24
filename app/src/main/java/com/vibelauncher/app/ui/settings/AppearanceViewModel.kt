package com.vibelauncher.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vibelauncher.app.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppearanceViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    // Null until the first real DataStore read arrives - no synthetic default, so callers
    // (esp. CustomAccentScreen, which seeds one-shot local drag state from this) can tell
    // "not loaded yet" apart from "loaded and happens to match some default value".
    val uiState: StateFlow<AppearanceUiState?> = combine(
        settingsRepository.fontScale, settingsRepository.accentColor
    ) { fontScale, accentColorArgb ->
        AppearanceUiState(fontScale = fontScale, accentColorArgb = accentColorArgb)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setFontScale(scale: Float) {
        viewModelScope.launch { settingsRepository.setFontScale(scale) }
    }

    fun setAccentColor(argb: Int) {
        viewModelScope.launch { settingsRepository.setAccentColor(argb) }
    }

    class Factory(private val settingsRepository: SettingsRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AppearanceViewModel(settingsRepository) as T
        }
    }
}
