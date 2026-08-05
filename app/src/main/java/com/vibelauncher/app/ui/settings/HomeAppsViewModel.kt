package com.vibelauncher.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vibelauncher.app.data.tiles.TileRepository
import com.vibelauncher.app.model.TileTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The same slot-reassignment capability already available by long-pressing a tile on the
 * home screen (see HomeViewModel.assignTile/resetTile), lifted out so it's reachable from
 * Launcher Settings without pulling in the rest of HomeViewModel's calendar/weather state.
 */
class HomeAppsViewModel(private val tileRepository: TileRepository) : ViewModel() {

    private val pickerForSlot = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<HomeAppsUiState> = combine(
        tileRepository.tiles,
        pickerForSlot
    ) { tiles, slot ->
        HomeAppsUiState(tiles = tiles, pickerForSlot = slot)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeAppsUiState())

    fun onTileTapped(slot: Int) {
        pickerForSlot.value = slot
    }

    fun dismissPicker() {
        pickerForSlot.value = null
    }

    fun assignTile(slot: Int, label: String, iconKey: String, target: TileTarget) {
        viewModelScope.launch {
            tileRepository.setTile(slot, label, iconKey, target)
            pickerForSlot.value = null
        }
    }

    fun resetTile(slot: Int) {
        viewModelScope.launch {
            tileRepository.resetTile(slot)
            pickerForSlot.value = null
        }
    }

    class Factory(private val tileRepository: TileRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return HomeAppsViewModel(tileRepository) as T
        }
    }
}
