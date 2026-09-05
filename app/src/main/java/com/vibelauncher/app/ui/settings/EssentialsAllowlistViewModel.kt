package com.vibelauncher.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vibelauncher.app.data.apps.AppInfo
import com.vibelauncher.app.data.apps.InstalledAppsRepository
import com.vibelauncher.app.data.monkmode.EssentialsAllowlistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** An unbounded checkbox list of every installed app (unlike HomeAppsScreen's capped-at-8
 *  tile selection) - what Vibe Mode's "essentials only" tier lets through to the drawer. */
class EssentialsAllowlistViewModel(
    private val essentialsAllowlistRepository: EssentialsAllowlistRepository,
    private val installedAppsRepository: InstalledAppsRepository
) : ViewModel() {

    private val installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val query = MutableStateFlow("")

    init {
        viewModelScope.launch {
            installedApps.value = withContext(Dispatchers.IO) { installedAppsRepository.getLaunchableApps() }
        }
    }

    val uiState = combine(installedApps, essentialsAllowlistRepository.allowlist, query) { apps, allowlist, q ->
        EssentialsAllowlistUiState(
            apps = if (q.isBlank()) apps else apps.filter { it.label.contains(q, ignoreCase = true) },
            selectedKeys = allowlist,
            query = q
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EssentialsAllowlistUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun toggle(app: AppInfo) {
        viewModelScope.launch { essentialsAllowlistRepository.toggle(app.packageName, app.className) }
    }

    class Factory(
        private val essentialsAllowlistRepository: EssentialsAllowlistRepository,
        private val installedAppsRepository: InstalledAppsRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return EssentialsAllowlistViewModel(essentialsAllowlistRepository, installedAppsRepository) as T
        }
    }
}
