package com.caseyfrancis.vibelauncher.ui.drawer

import android.content.ComponentName
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.caseyfrancis.vibelauncher.data.apps.AppInfo
import com.caseyfrancis.vibelauncher.data.apps.InstalledAppsRepository
import com.caseyfrancis.vibelauncher.data.icontheme.IconThemeRepository
import com.caseyfrancis.vibelauncher.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AppDrawerViewModel(
    private val repository: InstalledAppsRepository,
    private val settingsRepository: SettingsRepository,
    private val iconThemeRepository: IconThemeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppDrawerUiState())
    val uiState: StateFlow<AppDrawerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(apps = repository.getLaunchableApps())
        }
        viewModelScope.launch {
            settingsRepository.iconThemePackage.collectLatest { themePackage ->
                _uiState.value = _uiState.value.copy(iconThemePackage = themePackage)
            }
        }
    }

    fun onQueryChange(value: String) {
        _uiState.value = _uiState.value.copy(query = value)
    }

    /** Only ever called from the drawer - home-screen tiles never go through this, so
     *  they're unaffected by whatever icon theme is selected here. */
    fun iconFor(app: AppInfo): Drawable {
        val themePackage = _uiState.value.iconThemePackage
        if (themePackage.isBlank()) return app.icon
        return iconThemeRepository.getThemedIcon(ComponentName(app.packageName, app.className), themePackage)
            ?: app.icon
    }

    class Factory(
        private val repository: InstalledAppsRepository,
        private val settingsRepository: SettingsRepository,
        private val iconThemeRepository: IconThemeRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AppDrawerViewModel(repository, settingsRepository, iconThemeRepository) as T
        }
    }
}
