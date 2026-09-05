package com.vibelauncher.app.ui.hub

import com.vibelauncher.app.data.hub.HubFilter
import com.vibelauncher.app.data.hub.HubItem
import com.vibelauncher.app.data.hub.matchesFilter

data class HubUiState(
    val items: List<HubItem> = emptyList(),
    val filter: HubFilter = HubFilter.ALL,
    val searchQuery: String = "",
    val hasCallLogPermission: Boolean = false,
    val hasNotificationAccess: Boolean = false,
    val hasSmsRole: Boolean = false
) {
    val filteredItems: List<HubItem>
        get() {
            val byTab = items.filter { it.matchesFilter(filter) }
            if (searchQuery.isBlank()) return byTab
            val q = searchQuery.trim()
            return byTab.filter { item ->
                when (item) {
                    is HubItem.Message -> item.address.contains(q, true) || item.body.contains(q, true) ||
                        item.contactName?.contains(q, true) == true
                    is HubItem.Call -> item.number.contains(q, true) || item.contactName?.contains(q, true) == true
                    is HubItem.NotificationItem -> item.title?.contains(q, true) == true || item.text?.contains(q, true) == true
                }
            }
        }
}
