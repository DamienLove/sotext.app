package com.pulselink.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulselink.domain.model.AlertEvent
import com.pulselink.domain.model.EscalationTier
import com.pulselink.ui.state.PulseLinkUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertHistoryScreen(
    state: PulseLinkUiState,
    onBackClick: () -> Unit,
    onMarkAlertsAsRead: (List<Long>) -> Unit,
    onViewEmergencyMap: () -> Unit
) {
    // Automatically mark visible alerts as read when the screen opens
    LaunchedEffect(state.recentEvents, state.unreadAlertCount) {
        val unreadIds = state.recentEvents.filter { !it.isRead }.map { it.id }
        if (unreadIds.isNotEmpty()) {
            onMarkAlertsAsRead(unreadIds)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alert History") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    if (state.unreadAlertCount > 0) {
                        TextButton(onClick = {
                            val unreadIds = state.recentEvents.filter { !it.isRead }.map { it.id }
                            if (unreadIds.isNotEmpty()) {
                                onMarkAlertsAsRead(unreadIds)
                            }
                        }) {
                            Text("Mark all read")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.recentEvents.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.height(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No recent alerts",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.recentEvents, key = { it.id }) { event ->
                    AlertEventItem(
                        event = event,
                        onViewEmergencyMap = onViewEmergencyMap
                    )
                }
            }
        }
    }
}

@Composable
fun AlertEventItem(
    event: AlertEvent,
    onViewEmergencyMap: () -> Unit
) {
    val tierColor = when (event.tier) {
        EscalationTier.EMERGENCY -> Color(0xFFB91C1C) // Red
        EscalationTier.CHECK_IN -> Color(0xFF059669) // Green
    }

    val timestampStr = DateUtils.getRelativeTimeSpanString(
        event.timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    ).toString()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (event.tier == EscalationTier.EMERGENCY) "EMERGENCY ALERT" else "CHECK-IN",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = tierColor
                )
                Text(
                    text = timestampStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Triggered by: ${event.triggeredBy}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Notified: ${event.contactCount} contacts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (event.tier == EscalationTier.EMERGENCY) {
                        TextButton(onClick = onViewEmergencyMap) {
                            Text("View map")
                        }
                    }
                    if (!event.isRead) {
                        Text(
                            text = "NEW",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
