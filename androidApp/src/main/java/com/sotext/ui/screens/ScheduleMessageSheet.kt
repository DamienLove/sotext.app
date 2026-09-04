package com.sotext.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sotext.data.scheduled.ScheduledMessageDispatcher
import com.sotext.domain.model.RecurrenceFrequency
import com.sotext.domain.model.RecurrenceRule
import com.sotext.domain.model.ScheduledAttachment
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.UUID

/**
 * Shared sheet for both "schedule a new message" and "edit an existing schedule" - the same
 * confirm flow both the long-press-Send menu, the composer's schedule icon, and an AI schedule
 * suggestion open into, so there is exactly one path that creates/updates a
 * [com.sotext.domain.model.ScheduledMessage] row.
 *
 * [occurrenceKey] identifies where attachments picked in this session get copied
 * ([com.sotext.data.scheduled.ScheduledMessageDispatcher.scheduledAttachmentDir]) - the caller
 * generates a fresh UUID for a brand-new schedule (before the Room row/id exists) or passes the
 * existing row's occurrenceKey when editing, so a copy started here always lands in the directory
 * the dispatcher will actually look in at send time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleMessageSheet(
    sheetState: SheetState,
    occurrenceKey: String,
    initialBody: String,
    initialScheduledForUtcMillis: Long?,
    initialRecurrence: RecurrenceRule?,
    initialAttachments: List<ScheduledAttachment> = emptyList(),
    isEditing: Boolean,
    bodyEditable: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (body: String, scheduledForUtcMillis: Long, recurrence: RecurrenceRule?, attachments: List<ScheduledAttachment>) -> Unit
) {
    val context = LocalContext.current
    val zone = remember { ZoneId.systemDefault() }
    var body by remember { mutableStateOf(initialBody) }
    var attachments by remember { mutableStateOf(initialAttachments) }
    val attachmentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        // Copy-on-schedule: picker-granted content:// URIs are not guaranteed to survive until a
        // future send time, so the file is copied into app-owned storage immediately.
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)?.ifBlank { "bin" } ?: "bin"
        val dir = ScheduledMessageDispatcher.scheduledAttachmentDir(context, occurrenceKey).apply { mkdirs() }
        val destFile = File(dir, "${UUID.randomUUID()}.$ext")
        val copied = runCatching {
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output -> input.copyTo(output) }
            }
        }.isSuccess
        if (copied) {
            attachments = attachments + ScheduledAttachment(
                path = destFile.absolutePath,
                mimeType = mimeType,
                displayName = destFile.name
            )
        }
    }

    val defaultTime = remember { System.currentTimeMillis() + DEFAULT_LEAD_TIME_MS }
    var scheduledForUtcMillis by remember {
        mutableStateOf(initialScheduledForUtcMillis ?: defaultTime)
    }
    var frequency by remember {
        mutableStateOf(initialRecurrence?.frequency)
    }
    var recurrenceMenuExpanded by remember { mutableStateOf(false) }

    val zonedDateTime = remember(scheduledForUtcMillis) {
        Instant.ofEpochMilli(scheduledForUtcMillis).atZone(zone)
    }
    val dateLabel = remember(scheduledForUtcMillis) {
        zonedDateTime.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))
    }
    val timeLabel = remember(scheduledForUtcMillis) {
        zonedDateTime.format(DateTimeFormatter.ofPattern("h:mm a"))
    }
    val isInPast = scheduledForUtcMillis <= System.currentTimeMillis()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                text = if (isEditing) "Edit scheduled message" else "Schedule message",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (bodyEditable) {
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Message") },
                    minLines = 2,
                    maxLines = 5
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (attachments.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(attachments, key = { it.path }) { attachment ->
                        AssistChip(
                            onClick = {},
                            label = { Text(attachment.displayName, maxLines = 1) },
                            trailingIcon = {
                                IconButton(
                                    modifier = Modifier.height(20.dp),
                                    onClick = {
                                        runCatching { File(attachment.path).delete() }
                                        attachments = attachments.filterNot { it.path == attachment.path }
                                    }
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove attachment")
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = { attachmentPicker.launch("*/*") }) {
                    Icon(Icons.Filled.AttachFile, contentDescription = "Attach file")
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = scheduledForUtcMillis }
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val updated = zonedDateTime
                                    .withYear(year)
                                    .withMonth(month + 1)
                                    .withDayOfMonth(dayOfMonth)
                                scheduledForUtcMillis = updated.toInstant().toEpochMilli()
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                ) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text(dateLabel)
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = scheduledForUtcMillis }
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                val updated = zonedDateTime.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
                                scheduledForUtcMillis = updated.toInstant().toEpochMilli()
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            false
                        ).show()
                    }
                ) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text(timeLabel)
                }
            }

            if (isInPast) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pick a time in the future.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { recurrenceMenuExpanded = true }) {
                    Icon(Icons.Filled.Repeat, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text(frequency?.let { recurrenceLabel(it) } ?: "Doesn't repeat")
                }
                DropdownMenu(
                    expanded = recurrenceMenuExpanded,
                    onDismissRequest = { recurrenceMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Doesn't repeat") },
                        onClick = { frequency = null; recurrenceMenuExpanded = false }
                    )
                    listOf(
                        RecurrenceFrequency.DAILY,
                        RecurrenceFrequency.WEEKLY,
                        RecurrenceFrequency.MONTHLY,
                        RecurrenceFrequency.YEARLY
                    ).forEach { option ->
                        DropdownMenuItem(
                            text = { Text(recurrenceLabel(option)) },
                            onClick = { frequency = option; recurrenceMenuExpanded = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    enabled = body.isNotBlank() && !isInPast,
                    onClick = {
                        val rule = frequency?.let { freq ->
                            val dayOfMonth = if (freq == RecurrenceFrequency.MONTHLY) zonedDateTime.dayOfMonth else null
                            RecurrenceRule(frequency = freq, dayOfMonth = dayOfMonth)
                        }
                        onConfirm(body, scheduledForUtcMillis, rule, attachments)
                    }
                ) {
                    Text(if (isEditing) "Save" else "Schedule")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun recurrenceLabel(frequency: RecurrenceFrequency): String = when (frequency) {
    RecurrenceFrequency.DAILY -> "Daily"
    RecurrenceFrequency.WEEKLY -> "Weekly"
    RecurrenceFrequency.MONTHLY -> "Monthly"
    RecurrenceFrequency.YEARLY -> "Yearly"
    RecurrenceFrequency.CUSTOM -> "Custom"
}

private const val DEFAULT_LEAD_TIME_MS = 60 * 60 * 1000L // default to "an hour from now"
