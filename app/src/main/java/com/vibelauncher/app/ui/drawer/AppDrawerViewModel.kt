package com.vibelauncher.app.ui.drawer

import android.content.ComponentName
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vibelauncher.app.data.apps.AppInfo
import com.vibelauncher.app.data.apps.InstalledAppsRepository
import com.vibelauncher.app.data.icontheme.IconThemeRepository
import com.vibelauncher.app.data.monkmode.EssentialsAllowlistRepository
import com.vibelauncher.app.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppDrawerViewModel(
    private val repository: InstalledAppsRepository,
    private val settingsRepository: SettingsRepository,
    private val iconThemeRepository: IconThemeRepository,
    private val essentialsAllowlistRepository: EssentialsAllowlistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppDrawerUiState())
    val uiState: StateFlow<AppDrawerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) { repository.getLaunchableApps() }
            // Computed once per app-list load, not per recomposition/filter pass - category
            // and browser-resolution checks hit PackageManager, so this stays off the UI
            // thread and isn't repeated on every keystroke in the search field.
            val socialBrowser = withContext(Dispatchers.IO) {
                apps.filter { repository.isSocialOrBrowser(it.packageName) }.map { it.packageName }.toSet()
            }
            _uiState.value = _uiState.value.copy(apps = apps, socialBrowserPackages = socialBrowser)
        }
        viewModelScope.launch {
            settingsRepository.iconThemePackage.collectLatest { themePackage ->
                _uiState.value = _uiState.value.copy(iconThemePackage = themePackage)
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
        viewModelScope.launch {
            essentialsAllowlistRepository.allowlist.collectLatest { allowlist ->
                _uiState.value = _uiState.value.copy(essentialsAllowlist = allowlist)
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
        private val iconThemeRepository: IconThemeRepository,
        private val essentialsAllowlistRepository: EssentialsAllowlistRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AppDrawerViewModel(repository, settingsRepository, iconThemeRepository, essentialsAllowlistRepository) as T
        }
    }
}
