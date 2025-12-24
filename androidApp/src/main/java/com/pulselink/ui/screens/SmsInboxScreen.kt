package com.pulselink.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import android.text.format.DateUtils
import com.pulselink.R
import com.pulselink.data.sms.SmsThreadItem
import com.pulselink.domain.model.MessageUrgency
import com.pulselink.domain.model.ThemePreferences
import com.pulselink.util.parseColorOr
import com.pulselink.util.splitSmsDisplayAddress
import com.pulselink.ui.state.SearchResultState
import com.pulselink.data.sms.SmsMessageItem

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
    theme: ThemePreferences = ThemePreferences(),
    sectionTitle: String? = null,
    showFilterTabs: Boolean = true,
    banner: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    showSearchBar: Boolean = false,
    searchState: SearchResultState = SearchResultState.Idle,
    onSearch: (String) -> Unit = {},
    onClearSearch: () -> Unit = {},
    onOpenThreadById: (Long, String) -> Unit = { _, _ -> },
    floatingActionButton: @Composable () -> Unit = {},
    lineOptions: List<com.pulselink.domain.model.SmsLine> = emptyList(),
    deviceLineId: String? = null,
    activeLineId: String? = null,
    onSelectLine: (String) -> Unit = {},
    showLinePicker: Boolean = false
) {
    var filter by rememberSaveable { mutableStateOf(InboxFilter.ALL) }
    var searchText by rememberSaveable { mutableStateOf("") }

    val threadKey: (SmsThreadItem) -> String = { thread ->
        val lineKey = thread.lineId ?: deviceLineId ?: "local"
        "$lineKey:${thread.threadId}"
    }
    val gatedThreads = remember(threads, lineOptions, deviceLineId) {
        if (lineOptions.isEmpty() && deviceLineId != null) {
            threads.filter { thread ->
                thread.lineId.isNullOrBlank() || thread.lineId == deviceLineId
            }
        } else {
            threads
        }
    }
    val gatedArchivedThreads = remember(archivedThreads, lineOptions, deviceLineId) {
        if (lineOptions.isEmpty() && deviceLineId != null) {
            archivedThreads.filter { thread ->
                thread.lineId.isNullOrBlank() || thread.lineId == deviceLineId
            }
        } else {
            archivedThreads
        }
    }
    val archivedIds = remember(gatedArchivedThreads, deviceLineId) {
        gatedArchivedThreads.map { threadKey(it) }.toSet()
    }
    val filtered = remember(filter, gatedThreads, gatedArchivedThreads, privateThreadIds, showPrivateOnly) {
        val base = when (filter) {
            InboxFilter.ARCHIVED -> gatedArchivedThreads
            InboxFilter.ALL -> gatedThreads + gatedArchivedThreads
            else -> gatedThreads
        }
        val source = base.filter { thread ->
            val isPrivate = thread.isPrivate || privateThreadIds.contains(thread.threadId)
            if (showPrivateOnly) isPrivate else !isPrivate
        }
        source.filter { thread ->
            when (filter) {
                InboxFilter.ALL -> true
                InboxFilter.READ -> !thread.unread
                InboxFilter.UNREAD -> thread.unread
                InboxFilter.ARCHIVED -> true
            }
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val iconSize = (24f * theme.iconSizeFactor).coerceIn(18f, 34f).dp
    val largeIconSize = (64f * theme.iconSizeFactor).coerceIn(48f, 86f).dp
    val beaconHeaderIconSize = (28f * theme.iconSizeFactor).coerceIn(22f, 38f).dp
    val orderedLines = remember(lineOptions) { lineOptions.sortedBy { it.createdAt } }
    val lineIndexMap = remember(orderedLines) { orderedLines.mapIndexed { index, line -> line.id to index }.toMap() }
    val lineColors = remember(theme, colorScheme) {
        listOf(
            parseColorOr(colorScheme.primary, theme.primaryColor),
            parseColorOr(colorScheme.secondary, theme.secondaryColor),
            parseColorOr(colorScheme.tertiary, theme.bubbleOutgoing),
            parseColorOr(colorScheme.primaryContainer, theme.bubbleIncoming)
        )
    }
    val bgModifier = remember(theme, colorScheme) {
        if (theme.appBackgroundGradientStart != null && theme.appBackgroundGradientEnd != null) {
            Modifier.background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        parseColorOr(Color.White, theme.appBackgroundGradientStart!!),
                        parseColorOr(Color.White, theme.appBackgroundGradientEnd!!)
                    )
                )
            )
        } else {
            Modifier.background(parseColorOr(colorScheme.background, theme.backgroundColor))
        }
    }
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = if (isBeaconMode) {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    } else {
        TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
    }
    val beaconIconAlpha = (1f - scrollBehavior.state.collapsedFraction).coerceIn(0f, 1f)

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = if (theme.appBackgroundGradientStart != null) Color.Transparent else parseColorOr(MaterialTheme.colorScheme.background, theme.backgroundColor),
        topBar = {
            if (isBeaconMode) {
                LargeTopAppBar(
                    title = {
                        Text(
                            "Beacon Inbox",
                            color = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onTopBarColor)
                        )
                    },
                    navigationIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_beacon_inbox),
                            contentDescription = "Beacon",
                            tint = Color.Unspecified,
                            modifier = Modifier
                                .size(beaconHeaderIconSize)
                                .alpha(beaconIconAlpha)
                        )
                    },
                    actions = {
                        IconButton(onClick = onOpenPrivate) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = "Private inbox",
                                tint = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onTopBarColor),
                                modifier = Modifier.size(iconSize)
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onTopBarColor),
                                modifier = Modifier.size(iconSize)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = parseColorOr(MaterialTheme.colorScheme.surface, theme.topBarColor)
                    ),
                    scrollBehavior = scrollBehavior
                )
            } else {
                CenterAlignedTopAppBar(
                    title = { Text("Messages", color = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onTopBarColor)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onTopBarColor),
                                modifier = Modifier.size(iconSize)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = parseColorOr(MaterialTheme.colorScheme.surface, theme.topBarColor)
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        },
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(bgModifier) // Apply gradient here to fill size
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (showSearchBar) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                        if (it.isBlank()) {
                            onClearSearch()
                        } else {
                            onSearch(it)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    placeholder = { Text("Search all messages") },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchText.isNotBlank()) {
                            IconButton(onClick = {
                                searchText = ""
                                onClearSearch()
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { onSearch(searchText) }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = parseColorOr(MaterialTheme.colorScheme.primary, theme.primaryColor),
                        unfocusedBorderColor = parseColorOr(MaterialTheme.colorScheme.onSurfaceVariant, theme.onBackground).copy(alpha = 0.4f),
                        focusedContainerColor = parseColorOr(MaterialTheme.colorScheme.surface, theme.backgroundColor),
                        unfocusedContainerColor = parseColorOr(MaterialTheme.colorScheme.surface, theme.backgroundColor),
                        focusedTextColor = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onBackground),
                        unfocusedTextColor = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onBackground),
                        cursorColor = parseColorOr(MaterialTheme.colorScheme.primary, theme.primaryColor)
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
            }
            if (showLinePicker && orderedLines.isNotEmpty()) {
                LinePickerRow(
                    lines = orderedLines,
                    activeLineId = activeLineId,
                    lineColors = lineColors,
                    theme = theme,
                    onSelectLine = onSelectLine
                )
            }
            banner()
            if (showSearchBar) {
                when (searchState) {
                    is SearchResultState.Searching -> Text(
                        "Searching.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = parseColorOr(MaterialTheme.colorScheme.onSurfaceVariant, theme.onBackground)
                    )
                    is SearchResultState.Empty -> Text(
                        "No matches. Try a contact name or phrase.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = parseColorOr(MaterialTheme.colorScheme.onSurfaceVariant, theme.onBackground)
                    )
                    is SearchResultState.Contact -> SearchContactResult(
                        result = searchState,
                        theme = theme,
                        onOpen = onOpenThreadById
                    )
                    is SearchResultState.Messages -> SearchResults(
                        hits = searchState.hits,
                        theme = theme,
                        onOpen = onOpenThreadById
                    )
                    else -> Unit
                }
            }
            if (sectionTitle != null) {
                Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onBackground)
                )
            }
            val unreadCount = remember(threads) { threads.count { it.unread } }
            if (showFilterTabs) {
                TabsRow(
                    filter = filter,
                    unreadCount = unreadCount,
                    onFilterChange = { filter = it },
                    theme = theme
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filtered.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.alpha(0.6f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Inbox,
                                    contentDescription = null,
                                    modifier = Modifier.size(largeIconSize),
                                    tint = parseColorOr(
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                        theme.onBackground
                                    )
                                )
                                Text(
                                    text = "No messages here yet.",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = parseColorOr(
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                        theme.onBackground
                                    )
                                )
                            }
                        }
                    }
                }
                items(filtered, key = { threadKey(it) }) { thread ->
                    val lineIndex = thread.lineId?.let { lineIndexMap[it] }
                    val lineColor = lineIndex?.let { lineColors[it % lineColors.size] }
                    val isLocalLine = deviceLineId == null ||
                        thread.lineId.isNullOrBlank() ||
                        thread.lineId == deviceLineId
                    ThreadRow(
                        thread = thread,
                        onOpen = onOpenThread,
                        onArchive = onArchiveThread,
                        onUnarchive = onUnarchiveThread,
                        onDelete = onDeleteThread,
                        dateFormatter = dateFormatter,
                        isArchived = archivedIds.contains(threadKey(thread)),
                        isPrivate = thread.isPrivate || privateThreadIds.contains(thread.threadId),
                        onTogglePrivate = onTogglePrivate,
                        theme = theme,
                        lineIndex = lineIndex,
                        lineColor = lineColor,
                        actionsEnabled = isLocalLine
                    )
                }
            }
        }
    }
}

