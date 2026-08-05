package com.caseyfrancis.vibelauncher.ui.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.caseyfrancis.vibelauncher.data.apps.AppInfo
import com.caseyfrancis.vibelauncher.data.apps.InstalledAppsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppPickerViewModel(private val repository: InstalledAppsRepository) : ViewModel() {

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    init {
        viewModelScope.launch {
            _apps.value = repository.getLaunchableApps()
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    class Factory(private val repository: InstalledAppsRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AppPickerViewModel(repository) as T
        }
    }
}
