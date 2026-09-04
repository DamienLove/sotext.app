package com.sotext.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.rememberModalBottomSheetState
import com.sotext.domain.model.ScheduledMessage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Every scheduled message across every conversation, grouped by upcoming date, with a tap-through
 * back to the relevant thread - the "Scheduled" hub called for in the spec.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledMessagesScreen(
    scheduledMessages: List<ScheduledMessage>,
    onBack: () -> Unit,
    onOpenConversation: (ScheduledMessage) -> Unit,
    onCancel: (Long) -> Unit,
    onSendNow: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    onEdit: (
        id: Long,
        body: String,
        scheduledForUtcMillis: Long,
        recurrence: com.sotext.domain.model.RecurrenceRule?,
        attachments: List<com.sotext.domain.model.ScheduledAttachment>
    ) -> Unit
) {
    var editingMessage by remember { mutableStateOf<ScheduledMessage?>(null) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Scheduled") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (scheduledMessages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        modifier = Modifier.padding(bottom = 8.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "No scheduled messages",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Long-press Send on any message to schedule it for later.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val zone = remember { ZoneId.systemDefault() }
            val grouped = remember(scheduledMessages) {
                scheduledMessages
                    .sortedBy { it.scheduledForUtcMillis }
                    .groupBy { Instant.ofEpochMilli(it.scheduledForUtcMillis).atZone(zone).toLocalDate() }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                grouped.forEach { (date, messagesForDate) ->
                    item(key = "header_$date") {
                        Text(
                            text = dateSectionLabel(date, zone),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(messagesForDate, key = { it.id }) { message ->
                        Box(modifier = Modifier.clickable { onOpenConversation(message) }) {
                            ScheduledMessageCard(
                                message = message,
                                modifier = Modifier.fillMaxSize(),
                                onEdit = { editingMessage = message },
                                onSendNow = { onSendNow(message.id) },
                                onCancel = { onCancel(message.id) },
                                onRetry = { onRetry(message.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    val editing = editingMessage
    if (editing != null) {
        ScheduleMessageSheet(
            sheetState = sheetState,
            occurrenceKey = editing.occurrenceKey,
            initialBody = editing.body,
            initialScheduledForUtcMillis = editing.scheduledForUtcMillis,
            initialRecurrence = editing.recurrenceRule,
            initialAttachments = editing.attachments,
            isEditing = true,
            onDismiss = { editingMessage = null },
            onConfirm = { body, scheduledForUtcMillis, recurrence, attachments ->
                onEdit(editing.id, body, scheduledForUtcMillis, recurrence, attachments)
                editingMessage = null
            }
        )
    }
}

private fun dateSectionLabel(date: LocalDate, zone: ZoneId): String {
    val today = LocalDate.now(zone)
    return when (date) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> date.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    }
}
