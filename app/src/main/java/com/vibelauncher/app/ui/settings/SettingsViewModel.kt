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
            settingsRepository.tileBorderEnabled.collectLatest { enabled ->
                _uiState.value = _uiState.value.copy(tileBorderEnabled = enabled)
            }
        }
        viewModelScope.launch {
            settingsRepository.tileBorderSizeStep.collectLatest { step ->
                _uiState.value = _uiState.value.copy(tileBorderSizeStep = step)
            }
        }
        viewModelScope.launch {
            settingsRepository.vibeBarEnabled.collectLatest { enabled ->
                _uiState.value = _uiState.value.copy(vibeBarEnabled = enabled)
            }
        }
        viewModelScope.launch {
            settingsRepository.iconSizeStep.collectLatest { step ->
                _uiState.value = _uiState.value.copy(iconSizeStep = step)
            }
        }
        viewModelScope.launch {
            settingsRepository.homeIconsStayDefault.collectLatest { enabled ->
                _uiState.value = _uiState.value.copy(homeIconsStayDefault = enabled)
            }
        }
        viewModelScope.launch {
            settingsRepository.monkEssentialsOnlyEnabled.collectLatest { enabled ->
                _uiState.value = _uiState.value.copy(monkEssentialsOnlyEnabled = enabled)
            }
        }
        viewModelScope.launch {
            settingsRepository.monkHideSocialBrowserEnabled.collectLatest { enabled ->
                _uiState.value = _uiState.value.copy(monkHideSocialBrowserEnabled = enabled)
            }
        }
    }

    fun selectIconTheme(packageName: String) {
        viewModelScope.launch { settingsRepository.setIconThemePackage(packageName) }
    }

    fun setTileBorderEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setTileBorderEnabled(enabled) }
    }

    fun setTileBorderSizeStep(step: Int) {
        viewModelScope.launch { settingsRepository.setTileBorderSizeStep(step) }
    }

    fun setVibeBarEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setVibeBarEnabled(enabled) }
    }

    fun setIconSizeStep(step: Int) {
        viewModelScope.launch { settingsRepository.setIconSizeStep(step) }
    }

    fun setHomeIconsStayDefault(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHomeIconsStayDefault(enabled) }
    }

    fun setMonkEssentialsOnlyEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMonkEssentialsOnlyEnabled(enabled) }
    }

    fun setMonkHideSocialBrowserEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMonkHideSocialBrowserEnabled(enabled) }
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
