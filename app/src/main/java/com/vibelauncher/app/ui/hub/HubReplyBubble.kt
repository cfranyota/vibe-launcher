package com.vibelauncher.app.ui.hub

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.vibelauncher.app.data.sms.SmsMessageItem
import com.vibelauncher.app.data.sms.SmsRepository
import com.vibelauncher.app.ui.theme.LauncherBlack
import com.vibelauncher.app.ui.theme.LauncherMutedGray
import com.vibelauncher.app.ui.theme.LauncherWhite
import com.vibelauncher.app.ui.theme.LocalAccentColor
import com.vibelauncher.app.util.IntentDefaults
import kotlinx.coroutines.launch

/** Quick inline reply, without leaving Hub - mirrors NoteBubble's half-page bottom-sheet
 *  shape (scrim + slide-up Surface). Shows the last few messages exchanged with [address]
 *  as read-only context above the input. */
@Composable
fun HubReplyBubble(
    address: String,
    contactName: String?,
    recentMessages: List<SmsMessageItem>,
    onSent: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val smsRepository = remember { SmsRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    var body by remember { mutableStateOf("") }
    var hasSmsPermission by remember {
        mutableStateOf(androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED)
    }

    fun send() {
        if (body.isBlank()) return
        val sent = IntentDefaults.sendSmsDirect(context, address, body)
        if (sent) {
            coroutineScope.launch {
                smsRepository.recordSent(address, body)
                onSent()
            }
        } else {
            Toast.makeText(context, "Couldn't send message", Toast.LENGTH_SHORT).show()
        }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasSmsPermission = granted
        if (granted) send()
    }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
        keyboard?.show()
    }

    BackHandler(enabled = true) { onDismiss() }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(LauncherBlack.copy(alpha = .55f))
                .clickable(onClick = onDismiss)
        )

        AnimatedVisibility(
            visible = true,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(animationSpec = tween(220), initialOffsetY = { it }) + fadeIn(tween(220)),
            exit = slideOutVertically(animationSpec = tween(200), targetOffsetY = { it }) + fadeOut(tween(200))
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(contactName ?: address, color = LauncherWhite, style = MaterialTheme.typography.labelLarge)
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, "Close", tint = LauncherMutedGray)
                        }
                    }

                    recentMessages.forEach { message ->
                        Text(
                            text = message.body,
                            color = LauncherMutedGray,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .border(1.dp, LauncherMutedGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        )
                    }

                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = body,
                            onValueChange = { body = it },
                            placeholder = { Text("message", color = LauncherMutedGray) },
                            singleLine = false,
                            modifier = Modifier.weight(1f).focusRequester(focusRequester),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (hasSmsPermission) send() else smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                            })
                        )
                        FilledIconButton(
                            onClick = { if (hasSmsPermission) send() else smsPermissionLauncher.launch(Manifest.permission.SEND_SMS) },
                            enabled = body.isNotBlank(),
                            modifier = Modifier.padding(start = 8.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = LocalAccentColor.current,
                                contentColor = LauncherWhite
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, "Send")
                        }
                    }
                }
            }
        }
    }
}
