package com.sotext.ui.screens

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import coil.compose.AsyncImage
import com.sotext.domain.model.ThemePreferences
import com.sotext.util.ensureReadableOnColor
import com.sotext.util.parseColorOr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualSettingsScreen(
    theme: ThemePreferences,
    onSelectTheme: (ThemePreferences) -> Unit,
    onBack: () -> Unit,
    isGlobal: Boolean = true,
    isPremium: Boolean = false
) {
    var activeTab by remember { mutableStateOf(0) }
    var tempTheme by remember(theme) { mutableStateOf(theme) }

    LaunchedEffect(tempTheme) {
        if (activeTab == 1 && tempTheme != theme) { // Customize tab
            delay(300)
            onSelectTheme(tempTheme)
        }
    }

    // Design: the whole screen reads from the live theme record.
    val colorScheme = MaterialTheme.colorScheme
    val background = parseColorOr(colorScheme.background, tempTheme.backgroundColor)
    val onBackground = parseColorOr(colorScheme.onBackground, tempTheme.onBackground)
    val topBar = parseColorOr(colorScheme.surface, tempTheme.topBarColor)
    val onTopBar = parseColorOr(colorScheme.onSurface, tempTheme.onTopBarColor)
    val primary = parseColorOr(colorScheme.primary, tempTheme.primaryColor)
    val bgModifier = if (tempTheme.appBackgroundGradientStart != null && tempTheme.appBackgroundGradientEnd != null) {
        Modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    parseColorOr(Color.White, tempTheme.appBackgroundGradientStart!!),
                    parseColorOr(Color.White, tempTheme.appBackgroundGradientEnd!!)
                )
            )
        )
    } else {
        Modifier.background(background)
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.then(bgModifier),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (isGlobal) "Visual Settings" else "Chat Customization",
                            color = onTopBar
                        )
                        Text(
                            if (isGlobal) "Global theme · applies everywhere" else "Overrides the global theme for this chat",
                            style = MaterialTheme.typography.labelSmall,
                            color = onTopBar.copy(alpha = 0.62f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = onTopBar
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = topBar)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Design: segmented Themes / Customize control.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        parseColorOr(colorScheme.surfaceVariant, tempTheme.bubbleIncoming)
                            .copy(alpha = 0.7f)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Themes", "Customize").forEachIndexed { index, label ->
                    val selected = activeTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) primary else Color.Transparent)
                            .clickable(role = Role.Button) { activeTab = index }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) {
                                ensureReadableOnColor(
                                    background = primary,
                                    desired = Color.Black,
                                    fallback = Color.Black
                                )
                            } else {
                                onBackground
                            }
                        )
                    }
                }
            }

            if (activeTab == 0) {
                ThemesTab(
                    current = tempTheme,
                    onSelect = {
                        tempTheme = it
                        onSelectTheme(it) // Immediate update for presets
                    }
                )
            } else {
                CustomizeTab(
                    theme = tempTheme,
                    onUpdate = {
                        tempTheme = it
                        // Debounced via LaunchedEffect
                    },
                    isGlobal = isGlobal,
                    isPremium = isPremium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeTab(
    theme: ThemePreferences,
    onUpdate: (ThemePreferences) -> Unit,
    isGlobal: Boolean,
    isPremium: Boolean = false
) {
    val scrollState = rememberScrollState()
    var showColorPickerTarget by remember { mutableStateOf<String?>(null) }
    // Targets: "outgoing", "incoming", "accent", "bg", "text_outgoing", "text_incoming", "text_bg", "topbar", "text_topbar", "timestamp", "divider", "grad_start", "grad_end"

    if (showColorPickerTarget != null) {
        val initialColor = when(showColorPickerTarget) {
            "outgoing" -> theme.bubbleOutgoing
            "incoming" -> theme.bubbleIncoming
            "accent" -> theme.primaryColor
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
                    "accent" -> theme.copy(primaryColor = color)
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

    val colorScheme = MaterialTheme.colorScheme
    val onBackground = parseColorOr(colorScheme.onBackground, theme.onBackground)
    val sectionLabelColor = onBackground.copy(alpha = 0.55f)
    val primary = parseColorOr(colorScheme.primary, theme.primaryColor)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Design: live preview card with a labelled top bar.
        val previewBg = if (theme.appBackgroundGradientStart != null && theme.appBackgroundGradientEnd != null) {
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
        val dividerTint = parseColorOr(onBackground.copy(alpha = 0.15f), theme.dividerColor)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, dividerTint.copy(alpha = 0.9f), RoundedCornerShape(18.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(parseColorOr(MaterialTheme.colorScheme.surface, theme.topBarColor))
                    .clickable(role = Role.Button) { showColorPickerTarget = "topbar" }
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Text(
                    "Live preview",
                    style = MaterialTheme.typography.labelMedium,
                    color = parseColorOr(Color.Black, theme.onTopBarColor)
                )
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                if (!theme.backgroundImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = theme.backgroundImageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(previewBg)
                        .clickable(role = Role.Button) { showColorPickerTarget = "bg" }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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

        // Design: bubble color swatches — Out / In / Accent / Top bar.
        SectionLabel("Bubble colors", sectionLabelColor)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ColorChip("Out", parseColorOr(Color.Blue, theme.bubbleOutgoing), labelColor = onBackground) { showColorPickerTarget = "outgoing" }
            ColorChip("In", parseColorOr(Color.Gray, theme.bubbleIncoming), labelColor = onBackground) { showColorPickerTarget = "incoming" }
            ColorChip("Accent", primary, labelColor = onBackground) { showColorPickerTarget = "accent" }
            ColorChip("Top bar", parseColorOr(Color.White, theme.topBarColor), labelColor = onBackground) { showColorPickerTarget = "topbar" }
        }
        Text(
            "Tap a swatch to open the color picker.",
            style = MaterialTheme.typography.labelSmall,
            color = onBackground.copy(alpha = 0.5f)
        )

        SectionLabel("More colors", sectionLabelColor)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ColorChip("Text Bg", parseColorOr(Color.Black, theme.onBackground), labelColor = onBackground) { showColorPickerTarget = "text_bg" }
            ColorChip("TB Text", parseColorOr(Color.Black, theme.onTopBarColor), labelColor = onBackground) { showColorPickerTarget = "text_topbar" }
            ColorChip("Out Text", parseColorOr(Color.White, theme.onBubbleOutgoing), labelColor = onBackground) { showColorPickerTarget = "text_outgoing" }
            ColorChip("In Text", parseColorOr(Color.Black, theme.onBubbleIncoming), labelColor = onBackground) { showColorPickerTarget = "text_incoming" }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ColorChip("Solid Bg", parseColorOr(Color.White, theme.backgroundColor), labelColor = onBackground) { showColorPickerTarget = "bg" }
            ColorChip("Timestamp", parseColorOr(Color.Gray, theme.timestampColor ?: "#888888"), labelColor = onBackground) { showColorPickerTarget = "timestamp" }
            ColorChip("Divider", parseColorOr(Color.Gray, theme.dividerColor ?: "#CCCCCC"), labelColor = onBackground) { showColorPickerTarget = "divider" }
        }

        // Design: corner radius with a right-aligned value label.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Corner radius", style = MaterialTheme.typography.bodyMedium, color = onBackground.copy(alpha = 0.7f))
            Text("${theme.bubbleCornerRadius}dp", style = MaterialTheme.typography.bodyMedium, color = onBackground)
        }
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
            colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary),
            modifier = Modifier.semantics { contentDescription = "Base bubble corner radius" }
        )

        // Design: font scale with a right-aligned value label.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Font scale", style = MaterialTheme.typography.bodyMedium, color = onBackground.copy(alpha = 0.7f))
            Text("${"%.2f".format(theme.fontScale)}x", style = MaterialTheme.typography.bodyMedium, color = onBackground)
        }
        Slider(
            value = theme.fontScale,
            onValueChange = { onUpdate(theme.copy(fontScale = it)) },
            valueRange = 0.5f..2.0f,
            steps = 15,
            colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary),
            modifier = Modifier.semantics { contentDescription = "Font scale" }
        )

        SectionLabel("Typeface", sectionLabelColor)
        val fonts = listOf("Default", "Serif", "Monospace", "Cursive")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            fonts.forEach { font ->
                val fontFamily = when (font) {
                    "Serif" -> FontFamily.Serif
                    "Monospace" -> FontFamily.Monospace
                    "Cursive" -> FontFamily.Cursive
                    else -> FontFamily.Default
                }
                ThemedSelectChip(
                    label = font,
                    selected = theme.fontStyle == font,
                    primary = primary,
                    onBackground = onBackground,
                    dividerTint = dividerTint,
                    fontFamily = fontFamily
                ) { onUpdate(theme.copy(fontStyle = font)) }
            }
        }

        SectionLabel("Density", sectionLabelColor)
        val densities = listOf("Compact", "Comfortable", "Spacious")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            densities.forEach { density ->
                ThemedSelectChip(
                    label = density,
                    selected = theme.uiDensity == density,
                    primary = primary,
                    onBackground = onBackground,
                    dividerTint = dividerTint
                ) { onUpdate(theme.copy(uiDensity = density)) }
            }
        }

        // Design: effect rows with switches; Premium features carry a gold pill.
        SectionLabel("Effects", sectionLabelColor)
        EffectSwitchRow(
            title = "Holographic glow",
            subtitle = "Soft accent bloom on bubbles and bars",
            checked = theme.useHolographicGlow,
            primary = primary,
            onBackground = onBackground,
            dividerTint = dividerTint,
            onToggle = { onUpdate(theme.copy(useHolographicGlow = it)) }
        )
        EffectSwitchRow(
            title = "Gradient background",
            subtitle = "Two-stop vertical wallpaper",
            checked = theme.appBackgroundGradientStart != null && theme.appBackgroundGradientEnd != null,
            primary = primary,
            onBackground = onBackground,
            dividerTint = dividerTint,
            onToggle = { enabled ->
                if (enabled) {
                    onUpdate(
                        theme.copy(
                            appBackgroundGradientStart = theme.topBarColor,
                            appBackgroundGradientEnd = theme.bubbleOutgoing
                        )
                    )
                } else {
                    onUpdate(theme.copy(appBackgroundGradientStart = null, appBackgroundGradientEnd = null))
                }
            }
        )
        if (theme.appBackgroundGradientStart != null && theme.appBackgroundGradientEnd != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ColorChip("Grad Start", parseColorOr(Color.Transparent, theme.appBackgroundGradientStart ?: "#FFFFFF"), labelColor = onBackground) { showColorPickerTarget = "grad_start" }
                ColorChip("Grad End", parseColorOr(Color.Transparent, theme.appBackgroundGradientEnd ?: "#FFFFFF"), labelColor = onBackground) { showColorPickerTarget = "grad_end" }
            }
        }
        EffectSwitchRow(
            title = "Glass effect",
            subtitle = "Translucent bubbles with a soft border",
            checked = theme.useGlassEffect,
            primary = primary,
            onBackground = onBackground,
            dividerTint = dividerTint,
            onToggle = { onUpdate(theme.copy(useGlassEffect = it)) }
        )

        // Icon overrides — Premium.
        EffectHeaderRow(
            title = "Icon overrides",
            subtitle = "Swap any icon for your own image URL",
            locked = !isPremium,
            onBackground = onBackground,
            dividerTint = dividerTint
        )
        if (isPremium) {
            val iconKeys = listOf(
                "icon.back" to "Back",
                "icon.settings" to "Settings",
                "icon.lock" to "Private",
                "icon.search" to "Search",
                "icon.close" to "Close",
                "icon.inbox" to "Inbox",
                "icon.archive" to "Archive",
                "icon.unarchive" to "Unarchive",
                "icon.delete" to "Delete",
                "icon.send" to "Send",
                "icon.ai" to "AI",
                "icon.notifications" to "Notifications"
            )
            iconKeys.forEach { (key, label) ->
                OutlinedTextField(
                    value = theme.iconOverrides[key].orEmpty(),
                    onValueChange = { value ->
                        val updated = theme.iconOverrides.toMutableMap()
                        val trimmed = value.trim()
                        if (trimmed.isBlank()) {
                            updated.remove(key)
                        } else {
                            updated[key] = trimmed
                        }
                        onUpdate(theme.copy(iconOverrides = updated))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(label) },
                    placeholder = { Text("https://...") },
                    singleLine = true
                )
            }
        }

        // Background image — Premium.
        EffectHeaderRow(
            title = "Background image",
            subtitle = "Custom wallpaper behind the thread",
            locked = !isPremium,
            onBackground = onBackground,
            dividerTint = dividerTint
        )
        if (isPremium) {
            OutlinedTextField(
                value = theme.backgroundImageUrl.orEmpty(),
                onValueChange = { value ->
                    val trimmed = value.trim()
                    onUpdate(theme.copy(backgroundImageUrl = trimmed.ifBlank { null }))
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Image URL") },
                placeholder = { Text("https://...") },
                singleLine = true
            )
            Text(
                "Recommended: under 1920x1080 and 1.5MB. Images require approval to publish.",
                style = MaterialTheme.typography.bodySmall,
                color = onBackground.copy(alpha = 0.6f)
            )
        }

        SectionLabel("Advanced", sectionLabelColor)
        Text("Advanced corners (overrides base)", style = MaterialTheme.typography.labelLarge, color = onBackground)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Top Start: ${theme.bubbleCornerRadiusTopStart ?: "-"}", color = onBackground)
                Slider(
                    value = (theme.bubbleCornerRadiusTopStart ?: theme.bubbleCornerRadius).toFloat(),
                    onValueChange = { onUpdate(theme.copy(bubbleCornerRadiusTopStart = it.toInt())) },
                    valueRange = 0f..32f,
                    colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary),
                    modifier = Modifier.semantics { contentDescription = "Top start corner radius" }
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Top End: ${theme.bubbleCornerRadiusTopEnd ?: "-"}", color = onBackground)
                Slider(
                    value = (theme.bubbleCornerRadiusTopEnd ?: theme.bubbleCornerRadius).toFloat(),
                    onValueChange = { onUpdate(theme.copy(bubbleCornerRadiusTopEnd = it.toInt())) },
                    valueRange = 0f..32f,
                    colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary),
                    modifier = Modifier.semantics { contentDescription = "Top end corner radius" }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
             Column(modifier = Modifier.weight(1f)) {
                 Text("Bot Start: ${theme.bubbleCornerRadiusBottomStart ?: "-"}", color = onBackground)
                 Slider(
                     value = (theme.bubbleCornerRadiusBottomStart ?: theme.bubbleCornerRadius).toFloat(),
                     onValueChange = { onUpdate(theme.copy(bubbleCornerRadiusBottomStart = it.toInt())) },
                     valueRange = 0f..32f,
                     colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary),
                     modifier = Modifier.semantics { contentDescription = "Bottom start corner radius" }
                 )
             }
             Column(modifier = Modifier.weight(1f)) {
                 Text("Bot End: ${theme.bubbleCornerRadiusBottomEnd ?: "-"}", color = onBackground)
                 Slider(
                     value = (theme.bubbleCornerRadiusBottomEnd ?: theme.bubbleCornerRadius).toFloat(),
                     onValueChange = { onUpdate(theme.copy(bubbleCornerRadiusBottomEnd = it.toInt())) },
                     valueRange = 0f..32f,
                     colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary),
                     modifier = Modifier.semantics { contentDescription = "Bottom end corner radius" }
                 )
             }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Icon scale", style = MaterialTheme.typography.bodyMedium, color = onBackground.copy(alpha = 0.7f))
            Text("${(theme.iconSizeFactor * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = onBackground)
        }
        Slider(
            value = theme.iconSizeFactor,
            onValueChange = { onUpdate(theme.copy(iconSizeFactor = it)) },
            valueRange = 0.5f..1.5f,
            steps = 10,
            colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary),
            modifier = Modifier.semantics { contentDescription = "Icon size scaling" }
        )

        if (isGlobal) {
            Text("Beacon icon", style = MaterialTheme.typography.labelLarge, color = onBackground)
            val label = theme.inboxIconVariant
                .ifBlank { "default" }
                .replace('_', ' ')
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            Text(
                text = "Matches theme: $label",
                style = MaterialTheme.typography.bodySmall,
                color = onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String, color: Color) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
        color = color
    )
}

