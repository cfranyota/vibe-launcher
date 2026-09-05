package com.vibelauncher.app.data.hub

import kotlinx.serialization.Serializable

@Serializable
enum class HubCategory { WORK, PERSONAL }

/** Per-item flag/category, keyed by [itemId] - see HubItem.kt for how itemId is derived
 *  for each source type (SMS/call = real content-provider _ID; notification = a best-effort
 *  content hash, since dismissed notifications have no stable long-term ID). */
@Serializable
data class HubItemFlagState(
    val itemId: String,
    val flagged: Boolean = false,
    val category: HubCategory? = null
)
