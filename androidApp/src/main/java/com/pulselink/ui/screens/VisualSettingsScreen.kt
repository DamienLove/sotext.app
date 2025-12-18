package com.pulselink.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.pulselink.domain.model.ThemePreferences
import com.pulselink.util.parseColorOr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualSettingsScreen(
    theme: ThemePreferences,
    onSelectTheme: (ThemePreferences) -> Unit,
    onBack: () -> Unit,
    isGlobal: Boolean = true
) {
    var activeTab by remember { mutableStateOf(0) }
    var tempTheme by remember(theme) { mutableStateOf(theme) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isGlobal) "Visual Settings" else "Chat Customization") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = activeTab) {
                Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Customize") })
                Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Presets") })
            }

            if (activeTab == 0) {
                CustomizeTab(
                    theme = tempTheme,
                    onUpdate = {
                        tempTheme = it
                        onSelectTheme(it)
                    },
                    isGlobal = isGlobal
                )
            } else {
                PresetsTab(onSelect = {
                    tempTheme = it
                    onSelectTheme(it)
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeTab(
    theme: ThemePreferences,
    onUpdate: (ThemePreferences) -> Unit,
    isGlobal: Boolean
) {
    val scrollState = rememberScrollState()
    var showColorPickerTarget by remember { mutableStateOf<String?>(null) } // "outgoing", "incoming", "bg"

    if (showColorPickerTarget != null) {
        val initialColor = when(showColorPickerTarget) {
            "outgoing" -> theme.bubbleOutgoing
            "incoming" -> theme.bubbleIncoming
            else -> theme.backgroundColor
        }
        ColorPickerDialog(
            initialColor = initialColor,
            onColorSelected = { color ->
                val newTheme = when(showColorPickerTarget) {
                    "outgoing" -> theme.copy(bubbleOutgoing = color)
                    "incoming" -> theme.copy(bubbleIncoming = color)
                    else -> theme.copy(backgroundColor = color)
                }
                onUpdate(newTheme)
            },
            onDismiss = { showColorPickerTarget = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Live Preview (Tap bubble to change color)", style = MaterialTheme.typography.titleMedium)
        Card(
            modifier = Modifier.fillMaxWidth().clickable { showColorPickerTarget = "bg" },
            colors = CardDefaults.cardColors(containerColor = parseColorOr(Color.White, theme.backgroundColor))
        ) {
             Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                 PreviewBubble(
                     text = "Tap me to change outgoing color!",
                     color = parseColorOr(MaterialTheme.colorScheme.primaryContainer, theme.bubbleOutgoing),
                     align = Alignment.End,
                     radius = theme.bubbleCornerRadius,
                     fontStyle = theme.fontStyle,
                     onClick = { showColorPickerTarget = "outgoing" }
                 )
                 PreviewBubble(
                     text = "Or me for incoming color.",
                     color = parseColorOr(MaterialTheme.colorScheme.surfaceVariant, theme.bubbleIncoming),
                     align = Alignment.Start,
                     radius = theme.bubbleCornerRadius,
                     fontStyle = theme.fontStyle,
                     onClick = { showColorPickerTarget = "incoming" }
                 )
             }
        }

        HorizontalDivider()

        Text("Typography & Shape", style = MaterialTheme.typography.titleMedium)

        Text("Font Style")
        val fonts = listOf("Default", "Serif", "Monospace", "Cursive")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            fonts.forEach { font ->
                 FilterChip(
                     selected = theme.fontStyle == font,
                     onClick = { onUpdate(theme.copy(fontStyle = font)) },
                     label = { Text(font) }
                 )
            }
        }

        Text("Corner Radius: ${theme.bubbleCornerRadius}dp")
        Slider(
            value = theme.bubbleCornerRadius.toFloat(),
            onValueChange = { onUpdate(theme.copy(bubbleCornerRadius = it.toInt())) },
            valueRange = 4f..24f,
            steps = 20
        )

        HorizontalDivider()

        Text("Icon Sizing", style = MaterialTheme.typography.titleMedium)
        Text("Scale: ${(theme.iconSizeFactor * 100).toInt()}%")
        Slider(
            value = theme.iconSizeFactor,
            onValueChange = { onUpdate(theme.copy(iconSizeFactor = it)) },
            valueRange = 0.5f..1.5f,
            steps = 10
        )

        if (isGlobal) {
            HorizontalDivider()
            Text("App Icon", style = MaterialTheme.typography.titleMedium)
            val iconVariants = listOf("Default", "Logo", "Pro")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                iconVariants.forEach { variant ->
                    FilterChip(
                        selected = theme.inboxIconVariant == variant,
                        onClick = { onUpdate(theme.copy(inboxIconVariant = variant)) },
                        label = { Text(variant) }
                    )
                }
            }
        }
    }
}

@Composable
fun ColorPickerDialog(
    initialColor: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var hex by remember { mutableStateOf(initialColor.removePrefix("#")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Color") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = hex,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it in 'A'..'F' || it in 'a'..'f' }) {
                            hex = input.take(6)
                        }
                    },
                    label = { Text("HEX Code") },
                    prefix = { Text("#") },
                    singleLine = true
                )

                Text("Swatches")
                val colors = listOf(
                    "#D0BCFF", "#E8DEF8", "#FBCFE8", "#FFD8E4",
                    "#B69DF8", "#F6EDFF", "#C3FBC8", "#E6F9E8",
                    "#FFFFFF", "#F5F5F5", "#FFF1F2", "#F0F9FF",
                    "#0D9488", "#1D4ED8", "#E11D48", "#000000"
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(40.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(colors) { colorHex ->
                        val color = parseColorOr(Color.Gray, colorHex)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(1.dp, Color.Gray, CircleShape)
                                .clickable {
                                    hex = colorHex.removePrefix("#")
                                }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val fullHex = "#$hex"
                if (hex.length == 6 || hex.length == 8) {
                    onColorSelected(fullHex)
                }
                onDismiss()
            }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PresetsTab(onSelect: (ThemePreferences) -> Unit) {
    val presets = listOf(
        ThemePreferences(fontStyle = "Default", bubbleCornerRadius = 12),
        ThemePreferences(primaryColor = "#0D9488", secondaryColor = "#115E59", bubbleOutgoing = "#99F6E4", bubbleIncoming = "#E0F2F1", backgroundColor = "#FFFFFF", fontStyle = "Default", bubbleCornerRadius = 8),
        ThemePreferences(primaryColor = "#1D4ED8", secondaryColor = "#1E3A8A", bubbleOutgoing = "#BFDBFE", bubbleIncoming = "#E0E7FF", backgroundColor = "#F8FAFC", fontStyle = "Serif", bubbleCornerRadius = 4),
        ThemePreferences(primaryColor = "#E11D48", secondaryColor = "#9F1239", bubbleOutgoing = "#FBCFE8", bubbleIncoming = "#FFE4E6", backgroundColor = "#FFF1F2", fontStyle = "Cursive", bubbleCornerRadius = 16)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(presets) { preset ->
            Card(
                onClick = { onSelect(preset) },
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                 Column(modifier = Modifier.padding(12.dp)) {
                     Box(modifier = Modifier.fillMaxWidth().height(80.dp).background(parseColorOr(Color.White, preset.backgroundColor))) {
                     }
                     Text("Preset", modifier = Modifier.padding(top = 8.dp))
                 }
            }
        }
    }
}

@Composable
private fun PreviewBubble(
    text: String,
    color: Color,
    align: Alignment.Horizontal,
    radius: Int,
    fontStyle: String,
    onClick: () -> Unit
) {
    val shape = if (align == Alignment.End) {
         RoundedCornerShape(topStart = radius.dp, topEnd = 2.dp, bottomStart = radius.dp, bottomEnd = radius.dp)
    } else {
         RoundedCornerShape(topStart = 2.dp, topEnd = radius.dp, bottomStart = radius.dp, bottomEnd = radius.dp)
    }

    val font = when(fontStyle) {
        "Serif" -> FontFamily.Serif
        "Monospace" -> FontFamily.Monospace
        "Cursive" -> FontFamily.Cursive
        else -> FontFamily.Default
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (align == Alignment.End) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = color,
            shape = shape,
            modifier = Modifier.clickable(onClick = onClick)
        ) {
            Text(
                text,
                modifier = Modifier.padding(12.dp),
                fontFamily = font
            )
        }
    }
}
