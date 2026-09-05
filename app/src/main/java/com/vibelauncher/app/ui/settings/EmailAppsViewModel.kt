package com.vibelauncher.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vibelauncher.app.data.apps.AppInfo
import com.vibelauncher.app.data.apps.InstalledAppsRepository
import com.vibelauncher.app.data.hub.EmailAppsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A checkbox list of every installed app - which ones Hub should classify as "email" on
 *  top of the small built-in [com.vibelauncher.app.data.hub.EMAIL_PACKAGES] set. One row per
 *  distinct package (unlike EssentialsAllowlistViewModel, which is activity-scoped), since
 *  email classification only ever looks at a notification's packageName. */
class EmailAppsViewModel(
    private val emailAppsRepository: EmailAppsRepository,
    private val installedAppsRepository: InstalledAppsRepository
) : ViewModel() {

    private val installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val query = MutableStateFlow("")

    init {
        viewModelScope.launch {
            installedApps.value = withContext(Dispatchers.IO) {
                installedAppsRepository.getLaunchableApps().distinctBy { it.packageName }
            }
        }
    }

    val uiState = combine(installedApps, emailAppsRepository.selectedPackages, query) { apps, selected, q ->
        EmailAppsUiState(
            apps = if (q.isBlank()) apps else apps.filter { it.label.contains(q, ignoreCase = true) },
            selectedPackages = selected,
            query = q
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EmailAppsUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun toggle(app: AppInfo) {
        viewModelScope.launch { emailAppsRepository.toggle(app.packageName) }
    }

    class Factory(
        private val emailAppsRepository: EmailAppsRepository,
        private val installedAppsRepository: InstalledAppsRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return EmailAppsViewModel(emailAppsRepository, installedAppsRepository) as T
        }
    }
}
