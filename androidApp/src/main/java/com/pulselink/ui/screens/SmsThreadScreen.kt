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
import androidx.compose.ui.draw.clip
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
    onCustomizeTheme: () -> Unit
) {
    val effectiveTheme = contact?.themeOverride ?: globalTheme
    var showThemeMenu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = parseColorOr(MaterialTheme.colorScheme.background, effectiveTheme.backgroundColor),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(contact?.displayName?.takeIf { it.isNotBlank() } ?: address.ifBlank { "Conversation" })
                        if (contact?.remoteDisplayName != null) {
                             Text(contact.remoteDisplayName, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showThemeMenu = true }) {
                        Icon(Icons.Filled.Palette, contentDescription = "Theme")
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
                    containerColor = parseColorOr(MaterialTheme.colorScheme.surface, effectiveTheme.backgroundColor)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                MessageBubble(msg, dateFormatter, effectiveTheme, contact)
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

    val shape = if (isOutgoing) {
         RoundedCornerShape(topStart = theme.bubbleCornerRadius.dp, topEnd = 2.dp, bottomStart = theme.bubbleCornerRadius.dp, bottomEnd = theme.bubbleCornerRadius.dp)
    } else {
         RoundedCornerShape(topStart = 2.dp, topEnd = theme.bubbleCornerRadius.dp, bottomStart = theme.bubbleCornerRadius.dp, bottomEnd = theme.bubbleCornerRadius.dp)
    }

    val font = when(theme.fontStyle) {
        "Serif" -> FontFamily.Serif
        "Monospace" -> FontFamily.Monospace
        "Cursive" -> FontFamily.Cursive
        else -> FontFamily.Default
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
                    fontFamily = font
                )
                Text(
                    text = dateFormatter(msg.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = font
                )
            }
        }
    }
}
