package com.pulselink.beacon.ui

import android.text.format.DateUtils
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulselink.beacon.R
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.pulselink.beacon.data.SmsMessageItem
import com.pulselink.beacon.data.ThemePalette
import com.pulselink.beacon.ui.ads.NativeAdCard
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed interface ThreadUiItem {
    data class Message(val message: SmsMessageItem) : ThreadUiItem
    data class Header(val text: String, val id: String) : ThreadUiItem
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadScreen(
    address: String,
    messages: List<SmsMessageItem>,
    theme: ThemePalette,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onDeleteThread: () -> Unit,
    onEditNotificationSound: () -> Unit,
    onCustomize: () -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    val iconTint = theme.accentColor
    val context = LocalContext.current

    val attachmentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            sendAttachmentViaSms(context, address, uri)
        }
    }

    val listState = remember(address) { LazyListState() }
    val scope = rememberCoroutineScope()
    var initialScrollDone by remember(address) { mutableStateOf(false) }
    val isNearBottom by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= (layout.totalItemsCount - 2).coerceAtLeast(0)
        }
    }

    // Transform messages to UI items with headers
    val uiItems by remember {
        derivedStateOf {
            val list = mutableListOf<ThreadUiItem>()
            messages.forEachIndexed { index, msg ->
                list.add(ThreadUiItem.Message(msg))
                val nextMsg = messages.getOrNull(index + 1)
                // If nextMsg is null (top of list) or different day, add header
                if (nextMsg == null || !isSameDay(msg.timestamp, nextMsg.timestamp)) {
                    val headerText = getDateHeader(msg.timestamp, context)
                    // Use day's timestamp (midnight) as unique ID to prevent duplicates across months/years
                    val zone = ZoneId.systemDefault()
                    val dayStart = Instant.ofEpochMilli(msg.timestamp)
                        .atZone(zone)
                        .toLocalDate()
                        .atStartOfDay(zone)
                        .toInstant()
                        .toEpochMilli()
                    list.add(ThreadUiItem.Header(headerText, "header_$dayStart"))
                }
            }
            list
        }
    }

    val smartReplies = listOf(
        stringResource(R.string.smart_reply_yes),
        stringResource(R.string.smart_reply_no),
        stringResource(R.string.smart_reply_ok),
        stringResource(R.string.smart_reply_thanks),
        stringResource(R.string.smart_reply_cant_talk),
        stringResource(R.string.smart_reply_call_later),
        stringResource(R.string.smart_reply_on_way)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(address, maxLines = 1, color = theme.frameColor) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_desc_back), tint = iconTint)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        try {
                            // Sanitize phone number to prevent URI injection
                            val sanitizedAddress = address.filter { it.isDigit() || it in setOf('+', '#', '*') }
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$sanitizedAddress"))
                            context.startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_no_dialer),
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_unable_to_call),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }) {
                        Icon(Icons.Default.Call, contentDescription = stringResource(R.string.content_desc_call), tint = iconTint)
                    }
                    IconButton(onClick = onCustomize) {
                        Icon(Icons.Default.Palette, contentDescription = stringResource(R.string.content_desc_customize_theme), tint = iconTint)
                    }
                    IconButton(onClick = onEditNotificationSound) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = stringResource(R.string.content_desc_notification_sound), tint = iconTint)
                    }
                    IconButton(onClick = onDeleteThread) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.content_desc_delete_thread), tint = iconTint)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.threadBackgroundColor,
                    titleContentColor = theme.frameColor,
                    navigationIconContentColor = iconTint,
                    actionIconContentColor = iconTint
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(theme.threadBackgroundColor)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                state = listState,
                reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp, top = 8.dp)
            ) {
                items(uiItems, key = {
                    when(it) {
                        is ThreadUiItem.Message -> it.message.id
                        is ThreadUiItem.Header -> it.id
                    }
                }) { item ->
                    when (item) {
                        is ThreadUiItem.Message -> MessageBubble(message = item.message, theme = theme)
                        is ThreadUiItem.Header -> DateHeader(text = item.text, theme = theme)
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }

                item {
                    if (messages.size > 3) {
                        NativeAdCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }

            LaunchedEffect(messages.size) {
                if (messages.isNotEmpty() && (!initialScrollDone || isNearBottom)) {
                    // With reverseLayout, index 0 is bottom.
                    // Sort order is Newest -> Oldest (0 is newest), so scroll to 0 to show latest.
                    listState.animateScrollToItem(0)
                    initialScrollDone = true
                }
            }

            // Smart Replies
            if (draft.isEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    items(smartReplies) { reply ->
                        SuggestionChip(
                            onClick = {
                                onSend(reply)
                                draft = ""  // Clear draft state after sending
                            },
                            label = { Text(reply) }
                        )
                    }
                }
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
                    IconButton(onClick = { attachmentPicker.launch("*/*") }) {
                        Icon(Icons.Default.AttachFile, contentDescription = stringResource(R.string.content_desc_attach_file), tint = iconTint)
                    }
                    TextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.hint_write_message)) },
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
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
                        Icon(Icons.Default.Send, contentDescription = stringResource(R.string.content_desc_send), tint = iconTint)
                    }
                }
            }
        }
    }
}

