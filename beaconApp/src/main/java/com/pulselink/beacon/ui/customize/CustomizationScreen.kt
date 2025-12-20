package com.pulselink.beacon.ui.customize

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulselink.beacon.data.InboxIconVariant
import com.pulselink.beacon.data.ThemeFont
import com.pulselink.beacon.data.ThemePalette
import com.pulselink.beacon.data.ThemeState
import com.pulselink.beacon.data.ThemeTarget
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationScreen(
    address: String?,
    themeState: ThemeState,
    onBack: () -> Unit,
    onColorChange: (ThemeTarget, Color) -> Unit,
    onFontChange: (ThemeFont) -> Unit,
    onRadiusChange: (Float) -> Unit,
    onPreset: (ThemePalette) -> Unit,
    onResetContact: () -> Unit,
    onIconVariant: (InboxIconVariant) -> Unit
) {
    val scopeLabel = address?.takeIf { it.isNotBlank() } ?: "All new chats"
    val currentTheme = themeState.forAddress(address)
    var selectedTarget by remember { mutableStateOf(ThemeTarget.OutgoingBubble) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customize - $scopeLabel") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onColorChange(ThemeTarget.Accent, currentTheme.accentColor) }) {
                        Icon(Icons.Default.ColorLens, contentDescription = "Accent")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScopeBanner(
                address = address,
                onResetContact = onResetContact
            )

            PreviewSample(
                theme = currentTheme,
                selected = selectedTarget,
                onSelect = { selectedTarget = it }
            )

            PopularColorsRow(
                onSelect = { color -> onColorChange(selectedTarget, color) }
            )

            ColorWheel(
                selectedColor = currentTheme.accentColor,
                onSelect = { onColorChange(selectedTarget, it) }
            )

            FontRow(currentTheme.font, onFontChange)

            RadiusSlider(currentTheme.bubbleRadius, onRadiusChange)

            PresetRow(
                onPreset = { onPreset(it) }
            )

            IconRow(currentTheme.iconVariant, onIconVariant)
        }
    }
}

@Composable
private fun ScopeBanner(address: String?, onResetContact: () -> Unit) {
    val isContact = !address.isNullOrBlank()
    Surface(
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isContact) "Contact-specific" else "Global default",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isContact) "Changes override only this contact. Reset to fall back to global." else "Applies to all new chats and inbox.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (isContact) {
                OutlinedButton(onClick = onResetContact) {
                    Text("Use global")
                }
            }
        }
    }
}

@Composable
private fun PreviewSample(
    theme: ThemePalette,
    selected: ThemeTarget,
    onSelect: (ThemeTarget) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = theme.threadBackgroundColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Tap a part to adjust",
                style = MaterialTheme.typography.labelLarge,
                color = theme.frameColor
            )
            BubblePreview(
                text = "Hey! Want to grab coffee later?",
                isOutgoing = false,
                theme = theme,
                selected = selected == ThemeTarget.IncomingBubble,
                onClick = { onSelect(ThemeTarget.IncomingBubble) }
            )
            BubblePreview(
                text = "Sure! 5pm works. Also look at this new inbox icon.",
                isOutgoing = true,
                theme = theme,
                selected = selected == ThemeTarget.OutgoingBubble,
                onClick = { onSelect(ThemeTarget.OutgoingBubble) }
            )
            Surface(
                shape = RoundedCornerShape(theme.bubbleRadius.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, theme.frameColor),
                color = theme.inboxBackgroundColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(ThemeTarget.InboxBackground) }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Inbox preview", fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Default.Shield, contentDescription = null, tint = theme.accentColor)
                }
            }
        }
    }
}

