package com.vibelauncher.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vibelauncher.app.data.apps.AppInfo
import com.vibelauncher.app.data.apps.InstalledAppsRepository
import com.vibelauncher.app.data.contacts.ContactResult
import com.vibelauncher.app.data.contacts.ContactsRepository
import com.vibelauncher.app.data.lettershortcuts.LetterShortcut
import com.vibelauncher.app.data.lettershortcuts.LetterShortcutType
import com.vibelauncher.app.data.lettershortcuts.LetterShortcutsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Backs the A-Z hold-a-letter shortcuts editor - each letter can be assigned "open an
 *  app," "message a contact," or "call a contact," persisted via [LetterShortcutsRepository]. */
class LetterShortcutsViewModel(
    private val letterShortcutsRepository: LetterShortcutsRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val contactsRepository: ContactsRepository
) : ViewModel() {

    private val installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val appQuery = MutableStateFlow("")
    private val contactQuery = MutableStateFlow("")
    private val contactResults = MutableStateFlow<List<ContactResult>>(emptyList())

    init {
        viewModelScope.launch {
            installedApps.value = withContext(Dispatchers.IO) { installedAppsRepository.getLaunchableApps() }
        }
    }

    val uiState: StateFlow<LetterShortcutsUiState> = combine(
        letterShortcutsRepository.shortcuts, installedApps, appQuery, contactQuery, contactResults
    ) { shortcuts, apps, aq, cq, results ->
        LetterShortcutsUiState(
            shortcuts = shortcuts.associateBy { it.letter },
            apps = if (aq.isBlank()) apps else apps.filter { it.label.contains(aq, ignoreCase = true) },
            appQuery = aq,
            contactQuery = cq,
            contactResults = results
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LetterShortcutsUiState())

    fun onAppQueryChange(value: String) {
        appQuery.value = value
    }

    fun onContactQueryChange(value: String) {
        contactQuery.value = value
        viewModelScope.launch {
            contactResults.value = if (value.isBlank()) {
                emptyList()
            } else {
                withContext(Dispatchers.IO) { contactsRepository.searchContacts(value) }
            }
        }
    }

    fun resetPickerQueries() {
        appQuery.value = ""
        contactQuery.value = ""
        contactResults.value = emptyList()
    }

    fun assignApp(letter: Char, app: AppInfo) {
        viewModelScope.launch {
            letterShortcutsRepository.setShortcut(
                LetterShortcut(
                    letter = letter,
                    type = LetterShortcutType.OPEN_APP,
                    label = app.label,
                    packageName = app.packageName,
                    className = app.className
                )
            )
        }
    }

    fun assignContact(letter: Char, type: LetterShortcutType, contact: ContactResult) {
        viewModelScope.launch {
            letterShortcutsRepository.setShortcut(
                LetterShortcut(
                    letter = letter,
                    type = type,
                    label = contact.name,
                    contactId = contact.contactId,
                    phone = contact.phone
                )
            )
        }
    }

    fun clear(letter: Char) {
        viewModelScope.launch { letterShortcutsRepository.clearShortcut(letter) }
    }

    class Factory(
        private val letterShortcutsRepository: LetterShortcutsRepository,
        private val installedAppsRepository: InstalledAppsRepository,
        private val contactsRepository: ContactsRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return LetterShortcutsViewModel(letterShortcutsRepository, installedAppsRepository, contactsRepository) as T
        }
    }
}
