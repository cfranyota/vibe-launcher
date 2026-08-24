package com.vibelauncher.app.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibelauncher.app.data.contacts.ContactResult
import com.vibelauncher.app.ui.theme.CardCornerShape
import com.vibelauncher.app.ui.theme.LauncherMutedGray
import com.vibelauncher.app.ui.theme.LauncherWhite
import com.vibelauncher.app.ui.theme.LocalAccentColor

/** The locked-in-contact chip shown in the input row after selecting a contact for '@'. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CommandChip(label: String, color: Color, contentColor: Color, onClear: () -> Unit) {
    InputChip(
        selected = true,
        onClick = onClear,
        label = { Text(label, maxLines = 1) },
        trailingIcon = { Icon(Icons.Default.Close, "Clear", Modifier.padding(0.dp)) },
        colors = InputChipDefaults.inputChipColors(
            selectedContainerColor = color,
            selectedLabelColor = contentColor,
            selectedTrailingIconColor = contentColor
        ),
        modifier = Modifier.padding(end = 4.dp)
    )
}

@Composable
internal fun SuggestionRow(
    text: String,
    icon: ImageVector? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingContent != null) {
            leadingContent()
        } else if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp), tint = contentColor)
        }
        Text(text, Modifier.padding(start = 12.dp), color = contentColor, maxLines = 1, fontSize = 14.sp)
    }
}

/** A contact match: initial-letter avatar + name + trailing phone label (mobile/main/...).
 *  The top match ([emphasized]) gets a solid accent fill; the rest are flat outlined rows. */
@Composable
internal fun ContactSuggestionRow(contact: ContactResult, emphasized: Boolean, onClick: () -> Unit) {
    val accent = LocalAccentColor.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardCornerShape)
            .background(if (emphasized) accent else Color.Transparent)
            .then(
                if (!emphasized) Modifier.border(1.dp, LauncherMutedGray.copy(alpha = 0.35f), CardCornerShape)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (emphasized) LauncherWhite.copy(alpha = 0.25f) else accent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.name.firstOrNull()?.uppercase() ?: "?",
                color = if (emphasized) LauncherWhite else accent,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = contact.name,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
            color = if (emphasized) LauncherWhite else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
        Text(
            text = contact.phoneLabel,
            color = if (emphasized) LauncherWhite.copy(alpha = 0.8f) else LauncherMutedGray,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
