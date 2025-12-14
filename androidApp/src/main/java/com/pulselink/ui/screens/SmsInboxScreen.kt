package com.pulselink.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pulselink.data.sms.SmsThreadItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsInboxScreen(
    threads: List<SmsThreadItem>,
    archivedThreads: List<SmsThreadItem>,
    onOpenThread: (SmsThreadItem) -> Unit,
    onBack: () -> Unit,
    onArchiveThread: (SmsThreadItem) -> Unit = {},
    onUnarchiveThread: (SmsThreadItem) -> Unit = {},
    onDeleteThread: (SmsThreadItem) -> Unit = {},
    modifier: Modifier = Modifier,
    dateFormatter: (Long) -> String
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(InboxFilter.ALL) }
    val source = if (filter == InboxFilter.ARCHIVED) archivedThreads else threads
    val filtered = source.filter { thread ->
        val target = "${thread.address} ${thread.snippet}".lowercase()
        val matchesQuery = query.isBlank() || target.contains(query.lowercase())
        val matchesFilter = when (filter) {
            InboxFilter.ALL -> true
            InboxFilter.READ -> !thread.unread
            InboxFilter.UNREAD -> thread.unread
            InboxFilter.ARCHIVED -> true
        }
        matchesQuery && matchesFilter
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Messages") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search messages or numbers") }
            )

            FilterRow(
                filter = filter,
                unreadCount = threads.count { it.unread },
                onFilterChange = { filter = it }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            text = if (source.isEmpty()) "No SMS threads found." else "No matches for \"$query\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(filtered, key = { it.threadId }) { thread ->
                    ThreadRow(
                        thread = thread,
                        onClick = { onOpenThread(thread) },
                        onArchive = { onArchiveThread(thread) },
                        onUnarchive = { onUnarchiveThread(thread) },
                        onDelete = { onDeleteThread(thread) },
                        dateFormatter = dateFormatter,
                        isArchiveFilter = filter == InboxFilter.ARCHIVED
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThreadRow(
    thread: SmsThreadItem,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onDelete: () -> Unit,
    dateFormatter: (Long) -> String,
    isArchiveFilter: Boolean
) {
    val (displayName, number) = splitDisplay(thread.address)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (isArchiveFilter) onUnarchive() else onArchive()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete(); false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection ?: return@SwipeToDismissBox
            val isDelete = direction == SwipeToDismissBoxValue.EndToStart
            val color = if (isDelete) Color(0xFFE84A4A) else Color(0xFF5BC174)
            val label = if (isDelete) "Delete" else if (isArchiveFilter) "Unarchive" else "Archive"
            val icon = if (isDelete) Icons.Filled.Delete else Icons.Filled.Archive
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = if (isDelete) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = label, tint = Color.White)
                Spacer(modifier = Modifier.size(8.dp))
                Text(label, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        },
        content = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
                tonalElevation = 1.dp,
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvatarCircle(text = displayName)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displayName.ifBlank { number ?: "Unknown" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (thread.unread) FontWeight.Bold else FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = dateFormatter(thread.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!number.isNullOrBlank() && number != displayName) {
                            Text(
                                text = number,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = thread.snippet.ifBlank { "No preview available." },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (thread.unread) {
                            Spacer(modifier = Modifier.height(4.dp))
                            UnreadPill()
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun AvatarCircle(text: String) {
    val initial = text.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun UnreadPill() {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        shape = CircleShape
    ) {
        Text(
            text = "Unread",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private fun splitDisplay(address: String): Pair<String, String?> {
    val parts = address.split(" Ł ", limit = 2)
    return when (parts.size) {
        2 -> parts[0] to parts[1]
        else -> address to null
    }
}

@Composable
private fun FilterRow(
    filter: InboxFilter,
    unreadCount: Int,
    onFilterChange: (InboxFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = filter == InboxFilter.ALL,
            onClick = { onFilterChange(InboxFilter.ALL) },
            label = { Text("All") }
        )
        FilterChip(
            selected = filter == InboxFilter.READ,
            onClick = { onFilterChange(InboxFilter.READ) },
            label = { Text("Read") }
        )
        FilterChip(
            selected = filter == InboxFilter.UNREAD,
            onClick = { onFilterChange(InboxFilter.UNREAD) },
            label = { Text("Unread${if (unreadCount > 0) " ($unreadCount)" else ""}") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
        )
        FilterChip(
            selected = filter == InboxFilter.ARCHIVED,
            onClick = { onFilterChange(InboxFilter.ARCHIVED) },
            label = { Text("Archived") }
        )
    }
}

private enum class InboxFilter { ALL, READ, UNREAD, ARCHIVED }
