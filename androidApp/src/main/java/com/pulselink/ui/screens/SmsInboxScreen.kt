package com.pulselink.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Markunread
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulselink.data.sms.SmsThreadItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsInboxScreen(
    threads: List<SmsThreadItem>,
    onOpenThread: (SmsThreadItem) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Messages") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (threads.isEmpty()) {
                item {
                    Text(
                        text = "No SMS threads found.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            items(threads) { thread ->
                ThreadRow(thread = thread, onClick = { onOpenThread(thread) })
            }
        }
    }
}

@Composable
private fun ThreadRow(thread: SmsThreadItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = thread.address.ifBlank { "Unknown" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (thread.unread) FontWeight.Bold else FontWeight.SemiBold
        )
        Text(
            text = thread.snippet,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2
        )
        Text(
            text = "∙ ${thread.timestamp}",
            style = MaterialTheme.typography.labelSmall
        )
    }
}
