package com.pulselink.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.pulselink.domain.model.ThemePreferences
import com.pulselink.util.parseColorOr

/**
 * Placeholder screen for deep visual customization. For now it shows a stub;
 * real controls (color pickers, layout presets, icon sizing, chat bubble preview)
 * should be added here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualSettingsScreen(
    theme: ThemePreferences,
    onSelectTheme: (ThemePreferences) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Visual settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Quick presets", fontWeight = FontWeight.SemiBold)
            val presets = listOf(
                ThemePreferences(),
                ThemePreferences(primaryColor = "#0D9488", secondaryColor = "#115E59", bubbleOutgoing = "#99F6E4", bubbleIncoming = "#E0F2F1", backgroundColor = "#FFFFFF"),
                ThemePreferences(primaryColor = "#1D4ED8", secondaryColor = "#1E3A8A", bubbleOutgoing = "#BFDBFE", bubbleIncoming = "#E0E7FF", backgroundColor = "#F8FAFC"),
                ThemePreferences(primaryColor = "#E11D48", secondaryColor = "#9F1239", bubbleOutgoing = "#FBCFE8", bubbleIncoming = "#FFE4E6", backgroundColor = "#FFF1F2")
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(presets) { preset ->
                    Button(onClick = { onSelectTheme(preset) }) {
                        Text("Apply", modifier = Modifier.padding(end = 4.dp))
                        Text(preset.primaryColor)
                    }
                }
            }
            Text("Current preview", fontWeight = FontWeight.SemiBold)
            SampleConversation(theme)
        }
    }
}

@Composable
private fun SampleConversation(theme: ThemePreferences) {
    val outgoing = parseColorOr(MaterialTheme.colorScheme.primaryContainer, theme.bubbleOutgoing)
    val incoming = parseColorOr(MaterialTheme.colorScheme.surfaceVariant, theme.bubbleIncoming)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PreviewBubble("Hey, how are you?", outgoing, Alignment.End)
        PreviewBubble("Doing great — testing the new theme!", incoming, Alignment.Start)
        PreviewBubble("Looks good on my end.", outgoing, Alignment.End)
    }
}

@Composable
private fun PreviewBubble(text: String, color: Color, align: Alignment.Horizontal) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.CenterStart
    ) {
        Surface(
            color = color,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.padding(end = 60.dp)
        ) {
            Text(text, modifier = Modifier.padding(12.dp))
        }
    }
}
