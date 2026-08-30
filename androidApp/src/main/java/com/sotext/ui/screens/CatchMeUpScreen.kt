package com.sotext.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sotext.data.sms.SmsThreadItem
import com.sotext.domain.model.ThemePreferences
import com.sotext.ui.state.CatchMeUpState
import com.sotext.ui.state.CatchUpCard
import com.sotext.util.parseColorOr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatchMeUpScreen(
    state: CatchMeUpState,
    theme: ThemePreferences,
    dateFormatter: (Long) -> String,
    onBack: () -> Unit,
    onOpenThread: (SmsThreadItem) -> Unit,
    onReply: (SmsThreadItem) -> Unit,
    onMarkHandled: (CatchUpCard) -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Catch Me Up") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state !is CatchMeUpState.Loading) {
                        IconButton(onClick = onRetry) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = parseColorOr(MaterialTheme.colorScheme.surface, theme.topBarColor)
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (state) {
                is CatchMeUpState.Idle, is CatchMeUpState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.padding(top = 12.dp))
                        Text(
                            "Reading your unread conversations...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is CatchMeUpState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.padding(top = 12.dp))
                        OutlinedButton(onClick = onRetry) { Text("Try again") }
                    }
                }
                is CatchMeUpState.Success -> {
                    if (state.isEmpty) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = parseColorOr(MaterialTheme.colorScheme.primary, theme.primaryColor),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.padding(top = 8.dp))
                            Text("You're all caught up", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "No unread or recent conversations need a look right now.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            catchUpSection(
                                title = "Needs your response",
                                cards = state.needsResponse,
                                theme = theme,
                                dateFormatter = dateFormatter,
                                showReply = true,
                                onOpenThread = onOpenThread,
                                onReply = onReply,
                                onMarkHandled = onMarkHandled
                            )
                            catchUpSection(
                                title = "Important updates",
                                cards = state.importantUpdates,
                                theme = theme,
                                dateFormatter = dateFormatter,
                                showReply = false,
                                onOpenThread = onOpenThread,
                                onReply = onReply,
                                onMarkHandled = onMarkHandled
                            )
                            catchUpSection(
                                title = "No action needed",
                                cards = state.noActionNeeded,
                                theme = theme,
                                dateFormatter = dateFormatter,
                                showReply = false,
                                onOpenThread = onOpenThread,
                                onReply = onReply,
                                onMarkHandled = onMarkHandled
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.catchUpSection(
    title: String,
    cards: List<CatchUpCard>,
    theme: ThemePreferences,
    dateFormatter: (Long) -> String,
    showReply: Boolean,
    onOpenThread: (SmsThreadItem) -> Unit,
    onReply: (SmsThreadItem) -> Unit,
    onMarkHandled: (CatchUpCard) -> Unit
) {
    if (cards.isEmpty()) return
    item(key = "header_$title") {
        Text(
            text = "$title (${cards.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    items(cards, key = { "card_${it.thread.threadId}_$title" }) { card ->
        CatchUpCardView(
            card = card,
            theme = theme,
            dateFormatter = dateFormatter,
            showReply = showReply,
            onOpenThread = onOpenThread,
            onReply = onReply,
            onMarkHandled = onMarkHandled
        )
    }
}

@Composable
private fun CatchUpCardView(
    card: CatchUpCard,
    theme: ThemePreferences,
    dateFormatter: (Long) -> String,
    showReply: Boolean,
    onOpenThread: (SmsThreadItem) -> Unit,
    onReply: (SmsThreadItem) -> Unit,
    onMarkHandled: (CatchUpCard) -> Unit
) {
    val container = parseColorOr(MaterialTheme.colorScheme.surfaceVariant, theme.bubbleIncoming)
    val onContainer = parseColorOr(MaterialTheme.colorScheme.onSurfaceVariant, theme.onBubbleIncoming)
    val accent = parseColorOr(MaterialTheme.colorScheme.primary, theme.primaryColor)
    val name = card.contactName?.takeIf { it.isNotBlank() } ?: card.thread.address

    Surface(
        color = container,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = accent
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = onContainer
                    )
                    if (card.result.topic.isNotBlank()) {
                        Text(
                            text = card.result.topic,
                            style = MaterialTheme.typography.labelSmall,
                            color = onContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                Text(
                    text = dateFormatter(card.thread.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = onContainer.copy(alpha = 0.6f)
                )
            }

            if (card.result.summary.isNotBlank()) {
                Text(
                    text = card.result.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onContainer
                )
            }

            val question = card.result.questions.firstOrNull()
            if (!question.isNullOrBlank()) {
                Text(
                    text = "“$question”",
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer.copy(alpha = 0.85f)
                )
            }

            if (!card.result.mentionedWhen.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = card.result.mentionedWhen,
                        style = MaterialTheme.typography.labelSmall,
                        color = onContainer.copy(alpha = 0.75f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { onMarkHandled(card) }) {
                    Icon(Icons.Filled.Done, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mark as handled")
                }
                TextButton(onClick = { onOpenThread(card.thread) }) {
                    Text("Open")
                }
                if (showReply) {
                    Button(
                        onClick = { onReply(card.thread) },
                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reply")
                    }
                }
            }
        }
    }
}
