package com.vibelauncher.app.ui.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.ui.graphics.vector.ImageVector
import com.vibelauncher.app.model.BuiltInAction

/** Fixed per-category glyph - stays the same regardless of which app a tile is assigned to. */
fun builtInIcon(action: BuiltInAction): ImageVector = when (action) {
    BuiltInAction.NOTE -> Icons.Outlined.Notes
    BuiltInAction.EVENT -> Icons.Outlined.CalendarMonth
    BuiltInAction.TIMER -> Icons.Outlined.Timer
    BuiltInAction.TODO -> Icons.Outlined.Checklist
    BuiltInAction.CALL -> Icons.Outlined.Phone
    BuiltInAction.MESSAGE -> Icons.Outlined.Sms
    BuiltInAction.CAMERA -> Icons.Outlined.CameraAlt
    BuiltInAction.MEMO -> Icons.Outlined.Mic
}
