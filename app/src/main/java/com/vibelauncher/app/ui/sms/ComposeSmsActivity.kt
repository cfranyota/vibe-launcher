package com.vibelauncher.app.ui.sms

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vibelauncher.app.data.sms.SmsRepository
import com.vibelauncher.app.ui.theme.LauncherBlack
import com.vibelauncher.app.ui.theme.LauncherWhite
import com.vibelauncher.app.util.IntentDefaults

/** Minimal standalone compose target for ACTION_SEND/ACTION_SENDTO sms:/smsto:/mms:/mmsto:
 *  intents - the system-facing "compose a text" screen every default SMS app must provide.
 *  Not Vibe Bar's own '@' flow (that stays home-screen-only); this is what other apps'
 *  "share to Messages"/"text this number" actions land on. */
class ComposeSmsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialRecipient = intent?.data?.schemeSpecificPart?.substringBefore('?') ?: ""
        val initialBody = intent?.getStringExtra(Intent.EXTRA_TEXT) ?: ""

        setContent {
            MaterialTheme {
                Surface(color = LauncherBlack, modifier = Modifier.fillMaxSize()) {
                    var recipient by remember { mutableStateOf(initialRecipient) }
                    var body by remember { mutableStateOf(initialBody) }

                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("New message", color = LauncherWhite, style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(
                            value = recipient,
                            onValueChange = { recipient = it },
                            label = { Text("To") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = body,
                            onValueChange = { body = it },
                            label = { Text("Message") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { sendAndFinish(recipient, body) },
                            enabled = recipient.isNotBlank() && body.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = LauncherWhite, contentColor = LauncherBlack)
                        ) { Text("Send") }
                    }
                }
            }
        }
    }

    private fun sendAndFinish(recipient: String, body: String) {
        val sent = IntentDefaults.sendSmsDirect(this, recipient, body)
        if (sent) {
            SmsRepository(applicationContext).recordSent(recipient, body)
            finish()
        } else {
            Toast.makeText(this, "Couldn't send message", Toast.LENGTH_SHORT).show()
        }
    }
}
