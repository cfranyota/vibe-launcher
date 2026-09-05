package com.vibelauncher.app.ui.hub

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vibelauncher.app.data.hub.HubCategory
import com.vibelauncher.app.data.hub.HubFilter
import com.vibelauncher.app.data.hub.HubItem
import com.vibelauncher.app.data.hub.HubRepository
import com.vibelauncher.app.data.hub.HubStateRepository
import com.vibelauncher.app.data.sms.SmsMessageItem
import com.vibelauncher.app.data.sms.SmsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HubViewModel(
    private val appContext: Context,
    private val hubRepository: HubRepository,
    private val hubStateRepository: HubStateRepository,
    private val smsRepository: SmsRepository
) : ViewModel() {

    private val filter = MutableStateFlow(HubFilter.ALL)
    private val searchQuery = MutableStateFlow("")
    private val hasCallLogPermission = MutableStateFlow(checkCallLogPermission(appContext))
    private val hasNotificationAccess = MutableStateFlow(checkNotificationAccess(appContext))
    private val hasSmsRole = MutableStateFlow(checkSmsRole(appContext))
    private val _replyTarget = MutableStateFlow<HubItem.Message?>(null)
    val replyTarget: StateFlow<HubItem.Message?> = _replyTarget

    init {
        // Long-lived for as long as this ViewModel (and thus the Hub screen route) is
        // alive - see HubRepository/NotificationBadgeListenerService doc comments for why
        // this can't be done more passively than "something has to be watching."
        viewModelScope.launch { hubRepository.observeAndPersistNotifications() }
    }

    val uiState: StateFlow<HubUiState> = combine(
        hubRepository.items(), filter, searchQuery, hasCallLogPermission, hasNotificationAccess, hasSmsRole
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        HubUiState(
            items = values[0] as List<HubItem>,
            filter = values[1] as HubFilter,
            searchQuery = values[2] as String,
            hasCallLogPermission = values[3] as Boolean,
            hasNotificationAccess = values[4] as Boolean,
            hasSmsRole = values[5] as Boolean
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HubUiState())

    fun setFilter(value: HubFilter) {
        filter.value = value
    }

    fun setSearchQuery(value: String) {
        searchQuery.value = value
    }

    /** Call after returning to Hub (e.g. from a permission prompt or the notification-access
     *  settings page) to re-check both special-access grants and pick up any calls made
     *  since the feed was first loaded. */
    fun refresh() {
        hasCallLogPermission.value = checkCallLogPermission(appContext)
        hasNotificationAccess.value = checkNotificationAccess(appContext)
        hasSmsRole.value = checkSmsRole(appContext)
        hubRepository.refreshCalls()
    }

    fun openReply(message: HubItem.Message) {
        _replyTarget.value = message
    }

    fun closeReply() {
        _replyTarget.value = null
    }

    fun recentMessagesForReply(address: String): List<SmsMessageItem> = smsRepository.recentMessagesFor(address)

    fun onReplySent() {
        _replyTarget.value = null
        // Sending writes straight to the SMS provider (via SmsRepository.recordSent), which
        // hubRepository.items() doesn't observe as a live content-provider flow (same
        // reasoning as refreshCalls() below) - nudge the feed to pick it up immediately
        // rather than waiting for some unrelated flow to re-emit.
        hubRepository.refreshCalls()
    }

    fun toggleFlag(item: HubItem) {
        viewModelScope.launch { hubStateRepository.setFlagged(item.id, !item.flagged) }
    }

    fun setCategory(item: HubItem, category: HubCategory?) {
        viewModelScope.launch { hubStateRepository.setCategory(item.id, category) }
    }

    class Factory(
        private val appContext: Context,
        private val hubRepository: HubRepository,
        private val hubStateRepository: HubStateRepository,
        private val smsRepository: SmsRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return HubViewModel(appContext, hubRepository, hubStateRepository, smsRepository) as T
        }
    }
}

private fun checkCallLogPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED

private fun checkNotificationAccess(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

/** RoleManager.ROLE_SMS is API 29+; API 26-28 falls back to the legacy "default SMS
 *  package" check, the same signal the pre-RoleManager default-app picker itself used. */
private fun checkSmsRole(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        return roleManager?.isRoleHeld(RoleManager.ROLE_SMS) == true
    }
    return Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
}
