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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.layout.height
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pulselink.data.sms.SmsMessageItem
import com.pulselink.data.ai.AiComposeAction
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.ThemePreferences
import com.pulselink.ui.components.ThemeIcon
import com.pulselink.ui.components.ThemeIconKey
import com.pulselink.ui.state.AiComposeState
import com.pulselink.ui.state.AiSummaryState
import com.pulselink.util.parseColorOr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsThreadScreen(
    address: String,
    messages: List<SmsMessageItem>,
    contact: Contact?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    dateFormatter: (Long) -> String,
    globalTheme: ThemePreferences,
    onUpdateContactTheme: (ThemePreferences?) -> Unit,
    onCustomizeTheme: () -> Unit,
    onEditNotificationSound: () -> Unit = {},
    onSendMessage: (String, String?) -> Unit,
    lineOptions: List<com.pulselink.domain.model.SmsLine> = emptyList(),
    selectedLineId: String? = null,
    deviceLineId: String? = null,
    lineStatus: Map<String, Boolean> = emptyMap(),
    onSelectLine: (String) -> Unit = {},
    isArchived: Boolean,
    onToggleArchive: () -> Unit,
    aiSummaryState: AiSummaryState = AiSummaryState.Idle,
    onRequestSummary: () -> Unit = {},
    onClearSummary: () -> Unit = {},
    aiComposeState: AiComposeState = AiComposeState.Idle,
    onRequestCompose: (AiComposeAction, String?, String?) -> Unit = { _, _, _ -> },
    onClearCompose: () -> Unit = {},
    aiSummaryEnabled: Boolean = false,
    aiComposeEnabled: Boolean = false
) {
    val effectiveTheme = contact?.themeOverride ?: globalTheme
    var showThemeMenu by remember { mutableStateOf(false) }
    val iconSize = (24f * effectiveTheme.iconSizeFactor).coerceIn(18f, 34f).dp
    var draft by rememberSaveable { mutableStateOf("") }
    var showOfflineDialog by remember { mutableStateOf(false) }
    var pendingDraft by remember { mutableStateOf<String?>(null) }
    var pendingLineId by remember { mutableStateOf<String?>(null) }
    val lastInbound = remember(messages) { messages.lastOrNull { !it.outgoing }?.body }
    val backgroundImageUrl = effectiveTheme.backgroundImageUrl?.takeIf { it.isNotBlank() }
    val overlayAlpha = if (backgroundImageUrl != null) 0.35f else 1f

    val bgModifier = if (effectiveTheme.appBackgroundGradientStart != null && effectiveTheme.appBackgroundGradientEnd != null) {
        Modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    parseColorOr(Color.White, effectiveTheme.appBackgroundGradientStart!!).copy(alpha = overlayAlpha),
                    parseColorOr(Color.White, effectiveTheme.appBackgroundGradientEnd!!).copy(alpha = overlayAlpha)
                )
            )
        )
    } else {
        Modifier.background(parseColorOr(MaterialTheme.colorScheme.background, effectiveTheme.backgroundColor).copy(alpha = overlayAlpha))
    }

    Scaffold(
        containerColor = if (effectiveTheme.appBackgroundGradientStart != null) Color.Transparent else parseColorOr(MaterialTheme.colorScheme.background, effectiveTheme.backgroundColor),
        bottomBar = {
            MessageInput(
                draft = draft,
                onDraftChange = { draft = it },
                onSend = { message, lineId ->
                    if (message.isBlank()) return@MessageInput
                    val resolvedLineId = lineId ?: deviceLineId
                    val isRemoteLine = !resolvedLineId.isNullOrBlank() && resolvedLineId != deviceLineId
                    val isOnline = resolvedLineId?.let { lineStatus[it] != false } ?: true
                    if (isRemoteLine && !isOnline) {
                        pendingDraft = message
                        pendingLineId = resolvedLineId
                        showOfflineDialog = true
                    } else {
                        onSendMessage(message, resolvedLineId)
                        draft = ""
                    }
                },
                theme = effectiveTheme,
                iconSize = iconSize,
                lineOptions = lineOptions,
                selectedLineId = selectedLineId,
                onSelectLine = onSelectLine,
                lineStatus = lineStatus,
                aiEnabled = aiComposeEnabled,
                aiState = aiComposeState,
                onAiAction = { action ->
                    onRequestCompose(action, draft, lastInbound)
                }
            )
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            contact?.displayName?.takeIf { it.isNotBlank() } ?: address.ifBlank { "Conversation" },
                            color = parseColorOr(MaterialTheme.colorScheme.onSurface, effectiveTheme.onTopBarColor)
                        )
                        if (contact?.remoteDisplayName != null) {
                             Text(
                                 contact.remoteDisplayName,
                                 style = MaterialTheme.typography.bodySmall,
                                 color = parseColorOr(MaterialTheme.colorScheme.onSurfaceVariant, effectiveTheme.onTopBarColor).copy(alpha = 0.8f)
                             )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        ThemeIcon(
                            iconKey = ThemeIconKey.BACK,
                            theme = effectiveTheme,
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = parseColorOr(MaterialTheme.colorScheme.onSurface, effectiveTheme.onTopBarColor),
                            modifier = Modifier.size(iconSize)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleArchive) {
                        val icon = if (isArchived) Icons.Filled.Unarchive else Icons.Filled.Archive
                        val iconKey = if (isArchived) ThemeIconKey.UNARCHIVE else ThemeIconKey.ARCHIVE
                        val desc = if (isArchived) "Unarchive" else "Archive"
                        ThemeIcon(
                            iconKey = iconKey,
                            theme = effectiveTheme,
                            imageVector = icon,
                            contentDescription = desc,
                            tint = parseColorOr(MaterialTheme.colorScheme.onSurface, effectiveTheme.onTopBarColor),
                            modifier = Modifier.size(iconSize)
                        )
                    }
                    IconButton(onClick = onEditNotificationSound) {
                        ThemeIcon(
                            iconKey = ThemeIconKey.NOTIFICATIONS,
                            theme = effectiveTheme,
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = "Notification sound",
                            tint = parseColorOr(MaterialTheme.colorScheme.onSurface, effectiveTheme.onTopBarColor),
                            modifier = Modifier.size(iconSize)
                        )
                    }
                    IconButton(onClick = { showThemeMenu = true }) {
                        ThemeIcon(
                            iconKey = ThemeIconKey.PALETTE,
                            theme = effectiveTheme,
                            imageVector = Icons.Filled.Palette,
                            contentDescription = "Theme",
                            tint = parseColorOr(MaterialTheme.colorScheme.onSurface, effectiveTheme.onTopBarColor),
                            modifier = Modifier.size(iconSize)
                        )
                    }
                    DropdownMenu(
                        expanded = showThemeMenu,
                        onDismissRequest = { showThemeMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Customize Theme") },
                            onClick = {
                                showThemeMenu = false
                                onCustomizeTheme()
                            }
                        )
                        if (contact?.themeOverride != null) {
                            DropdownMenuItem(
                                text = { Text("Reset to Global") },
                                onClick = {
                                    showThemeMenu = false
                                    onUpdateContactTheme(null)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = parseColorOr(MaterialTheme.colorScheme.surface, effectiveTheme.topBarColor)
                )
            )
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (backgroundImageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(backgroundImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Box(modifier = Modifier.fillMaxSize().then(bgModifier))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (aiSummaryEnabled) {
                    item {
                        AiSummaryCard(
                            state = aiSummaryState,
                            onGenerate = onRequestSummary,
                            onClear = onClearSummary,
                            theme = effectiveTheme
                        )
                    }
                }
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(msg, dateFormatter, effectiveTheme, contact)
                }
            }
        }
    }

    if (aiComposeState is AiComposeState.Suggestion) {
        val suggestion = aiComposeState
        AlertDialog(
            onDismissRequest = onClearCompose,
            title = { Text("AI suggestion") },
            text = { Text(suggestion.text) },
            confirmButton = {
                TextButton(onClick = {
                    draft = suggestion.text
                    onClearCompose()
                }) {
                    Text("Use")
                }
            },
            dismissButton = {
                TextButton(onClick = onClearCompose) {
                    Text("Dismiss")
                }
            }
        )
    }

    if (showOfflineDialog) {
        AlertDialog(
            onDismissRequest = { showOfflineDialog = false },
            title = { Text("Line offline") },
            text = {
                Text("That line is offline. You can queue the message or send now from this device.")
            },
            confirmButton = {
                TextButton(onClick = {
                    val message = pendingDraft
                    if (!message.isNullOrBlank()) {
                        onSendMessage(message, pendingLineId)
                        draft = ""
                    }
                    pendingDraft = null
                    pendingLineId = null
                    showOfflineDialog = false
                }) {
                    Text("Queue")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    val message = pendingDraft
                    if (!message.isNullOrBlank()) {
                        onSendMessage(message, deviceLineId)
                        draft = ""
                    }
                    pendingDraft = null
                    pendingLineId = null
                    showOfflineDialog = false
                }) {
                    Text("Send now")
                }
            }
        )
    }
}

