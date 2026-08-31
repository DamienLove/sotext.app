package com.sotext.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.widget.Toast
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sotext.data.context.ContextCard
import com.sotext.data.context.MessageContextParser
import com.sotext.data.sms.SmsMessageItem
import com.sotext.data.sms.SmsMessageStatus
import com.sotext.data.ai.AiComposeAction
import com.sotext.domain.model.Contact
import com.sotext.domain.model.ThemePreferences
import com.sotext.ui.components.ThemeIcon
import com.sotext.ui.components.ThemeIconKey
import com.sotext.ui.state.AiComposeState
import com.sotext.ui.state.AiSummaryState
import com.sotext.util.sendAttachmentViaSms
import com.sotext.util.ensureReadableOnColor
import com.sotext.util.parseColorOr
import com.sotext.util.themeGradientColors
import com.sotext.ui.theme.starfieldOverlay

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
    onEditNotificationVibration: () -> Unit = {},
    onEditContact: () -> Unit = {},
    onCall: (() -> Unit)? = null,
    callEnabled: Boolean = false,
    onSendMessage: (String, String?) -> Unit,
    lineOptions: List<com.sotext.domain.model.SmsLine> = emptyList(),
    selectedLineId: String? = null,
    deviceLineId: String? = null,
    lineStatus: Map<String, Boolean> = emptyMap(),
    onSelectLine: (String) -> Unit = {},
    isArchived: Boolean,
    onToggleArchive: () -> Unit,
    isDatabaseBusy: Boolean = false,
    aiSummaryState: AiSummaryState = AiSummaryState.Idle,
    onRequestSummary: () -> Unit = {},
    onClearSummary: () -> Unit = {},
    aiComposeState: AiComposeState = AiComposeState.Idle,
    onRequestCompose: (AiComposeAction, String?, String?) -> Unit = { _, _, _ -> },
    onClearCompose: () -> Unit = {},
    aiSummaryEnabled: Boolean = false,
    aiComposeEnabled: Boolean = false,
    aiSignInRequired: Boolean = false,
    onRequestAiSignIn: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    hasMoreToLoad: Boolean = true,
    smartRepliesEnabled: Boolean = false,
    isPremium: Boolean = false,
    contextCardsEnabled: Boolean = true
) {
    val effectiveTheme = contact?.themeOverride ?: globalTheme
    var showThemeMenu by remember { mutableStateOf(false) }
    var showNotificationMenu by remember { mutableStateOf(false) }
    val iconSize = (24f * effectiveTheme.iconSizeFactor).coerceIn(18f, 34f).dp
    var draft by rememberSaveable { mutableStateOf("") }
    var showOfflineDialog by remember { mutableStateOf(false) }
    var pendingDraft by remember { mutableStateOf<String?>(null) }
    var pendingLineId by remember { mutableStateOf<String?>(null) }
    val lastInbound = remember(messages) { messages.lastOrNull { !it.outgoing }?.body }
    val backgroundImageUrl = effectiveTheme.backgroundImageUrl?.takeIf { it.isNotBlank() }
    val overlayAlpha = if (backgroundImageUrl != null) 0.35f else 1f
    val listState = remember(address) { LazyListState() }
    val isNearBottom by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: 0
            // Consider "near bottom" if within 5 items of the end, so user isn't disrupted
            // while reading recent messages but still gets auto-scroll for new messages
            lastVisible >= (layout.totalItemsCount - 5).coerceAtLeast(0)
        }
    }
    val context = LocalContext.current
    val attachmentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            sendAttachmentViaSms(context, address, uri)
        }
    }
    var initialScrollDone by remember(address) { mutableStateOf(false) }

    val threadGradientColors = themeGradientColors(effectiveTheme, alpha = overlayAlpha)
    val bgModifier = if (threadGradientColors != null) {
        Modifier.background(brush = Brush.verticalGradient(colors = threadGradientColors))
    } else {
        Modifier.background(parseColorOr(MaterialTheme.colorScheme.background, effectiveTheme.backgroundColor).copy(alpha = overlayAlpha))
    }

    Scaffold(
        containerColor = if (themeGradientColors(effectiveTheme) != null) Color.Transparent else parseColorOr(MaterialTheme.colorScheme.background, effectiveTheme.backgroundColor),
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
                onPickAttachment = { attachmentPicker.launch("*/*") },
                lineStatus = lineStatus,
                aiEnabled = aiComposeEnabled,
                aiSignInRequired = aiSignInRequired,
                onRequestAiSignIn = onRequestAiSignIn,
                aiState = aiComposeState,
                onAiAction = { action ->
                    onRequestCompose(action, draft, lastInbound)
                },
                smartRepliesEnabled = smartRepliesEnabled,
                isPremium = isPremium,
                onUseSuggestion = { text ->
                    draft = text
                    onClearCompose()
                },
                onDismissSuggestion = onClearCompose
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
                    if (onCall != null) {
                        val callTint = parseColorOr(
                            MaterialTheme.colorScheme.onSurface,
                            effectiveTheme.onTopBarColor
                        )
                        IconButton(onClick = onCall, enabled = callEnabled) {
                            ThemeIcon(
                                iconKey = ThemeIconKey.CALL,
                                theme = effectiveTheme,
                                imageVector = Icons.Filled.Call,
                                contentDescription = "Call",
                                tint = if (callEnabled) callTint else callTint.copy(alpha = 0.3f),
                                modifier = Modifier.size(iconSize)
                            )
                        }
                    }
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
                    IconButton(onClick = { showNotificationMenu = true }) {
                        ThemeIcon(
                            iconKey = ThemeIconKey.NOTIFICATIONS,
                            theme = effectiveTheme,
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = "Notification settings",
                            tint = parseColorOr(MaterialTheme.colorScheme.onSurface, effectiveTheme.onTopBarColor),
                            modifier = Modifier.size(iconSize)
                        )
                    }
                    DropdownMenu(
                        expanded = showNotificationMenu,
                        onDismissRequest = { showNotificationMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Notification sound") },
                            onClick = {
                                showNotificationMenu = false
                                onEditNotificationSound()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Notification vibration") },
                            onClick = {
                                showNotificationMenu = false
                                onEditNotificationVibration()
                            }
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
            Box(modifier = Modifier.fillMaxSize().then(bgModifier).starfieldOverlay(effectiveTheme.useStarfield))
            if (isDatabaseBusy) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                reverseLayout = true,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.isEmpty() && isDatabaseBusy) {
                    items(6) { index ->
                        MessageBubbleSkeleton(
                            isOutgoing = index % 2 == 0,
                            theme = effectiveTheme
                        )
                    }
                } else {
                    items(messages, key = { it.id }) { msg ->
                        MessageBubble(
                            msg = msg,
                            dateFormatter = dateFormatter,
                            theme = effectiveTheme,
                            contact = contact,
                            onRetry = { failed -> onSendMessage(failed.body, selectedLineId ?: deviceLineId) },
                            onAvatarClick = if (!msg.outgoing) {
                                { onEditContact() }
                            } else null,
                            contextCardsEnabled = contextCardsEnabled
                        )
                    }
                }
                if (hasMoreToLoad && messages.size >= 20) {
                     item {
                         Box(
                             modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                             contentAlignment = Alignment.Center
                         ) {
                             OutlinedButton(onClick = onLoadMore) {
                                 Text("Load older messages")
                             }
                         }
                     }
                }
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
            }
        }
    }

    LaunchedEffect(messages) {
        if (messages.isEmpty()) return@LaunchedEffect
        // Auto-scroll to newest message if this is the initial load or if user is near the bottom
        // This prevents disrupting users who have scrolled up to read old messages
        if (!initialScrollDone || isNearBottom) {
            listState.animateScrollToItem(0)
            initialScrollDone = true
        }
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
    lineOptions: List<com.sotext.domain.model.SmsLine>,
    selectedLineId: String?,
    onSelectLine: (String) -> Unit,
    onPickAttachment: () -> Unit,
    lineStatus: Map<String, Boolean>,
    aiEnabled: Boolean,
    aiSignInRequired: Boolean,
    onRequestAiSignIn: () -> Unit,
    aiState: AiComposeState,
    onAiAction: (AiComposeAction) -> Unit,
    smartRepliesEnabled: Boolean,
    isPremium: Boolean = false,
    onUseSuggestion: (String) -> Unit = {},
    onDismissSuggestion: () -> Unit = {}
) {
    val context = LocalContext.current
    val primary = parseColorOr(MaterialTheme.colorScheme.primary, theme.primaryColor)
    val onSurface = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onBackground)
    val onTopBar = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onTopBarColor)
    val secondary = parseColorOr(MaterialTheme.colorScheme.secondary, theme.secondaryColor)
    val timestamp = parseColorOr(
        onTopBar.copy(alpha = 0.7f),
        theme.timestampColor
    )
    val dividerColor = parseColorOr(onTopBar.copy(alpha = 0.12f), theme.dividerColor)
        .copy(alpha = 0.9f)
    val errorMessage = (aiState as? AiComposeState.Error)?.message

    Surface(
        // Design: composer sits on the top-bar color, separated by a hairline.
        color = parseColorOr(MaterialTheme.colorScheme.surface, theme.topBarColor)
            .copy(alpha = 0.92f),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = dividerColor, thickness = 1.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
            if (aiState is AiComposeState.Suggestion) {
                // Design: inline AI suggestion card with Use / Dismiss pills.
                Surface(
                    color = secondary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, secondary.copy(alpha = 0.34f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeIcon(
                                iconKey = ThemeIconKey.AI,
                                theme = theme,
                                imageVector = Icons.Filled.AutoFixHigh,
                                contentDescription = null,
                                tint = onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "AI SUGGESTION",
                                style = MaterialTheme.typography.labelSmall,
                                color = onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Text(
                            text = aiState.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onUseSuggestion(aiState.text) },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = primary)
                            ) {
                                Text("Use")
                            }
                            OutlinedButton(
                                onClick = onDismissSuggestion,
                                shape = CircleShape,
                                border = BorderStroke(1.dp, dividerColor),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = onSurface)
                            ) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }
            if (smartRepliesEnabled) {
                val suggestions = stringArrayResource(com.sotext.R.array.smart_replies_defaults).toList()
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(suggestions) { label ->
                        SuggestionChip(
                            onClick = { onDraftChange(if (draft.isBlank()) label else "$draft $label") },
                            label = { Text(label) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = primary.copy(alpha = 0.1f),
                                labelColor = primary
                            ),
                            border = BorderStroke(1.dp, primary.copy(alpha = 0.3f))
                        )
                    }
                }
            }
            if (aiSignInRequired) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Sign in to use AI assist",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = onRequestAiSignIn) {
                        Text("Sign in")
                    }
                }
            }
            if (aiEnabled || !isPremium) {
                // Design: AI compose actions as a scrolling chip row.
                // Locked chips (non-Premium) show a padlock and surface the upgrade note.
                val chipsEnabled = aiEnabled && aiState !is AiComposeState.Loading
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(AiComposeAction.values().toList()) { action ->
                            Surface(
                                shape = CircleShape,
                                color = Color.Transparent,
                                border = BorderStroke(1.dp, dividerColor),
                                modifier = Modifier.clickable {
                                    if (!aiEnabled) {
                                        Toast.makeText(
                                            context,
                                            "AI assist is a Premium feature.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else if (chipsEnabled) {
                                        onAiAction(action)
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 14.dp, vertical = 9.dp)
                                        .then(if (aiEnabled) Modifier else Modifier.alpha(0.55f)),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (!aiEnabled) {
                                        Icon(
                                            imageVector = Icons.Filled.Lock,
                                            contentDescription = null,
                                            tint = onTopBar,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = action.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = onTopBar
                                    )
                                }
                            }
                        }
                    }
                    if (aiState is AiComposeState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = primary
                        )
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
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onPickAttachment
                ) {
                    ThemeIcon(
                        iconKey = ThemeIconKey.ATTACH,
                        theme = theme,
                        imageVector = Icons.Filled.AttachFile,
                        contentDescription = "Attach file",
                        tint = onTopBar,
                        modifier = Modifier.size(iconSize)
                    )
                }
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primary,
                        unfocusedBorderColor = dividerColor,
                        focusedTextColor = onSurface,
                        unfocusedTextColor = onSurface,
                        focusedContainerColor = parseColorOr(
                            MaterialTheme.colorScheme.surfaceVariant,
                            theme.bubbleIncoming
                        ).copy(alpha = 0.7f),
                        unfocusedContainerColor = parseColorOr(
                            MaterialTheme.colorScheme.surfaceVariant,
                            theme.bubbleIncoming
                        ).copy(alpha = 0.7f),
                        cursorColor = primary
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                // Design: circular filled send button.
                val sendEnabled = draft.isNotBlank()
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (sendEnabled) primary else primary.copy(alpha = 0.38f))
                        .clickable(enabled = sendEnabled) { onSend(draft, selectedLineId) }
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ThemeIcon(
                            iconKey = ThemeIconKey.SEND,
                            theme = theme,
                            imageVector = Icons.Filled.Send,
                            contentDescription = "Send",
                            tint = ensureReadableOnColor(
                                background = primary,
                                desired = parseColorOr(Color.Black, theme.onBubbleOutgoing),
                                fallback = Color.Black
                            ),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                if (lineOptions.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    var expanded by remember { mutableStateOf(false) }
                    val ordered = remember(lineOptions) {
                        lineOptions.sortedWith(
                            compareBy<com.sotext.domain.model.SmsLine> { it.phoneNumber.ifBlank { "~" } }
                                .thenBy { it.createdAt }
                        )
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
                    val isOnline = ordered.getOrNull(selectedIndex)?.id?.let { lineStatus[it] != false } ?: true

                    Box {
                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .clickable { expanded = true },
                            color = Color.Transparent
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                LineIndicatorBadge(
                                    index = selectedIndex,
                                    color = badgeColor,
                                    isActive = true,
                                    size = 18.dp
                                )
                                if (!isOnline) {
                                    LineStatusDot(
                                        modifier = Modifier.align(Alignment.TopEnd),
                                        color = Color.Red
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
                                val isSelected = index == selectedIndex
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            LineIndicatorBadge(
                                                index = index,
                                                color = itemColor,
                                                isActive = isSelected,
                                                size = 16.dp
                                            )
                                            Text(itemLabel)
                                            Spacer(modifier = Modifier.weight(1f))
                                            if (isSelected) {
                                                Text(
                                                    text = "Active",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = itemColor
                                                )
                                            }
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
            // Design: delivery-channel note under the composer.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = timestamp,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (isPremium) {
                        "Firebase first, SMS fallback after 5s"
                    } else {
                        "SMS · Firebase delivery needs Premium sync"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = timestamp
                )
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
private fun MessageBubble(
    msg: SmsMessageItem,
    dateFormatter: (Long) -> String,
    theme: ThemePreferences,
    contact: Contact?,
    onRetry: (SmsMessageItem) -> Unit = {},
    onAvatarClick: (() -> Unit)? = null,
    contextCardsEnabled: Boolean = true
) {
    val isOutgoing = msg.outgoing
    val rawBubbleColor = if (isOutgoing) {
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

    // Advanced Effects Logic
    val bubbleColor = if (theme.useGlassEffect) {
        rawBubbleColor.copy(alpha = 0.65f)
    } else {
        rawBubbleColor
    }

    var bubbleModifier: Modifier = Modifier
    if (theme.useGlassEffect) {
         bubbleModifier = bubbleModifier.border(
            BorderStroke(1.dp, Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.4f), Color.White.copy(alpha = 0.1f))
            )),
            shape
         )
    }

    if (theme.useHolographicGlow) {
         val glowColor = if (isOutgoing) rawBubbleColor else parseColorOr(MaterialTheme.colorScheme.primary, theme.primaryColor)
         bubbleModifier = bubbleModifier.border(
             BorderStroke(
                 1.dp,
                 Brush.linearGradient(
                     listOf(
                         glowColor.copy(alpha = 0.3f),
                         glowColor.copy(alpha = 0.8f),
                         glowColor.copy(alpha = 0.3f)
                     )
                 )
             ),
             shape
         )
    }

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
                     .background(Color.Gray)
                     .then(
                         if (onAvatarClick != null) {
                             Modifier.clickable(onClick = onAvatarClick)
                         } else {
                             Modifier
                         }
                     ),
                 contentAlignment = Alignment.Center
             ) {
                 val avatarUrl = contact?.avatarUrl
                 if (!avatarUrl.isNullOrBlank()) {
                     AsyncImage(
                         model = ImageRequest.Builder(LocalContext.current)
                             .data(avatarUrl)
                             .crossfade(true)
                             .build(),
                         contentDescription = null,
                         contentScale = ContentScale.Crop,
                         modifier = Modifier.fillMaxSize()
                     )
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

        Column(
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
        ) {
            val statusLabel = when (msg.status) {
                SmsMessageStatus.SENDING -> "Sending"
                SmsMessageStatus.SENT -> "Sent"
                SmsMessageStatus.DELIVERED -> "Delivered"
                SmsMessageStatus.RECEIVED -> "Received"
                SmsMessageStatus.READ -> "Read"
                SmsMessageStatus.FAILED -> "Failed"
                null -> null
            }
            if (statusLabel != null && (isOutgoing || msg.status == SmsMessageStatus.READ)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = font,
                        color = textColor.copy(alpha = 0.6f)
                    )
                    if (isOutgoing && msg.status == SmsMessageStatus.FAILED) {
                        TextButton(
                            onClick = { onRetry(msg) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "Retry",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = font
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            Surface(
                color = bubbleColor,
                shape = shape,
                modifier = bubbleModifier
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (msg.isMms && msg.mediaParts.isNotEmpty()) {
                        msg.mediaParts.forEach { part ->
                            if (part.contentType.startsWith("image/")) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(part.dataUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "MMS Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .padding(bottom = 8.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                    if (msg.body.isNotBlank() && msg.body != "[MMS]") {
                        Text(
                            text = msg.body,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            fontFamily = font,
                            color = textColor,
                            fontSize = fontSize
                        )
                    }
                    Text(
                        text = dateFormatter(msg.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = font,
                        color = parseColorOr(textColor.copy(alpha = 0.7f), theme.timestampColor) // Override if specific timestamp color
                    )
                }
            }
            if (contextCardsEnabled && msg.body.isNotBlank()) {
                val contextCards = remember(msg.id, msg.body) {
                    MessageContextParser.extract(
                        messageId = msg.id,
                        body = msg.body,
                        timestampMillis = msg.timestamp,
                        senderAddress = msg.address
                    )
                }
                if (contextCards.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        contextCards.forEach { card ->
                            ContextActionCard(card = card, theme = theme)
                        }
                    }
                }
            }
        }
    }
}

private data class ContextCardAction(val label: String, val isPrimary: Boolean, val onClick: () -> Unit)

@Composable
private fun ContextActionCard(card: ContextCard, theme: ThemePreferences) {
    var dismissed by remember(card.id) { mutableStateOf(false) }
    if (dismissed) return

    val context = LocalContext.current
    val container = parseColorOr(MaterialTheme.colorScheme.surfaceVariant, theme.bubbleIncoming)
    val onContainer = parseColorOr(MaterialTheme.colorScheme.onSurfaceVariant, theme.onBubbleIncoming)
    val accent = parseColorOr(MaterialTheme.colorScheme.primary, theme.primaryColor)

    val (iconKey, icon, label, actions) = remember(card) { contextCardPresentation(card, context) }

    Surface(
        color = container,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemeIcon(
                iconKey = iconKey,
                theme = theme,
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = onContainer,
                maxLines = 2,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            actions.forEach { action ->
                TextButton(
                    onClick = action.onClick,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = action.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (action.isPrimary) accent else onContainer.copy(alpha = 0.7f)
                    )
                }
            }
            IconButton(
                onClick = { dismissed = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Dismiss",
                    modifier = Modifier.size(14.dp),
                    tint = onContainer.copy(alpha = 0.5f)
                )
            }
        }
    }
}

private fun contextCardPresentation(
    card: ContextCard,
    context: Context
): Quadruple<String, ImageVector, String, List<ContextCardAction>> {
    return when (card) {
        is ContextCard.Event -> Quadruple(
            ThemeIconKey.CONTEXT_EVENT,
            Icons.Filled.Event,
            card.title,
            listOf(ContextCardAction("Add to calendar", true) { openCalendarEvent(context, card) })
        )
        is ContextCard.Place -> Quadruple(
            ThemeIconKey.CONTEXT_PLACE,
            Icons.Filled.LocationOn,
            card.matchedText,
            listOf(ContextCardAction("Directions", true) { openMaps(context, card.query) })
        )
        is ContextCard.Phone -> Quadruple(
            ThemeIconKey.CONTEXT_PHONE,
            Icons.Filled.Call,
            card.number,
            listOf(
                ContextCardAction("Save", false) { saveContact(context, card.number) },
                ContextCardAction("Call", true) { dialNumber(context, card.number) }
            )
        )
        is ContextCard.Link -> Quadruple(
            ThemeIconKey.CONTEXT_LINK,
            Icons.Filled.Link,
            card.matchedText,
            listOf(ContextCardAction("Open", true) { openUrl(context, card.url) })
        )
        is ContextCard.Tracking -> Quadruple(
            ThemeIconKey.CONTEXT_TRACKING,
            Icons.Filled.LocalShipping,
            "${card.carrier} · ${card.number}",
            listOf(ContextCardAction("Track", true) { openUrl(context, card.trackingUrl) })
        )
        is ContextCard.VerificationCode -> Quadruple(
            ThemeIconKey.CONTEXT_CODE,
            Icons.Filled.ContentCopy,
            "Code: ${card.code}",
            listOf(ContextCardAction("Copy", true) { copyToClipboard(context, card.code) })
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun launchSafely(context: Context, intent: Intent) {
    runCatching {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }.onFailure {
        if (it is ActivityNotFoundException) {
            Toast.makeText(context, "No app found to handle this action.", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun openUrl(context: Context, url: String) {
    launchSafely(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

private fun openMaps(context: Context, query: String) {
    launchSafely(context, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(query)}")))
}

private fun dialNumber(context: Context, number: String) {
    launchSafely(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}")))
}

private fun saveContact(context: Context, number: String) {
    val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
        type = ContactsContract.RawContacts.CONTENT_TYPE
        putExtra(ContactsContract.Intents.Insert.PHONE, number)
    }
    launchSafely(context, intent)
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
    clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("Code", text))
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}

private fun openCalendarEvent(context: Context, event: ContextCard.Event) {
    val intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.startMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.endMillis)
        putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, event.allDay)
        putExtra(CalendarContract.Events.TITLE, event.title)
    }
    launchSafely(context, intent)
}

@Composable
private fun MessageBubbleSkeleton(
    isOutgoing: Boolean,
    theme: ThemePreferences
) {
    val transition = rememberInfiniteTransition(label = "messageSkeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "messageSkeletonAlpha"
    )
    val bubbleColor = parseColorOr(
        MaterialTheme.colorScheme.surfaceVariant,
        if (isOutgoing) theme.bubbleOutgoing else theme.bubbleIncoming
    ).copy(alpha = alpha)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (isOutgoing) 0.65f else 0.75f)
                    .height(18.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bubbleColor)
            )
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(bubbleColor.copy(alpha = alpha * 0.7f))
            )
        }
    }
}

@Composable
private fun LineIndicatorBadge(
    index: Int,
    color: Color,
    isActive: Boolean,
    size: Dp
) {
    val shape = RoundedCornerShape(3.dp)
    val borderColor = if (isActive) color else color.copy(alpha = 0.45f)
    val backgroundColor = if (isActive) color else color.copy(alpha = 0.12f)
    Box(
        modifier = Modifier
            .size(size)
            .border(width = if (isActive) 2.dp else 1.dp, color = borderColor, shape = shape)
            .background(backgroundColor, shape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = (index + 1).toString(),
            color = if (isActive) Color.White else borderColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
private fun LineStatusDot(
    modifier: Modifier = Modifier,
    color: Color
) {
    Box(
        modifier = modifier
            .size(8.dp)
            .background(color, shape = CircleShape)
    )
}

