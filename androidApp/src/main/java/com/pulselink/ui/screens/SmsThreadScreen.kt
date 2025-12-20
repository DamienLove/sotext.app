package com.pulselink.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulselink.data.sms.SmsMessageItem
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.ThemePreferences
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
    onSendMessage: (String) -> Unit
) {
    val effectiveTheme = contact?.themeOverride ?: globalTheme
    var showThemeMenu by remember { mutableStateOf(false) }

    val bgModifier = if (effectiveTheme.appBackgroundGradientStart != null && effectiveTheme.appBackgroundGradientEnd != null) {
        Modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    parseColorOr(Color.White, effectiveTheme.appBackgroundGradientStart!!),
                    parseColorOr(Color.White, effectiveTheme.appBackgroundGradientEnd!!)
                )
            )
        )
    } else {
        Modifier.background(parseColorOr(MaterialTheme.colorScheme.background, effectiveTheme.backgroundColor))
    }

    Scaffold(
        containerColor = if (effectiveTheme.appBackgroundGradientStart != null) Color.Transparent else parseColorOr(MaterialTheme.colorScheme.background, effectiveTheme.backgroundColor),
        bottomBar = {
            MessageInput(
                onSend = onSendMessage,
                theme = effectiveTheme
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = parseColorOr(MaterialTheme.colorScheme.onSurface, effectiveTheme.onTopBarColor))
                    }
                },
                actions = {
                    IconButton(onClick = { showThemeMenu = true }) {
                        Icon(Icons.Filled.Palette, contentDescription = "Theme", tint = parseColorOr(MaterialTheme.colorScheme.onSurface, effectiveTheme.onTopBarColor))
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
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .then(bgModifier) // Apply gradient here
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                MessageBubble(msg, dateFormatter, effectiveTheme, contact)
            }
        }
    }
}

@Composable
private fun MessageInput(
    onSend: (String) -> Unit,
    theme: ThemePreferences
) {
    var text by remember { mutableStateOf("") }
    val primary = parseColorOr(MaterialTheme.colorScheme.primary, theme.primaryColor)
    val onSurface = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onBackground)

    Surface(
        color = parseColorOr(MaterialTheme.colorScheme.surface, theme.backgroundColor), // Or distinct input BG
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
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
                    val enabled = text.isNotBlank()
                    IconButton(
                        onClick = {
                            onSend(text)
                            text = ""
                        },
                        enabled = enabled
                    ) {
                        Icon(
                            Icons.Filled.Send,
                            contentDescription = "Send",
                            tint = if (enabled) primary else primary.copy(alpha = 0.38f)
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
