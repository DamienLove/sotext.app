package com.pulselink.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulselink.domain.model.PulseLinkSettings
import com.pulselink.domain.model.TimeFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeaconSettingsScreen(
    settings: PulseLinkSettings,
    onBack: () -> Unit,
    onTimeFormatChange: (TimeFormat) -> Unit,
    onOpenVisualSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Beacon Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BeaconSettingsActionRow(
                title = "Time format",
                subtitle = when (settings.timeFormat) {
                    TimeFormat.AUTO -> "Follow device"
                    TimeFormat.TWELVE_HOUR -> "12-hour"
                    TimeFormat.TWENTY_FOUR_HOUR -> "24-hour"
                },
                actionLabel = "Change",
                onAction = {
                    val next = when (settings.timeFormat) {
                        TimeFormat.AUTO -> TimeFormat.TWELVE_HOUR
                        TimeFormat.TWELVE_HOUR -> TimeFormat.TWENTY_FOUR_HOUR
                        TimeFormat.TWENTY_FOUR_HOUR -> TimeFormat.AUTO
                    }
                    onTimeFormatChange(next)
                },
                leadingIcon = Icons.Filled.AccessTime
            )
            BeaconSettingsActionRow(
                title = "Visual customization",
                subtitle = "Colors, layout, icons, and chat appearance.",
                actionLabel = "Open",
                onAction = onOpenVisualSettings,
                leadingIcon = Icons.Filled.Palette
            )
        }
    }
}

@Composable
private fun BeaconSettingsActionRow(
    title: String,
    subtitle: String? = null,
    actionLabel: String,
    onAction: () -> Unit,
    leadingIcon: ImageVector
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            TextButton(onClick = onAction) {
                Text(text = actionLabel)
            }
        }
    }
}
