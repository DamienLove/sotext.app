package com.sotext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.sotext.domain.model.ScheduledMessage
import com.sotext.domain.model.ScheduledMessageStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Distinct, non-bubble visual state for a message that hasn't sent yet - dashed border, clock
 * icon, and inline actions instead of the normal [MessageBubble] chrome, per the "🕐 Scheduled for
 * tomorrow at 9:00 AM [Edit] [Send now] [Cancel]" design. Used both inline in the conversation
 * thread and in the Scheduled hub.
 */
@Composable
fun ScheduledMessageCard(
    message: ScheduledMessage,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit,
    onSendNow: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    val zone = remember(message.timezoneId) {
        runCatching { ZoneId.of(message.timezoneId) }.getOrDefault(ZoneId.systemDefault())
    }
    val label = formatScheduledLabel(message.scheduledForUtcMillis, zone)

    Surface(
        modifier = modifier.widthIn(max = 320.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (message.status) {
                    ScheduledMessageStatus.PROCESSING -> CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp
                    )
                    ScheduledMessageStatus.FAILED -> Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    else -> Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when (message.status) {
                        ScheduledMessageStatus.FAILED -> "Failed to send"
                        ScheduledMessageStatus.PROCESSING -> "Sending…"
                        else -> "Scheduled for $label"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (message.status == ScheduledMessageStatus.FAILED) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                if (message.recurrenceRule != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.Repeat,
                        contentDescription = "Repeats",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message.body,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = if (message.status == ScheduledMessageStatus.FAILED) FontStyle.Italic else FontStyle.Normal
            )
            if (message.status == ScheduledMessageStatus.FAILED && !message.lastError.isNullOrBlank()) {
                Text(
                    text = message.lastError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (message.status != ScheduledMessageStatus.PROCESSING) {
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    if (message.status == ScheduledMessageStatus.FAILED) {
                        TextButton(onClick = onRetry) { Text("Retry") }
                    } else {
                        TextButton(onClick = onEdit) { Text("Edit") }
                        TextButton(onClick = onSendNow) { Text("Send now") }
                    }
                    TextButton(onClick = onCancel) { Text("Cancel") }
                }
            }
        }
    }
}

fun formatScheduledLabel(scheduledForUtcMillis: Long, zone: ZoneId): String {
    val target = Instant.ofEpochMilli(scheduledForUtcMillis).atZone(zone)
    val today = LocalDate.now(zone)
    val timeText = target.format(DateTimeFormatter.ofPattern("h:mm a"))
    return when (target.toLocalDate()) {
        today -> "today at $timeText"
        today.plusDays(1) -> "tomorrow at $timeText"
        else -> target.format(DateTimeFormatter.ofPattern("EEE, MMM d")) + " at $timeText"
    }
}