@Composable
private fun ThemedSelectChip(
    label: String,
    selected: Boolean,
    primary: Color,
    onBackground: Color,
    dividerTint: Color,
    fontFamily: FontFamily = FontFamily.Default,
    onClick: () -> Unit
) {
    val contentColor = if (selected) {
        ensureReadableOnColor(background = primary, desired = Color.Black, fallback = Color.Black)
    } else {
        onBackground
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(if (selected) primary else Color.Transparent)
            .border(1.dp, if (selected) Color.Transparent else dividerTint, RoundedCornerShape(100.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontFamily = fontFamily,
            color = contentColor
        )
    }
}

@Composable
private fun EffectSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    primary: Color,
    onBackground: Color,
    dividerTint: Color,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, dividerTint, RoundedCornerShape(14.dp))
            .clickable { onToggle(!checked) }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = onBackground)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = onBackground.copy(alpha = 0.6f))
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedTrackColor = primary)
        )
    }
}

@Composable
private fun EffectHeaderRow(
    title: String,
    subtitle: String,
    locked: Boolean,
    onBackground: Color,
    dividerTint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, dividerTint, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp)
            .then(if (locked) Modifier.alpha(0.72f) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = onBackground)
                if (locked) {
                    // Design: gold Premium pill.
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0xFFF5C542))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "Premium",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2B1E00)
                        )
                    }
                }
            }
            Text(
                if (locked) "$subtitle — upgrade to unlock" else subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun ColorChip(
    label: String,
    color: Color,
    labelColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clickable(onClick = onClick, role = Role.Button)
            .semantics { contentDescription = "Select $label color" }
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color)
                .border(2.dp, labelColor.takeOrElse { Color.Gray }.copy(alpha = 0.25f), CircleShape)
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = labelColor)
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