@Composable
private fun MessageInput(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: (String, String?) -> Unit,
    theme: ThemePreferences,
    iconSize: androidx.compose.ui.unit.Dp,
    lineOptions: List<com.pulselink.domain.model.SmsLine>,
    selectedLineId: String?,
    onSelectLine: (String) -> Unit,
    lineStatus: Map<String, Boolean>,
    aiEnabled: Boolean,
    aiState: AiComposeState,
    onAiAction: (AiComposeAction) -> Unit
) {
    var showAiMenu by remember { mutableStateOf(false) }
    val primary = parseColorOr(MaterialTheme.colorScheme.primary, theme.primaryColor)
    val onSurface = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onBackground)
    val errorMessage = (aiState as? AiComposeState.Error)?.message

    Surface(
        color = parseColorOr(MaterialTheme.colorScheme.surface, theme.backgroundColor), // Or distinct input BG
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (aiEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI assist",
                        style = MaterialTheme.typography.labelLarge,
                        color = onSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (aiState is AiComposeState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 4.dp),
                            strokeWidth = 2.dp,
                            color = primary
                        )
                    }
                    IconButton(
                        onClick = { showAiMenu = true },
                        enabled = aiState !is AiComposeState.Loading
                    ) {
                        ThemeIcon(
                            iconKey = ThemeIconKey.AI,
                            theme = theme,
                            imageVector = Icons.Filled.AutoFixHigh,
                            contentDescription = "AI assist",
                            tint = primary,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                    DropdownMenu(
                        expanded = showAiMenu,
                        onDismissRequest = { showAiMenu = false }
                    ) {
                        AiComposeAction.values().forEach { action ->
                            DropdownMenuItem(
                                text = { Text(action.label) },
                                onClick = {
                                    showAiMenu = false
                                    onAiAction(action)
                                }
                            )
                        }
                    }
                }
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Text message") },
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        autoCorrect = true,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Send
                    ),
                    trailingIcon = {
                        val enabled = draft.isNotBlank()
                        IconButton(
                            onClick = {
                                onSend(draft, selectedLineId)
                            },
                            enabled = enabled
                        ) {
                            ThemeIcon(
                                iconKey = ThemeIconKey.SEND,
                                theme = theme,
                                imageVector = Icons.Filled.Send,
                                contentDescription = "Send",
                                tint = if (enabled) primary else primary.copy(alpha = 0.38f),
                                modifier = Modifier.size(iconSize)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primary,
                        unfocusedBorderColor = onSurface.copy(alpha = 0.5f),
                        focusedTextColor = onSurface,
                        unfocusedTextColor = onSurface,
                        cursorColor = primary
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                if (lineOptions.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    var expanded by remember { mutableStateOf(false) }
                    val ordered = remember(lineOptions) {
                        lineOptions.sortedBy { it.createdAt }
                    }
                    val selectedIndex = ordered.indexOfFirst { it.id == selectedLineId }
                        .takeIf { it >= 0 } ?: 0
                    val palette = listOf(
                        parseColorOr(MaterialTheme.colorScheme.primary, theme.primaryColor),
                        parseColorOr(MaterialTheme.colorScheme.secondary, theme.secondaryColor),
                        parseColorOr(MaterialTheme.colorScheme.tertiary, theme.bubbleOutgoing),
                        parseColorOr(MaterialTheme.colorScheme.primaryContainer, theme.bubbleIncoming)
                    )
                    val badgeColor = palette[selectedIndex % palette.size]
                    val label = (selectedIndex + 1).toString()
                    val isOnline = ordered.getOrNull(selectedIndex)?.id?.let { lineStatus[it] != false } ?: true

                    Box {
                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .clickable { expanded = true },
                            color = Color.Transparent
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(badgeColor, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (!isOnline) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color.Red, shape = CircleShape)
                                            .align(Alignment.TopEnd)
                                    )
                                }
                            }
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            ordered.forEachIndexed { index, line ->
                                val itemColor = palette[index % palette.size]
                                val numberLabel = line.phoneNumber.takeIf { it.isNotBlank() }
                                val itemLabel = if (numberLabel != null) "Line ${index + 1} | $numberLabel" else "Line ${index + 1}"
                                val online = lineStatus[line.id] != false
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .background(itemColor, RoundedCornerShape(4.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = (index + 1).toString(),
                                                    color = Color.White,
                                                    fontSize = MaterialTheme.typography.labelSmall.fontSize
                                                )
                                            }
                                            Text(itemLabel)
                                            if (!online) {
                                                Text("(offline)", style = MaterialTheme.typography.labelSmall)
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
            }
        }
    }
}

