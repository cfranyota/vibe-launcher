package com.vibelauncher.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vibelauncher.app.data.apps.AppInfo
import com.vibelauncher.app.data.apps.InstalledAppsRepository
import com.vibelauncher.app.data.tiles.TileRepository
import com.vibelauncher.app.model.BuiltInAction
import com.vibelauncher.app.model.Tile
import com.vibelauncher.app.model.TileTarget
import com.vibelauncher.app.util.IntentDefaults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * A single scrolling checkbox list of every installed app plus the 8 built-in Vibe
 * Launcher actions - check up to 8 total, those become the home screen tiles, in the
 * order checked. Every toggle persists immediately, same as the rest of Settings.
 */
class HomeAppsViewModel(
    private val tileRepository: TileRepository,
    private val installedAppsRepository: InstalledAppsRepository
) : ViewModel() {

    private val installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val selected = MutableStateFlow<List<Tile>>(emptyList())
    private val query = MutableStateFlow("")

    init {
        viewModelScope.launch { installedApps.value = installedAppsRepository.getLaunchableApps() }
        viewModelScope.launch { selected.value = tileRepository.tiles.first() }
    }

    val uiState: StateFlow<HomeAppsUiState> = combine(installedApps, selected, query) { apps, sel, q ->
        val filtered = if (q.isBlank()) apps else apps.filter { it.label.contains(q, ignoreCase = true) }
        HomeAppsUiState(
            builtInActions = BuiltInAction.entries.toList(),
            apps = filtered,
            selectedKeys = sel.map { keyOf(it.target) }.toSet(),
            atCap = sel.size >= 8,
            query = q
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeAppsUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun toggleApp(app: AppInfo) {
        toggle(TileTarget.App(app.packageName, app.className), app.label, "app:${app.packageName}")
    }

    fun toggleBuiltIn(action: BuiltInAction) {
        val default = IntentDefaults.defaultTiles().first { (it.target as TileTarget.BuiltIn).kind == action }
        toggle(TileTarget.BuiltIn(action), default.label, default.iconKey)
    }

    private fun toggle(target: TileTarget, label: String, iconKey: String) {
        val current = selected.value
        val key = keyOf(target)
        val isChecked = current.any { keyOf(it.target) == key }
        val next = when {
            isChecked -> current.filterNot { keyOf(it.target) == key }
            current.size >= 8 -> return
            else -> current + Tile(id = current.size, label = label, iconKey = iconKey, target = target)
        }
        selected.value = next
        viewModelScope.launch { tileRepository.setTiles(next) }
    }

    private fun keyOf(target: TileTarget): Any = when (target) {
        is TileTarget.App -> target.packageName to target.className
        is TileTarget.BuiltIn -> target.kind
    }

    fun resetToDefault() {
        selected.value = IntentDefaults.defaultTiles()
        viewModelScope.launch { tileRepository.resetToDefault() }
    }

    class Factory(
        private val tileRepository: TileRepository,
        private val installedAppsRepository: InstalledAppsRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return HomeAppsViewModel(tileRepository, installedAppsRepository) as T
        }
    }
}