@Composable
private fun DateHeader(text: String, theme: ThemePalette) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics { heading() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = theme.frameColor.copy(alpha = 0.1f),
            modifier = Modifier.padding(4.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = theme.frameColor.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun MessageBubble(message: SmsMessageItem, theme: ThemePalette) {
    val isOutgoing = message.outgoing
    val background = if (isOutgoing) theme.outgoingColor else theme.incomingColor
    val alignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    val frameColor = theme.frameColor
    val bubbleShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isOutgoing) 16.dp else 4.dp,
        bottomEnd = if (isOutgoing) 4.dp else 16.dp
    )

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Surface(
            color = background,
            shape = bubbleShape,
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, frameColor.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(bubbleShape)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = message.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                }
                if (message.isMms && message.mediaParts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    message.mediaParts.filter { it.dataUri != null && it.contentType.startsWith("image") }
                        .forEach { part ->
                            AsyncImage(
                                model = part.dataUri,
                                contentDescription = stringResource(R.string.content_desc_mms_image),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                }
                Text(
                    text = DateUtils.formatDateTime(
                        LocalContext.current,
                        message.timestamp,
                        DateUtils.FORMAT_SHOW_TIME
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Light,
                    color = Color.DarkGray,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

private fun isSameDay(t1: Long, t2: Long): Boolean {
    val zone = ZoneId.systemDefault()
    val date1 = Instant.ofEpochMilli(t1).atZone(zone).toLocalDate()
    val date2 = Instant.ofEpochMilli(t2).atZone(zone).toLocalDate()
    return date1 == date2
}

private fun getDateHeader(t: Long, context: Context): String {
    val now = System.currentTimeMillis()
    val oneDayMillis = TimeUnit.DAYS.toMillis(1)
    return if (isSameDay(t, now)) {
        context.getString(R.string.date_header_today)
    } else if (isSameDay(t, now - oneDayMillis)) {
        context.getString(R.string.date_header_yesterday)
    } else {
        val date = Instant.ofEpochMilli(t).atZone(ZoneId.systemDefault()).toLocalDate()
        val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
        date.format(formatter)
    }
}

private fun sendAttachmentViaSms(
    context: Context,
    address: String,
    uri: Uri
) {
    val mimeType = context.contentResolver.getType(uri) ?: "*/*"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        data = Uri.parse("smsto:$address")
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra("address", address)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val resolved = context.packageManager.queryIntentActivities(
        intent,
        PackageManager.MATCH_DEFAULT_ONLY
    )
    resolved.forEach { resolveInfo ->
        context.grantUriPermission(
            resolveInfo.activityInfo.packageName,
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }

    try {
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.chooser_send_attachment)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(
            context,
            context.getString(R.string.toast_no_messaging_app),
            Toast.LENGTH_LONG
        ).show()
    }
}
