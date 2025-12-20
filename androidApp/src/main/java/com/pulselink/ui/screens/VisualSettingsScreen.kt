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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    var showColorPickerTarget by remember { mutableStateOf<String?>(null) }
    // Targets: "outgoing", "incoming", "bg", "text_outgoing", "text_incoming", "text_bg", "topbar", "text_topbar", "timestamp", "divider", "grad_start", "grad_end"

    if (showColorPickerTarget != null) {
        val initialColor = when(showColorPickerTarget) {
            "outgoing" -> theme.bubbleOutgoing
            "incoming" -> theme.bubbleIncoming
            "text_outgoing" -> theme.onBubbleOutgoing
            "text_incoming" -> theme.onBubbleIncoming
            "text_bg" -> theme.onBackground
            "topbar" -> theme.topBarColor
            "text_topbar" -> theme.onTopBarColor
            "timestamp" -> theme.timestampColor ?: theme.onBubbleOutgoing
            "divider" -> theme.dividerColor ?: theme.onBackground
            "grad_start" -> theme.appBackgroundGradientStart ?: theme.backgroundColor
            "grad_end" -> theme.appBackgroundGradientEnd ?: theme.backgroundColor
            else -> theme.backgroundColor
        }
        ColorPickerDialog(
            initialColor = initialColor,
            onColorSelected = { color ->
                val newTheme = when(showColorPickerTarget) {
                    "outgoing" -> theme.copy(bubbleOutgoing = color)
                    "incoming" -> theme.copy(bubbleIncoming = color)
                    "text_outgoing" -> theme.copy(onBubbleOutgoing = color)
                    "text_incoming" -> theme.copy(onBubbleIncoming = color)
                    "text_bg" -> theme.copy(onBackground = color)
                    "topbar" -> theme.copy(topBarColor = color)
                    "text_topbar" -> theme.copy(onTopBarColor = color)
                    "timestamp" -> theme.copy(timestampColor = color)
                    "divider" -> theme.copy(dividerColor = color)
                    "grad_start" -> theme.copy(appBackgroundGradientStart = color)
                    "grad_end" -> theme.copy(appBackgroundGradientEnd = color)
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
        Text("Live Preview", style = MaterialTheme.typography.titleMedium)

        // Preview Container
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
             Modifier.background(parseColorOr(Color.White, theme.backgroundColor))
        }

        Card(
            modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { showColorPickerTarget = "bg" },
            colors = CardDefaults.cardColors(containerColor = Color.Transparent) // Use manual background
        ) {
             Box(modifier = bgModifier.fillMaxWidth()) {
                 Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                     // Top Bar Preview
                     Surface(
                         color = parseColorOr(MaterialTheme.colorScheme.surface, theme.topBarColor),
                         modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { showColorPickerTarget = "topbar" }
                     ) {
                         Row(
                             modifier = Modifier.padding(12.dp),
                             horizontalArrangement = Arrangement.SpaceBetween,
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = parseColorOr(Color.Black, theme.onTopBarColor))
                             Text(
                                 "Contact Name",
                                 color = parseColorOr(Color.Black, theme.onTopBarColor),
                                 style = MaterialTheme.typography.titleMedium
                             )
                             Box(modifier = Modifier.size(24.dp))
                         }
                     }

                     Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                         Text(
                             "Today 10:23 AM",
                             modifier = Modifier.align(Alignment.CenterHorizontally).clickable(role = Role.Button) { showColorPickerTarget = "text_bg" },
                             style = MaterialTheme.typography.labelSmall,
                             color = parseColorOr(Color.Black, theme.onBackground)
                         )

                         PreviewBubble(
                             text = "Outgoing message.",
                             color = parseColorOr(MaterialTheme.colorScheme.primaryContainer, theme.bubbleOutgoing),
                             textColor = parseColorOr(MaterialTheme.colorScheme.onPrimaryContainer, theme.onBubbleOutgoing),
                             timestampColor = parseColorOr(Color.Gray, theme.timestampColor ?: theme.onBubbleOutgoing),
                             align = Alignment.End,
                             theme = theme,
                             onBubbleClick = { showColorPickerTarget = "outgoing" },
                             onTextClick = { showColorPickerTarget = "text_outgoing" }
                         )
                         PreviewBubble(
                             text = "Incoming message.",
                             color = parseColorOr(MaterialTheme.colorScheme.surfaceVariant, theme.bubbleIncoming),
                             textColor = parseColorOr(MaterialTheme.colorScheme.onSurfaceVariant, theme.onBubbleIncoming),
                             timestampColor = parseColorOr(Color.Gray, theme.timestampColor ?: theme.onBubbleIncoming),
                             align = Alignment.Start,
                             theme = theme,
                             onBubbleClick = { showColorPickerTarget = "incoming" },
                             onTextClick = { showColorPickerTarget = "text_incoming" }
                         )
                     }
                 }
             }
        }

        HorizontalDivider()

        Text("Background", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ColorChip("Solid Bg", parseColorOr(Color.White, theme.backgroundColor)) { showColorPickerTarget = "bg" }
            ColorChip("Grad Start", parseColorOr(Color.Transparent, theme.appBackgroundGradientStart ?: "#FFFFFF")) { showColorPickerTarget = "grad_start" }
            ColorChip("Grad End", parseColorOr(Color.Transparent, theme.appBackgroundGradientEnd ?: "#FFFFFF")) { showColorPickerTarget = "grad_end" }
        }
        Button(
            onClick = {
                 onUpdate(theme.copy(appBackgroundGradientStart = null, appBackgroundGradientEnd = null))
            },
            enabled = theme.appBackgroundGradientStart != null
        ) {
            Text("Clear Gradient")
        }


        Text("Colors", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ColorChip("Text Bg", parseColorOr(Color.Black, theme.onBackground)) { showColorPickerTarget = "text_bg" }
            ColorChip("Top Bar", parseColorOr(Color.White, theme.topBarColor)) { showColorPickerTarget = "topbar" }
            ColorChip("TB Text", parseColorOr(Color.Black, theme.onTopBarColor)) { showColorPickerTarget = "text_topbar" }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ColorChip("Out", parseColorOr(Color.Blue, theme.bubbleOutgoing)) { showColorPickerTarget = "outgoing" }
            ColorChip("Out Text", parseColorOr(Color.White, theme.onBubbleOutgoing)) { showColorPickerTarget = "text_outgoing" }
            ColorChip("In", parseColorOr(Color.Gray, theme.bubbleIncoming)) { showColorPickerTarget = "incoming" }
            ColorChip("In Text", parseColorOr(Color.Black, theme.onBubbleIncoming)) { showColorPickerTarget = "text_incoming" }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
             ColorChip("Timestamp", parseColorOr(Color.Gray, theme.timestampColor ?: "#888888")) { showColorPickerTarget = "timestamp" }
             ColorChip("Divider", parseColorOr(Color.Gray, theme.dividerColor ?: "#CCCCCC")) { showColorPickerTarget = "divider" }
        }

        HorizontalDivider()

        Text("Typography", style = MaterialTheme.typography.titleMedium)
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
        Text("Font Scale: ${"%.1f".format(theme.fontScale)}x")
        Slider(
            value = theme.fontScale,
            onValueChange = { onUpdate(theme.copy(fontScale = it)) },
            valueRange = 0.5f..2.0f,
            steps = 15,
            modifier = Modifier.semantics { contentDescription = "Font scale" }
        )

        HorizontalDivider()

        Text("Shape (Corner Radius)", style = MaterialTheme.typography.titleMedium)

        Text("Base Radius: ${theme.bubbleCornerRadius}dp")
        Slider(
            value = theme.bubbleCornerRadius.toFloat(),
            onValueChange = {
                onUpdate(theme.copy(
                    bubbleCornerRadius = it.toInt(),
                    bubbleCornerRadiusTopStart = null,
                    bubbleCornerRadiusTopEnd = null,
                    bubbleCornerRadiusBottomStart = null,
                    bubbleCornerRadiusBottomEnd = null
                ))
            },
            valueRange = 0f..32f,
            steps = 32,
            modifier = Modifier.semantics { contentDescription = "Base bubble corner radius" }
        )

        Text("Advanced Corners (Overrides Base)", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Top Start: ${theme.bubbleCornerRadiusTopStart ?: "-"}")
                Slider(
                    value = (theme.bubbleCornerRadiusTopStart ?: theme.bubbleCornerRadius).toFloat(),
                    onValueChange = { onUpdate(theme.copy(bubbleCornerRadiusTopStart = it.toInt())) },
                    valueRange = 0f..32f,
                    modifier = Modifier.semantics { contentDescription = "Top start corner radius" }
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Top End: ${theme.bubbleCornerRadiusTopEnd ?: "-"}")
                Slider(
                    value = (theme.bubbleCornerRadiusTopEnd ?: theme.bubbleCornerRadius).toFloat(),
                    onValueChange = { onUpdate(theme.copy(bubbleCornerRadiusTopEnd = it.toInt())) },
                    valueRange = 0f..32f,
                    modifier = Modifier.semantics { contentDescription = "Top end corner radius" }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
             Column(modifier = Modifier.weight(1f)) {
                 Text("Bot Start: ${theme.bubbleCornerRadiusBottomStart ?: "-"}")
                 Slider(
                     value = (theme.bubbleCornerRadiusBottomStart ?: theme.bubbleCornerRadius).toFloat(),
                     onValueChange = { onUpdate(theme.copy(bubbleCornerRadiusBottomStart = it.toInt())) },
                     valueRange = 0f..32f,
                     modifier = Modifier.semantics { contentDescription = "Bottom start corner radius" }
                 )
             }
             Column(modifier = Modifier.weight(1f)) {
                 Text("Bot End: ${theme.bubbleCornerRadiusBottomEnd ?: "-"}")
                 Slider(
                     value = (theme.bubbleCornerRadiusBottomEnd ?: theme.bubbleCornerRadius).toFloat(),
                     onValueChange = { onUpdate(theme.copy(bubbleCornerRadiusBottomEnd = it.toInt())) },
                     valueRange = 0f..32f,
                     modifier = Modifier.semantics { contentDescription = "Bottom end corner radius" }
                 )
             }
        }


        HorizontalDivider()

        Text("Icon Sizing", style = MaterialTheme.typography.titleMedium)
        Text("Scale: ${(theme.iconSizeFactor * 100).toInt()}%")
        Slider(
            value = theme.iconSizeFactor,
            onValueChange = { onUpdate(theme.copy(iconSizeFactor = it)) },
            valueRange = 0.5f..1.5f,
            steps = 10,
            modifier = Modifier.semantics { contentDescription = "Icon size scaling" }
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
fun ColorChip(label: String, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick, role = Role.Button)
            .semantics { contentDescription = "Select $label color" }
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, Color.Gray, CircleShape)
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
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
                    "#0D9488", "#1D4ED8", "#E11D48", "#000000",
                    "#212121", "#424242", "#616161", "#FF5722"
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
        // Default Light
        ThemePreferences(
            fontStyle = "Default",
            bubbleCornerRadius = 12,
            backgroundColor = "#FFFFFF",
            onBackground = "#000000",
            topBarColor = "#F3F3F3",
            onTopBarColor = "#000000",
            bubbleOutgoing = "#D0BCFF",
            onBubbleOutgoing = "#000000",
            bubbleIncoming = "#E8DEF8",
            onBubbleIncoming = "#000000"
        ),
        // Dark Mode
        ThemePreferences(
            fontStyle = "Default",
            bubbleCornerRadius = 12,
            backgroundColor = "#121212",
            onBackground = "#FFFFFF",
            topBarColor = "#1E1E1E",
            onTopBarColor = "#FFFFFF",
            bubbleOutgoing = "#BB86FC",
            onBubbleOutgoing = "#000000",
            bubbleIncoming = "#333333",
            onBubbleIncoming = "#FFFFFF"
        ),
        // Ocean (High Contrast Blue)
        ThemePreferences(
            fontStyle = "Serif",
            bubbleCornerRadius = 4,
            backgroundColor = "#0F172A", // Slate 900
            onBackground = "#F8FAFC",
            topBarColor = "#1E293B", // Slate 800
            onTopBarColor = "#F8FAFC",
            bubbleOutgoing = "#3B82F6", // Blue 500
            onBubbleOutgoing = "#FFFFFF",
            bubbleIncoming = "#334155", // Slate 700
            onBubbleIncoming = "#FFFFFF",
            primaryColor = "#3B82F6"
        ),
        // Rose (Warm)
        ThemePreferences(
            fontStyle = "Cursive",
            bubbleCornerRadius = 16,
            backgroundColor = "#FFF1F2", // Rose 50
            onBackground = "#881337", // Rose 900
            topBarColor = "#FFE4E6", // Rose 100
            onTopBarColor = "#881337",
            bubbleOutgoing = "#FB7185", // Rose 400
            onBubbleOutgoing = "#FFFFFF",
            bubbleIncoming = "#FECDD3", // Rose 200
            onBubbleIncoming = "#881337",
            primaryColor = "#E11D48"
        ),
        // Gradient Sunset
        ThemePreferences(
            fontStyle = "Default",
            bubbleCornerRadius = 24,
            appBackgroundGradientStart = "#FF5F6D",
            appBackgroundGradientEnd = "#FFC371",
            onBackground = "#FFFFFF",
            topBarColor = "#FF5F6D",
            onTopBarColor = "#FFFFFF",
            bubbleOutgoing = "#FFFFFF",
            onBubbleOutgoing = "#FF5F6D",
            bubbleIncoming = "#FFFFFF",
            onBubbleIncoming = "#FF5F6D",
            bubbleCornerRadiusTopStart = 0,
            bubbleCornerRadiusBottomEnd = 0
        )
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
                     Box(
                         modifier = Modifier
                             .fillMaxWidth()
                             .height(80.dp)
                             .then(
                                 if (preset.appBackgroundGradientStart != null) {
                                     Modifier.background(
                                         brush = Brush.verticalGradient(
                                             colors = listOf(
                                                 parseColorOr(Color.White, preset.appBackgroundGradientStart),
                                                 parseColorOr(Color.White, preset.appBackgroundGradientEnd ?: preset.appBackgroundGradientStart)
                                             )
                                         )
                                     )
                                 } else {
                                     Modifier.background(parseColorOr(Color.White, preset.backgroundColor))
                                 }
                             )
                             .border(1.dp, Color.LightGray)
                     ) {
                         Box(modifier = Modifier.size(30.dp).align(Alignment.Center).background(parseColorOr(Color.Blue, preset.bubbleOutgoing), CircleShape))
                     }
                     val name = when(preset.backgroundColor) {
                         "#121212" -> "Dark Mode"
                         "#0F172A" -> "Ocean"
                         "#FFF1F2" -> "Rose"
                         else -> if (preset.appBackgroundGradientStart != null) "Sunset" else "Default Light"
                     }
                     Text(name, modifier = Modifier.padding(top = 8.dp))
                 }
            }
        }
    }
}