private data class ThemePreset(
    val name: String,
    val theme: ThemePreferences
)

@Composable
fun ThemesTab(
    onSelect: (ThemePreferences) -> Unit,
    current: ThemePreferences? = null
) {
    val themes = listOf(
        ThemePreset(
            name = "Future Hologram",
            theme = ThemePreferences(
                fontStyle = "Default",
                bubbleCornerRadius = 24,
                appBackgroundGradientStart = "#0F172A",
                appBackgroundGradientEnd = "#1E1B4B",
                onBackground = "#E2E8F0",
                topBarColor = "#0F172A",
                onTopBarColor = "#E2E8F0",
                bubbleOutgoing = "#22D3EE",
                onBubbleOutgoing = "#FFFFFF",
                bubbleIncoming = "#312E81",
                onBubbleIncoming = "#E2E8F0",
                primaryColor = "#22D3EE",
                secondaryColor = "#D8B4FE",
                dividerColor = "#1E293B",
                inboxIconVariant = "neon_noir",
                useGlassEffect = true,
                useHolographicGlow = true,
                uiDensity = "Comfortable"
            )
        ),
        ThemePreset(
            name = "Neon Cyber",
            theme = ThemePreferences(
                fontStyle = "Monospace",
                bubbleCornerRadius = 4,
                backgroundColor = "#000000",
                onBackground = "#00FF41",
                topBarColor = "#000000",
                onTopBarColor = "#00FF41",
                bubbleOutgoing = "#003B00",
                onBubbleOutgoing = "#00FF41",
                bubbleIncoming = "#0D0D0D",
                onBubbleIncoming = "#00FF41",
                primaryColor = "#00FF41",
                secondaryColor = "#008F11",
                dividerColor = "#003B00",
                inboxIconVariant = "midnight_oled",
                useHolographicGlow = true,
                uiDensity = "Compact"
            )
        ),
        ThemePreset(
            name = "Default Light",
            theme = ThemePreferences(
                fontStyle = "Default",
                bubbleCornerRadius = 12,
                backgroundColor = "#FFFFFF",
                onBackground = "#111827",
                topBarColor = "#F3F4F6",
                onTopBarColor = "#111827",
                bubbleOutgoing = "#D0BCFF",
                onBubbleOutgoing = "#111827",
                bubbleIncoming = "#E8DEF8",
                onBubbleIncoming = "#111827",
                primaryColor = "#6750A4",
                secondaryColor = "#625B71",
                dividerColor = "#E5E7EB",
                inboxIconVariant = "default_light"
            )
        ),
        ThemePreset(
            name = "Midnight OLED",
            theme = ThemePreferences(
                fontStyle = "Default",
                bubbleCornerRadius = 14,
                backgroundColor = "#0B0B0F",
                onBackground = "#F1F5F9",
                topBarColor = "#111827",
                onTopBarColor = "#F8FAFC",
                bubbleOutgoing = "#1F2937",
                onBubbleOutgoing = "#F8FAFC",
                bubbleIncoming = "#0F172A",
                onBubbleIncoming = "#E2E8F0",
                primaryColor = "#38BDF8",
                secondaryColor = "#22D3EE",
                timestampColor = "#94A3B8",
                dividerColor = "#1F2937",
                inboxIconVariant = "midnight_oled"
            )
        ),
        ThemePreset(
            name = "Ocean Deep",
            theme = ThemePreferences(
                fontStyle = "Serif",
                bubbleCornerRadius = 8,
                backgroundColor = "#0F172A",
                onBackground = "#E2E8F0",
                topBarColor = "#1E293B",
                onTopBarColor = "#E2E8F0",
                bubbleOutgoing = "#2563EB",
                onBubbleOutgoing = "#FFFFFF",
                bubbleIncoming = "#334155",
                onBubbleIncoming = "#E2E8F0",
                primaryColor = "#38BDF8",
                secondaryColor = "#1D4ED8",
                dividerColor = "#334155",
                inboxIconVariant = "ocean_deep"
            )
        ),
        ThemePreset(
            name = "Rose Petal",
            theme = ThemePreferences(
                fontStyle = "Cursive",
                bubbleCornerRadius = 18,
                backgroundColor = "#FFF1F2",
                onBackground = "#881337",
                topBarColor = "#FFE4E6",
                onTopBarColor = "#881337",
                bubbleOutgoing = "#FB7185",
                onBubbleOutgoing = "#FFFFFF",
                bubbleIncoming = "#FECACA",
                onBubbleIncoming = "#7F1D1D",
                primaryColor = "#E11D48",
                secondaryColor = "#F43F5E",
                dividerColor = "#FBCFE8",
                inboxIconVariant = "rose_petal"
            )
        ),
        ThemePreset(
            name = "Sunset Fade",
            theme = ThemePreferences(
                fontStyle = "Default",
                bubbleCornerRadius = 24,
                appBackgroundGradientStart = "#FF5F6D",
                appBackgroundGradientEnd = "#FFC371",
                onBackground = "#FFFFFF",
                topBarColor = "#FF5F6D",
                onTopBarColor = "#FFFFFF",
                bubbleOutgoing = "#FFFFFF",
                onBubbleOutgoing = "#FF5F6D",
                bubbleIncoming = "#FFF7ED",
                onBubbleIncoming = "#C2410C",
                primaryColor = "#FF5F6D",
                secondaryColor = "#F97316",
                dividerColor = "#FED7AA",
                inboxIconVariant = "sunset_fade",
                bubbleCornerRadiusTopStart = 0,
                bubbleCornerRadiusBottomEnd = 0
            )
        ),
        ThemePreset(
            name = "Citrus Pop",
            theme = ThemePreferences(
                fontStyle = "Default",
                bubbleCornerRadius = 10,
                backgroundColor = "#F7FEE7",
                onBackground = "#365314",
                topBarColor = "#ECFCCB",
                onTopBarColor = "#365314",
                bubbleOutgoing = "#84CC16",
                onBubbleOutgoing = "#1A2E05",
                bubbleIncoming = "#DCFCE7",
                onBubbleIncoming = "#14532D",
                primaryColor = "#65A30D",
                secondaryColor = "#84CC16",
                dividerColor = "#D9F99D",
                inboxIconVariant = "citrus_pop"
            )
        ),
        ThemePreset(
            name = "Forest Trail",
            theme = ThemePreferences(
                fontStyle = "Serif",
                bubbleCornerRadius = 14,
                backgroundColor = "#ECFDF5",
                onBackground = "#064E3B",
                topBarColor = "#D1FAE5",
                onTopBarColor = "#064E3B",
                bubbleOutgoing = "#059669",
                onBubbleOutgoing = "#ECFDF5",
                bubbleIncoming = "#A7F3D0",
                onBubbleIncoming = "#064E3B",
                primaryColor = "#10B981",
                secondaryColor = "#059669",
                dividerColor = "#A7F3D0",
                inboxIconVariant = "forest_trail"
            )
        ),
        ThemePreset(
            name = "Lavender Haze",
            theme = ThemePreferences(
                fontStyle = "Cursive",
                bubbleCornerRadius = 16,
                backgroundColor = "#F5F3FF",
                onBackground = "#4C1D95",
                topBarColor = "#EDE9FE",
                onTopBarColor = "#4C1D95",
                bubbleOutgoing = "#C4B5FD",
                onBubbleOutgoing = "#312E81",
                bubbleIncoming = "#EDE9FE",
                onBubbleIncoming = "#4C1D95",
                primaryColor = "#7C3AED",
                secondaryColor = "#A78BFA",
                dividerColor = "#DDD6FE",
                inboxIconVariant = "lavender_haze",
                iconSizeFactor = 1.1f
            )
        ),
        ThemePreset(
            name = "Slate Mono",
            theme = ThemePreferences(
                fontStyle = "Monospace",
                bubbleCornerRadius = 6,
                backgroundColor = "#F8FAFC",
                onBackground = "#0F172A",
                topBarColor = "#E2E8F0",
                onTopBarColor = "#0F172A",
                bubbleOutgoing = "#CBD5E1",
                onBubbleOutgoing = "#0F172A",
                bubbleIncoming = "#F1F5F9",
                onBubbleIncoming = "#0F172A",
                primaryColor = "#475569",
                secondaryColor = "#94A3B8",
                dividerColor = "#CBD5E1",
                inboxIconVariant = "slate_mono",
                iconSizeFactor = 0.95f
            )
        ),
        ThemePreset(
            name = "Aurora",
            theme = ThemePreferences(
                fontStyle = "Default",
                bubbleCornerRadius = 20,
                appBackgroundGradientStart = "#0F766E",
                appBackgroundGradientEnd = "#6366F1",
                onBackground = "#F8FAFC",
                topBarColor = "#0F766E",
                onTopBarColor = "#F8FAFC",
                bubbleOutgoing = "#6366F1",
                onBubbleOutgoing = "#FFFFFF",
                bubbleIncoming = "#14B8A6",
                onBubbleIncoming = "#FFFFFF",
                primaryColor = "#14B8A6",
                secondaryColor = "#6366F1",
                dividerColor = "#5EEAD4",
                inboxIconVariant = "aurora",
                iconSizeFactor = 1.15f
            )
        ),
        ThemePreset(
            name = "Desert Clay",
            theme = ThemePreferences(
                fontStyle = "Default",
                bubbleCornerRadius = 12,
                backgroundColor = "#FFF7ED",
                onBackground = "#7C2D12",
                topBarColor = "#FFEDD5",
                onTopBarColor = "#7C2D12",
                bubbleOutgoing = "#EA580C",
                onBubbleOutgoing = "#FFFFFF",
                bubbleIncoming = "#FED7AA",
                onBubbleIncoming = "#7C2D12",
                primaryColor = "#EA580C",
                secondaryColor = "#FDBA74",
                dividerColor = "#FED7AA",
                inboxIconVariant = "desert_clay"
            )
        ),
        ThemePreset(
            name = "Nord Frost",
            theme = ThemePreferences(
                fontStyle = "Default",
                bubbleCornerRadius = 10,
                backgroundColor = "#ECEFF4",
                onBackground = "#2E3440",
                topBarColor = "#E5E9F0",
                onTopBarColor = "#2E3440",
                bubbleOutgoing = "#81A1C1",
                onBubbleOutgoing = "#2E3440",
                bubbleIncoming = "#D8DEE9",
                onBubbleIncoming = "#2E3440",
                primaryColor = "#5E81AC",
                secondaryColor = "#88C0D0",
                dividerColor = "#D8DEE9",
                inboxIconVariant = "nord_frost"
            )
        ),
        ThemePreset(
            name = "Neon Noir",
            theme = ThemePreferences(
                fontStyle = "Default",
                bubbleCornerRadius = 16,
                backgroundColor = "#0B0F14",
                onBackground = "#E2E8F0",
                topBarColor = "#111827",
                onTopBarColor = "#E2E8F0",
                bubbleOutgoing = "#22D3EE",
                onBubbleOutgoing = "#000000",
                bubbleIncoming = "#1F2937",
                onBubbleIncoming = "#E2E8F0",
                primaryColor = "#22D3EE",
                secondaryColor = "#F472B6",
                dividerColor = "#1F2937",
                inboxIconVariant = "neon_noir"
            )
        ),
        ThemePreset(
            name = "Paperback",
            theme = ThemePreferences(
                fontStyle = "Serif",
                bubbleCornerRadius = 14,
                backgroundColor = "#FDF6E3",
                onBackground = "#3F2D1C",
                topBarColor = "#FEF3C7",
                onTopBarColor = "#3F2D1C",
                bubbleOutgoing = "#FCD34D",
                onBubbleOutgoing = "#3F2D1C",
                bubbleIncoming = "#FDE68A",
                onBubbleIncoming = "#3F2D1C",
                primaryColor = "#B45309",
                secondaryColor = "#D97706",
                dividerColor = "#FDE68A",
                inboxIconVariant = "paperback"
            )
        ),
        ThemePreset(
            name = "Mint Breeze",
            theme = ThemePreferences(
                fontStyle = "Default",
                bubbleCornerRadius = 12,
                backgroundColor = "#ECFDF3",
                onBackground = "#064E3B",
                topBarColor = "#D1FAE5",
                onTopBarColor = "#064E3B",
                bubbleOutgoing = "#34D399",
                onBubbleOutgoing = "#064E3B",
                bubbleIncoming = "#A7F3D0",
                onBubbleIncoming = "#064E3B",
                primaryColor = "#10B981",
                secondaryColor = "#34D399",
                dividerColor = "#A7F3D0",
                inboxIconVariant = "mint_breeze"
            )
        ),
        ThemePreset(
            name = "Amethyst Night",
            theme = ThemePreferences(
                fontStyle = "Default",
                bubbleCornerRadius = 18,
                appBackgroundGradientStart = "#312E81",
                appBackgroundGradientEnd = "#0F172A",
                onBackground = "#E2E8F0",
                topBarColor = "#312E81",
                onTopBarColor = "#E2E8F0",
                bubbleOutgoing = "#7C3AED",
                onBubbleOutgoing = "#FFFFFF",
                bubbleIncoming = "#1E293B",
                onBubbleIncoming = "#E2E8F0",
                primaryColor = "#8B5CF6",
                secondaryColor = "#6366F1",
                dividerColor = "#312E81",
                inboxIconVariant = "amethyst_night"
            )
        ),
        ThemePreset(
            name = "Cyber Mist",
            theme = ThemePreferences(
                fontStyle = "Monospace",
                bubbleCornerRadius = 18,
                backgroundColor = "#1e1b4b",
                onBackground = "#e879f9",
                topBarColor = "#2e1065",
                onTopBarColor = "#e879f9",
                bubbleOutgoing = "#d946ef",
                onBubbleOutgoing = "#FFFFFF",
                bubbleIncoming = "#4c1d95",
                onBubbleIncoming = "#e879f9",
                primaryColor = "#d946ef",
                secondaryColor = "#06b6d4",
                dividerColor = "#4c1d95",
                inboxIconVariant = "midnight_oled"
            )
        ),
        ThemePreset(
            name = "Deep Ocean",
            theme = ThemePreferences(
                fontStyle = "Default",
                bubbleCornerRadius = 24,
                backgroundColor = "#0f172a",
                onBackground = "#e0f2fe",
                topBarColor = "#1e3a8a",
                onTopBarColor = "#e0f2fe",
                bubbleOutgoing = "#0ea5e9",
                onBubbleOutgoing = "#FFFFFF",
                bubbleIncoming = "#172554",
                onBubbleIncoming = "#38bdf8",
                primaryColor = "#0ea5e9",
                secondaryColor = "#38bdf8",
                dividerColor = "#1e3a8a",
                inboxIconVariant = "ocean_deep"
            )
        )
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(themes) { preset ->
            // Active when the preset's key colors match the live theme record.
            val active = current != null &&
                current.primaryColor == preset.theme.primaryColor &&
                current.backgroundColor == preset.theme.backgroundColor &&
                current.bubbleOutgoing == preset.theme.bubbleOutgoing &&
                current.bubbleIncoming == preset.theme.bubbleIncoming
            ThemePreviewCard(
                preset = preset,
                isActive = active,
                onSelect = { onSelect(preset.theme) }
            )
        }
    }
}