@Composable
private fun AiSummaryCard(
    state: AiSummaryState,
    onGenerate: () -> Unit,
    onClear: () -> Unit,
    theme: ThemePreferences
) {
    val container = parseColorOr(MaterialTheme.colorScheme.surfaceVariant, theme.bubbleIncoming)
    val onContainer = parseColorOr(MaterialTheme.colorScheme.onSurfaceVariant, theme.onBubbleIncoming)
    val accent = parseColorOr(MaterialTheme.colorScheme.primary, theme.primaryColor)

    Surface(
        color = container,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemeIcon(
                    iconKey = ThemeIconKey.AI,
                    theme = theme,
                    imageVector = Icons.Filled.AutoFixHigh,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI summary",
                    style = MaterialTheme.typography.titleSmall,
                    color = onContainer
                )
                Spacer(modifier = Modifier.weight(1f))
                when (state) {
                    is AiSummaryState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = accent
                        )
                    }
                    is AiSummaryState.Success -> {
                        TextButton(onClick = onGenerate) { Text("Refresh") }
                        TextButton(onClick = onClear) { Text("Clear") }
                    }
                    is AiSummaryState.Error -> {
                        TextButton(onClick = onGenerate) { Text("Retry") }
                    }
                    AiSummaryState.Idle -> {
                        TextButton(onClick = onGenerate) { Text("Generate") }
                    }
                }
            }
            when (state) {
                is AiSummaryState.Success -> {
                    Text(
                        text = state.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = onContainer
                    )
                }
                is AiSummaryState.Error -> {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is AiSummaryState.Loading -> {
                    Text(
                        text = "Summarizing recent messages...",
                        style = MaterialTheme.typography.bodySmall,
                        color = onContainer.copy(alpha = 0.7f)
                    )
                }
                AiSummaryState.Idle -> {
                    Text(
                        text = "Summarize the latest messages in this thread.",
                        style = MaterialTheme.typography.bodySmall,
                        color = onContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: SmsMessageItem, dateFormatter: (Long) -> String, theme: ThemePreferences, contact: Contact?) {
    val isOutgoing = msg.outgoing
    val bubbleColor = if (isOutgoing) {
        parseColorOr(MaterialTheme.colorScheme.primaryContainer, theme.bubbleOutgoing)
    } else {
        parseColorOr(MaterialTheme.colorScheme.surfaceVariant, theme.bubbleIncoming)
    }

    val textColor = if (isOutgoing) {
        parseColorOr(MaterialTheme.colorScheme.onPrimaryContainer, theme.onBubbleOutgoing)
    } else {
        parseColorOr(MaterialTheme.colorScheme.onSurfaceVariant, theme.onBubbleIncoming)
    }

    val baseRadius = theme.bubbleCornerRadius
    val shape = if (isOutgoing) {
         RoundedCornerShape(
             topStart = (theme.bubbleCornerRadiusTopStart ?: baseRadius).dp,
             topEnd = (theme.bubbleCornerRadiusTopEnd ?: 2).dp, // Default smaller for outgoing effect
             bottomStart = (theme.bubbleCornerRadiusBottomStart ?: baseRadius).dp,
             bottomEnd = (theme.bubbleCornerRadiusBottomEnd ?: baseRadius).dp
         )
    } else {
         RoundedCornerShape(
             topStart = (theme.bubbleCornerRadiusTopStart ?: 2).dp, // Default smaller for incoming effect
             topEnd = (theme.bubbleCornerRadiusTopEnd ?: baseRadius).dp,
             bottomStart = (theme.bubbleCornerRadiusBottomStart ?: baseRadius).dp,
             bottomEnd = (theme.bubbleCornerRadiusBottomEnd ?: baseRadius).dp
         )
    }

    val font = when(theme.fontStyle) {
        "Serif" -> FontFamily.Serif
        "Monospace" -> FontFamily.Monospace
        "Cursive" -> FontFamily.Cursive
        else -> FontFamily.Default
    }

    val fontSize = MaterialTheme.typography.bodyMedium.fontSize * theme.fontScale

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isOutgoing) {
             // Avatar
             Box(
                 modifier = Modifier
                     .size(32.dp)
                     .clip(CircleShape)
                     .background(Color.Gray),
                 contentAlignment = Alignment.Center
             ) {
                 if (contact?.avatarUrl != null) {
                     // Placeholder for image loading. Ideally use Coil/Glide
                     Text("IMG", style = MaterialTheme.typography.labelSmall, color = Color.White)
                 } else {
                     Text(
                        text = contact?.displayName?.take(1)?.uppercase() ?: "?",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                     )
                 }
             }
             Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            color = bubbleColor,
            shape = shape
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = msg.body,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    fontFamily = font,
                    color = textColor,
                    fontSize = fontSize
                )
                Text(
                    text = dateFormatter(msg.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = font,
                    color = parseColorOr(textColor.copy(alpha = 0.7f), theme.timestampColor) // Override if specific timestamp color
                )
            }
        }
    }
}