@Composable
private fun PreviewBubble(
    text: String,
    color: Color,
    textColor: Color,
    timestampColor: Color,
    align: Alignment.Horizontal,
    theme: ThemePreferences,
    onBubbleClick: () -> Unit,
    onTextClick: () -> Unit
) {
    val radius = theme.bubbleCornerRadius
    val shape = if (align == Alignment.End) {
         RoundedCornerShape(
             topStart = (theme.bubbleCornerRadiusTopStart ?: radius).dp,
             topEnd = (theme.bubbleCornerRadiusTopEnd ?: 2).dp, // Default small for outgoing arrow effect if not overridden
             bottomStart = (theme.bubbleCornerRadiusBottomStart ?: radius).dp,
             bottomEnd = (theme.bubbleCornerRadiusBottomEnd ?: radius).dp
         )
    } else {
         RoundedCornerShape(
             topStart = (theme.bubbleCornerRadiusTopStart ?: 2).dp,
             topEnd = (theme.bubbleCornerRadiusTopEnd ?: radius).dp,
             bottomStart = (theme.bubbleCornerRadiusBottomStart ?: radius).dp,
             bottomEnd = (theme.bubbleCornerRadiusBottomEnd ?: radius).dp
         )
    }

    val font = when(theme.fontStyle) {
        "Serif" -> FontFamily.Serif
        "Monospace" -> FontFamily.Monospace
        "Cursive" -> FontFamily.Cursive
        else -> FontFamily.Default
    }

    val fontSize = MaterialTheme.typography.bodyMedium.fontSize * theme.fontScale

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (align == Alignment.End) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = color,
            shape = shape,
            modifier = Modifier
                .clickable(onClick = onBubbleClick, role = Role.Button)
                .semantics { contentDescription = "Change bubble color" }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text,
                    modifier = Modifier
                        .clickable(onClick = onTextClick, role = Role.Button)
                        .semantics { contentDescription = "Change text color" },
                    fontFamily = font,
                    color = textColor,
                    fontSize = fontSize
                )
                Text(
                    "10:00 AM",
                    fontFamily = font,
                    color = timestampColor,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