@Composable
private fun BubblePreview(
    text: String,
    isOutgoing: Boolean,
    theme: ThemePalette,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(theme.bubbleRadius.dp)
    Surface(
        color = if (isOutgoing) theme.outgoingColor else theme.incomingColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.frameColor.copy(alpha = 0.6f)),
        shape = shape,
        tonalElevation = if (selected) 3.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = Color.Black)
            Text(
                if (isOutgoing) "You - 5:00 PM" else "Alex - 5:00 PM",
                style = MaterialTheme.typography.labelSmall,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
private fun PopularColorsRow(onSelect: (Color) -> Unit) {
    val popular = listOf(
        Color(0xFF1B6EF3),
        Color(0xFF10B981),
        Color(0xFFFFB020),
        Color(0xFFEF4444),
        Color(0xFF8B5CF6),
        Color(0xFF0EA5E9),
        Color(0xFF111827)
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ColorLens, contentDescription = null, tint = popular.first())
            Spacer(modifier = Modifier.width(6.dp))
            Text("Popular quick-picks")
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(popular.size) { idx ->
                val color = popular[idx]
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { onSelect(color) }
                )
            }
        }
    }
}

@Composable
private fun ColorWheel(selectedColor: Color, onSelect: (Color) -> Unit) {
    val gradient = listOf(
        Color.Red, Color.Magenta, Color.Blue, Color.Cyan, Color.Green, Color.Yellow, Color.Red
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Canvas(
            modifier = Modifier
                .size(220.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val radius = min(size.width, size.height) / 2f
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = offset.x - cx
                        val dy = offset.y - cy
                        val dist = hypot(dx.toDouble(), dy.toDouble())
                        if (dist <= radius.toDouble()) {
                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < 0) angle += 360f
                            val color = Color.hsv(angle, 0.8f, 1f)
                            onSelect(color)
                        }
                    }
                }
        ) {
            drawCircle(brush = Brush.sweepGradient(gradient))
            drawCircle(color = Color.White.copy(alpha = 0.12f), radius = min(size.width, size.height) / 2.6f)
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(selectedColor)
        )
    }
}

@Composable
private fun FontRow(current: ThemeFont, onFontChange: (ThemeFont) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.TextFields, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Font style")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeFont.values().forEach { font ->
                FilterChip(
                    selected = current == font,
                    onClick = { onFontChange(font) },
                    label = { Text(font.label) }
                )
            }
        }
    }
}

@Composable
private fun RadiusSlider(current: Float, onRadiusChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Tune, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Bubble corners")
        }
        Slider(
            value = current,
            onValueChange = { onRadiusChange(it) },
            valueRange = 6f..28f,
            steps = 8
        )
    }
}

@Composable
private fun PresetRow(onPreset: (ThemePalette) -> Unit) {
    val presets = listOf(
        "Clear Sky" to ThemePalette.default().copy(
            incoming = Color(0xFFE8F4FF).toArgb().toLong(),
            outgoing = Color(0xFFD0E8FF).toArgb().toLong(),
            frame = Color(0xFF1B6EF3).toArgb().toLong(),
            accent = Color(0xFF1B6EF3).toArgb().toLong(),
            threadBackground = Color(0xFFF5F8FF).toArgb().toLong()
        ),
        "Midnight OLED" to ThemePalette.default().copy(
            incoming = Color(0xFF111827).toArgb().toLong(),
            outgoing = Color(0xFF0B1220).toArgb().toLong(),
            frame = Color(0xFF10B981).toArgb().toLong(),
            accent = Color(0xFF10B981).toArgb().toLong(),
            threadBackground = Color(0xFF000000).toArgb().toLong(),
            inboxBackground = Color(0xFF050505).toArgb().toLong()
        ),
        "Sunrise" to ThemePalette.default().copy(
            incoming = Color(0xFFFFF3E0).toArgb().toLong(),
            outgoing = Color(0xFFFFE0B2).toArgb().toLong(),
            frame = Color(0xFFFF9800).toArgb().toLong(),
            accent = Color(0xFFFF9800).toArgb().toLong(),
            threadBackground = Color(0xFFFFFBF2).toArgb().toLong()
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Presets", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { (label, palette) ->
                Button(onClick = { onPreset(palette) }) {
                    Text(label)
                }
            }
        }
    }
}

@Composable
private fun IconRow(current: InboxIconVariant, onIconVariant: (InboxIconVariant) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Inbox icon")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InboxIconVariant.values().forEach { variant ->
                FilterChip(
                    selected = current == variant,
                    onClick = { onIconVariant(variant) },
                    label = { Text(variant.label) }
                )
            }
        }
    }
}
