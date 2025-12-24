package com.pulselink.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pulselink.domain.model.LineInboxMode
import com.pulselink.domain.model.LineSendPreference
import com.pulselink.domain.model.SmsLine

@Composable
fun MultiLineSetupDialog(
    lines: List<SmsLine>,
    selectedMode: LineInboxMode,
    onModeChange: (LineInboxMode) -> Unit,
    selectedDefaultLineId: String?,
    onDefaultLineChange: (String) -> Unit,
    lineSendPreference: LineSendPreference,
    onLineSendPreferenceChange: (LineSendPreference) -> Unit,
    devicePhoneInput: String,
    onDevicePhoneChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val safeLines = lines.ifEmpty { listOf(SmsLine(id = "", phoneNumber = "")) }
    val selectedLine = safeLines.firstOrNull { it.id == selectedDefaultLineId } ?: safeLines.first()
    val modeLabels = mapOf(
        LineInboxMode.COMBINED to "Combined inbox",
        LineInboxMode.PER_LINE to "Per-line inbox"
    )
    val preferenceLabels = mapOf(
        LineSendPreference.LAST_USED to "Last used per conversation",
        LineSendPreference.DEVICE_DEFAULT to "Always this device",
        LineSendPreference.LINE_DEFAULT to "Always selected line"
    )
    val dropdownExpanded = remember { mutableStateOf(false) }
    val preferenceExpanded = remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set up multi-device lines") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Choose how you want your inbox organized and which line to use by default.")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Inbox mode")
                    modeLabels.forEach { (mode, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedMode == mode,
                                onClick = { onModeChange(mode) }
                            )
                            Text(label)
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Default line")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedLine.phoneNumber.ifBlank { "Line" }, modifier = Modifier.weight(1f))
                        TextButton(onClick = { dropdownExpanded.value = true }) {
                            Text("Change")
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded.value,
                            onDismissRequest = { dropdownExpanded.value = false }
                        ) {
                            safeLines.forEachIndexed { index, line ->
                                val label = if (line.phoneNumber.isNotBlank()) {
                                    "Line ${index + 1} | ${line.phoneNumber}"
                                } else {
                                    "Line ${index + 1}"
                                }
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        dropdownExpanded.value = false
                                        onDefaultLineChange(line.id)
                                    }
                                )
                            }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Send preference")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(preferenceLabels[lineSendPreference].orEmpty(), modifier = Modifier.weight(1f))
                        TextButton(onClick = { preferenceExpanded.value = true }) {
                            Text("Change")
                        }
                        DropdownMenu(
                            expanded = preferenceExpanded.value,
                            onDismissRequest = { preferenceExpanded.value = false }
                        ) {
                            preferenceLabels.forEach { (pref, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        preferenceExpanded.value = false
                                        onLineSendPreferenceChange(pref)
                                    }
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = devicePhoneInput,
                    onValueChange = onDevicePhoneChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("This device's phone number") },
                    placeholder = { Text("Optional if not detected") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        }
    )
}

@Composable
fun LineLimitDialog(
    lines: List<SmsLine>,
    onDisableLine: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add another line") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Your plan includes 2 lines. To add more, upgrade your subscription or remove an existing line.")
                lines.forEachIndexed { index, line ->
                    val label = if (line.phoneNumber.isNotBlank()) {
                        "Line ${index + 1} | ${line.phoneNumber}"
                    } else {
                        "Line ${index + 1}"
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onDisableLine(line.id) }) {
                            Text("Remove")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Add another line")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
