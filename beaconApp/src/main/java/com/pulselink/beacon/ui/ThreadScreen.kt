package com.pulselink.beacon.ui

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pulselink.beacon.data.SmsMessageItem
import com.pulselink.beacon.ui.ads.NativeAdCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadScreen(
    address: String,
    messages: List<SmsMessageItem>,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onDeleteThread: () -> Unit,
) {
    var draft by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(address, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onDeleteThread) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete thread")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(message = msg)
                }
                item {
                    if (messages.size > 3) {
                        NativeAdCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(40.dp)) }
            }

            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    TextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Write your message") }
                    )
                    IconButton(
                        onClick = {
                            val text = draft.trim()
                            if (text.isNotEmpty()) {
                                onSend(text)
                                draft = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: SmsMessageItem) {
    val isOutgoing = message.outgoing
    val background = if (isOutgoing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val alignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Surface(
            color = background,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
                if (message.isMms && message.mediaParts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    message.mediaParts.filter { it.dataUri != null && it.contentType.startsWith("image") }
                        .forEach { part ->
                            AsyncImage(
                                model = part.dataUri,
                                contentDescription = "MMS image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                        }
                }
                Text(
                    text = DateUtils.getRelativeTimeSpanString(
                        message.timestamp,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    ).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Light,
                    color = Color.DarkGray
                )
            }
        }
    }
}