/**
 * Row component for an SMS thread.
 *
 * Optimization Note: Action callbacks (onOpen, onArchive, etc.) are passed as stable function references
 * taking [SmsThreadItem] as a parameter. This avoids creating unstable lambdas in the parent loop
 * (e.g., `{ onOpen(thread) }`), allowing Compose to skip recomposition of this row when parent state changes.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun ThreadRow(
    thread: SmsThreadItem,
    onOpen: (SmsThreadItem) -> Unit,
    onArchive: (SmsThreadItem) -> Unit,
    onUnarchive: (SmsThreadItem) -> Unit,
    onDelete: (SmsThreadItem) -> Unit,
    dateFormatter: (Long) -> String,
    isArchived: Boolean,
    isPrivate: Boolean,
    onTogglePrivate: (SmsThreadItem, Boolean) -> Unit,
    theme: ThemePreferences,
    lineIndex: Int? = null,
    lineColor: Color? = null,
    actionsEnabled: Boolean = true
) {
    val (displayName, number) = splitDisplay(thread.address)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (!actionsEnabled) return@rememberSwipeToDismissBoxState false
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (isArchived) onUnarchive(thread) else onArchive(thread)
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete(thread); false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )
    val actionIconSize = (20f * theme.iconSizeFactor).coerceIn(16f, 28f).dp

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection ?: return@SwipeToDismissBox
            val isDelete = direction == SwipeToDismissBoxValue.EndToStart
            val color = if (isDelete) Color(0xFFE84A4A) else Color(0xFF5BC174)
            val label = if (isDelete) "Delete" else if (isArchived) "Unarchive" else "Archive"
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
                Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(actionIconSize))
                Spacer(modifier = Modifier.size(8.dp))
                Text(label, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        },
        content = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onOpen(thread) },
                        onLongClick = {
                            if (actionsEnabled) {
                                onTogglePrivate(thread, !isPrivate)
                            }
                        }
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
                    if (lineIndex != null && lineColor != null) {
                        LineBadge(
                            index = lineIndex,
                            color = lineColor,
                            theme = theme
                        )
                    }
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
                        if (thread.isTrusted && thread.trustedUrgency != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            TrustedPill(thread.trustedUrgency)
                        }
                        if (thread.isOtp) {
                            Spacer(modifier = Modifier.height(4.dp))
                            OtpPill()
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
private fun LineBadge(
    index: Int,
    color: Color,
    theme: ThemePreferences
) {
    val size = (22f * theme.iconSizeFactor).coerceIn(16f, 28f).dp
    Box(
        modifier = Modifier
            .size(size)
            .background(color, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = (index + 1).toString(),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun LinePickerRow(
    lines: List<com.pulselink.domain.model.SmsLine>,
    activeLineId: String?,
    lineColors: List<Color>,
    theme: ThemePreferences,
    onSelectLine: (String) -> Unit
) {
    if (lines.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val selectedIndex = lines.indexOfFirst { it.id == activeLineId }.takeIf { it >= 0 } ?: 0
    val selectedLine = lines.getOrNull(selectedIndex)
    val badgeColor = lineColors[selectedIndex % lineColors.size]
    val label = "Line ${selectedIndex + 1}"
    val subtitle = selectedLine?.phoneNumber ?: ""
    val onBackground = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onBackground)

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            tonalElevation = 1.dp,
            shape = RoundedCornerShape(12.dp),
            color = parseColorOr(MaterialTheme.colorScheme.surface, theme.backgroundColor)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LineBadge(index = selectedIndex, color = badgeColor, theme = theme)
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, fontWeight = FontWeight.SemiBold, color = onBackground)
                    if (subtitle.isNotBlank()) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = onBackground.copy(alpha = 0.7f))
                    }
                }
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = onBackground)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            lines.forEachIndexed { index, line ->
                val itemColor = lineColors[index % lineColors.size]
                val itemLabel = "Line ${index + 1}"
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LineBadge(index = index, color = itemColor, theme = theme)
                            Column {
                                Text(itemLabel)
                                if (line.phoneNumber.isNotBlank()) {
                                    Text(line.phoneNumber, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelectLine(line.id)
                    }
                )
            }
        }
    }
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
private fun TrustedPill(urgency: MessageUrgency) {
    val (label, color) = when (urgency) {
        MessageUrgency.EMERGENCY -> "Emergency" to Color(0xFFB91C1C)
        MessageUrgency.URGENT -> "Urgent" to Color(0xFFF59E0B)
        MessageUrgency.STANDARD -> "Check-in" to Color(0xFF059669)
    }
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = CircleShape
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun OtpPill() {
    val color = Color(0xFF2563EB)
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = CircleShape
    ) {
        Text(
            text = "2-step",
            style = MaterialTheme.typography.labelSmall,
            color = color,
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

@Composable
private fun SearchContactResult(
    result: SearchResultState.Contact,
    theme: ThemePreferences,
    onOpen: (Long, String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(result.threadId, result.address) }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Inbox,
                contentDescription = null,
                tint = parseColorOr(MaterialTheme.colorScheme.primary, theme.primaryColor)
            )
            Column {
                Text(
                    text = result.address.ifBlank { "Open conversation" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onBackground)
                )
                Text(
                    text = "Open conversation",
                    style = MaterialTheme.typography.bodySmall,
                    color = parseColorOr(MaterialTheme.colorScheme.onSurfaceVariant, theme.onBackground).copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun SearchResults(
    hits: List<SmsMessageItem>,
    theme: ThemePreferences,
    onOpen: (Long, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        hits.take(5).forEach { msg ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(msg.threadId, msg.address) }
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        text = msg.address.ifBlank { "Unknown sender" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = parseColorOr(MaterialTheme.colorScheme.primary, theme.primaryColor)
                    )
                    Text(
                        text = msg.body,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onBackground)
                    )
                    Text(
                        text = DateUtils.getRelativeTimeSpanString(
                            msg.timestamp,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS
                        ).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = parseColorOr(MaterialTheme.colorScheme.onSurfaceVariant, theme.onBackground).copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private fun splitDisplay(address: String): Pair<String, String?> = splitSmsDisplayAddress(address)

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
            .selectableGroup()
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
        modifier = Modifier.selectable(
            selected = selected,
            role = Role.Tab,
            onClick = onClick
        )
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

