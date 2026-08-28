package com.vibelauncher.app.ui.home.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType

private val US_ZIP_REGEX = Regex("^\\d{5}$")
private val CA_POSTAL_REGEX = Regex("^[A-Z]\\d[A-Z]\\d[A-Z]\\d$")

/** Accepts either a US 5-digit zip or a Canadian postal code (space optional, e.g. both
 *  "K1A 0B1" and "K1A0B1" are valid). */
private fun isValidPostalCode(raw: String): Boolean {
    val compact = raw.replace(" ", "").uppercase()
    return US_ZIP_REGEX.matches(compact) || CA_POSTAL_REGEX.matches(compact)
}

@Composable
fun ZipCodeDialog(
    currentZipCode: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(currentZipCode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Weather location") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { input ->
                    val filtered = input.filter { c -> c.isLetterOrDigit() || c == ' ' }.uppercase()
                    if (filtered.length <= 7) value = filtered
                },
                label = { Text("Zip / postal code") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(value) },
                enabled = isValidPostalCode(value)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
