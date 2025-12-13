package com.pulselink.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulselink.data.sms.SmsMessageItem
import com.pulselink.domain.model.ThemePreferences
import com.pulselink.util.parseColorOr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsThreadScreen(
    address: String,
    messages: List<SmsMessageItem>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    dateFormatter: (Long) -> String,
    themePreferences: ThemePreferences
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(address.ifBlank { "Conversation" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                MessageBubble(msg, dateFormatter, themePreferences)
            }
        }
    }
}

@Composable
private fun MessageBubble(message: SmsMessageItem, dateFormatter: (Long) -> String, theme: ThemePreferences) {
    val alignment = if (message.outgoing) Alignment.End else Alignment.Start
    val bubbleColor = if (message.outgoing) {
        parseColorOr(MaterialTheme.colorScheme.primaryContainer, theme.bubbleOutgoing)
    } else {
        parseColorOr(MaterialTheme.colorScheme.surfaceVariant, theme.bubbleIncoming)
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = bubbleColor,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = dateFormatter(message.timestamp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
