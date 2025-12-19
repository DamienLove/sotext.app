package com.pulselink.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pulselink.R
import com.pulselink.data.sms.SmsThreadItem
import com.pulselink.domain.model.ThemePreferences
import com.pulselink.util.parseColorOr

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
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
    dateFormatter: (Long) -> String,
    isBeaconMode: Boolean = false,
    onOpenSettings: () -> Unit = {},
    onOpenPrivate: () -> Unit = {},
    privateThreadIds: Set<Long> = emptySet(),
    showPrivateOnly: Boolean = false,
    onTogglePrivate: (SmsThreadItem, Boolean) -> Unit = { _, _ -> },
    theme: ThemePreferences = ThemePreferences()
) {
    var filter by rememberSaveable { mutableStateOf(InboxFilter.ALL) }
    val base = if (filter == InboxFilter.ARCHIVED) archivedThreads else threads
    val source = base.filter { thread ->
        val isPrivate = privateThreadIds.contains(thread.threadId)
        if (showPrivateOnly) isPrivate else !isPrivate
    }
    val filtered = source.filter { thread ->
        when (filter) {
            InboxFilter.ALL -> true
            InboxFilter.READ -> !thread.unread
            InboxFilter.UNREAD -> thread.unread
            InboxFilter.ARCHIVED -> true
        }
    }

    val bgModifier = if (theme.appBackgroundGradientStart != null && theme.appBackgroundGradientEnd != null) {
        Modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    parseColorOr(Color.White, theme.appBackgroundGradientStart!!),
                    parseColorOr(Color.White, theme.appBackgroundGradientEnd!!)
                )
            )
        )
    } else {
        Modifier.background(parseColorOr(MaterialTheme.colorScheme.background, theme.backgroundColor))
    }

    Scaffold(
        containerColor = if (theme.appBackgroundGradientStart != null) Color.Transparent else parseColorOr(MaterialTheme.colorScheme.background, theme.backgroundColor),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isBeaconMode) "Beacon Inbox" else "Messages", color = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onTopBarColor)) },
                navigationIcon = {
                    if (!isBeaconMode) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onTopBarColor))
                        }
                    }
                },
                actions = {
                    if (isBeaconMode) {
                        IconButton(onClick = onOpenPrivate) {
                            Icon(Icons.Filled.Lock, contentDescription = "Private inbox", tint = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onTopBarColor))
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onTopBarColor))
                        }
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = parseColorOr(MaterialTheme.colorScheme.surface, theme.topBarColor)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .then(bgModifier) // Apply gradient here to fill size
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TabsRow(
                filter = filter,
                unreadCount = threads.count { it.unread },
                onFilterChange = { filter = it },
                theme = theme
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filtered.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(bottom = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Inbox,
                                    contentDescription = null,
                                    modifier = Modifier.size(72.dp),
                                    tint = parseColorOr(MaterialTheme.colorScheme.onSurfaceVariant, theme.onBackground).copy(alpha = 0.2f)
                                )
                                Text(
                                    text = "No messages here yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = parseColorOr(MaterialTheme.colorScheme.onSurfaceVariant, theme.onBackground).copy(alpha = 0.6f)
                                )
                            }
                        }
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
                        isArchiveFilter = filter == InboxFilter.ARCHIVED,
                        isPrivate = privateThreadIds.contains(thread.threadId),
                        onTogglePrivate = { makePrivate -> onTogglePrivate(thread, makePrivate) },
                        theme = theme
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun ThreadRow(
    thread: SmsThreadItem,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onDelete: () -> Unit,
    dateFormatter: (Long) -> String,
    isArchiveFilter: Boolean,
    isPrivate: Boolean,
    onTogglePrivate: (Boolean) -> Unit,
    theme: ThemePreferences
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
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = { onTogglePrivate(!isPrivate) }
                    ),
                tonalElevation = 1.dp,
                shape = RoundedCornerShape(14.dp),
                color = parseColorOr(MaterialTheme.colorScheme.surface, theme.backgroundColor) // Use surface or BG? Or a slightly lighter shade if BG is dark?
                // Actually, Surface defaults to surface color. If BG is custom, we might want this to be custom too or transparent?
                // For simplicity, let's keep it tonal or slightly varied if needed, or just follow BG + onBG.
                // Let's make it transparent so it blends with scaffold BG, or keep elevation.
                // If the user sets BG to Black, Tonal Elevation 1.dp will make it Dark Grey. That is good.
                // But we need to ensure text color matches onBackground.
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvatarCircle(text = displayName, theme = theme)
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
                                overflow = TextOverflow.Ellipsis,
                            color = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onBackground),
                            fontSize = MaterialTheme.typography.titleMedium.fontSize * theme.fontScale
                            )
                            Text(
                                text = dateFormatter(thread.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                            color = parseColorOr(MaterialTheme.colorScheme.onSurfaceVariant, theme.timestampColor ?: theme.onBackground).copy(alpha = 0.7f)
                            )
                        }
                        if (!number.isNullOrBlank() && number != displayName) {
                            Text(
                                text = number,
                                style = MaterialTheme.typography.bodySmall,
                                color = parseColorOr(MaterialTheme.colorScheme.onSurfaceVariant, theme.onBackground).copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = thread.snippet.ifBlank { "No preview available." },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        color = parseColorOr(MaterialTheme.colorScheme.onSurfaceVariant, theme.onBackground).copy(alpha = 0.8f),
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize * theme.fontScale
                        )
                        if (thread.unread) {
                            Spacer(modifier = Modifier.height(4.dp))
                            UnreadPill()
                        }
                        if (isPrivate) {
                            Spacer(modifier = Modifier.height(4.dp))
                            PrivatePill()
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun AvatarCircle(text: String, theme: ThemePreferences) {
    val initial = text.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(parseColorOr(MaterialTheme.colorScheme.primaryContainer, theme.bubbleOutgoing)), // Reuse outgoing bubble color for avatar bg? Or Primary?
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = parseColorOr(MaterialTheme.colorScheme.onPrimaryContainer, theme.onBubbleOutgoing),
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

@Composable
private fun PrivatePill() {
    Surface(
        color = Color(0xFF2C2C2E).copy(alpha = 0.12f),
        shape = CircleShape
    ) {
        Text(
            text = "Private",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
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
private fun TabsRow(
    filter: InboxFilter,
    unreadCount: Int,
    onFilterChange: (InboxFilter) -> Unit,
    theme: ThemePreferences
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TabText(label = "All Messages", selected = filter == InboxFilter.ALL, theme = theme) {
            onFilterChange(InboxFilter.ALL)
        }
        TabText(label = "Read", selected = filter == InboxFilter.READ, theme = theme) {
            onFilterChange(InboxFilter.READ)
        }
        TabText(label = "Unread${if (unreadCount > 0) " ($unreadCount)" else ""}", selected = filter == InboxFilter.UNREAD, theme = theme) {
            onFilterChange(InboxFilter.UNREAD)
        }
        TabText(label = "Archived", selected = filter == InboxFilter.ARCHIVED, theme = theme) {
            onFilterChange(InboxFilter.ARCHIVED)
        }
    }
}

@Composable
private fun TabText(label: String, selected: Boolean, theme: ThemePreferences, onClick: () -> Unit) {
    val selectedColor = parseColorOr(MaterialTheme.colorScheme.primary, theme.primaryColor)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(role = Role.Tab) { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) parseColorOr(MaterialTheme.colorScheme.onBackground, theme.onBackground) else parseColorOr(MaterialTheme.colorScheme.onSurfaceVariant, theme.onBackground).copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth(0.8f)
                .background(if (selected) selectedColor else Color.Transparent)
        )
    }
}

private enum class InboxFilter { ALL, READ, UNREAD, ARCHIVED }
