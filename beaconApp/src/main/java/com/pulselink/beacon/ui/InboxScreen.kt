package com.pulselink.beacon.ui

import android.provider.Telephony
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import com.pulselink.beacon.data.SmsMessageItem
import com.pulselink.beacon.data.SmsThreadItem
import com.pulselink.beacon.data.ThemePalette
import com.pulselink.beacon.ui.ads.NativeAdCard
import com.pulselink.beacon.R
import com.pulselink.ui.theme.Layout // Use DesignSystem tokens
import com.pulselink.ui.theme.Spacing
import com.pulselink.ui.theme.Gradients
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    threads: List<SmsThreadItem>,
    theme: ThemePalette,
    searchState: SearchResultState,
    isDefaultSms: Boolean,
    isCheckingDefaultSms: Boolean,
    missingPermissions: List<String>,
    onRequestPermissions: () -> Unit,
    onRequestDefault: () -> Unit,
    onRefreshDefaultStatus: () -> Unit,
    onOpenThread: (Long, String) -> Unit,
    onCompose: () -> Unit,
    onDeleteThread: (Long) -> Unit,
    onRefresh: () -> Unit,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onCustomize: () -> Unit,
    onOpenNotifications: () -> Unit,
    notificationsEnabled: Boolean,
    notificationsSilent: Boolean,
    onOpenNotificationSettings: () -> Unit,
) {
    val host = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var filter by rememberSaveable { mutableStateOf(InboxFilter.ALL) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var navigatedFromSearch by remember { mutableStateOf(false) }
    val iconTint = theme.accentColor

    val filtered = remember(filter, threads) {
        threads.filter { thread ->
            when (filter) {
                InboxFilter.ALL -> true
                InboxFilter.READ -> !thread.unread
                InboxFilter.UNREAD -> thread.unread
                InboxFilter.ARCHIVED -> false // Beacon doesn't support archived yet
            }
        }
    }
    val unreadCount = remember(threads) { threads.count { it.unread } }
    val mutedTint = theme.frameColor.copy(alpha = 0.7f)
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val beaconIconAlpha = (1f - scrollBehavior.state.collapsedFraction).coerceIn(0f, 1f)

    LaunchedEffect(searchState) {
        if (searchState is SearchResultState.Contact && !navigatedFromSearch) {
            navigatedFromSearch = true
            onOpenThread(searchState.threadId, searchState.address)
            onClearSearch()
        } else if (searchState !is SearchResultState.Contact) {
            navigatedFromSearch = false
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Beacon Inbox",
                        fontWeight = FontWeight.Bold,
                        color = theme.frameColor
                    )
                },
                navigationIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.beacon_logo),
                        contentDescription = "Beacon",
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .padding(start = Spacing.medium)
                            .size(32.dp)
                            .alpha(beaconIconAlpha)
                    )
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = iconTint)
                    }
                    IconButton(onClick = onOpenNotifications) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = "Notifications", tint = iconTint)
                    }
                    IconButton(onClick = onCustomize) {
                        Icon(Icons.Default.ColorLens, contentDescription = "Customize", tint = iconTint)
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = theme.inboxBackgroundColor,
                    titleContentColor = theme.frameColor,
                    actionIconContentColor = iconTint
                ),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCompose,
                containerColor = theme.accentColor,
                contentColor = theme.inboxBackgroundColor, // High contrast
                shape = RoundedCornerShape(Layout.buttonCornerRadius)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "New message"
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        snackbarHost = { SnackbarHost(host) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(theme.inboxBackgroundColor)
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                    if (it.isBlank()) onClearSearch()
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = mutedTint) },
                trailingIcon = {
                    if (searchText.isNotBlank()) {
                        IconButton(onClick = {
                            searchText = ""
                            onClearSearch()
                        }) { Icon(Icons.Default.Clear, contentDescription = "Clear", tint = mutedTint) }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(Layout.inputCornerRadius),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.medium, vertical = Spacing.small),
                placeholder = { Text("Search contacts or messages") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.accentColor,
                    unfocusedBorderColor = theme.frameColor.copy(alpha = 0.2f),
                    focusedContainerColor = theme.inboxBackgroundColor.copy(alpha = 0.5f),
                    unfocusedContainerColor = theme.inboxBackgroundColor.copy(alpha = 0.5f)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onSearch(searchText)
                    }
                )
            )

            TabsRow(
                filter = filter,
                unreadCount = unreadCount,
                onFilterChange = { filter = it },
                theme = theme
            )

            AnimatedContent(targetState = searchState, label = "search_results") { state ->
                when (state) {
                    is SearchResultState.Messages -> SearchResults(
                        hits = state.hits,
                        theme = theme,
                        onOpenThread = onOpenThread
                    )
                    SearchResultState.Empty -> Text(
                        "No matches. Try a contact name or phrase.",
                        modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.small),
                        style = MaterialTheme.typography.bodyMedium,
                        color = mutedTint
                    )
                    SearchResultState.Searching -> Text(
                        "Searching…",
                        modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.small),
                        style = MaterialTheme.typography.bodyMedium,
                        color = mutedTint
                    )
                    else -> Unit
                }
            }

            AnimatedVisibility(visible = !notificationsEnabled || notificationsSilent) {
                Surface(
                    shape = RoundedCornerShape(Layout.cardCornerRadius),
                    color = theme.inboxBackgroundColor.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.frameColor.copy(alpha = 0.1f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.medium, vertical = Spacing.small)
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.medium),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = iconTint)
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = if (!notificationsEnabled) "Message notifications are off" else "Message alerts are silent",
                                fontWeight = FontWeight.SemiBold,
                                color = theme.frameColor
                            )
                            Text(
                                text = if (!notificationsEnabled) {
                                    "Turn on notifications so Beacon can alert you."
                                } else {
                                    "Enable sound or vibration for incoming texts."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = mutedTint
                            )
                        }
                        OutlinedButton(onClick = onOpenNotificationSettings) {
                            Text("Open")
                        }
                    }
                }
            }

            if (missingPermissions.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(Layout.cardCornerRadius),
                    color = theme.inboxBackgroundColor.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.frameColor.copy(alpha = 0.1f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.medium, vertical = Spacing.small)
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.medium),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = iconTint)
                        Column(Modifier.weight(1f)) {
                            Text("Permissions needed", fontWeight = FontWeight.SemiBold, color = theme.frameColor)
                            Text(
                                "Grant SMS permission so Beacon can read and show messages.",
                                style = MaterialTheme.typography.bodySmall,
                                color = mutedTint
                            )
                        }
                        OutlinedButton(onClick = onRequestPermissions) {
                            Text("Grant")
                        }
                    }
                }
            }

            if (!isDefaultSms || isCheckingDefaultSms) {
                Surface(
                    shape = RoundedCornerShape(Layout.cardCornerRadius),
                    color = theme.inboxBackgroundColor.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.frameColor.copy(alpha = 0.1f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.medium, vertical = Spacing.small)
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.medium),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Sms, contentDescription = null, tint = iconTint)
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (isCheckingDefaultSms) "Checking default SMS status..." else "Set as default SMS",
                                fontWeight = FontWeight.SemiBold,
                                color = theme.frameColor
                            )
                            Text(
                                "Required for receiving texts and showing notifications.",
                                style = MaterialTheme.typography.bodySmall,
                                color = mutedTint
                            )
                        }
                        if (isCheckingDefaultSms) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp), color = iconTint)
                        } else {
                            OutlinedButton(onClick = onRequestDefault) {
                                Text("Set")
                            }
                        }
                        OutlinedButton(onClick = onRefreshDefaultStatus, enabled = !isCheckingDefaultSms) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            tint = iconTint.copy(alpha = 0.5f),
                            modifier = Modifier
                                .padding(bottom = Spacing.small)
                                .size(64.dp)
                        )
                        Text(
                            "No messages yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = theme.frameColor
                        )
                        Text(
                            when (filter) {
                                InboxFilter.UNREAD -> "No unread messages."
                                InboxFilter.READ -> "No read messages."
                                else -> "New texts will appear here once Beacon is the default SMS app."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = mutedTint
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    itemsIndexed(filtered, key = { _, item -> item.threadId }) { index, item ->
                        if (index == 3) {
                            NativeAdCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Spacing.small)
                            )
                        }
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    onDeleteThread(item.threadId)
                                    scope.launch { host.showSnackbar("Thread deleted") }
                                }
                                false
                            }
                        )
                        SwipeableThreadRow(
                            thread = item,
                            state = dismissState,
                            theme = theme,
                            onClick = { onOpenThread(item.threadId, item.address) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SearchResults(
    hits: List<SmsMessageItem>,
    theme: ThemePalette,
    onOpenThread: (Long, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.medium, vertical = Spacing.small)
    ) {
        hits.take(5).forEach { msg ->
            Surface(
                shape = RoundedCornerShape(Layout.cardCornerRadius),
                color = theme.inboxBackgroundColor.copy(alpha = 0.8f),
                border = androidx.compose.foundation.BorderStroke(1.dp, theme.frameColor.copy(alpha = 0.1f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.extraSmall)
                    .clickable { onOpenThread(msg.threadId, msg.address) }
            ) {
                Column(Modifier.padding(Spacing.medium)) {
                    Text(
                        text = msg.address.ifBlank { "Unknown sender" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.accentColor
                    )
                    Text(
                        text = msg.body,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        color = theme.frameColor
                    )
                    Text(
                        text = DateUtils.getRelativeTimeSpanString(
                            msg.timestamp,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS
                        ).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.frameColor.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableThreadRow(
    thread: SmsThreadItem,
    state: SwipeToDismissBoxState,
    theme: ThemePalette,
    onClick: () -> Unit
) {
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            val color = if (state.targetValue == SwipeToDismissBoxValue.EndToStart) Color(0xFFE53935) else Color.Transparent
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(Layout.cardCornerRadius))
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (state.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                }
            }
        },
        content = { ThreadRow(thread = thread, theme = theme, onClick = onClick) },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    )
}

@Composable
private fun ThreadRow(thread: SmsThreadItem, theme: ThemePalette, onClick: () -> Unit) {
    // Determine background color based on unread state for "Future Deep" feel
    val backgroundColor = if (thread.unread) {
        theme.accentColor.copy(alpha = 0.12f)
    } else {
        theme.inboxBackgroundColor
    }

    val borderColor = if (thread.unread) {
        theme.accentColor.copy(alpha = 0.3f)
    } else {
        theme.frameColor.copy(alpha = 0.15f)
    }

    Surface(
        shape = RoundedCornerShape(Layout.cardCornerRadius),
        // Use manual border for glass effect instead of tonal elevation
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        color = backgroundColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.medium, vertical = Spacing.medium)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = thread.address.ifBlank { "Unknown" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (thread.unread) FontWeight.Bold else FontWeight.Medium,
                    color = theme.frameColor,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = DateUtils.getRelativeTimeSpanString(
                        thread.timestamp,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    ).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.frameColor.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(Spacing.extraSmall))
            Text(
                text = thread.snippet,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                color = theme.frameColor.copy(alpha = if (thread.unread) 1f else 0.7f)
            )
        }
    }
}

@Composable
private fun TabsRow(
    filter: InboxFilter,
    unreadCount: Int,
    onFilterChange: (InboxFilter) -> Unit,
    theme: ThemePalette
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
            .padding(horizontal = Spacing.medium, vertical = Spacing.small),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TabText(label = "All", selected = filter == InboxFilter.ALL, theme = theme) {
            onFilterChange(InboxFilter.ALL)
        }
        TabText(label = "Read", selected = filter == InboxFilter.READ, theme = theme) {
            onFilterChange(InboxFilter.READ)
        }
        TabText(
            label = "Unread${if (unreadCount > 0) " ($unreadCount)" else ""}",
            selected = filter == InboxFilter.UNREAD,
            theme = theme
        ) {
            onFilterChange(InboxFilter.UNREAD)
        }
    }
}

@Composable
private fun TabText(label: String, selected: Boolean, theme: ThemePalette, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.selectable(
            selected = selected,
            role = Role.Tab,
            onClick = onClick
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) theme.accentColor else theme.frameColor.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(Spacing.extraSmall))

        // Animated indicator simulation
        Box(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth(0.8f)
                .background(if (selected) theme.accentColor else Color.Transparent, RoundedCornerShape(2.dp))
        )
    }
}

private enum class InboxFilter { ALL, READ, UNREAD, ARCHIVED }
