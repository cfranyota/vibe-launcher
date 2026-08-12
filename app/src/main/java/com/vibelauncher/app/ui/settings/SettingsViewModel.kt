package com.vibelauncher.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vibelauncher.app.data.icontheme.IconThemeRepository
import com.vibelauncher.app.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val iconThemeRepository: IconThemeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(iconPacks = iconThemeRepository.getInstalledIconPacks())
        viewModelScope.launch {
            settingsRepository.iconThemePackage.collectLatest { themePackage ->
                _uiState.value = _uiState.value.copy(selectedIconThemePackage = themePackage)
            }
        }
        viewModelScope.launch {
            settingsRepository.applyIconThemeToHomeTiles.collectLatest { enabled ->
                _uiState.value = _uiState.value.copy(applyIconThemeToHomeTiles = enabled)
            }
        }
        viewModelScope.launch {
            settingsRepository.tileBorderEnabled.collectLatest { enabled ->
                _uiState.value = _uiState.value.copy(tileBorderEnabled = enabled)
            }
        }
        viewModelScope.launch {
            settingsRepository.tileBorderSizeStep.collectLatest { step ->
                _uiState.value = _uiState.value.copy(tileBorderSizeStep = step)
            }
        }
    }

    fun selectIconTheme(packageName: String) {
        viewModelScope.launch { settingsRepository.setIconThemePackage(packageName) }
    }

    fun setApplyIconThemeToHomeTiles(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setApplyIconThemeToHomeTiles(enabled) }
    }

    fun setTileBorderEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setTileBorderEnabled(enabled) }
    }

    fun setTileBorderSizeStep(step: Int) {
        viewModelScope.launch { settingsRepository.setTileBorderSizeStep(step) }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val iconThemeRepository: IconThemeRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(settingsRepository, iconThemeRepository) as T
        }
    }
}
