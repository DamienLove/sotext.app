package com.pulselink.beacon

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.pulselink.beacon.data.ThemePalette
import com.pulselink.beacon.data.ThemeFont

@Composable
fun BeaconTheme(
    theme: ThemePalette = ThemePalette.default(),
    content: @Composable () -> Unit
) {
    val lightColors = lightColorScheme(
        primary = theme.accentColor,
        onPrimary = Color.Black,
        primaryContainer = theme.outgoingColor,
        secondary = theme.incomingColor,
        surface = theme.inboxBackgroundColor,
        background = theme.inboxBackgroundColor,
        onSurface = Color.Black,
        onSecondary = Color.Black
    )
    val shapes = Shapes(
        extraSmall = MaterialTheme.shapes.extraSmall,
        small = MaterialTheme.shapes.small,
        medium = androidx.compose.foundation.shape.RoundedCornerShape(theme.bubbleRadius.dp),
        large = MaterialTheme.shapes.large,
        extraLarge = MaterialTheme.shapes.extraLarge
    )
    MaterialTheme(
        colorScheme = lightColors,
        shapes = shapes,
        typography = MaterialTheme.typography.copy(
            bodyMedium = MaterialTheme.typography.bodyMedium.merge(theme.font.toTextStyle()),
            bodyLarge = MaterialTheme.typography.bodyLarge.merge(theme.font.toTextStyle()),
            titleMedium = MaterialTheme.typography.titleMedium.merge(theme.font.toTextStyle()),
            labelLarge = MaterialTheme.typography.labelLarge.merge(theme.font.toTextStyle())
        ),
        content = content
    )
}

private fun ThemeFont.toTextStyle(): TextStyle =
    TextStyle(fontFamily = family, fontWeight = weight)