@Composable
private fun ThemePreviewCard(
    preset: ThemePreset,
    onSelect: () -> Unit,
    isActive: Boolean = false
) {
    val theme = preset.theme
    val topBarColor = parseColorOr(MaterialTheme.colorScheme.surface, theme.topBarColor)
    val onTopBar = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onTopBarColor)
    val iconTint = parseColorOr(MaterialTheme.colorScheme.primary, theme.primaryColor)
    val timestamp = parseColorOr(MaterialTheme.colorScheme.onSurfaceVariant, theme.timestampColor ?: theme.onBackground)
    val divider = parseColorOr(MaterialTheme.colorScheme.outlineVariant, theme.dividerColor ?: theme.onBackground).copy(alpha = 0.2f)
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
        Modifier.background(parseColorOr(MaterialTheme.colorScheme.background, theme.backgroundColor))
    }
    val iconSize = (18f * theme.iconSizeFactor).coerceIn(14f, 24f).dp

    Card(
        onClick = onSelect,
        elevation = CardDefaults.cardElevation(4.dp),
        border = if (isActive) {
            androidx.compose.foundation.BorderStroke(1.5.dp, iconTint)
        } else {
            null
        }
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .then(bgModifier)
                    .border(1.dp, divider, RoundedCornerShape(14.dp))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = topBarColor, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(iconSize)
                            )
                            Text(
                                "Beacon Inbox",
                                style = MaterialTheme.typography.labelMedium,
                                color = onTopBar
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(iconSize)
                                )
                                Icon(
                                    Icons.Filled.Settings,
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(iconSize)
                                )
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Today 9:12 AM",
                            style = MaterialTheme.typography.labelSmall,
                            color = timestamp
                        )
                        PreviewBubble(
                            text = "See you soon!",
                            color = parseColorOr(MaterialTheme.colorScheme.primaryContainer, theme.bubbleOutgoing),
                            textColor = parseColorOr(MaterialTheme.colorScheme.onPrimaryContainer, theme.onBubbleOutgoing),
                            timestampColor = timestamp,
                            align = Alignment.End,
                            theme = theme
                        )
                        PreviewBubble(
                            text = "On my way.",
                            color = parseColorOr(MaterialTheme.colorScheme.surfaceVariant, theme.bubbleIncoming),
                            textColor = parseColorOr(MaterialTheme.colorScheme.onSurfaceVariant, theme.onBubbleIncoming),
                            timestampColor = timestamp,
                            align = Alignment.Start,
                            theme = theme
                        )
                    }
                }
            }
            // Design: name row with the preset's accent dot and an active check.
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(iconTint)
                )
                Text(
                    preset.name,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                    maxLines = 2
                )
                if (isActive) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Active theme",
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
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
    onBubbleClick: (() -> Unit)? = null,
    onTextClick: (() -> Unit)? = null
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
        val bubbleModifier = if (onBubbleClick != null) {
            Modifier
                .clickable(onClick = onBubbleClick, role = Role.Button)
                .semantics { contentDescription = "Change bubble color" }
        } else {
            Modifier
        }
        Surface(
            color = color,
            shape = shape,
            modifier = bubbleModifier
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text,
                    modifier = if (onTextClick != null) {
                        Modifier
                            .clickable(onClick = onTextClick, role = Role.Button)
                            .semantics { contentDescription = "Change text color" }
                    } else {
                        Modifier
                    },
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
