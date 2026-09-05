package com.vibelauncher.app.ui.hub

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vibelauncher.app.data.hub.HubCategory
import com.vibelauncher.app.data.hub.HubFilter
import com.vibelauncher.app.data.hub.HubItem
import com.vibelauncher.app.ui.home.components.SuggestionRow
import com.vibelauncher.app.ui.theme.LauncherMutedGray
import com.vibelauncher.app.ui.theme.LauncherWhite
import com.vibelauncher.app.ui.theme.LocalAccentColor
import com.vibelauncher.app.ui.theme.TileCornerShape
import com.vibelauncher.app.ui.theme.settingsTypography
import com.vibelauncher.app.util.IntentDefaults
import java.text.SimpleDateFormat
import java.util.Locale

private val TIME_FORMAT = SimpleDateFormat("MMM d", Locale.getDefault())

/** Unified feed - messages, calls, and notifications. Tap a message row to reply inline;
 *  tap a row's star to flag it; tap the category chip to cycle Work -> Personal -> none. */
@Composable
fun HubScreen(viewModel: HubViewModel, onBack: () -> Unit, onOpenEmailApps: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val replyTarget by viewModel.replyTarget.collectAsState()
    val context = LocalContext.current
    var showSearch by remember { mutableStateOf(false) }

    val callLogPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refresh() }

    val smsRoleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { viewModel.refresh() }

    LaunchedEffect(Unit) { viewModel.refresh() }

    MaterialTheme(typography = settingsTypography()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back", tint = LauncherWhite)
                }
                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(text = "hub", color = LauncherWhite, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = if (uiState.items.isEmpty()) "all caught up" else "${uiState.items.size} items",
                        color = LauncherMutedGray,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                IconButton(onClick = { showSearch = !showSearch; if (!showSearch) viewModel.setSearchQuery("") }) {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = "Search", tint = LauncherWhite)
                }
            }

            if (showSearch) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = { Text("Search hub") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (!uiState.hasSmsRole) {
                SuggestionRowCompat(text = "Become default SMS app to see messages") {
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        context.getSystemService(RoleManager::class.java)?.createRequestRoleIntent(RoleManager.ROLE_SMS)
                    } else {
                        Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                            .putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
                    }
                    if (intent != null) runCatching { smsRoleLauncher.launch(intent) }
                }
            } else {
                SuggestionRowCompat(text = "Switch to Google Messages") {
                    val intent = IntentDefaults.requestSmsRoleFor(IntentDefaults.GOOGLE_MESSAGES_PACKAGE)
                    val resolved = if (intent.resolveActivity(context.packageManager) != null) {
                        intent
                    } else {
                        Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                    }
                    runCatching { smsRoleLauncher.launch(resolved) }
                }
            }
            if (!uiState.hasCallLogPermission) {
                SuggestionRowCompat(text = "Allow call log access") {
                    callLogPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
                }
            }
            if (!uiState.hasNotificationAccess) {
                SuggestionRowCompat(text = "Enable notification access for the full feed") {
                    runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                }
            }

            HubTabRow(selected = uiState.filter, onSelect = viewModel::setFilter)

            if (uiState.filter == HubFilter.EMAIL && uiState.filteredItems.isEmpty()) {
                SuggestionRowCompat(text = "Add your email app") { onOpenEmailApps() }
            }

            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
            ) {
                items(uiState.filteredItems, key = { it.id }) { item ->
                    HubRow(
                        item = item,
                        onOpenReply = { if (item is HubItem.Message) viewModel.openReply(item) },
                        onToggleFlag = { viewModel.toggleFlag(item) },
                        onCycleCategory = {
                            val next = when (item.category) {
                                null -> HubCategory.WORK
                                HubCategory.WORK -> HubCategory.PERSONAL
                                HubCategory.PERSONAL -> null
                            }
                            viewModel.setCategory(item, next)
                        }
                    )
                }
            }
        }
    }

    replyTarget?.let { message ->
        HubReplyBubble(
            address = message.address,
            contactName = message.contactName,
            recentMessages = remember(message.address) { viewModel.recentMessagesForReply(message.address) },
            onSent = viewModel::onReplySent,
            onDismiss = viewModel::closeReply
        )
    }
}

@Composable
private fun SuggestionRowCompat(text: String, onClick: () -> Unit) {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        SuggestionRow(text = text, onClick = onClick)
    }
}

@Composable
private fun HubTabRow(selected: HubFilter, onSelect: (HubFilter) -> Unit) {
    val accent = LocalAccentColor.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HubFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            Column(
                modifier = Modifier.clickable { onSelect(filter) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = filter.name.lowercase(),
                    color = if (isSelected) LauncherWhite else LauncherMutedGray,
                    style = MaterialTheme.typography.labelSmall
                )
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .height(2.dp)
                        .width(if (isSelected) 20.dp else 0.dp)
                        .background(accent)
                )
            }
        }
    }
}

@Composable
private fun HubRow(item: HubItem, onOpenReply: () -> Unit, onToggleFlag: () -> Unit, onCycleCategory: () -> Unit) {
    val (icon, typeLabel, title, preview) = when (item) {
        is HubItem.Call -> HubRowContent(
            Icons.Filled.Call,
            "CALL",
            item.contactName ?: item.number,
            item.callType.name.lowercase()
        )
        is HubItem.NotificationItem -> HubRowContent(
            Icons.Filled.Notifications,
            "APP",
            item.title ?: item.packageName,
            item.text.orEmpty()
        )
        is HubItem.Message -> HubRowContent(
            Icons.Filled.Sms,
            "SMS",
            item.contactName ?: item.address,
            item.body
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LauncherMutedGray.copy(alpha = 0.3f), TileCornerShape)
            .clickable(enabled = item is HubItem.Message, onClick = onOpenReply)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = LauncherMutedGray, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(text = title, color = LauncherWhite, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = preview, color = LauncherMutedGray, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$typeLabel ${TIME_FORMAT.format(item.timestampMillis)}",
                color = LauncherMutedGray,
                style = MaterialTheme.typography.labelSmall
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                item.category?.let { category ->
                    Text(
                        text = if (category == HubCategory.WORK) "W" else "P",
                        color = LocalAccentColor.current,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.clickable(onClick = onCycleCategory).padding(horizontal = 6.dp)
                    )
                } ?: Text(
                    text = "+",
                    color = LauncherMutedGray,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.clickable(onClick = onCycleCategory).padding(horizontal = 6.dp)
                )
                IconButton(onClick = onToggleFlag, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (item.flagged) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = if (item.flagged) "Unflag" else "Flag",
                        tint = if (item.flagged) LocalAccentColor.current else LauncherMutedGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private data class HubRowContent(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val typeLabel: String,
    val title: String,
    val preview: String
)
