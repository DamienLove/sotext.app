package com.pulselink.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulselink.domain.model.PulseLinkSettings

data class Extension(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isEnabled: Boolean,
    val onToggle: (Boolean) -> Unit,
    val isAvailable: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsStoreScreen(
    settings: PulseLinkSettings,
    onToggleBeaconLauncher: (Boolean) -> Unit,
    onToggleFirebaseMessaging: (Boolean) -> Unit,
    onToggleEmailFallback: (Boolean) -> Unit,
    onToggleCrashDetection: (Boolean) -> Unit,
    onToggleOtpCleanup: (Boolean) -> Unit,
    onToggleRemoteWebAccess: (Boolean) -> Unit,
    onToggleAiSummaries: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val extensions = remember(settings) {
        listOf(
            Extension(
                id = "beacon",
                title = "Beacon Inbox",
                description = "A focused, distraction-free SMS inbox with privacy features and custom themes.",
                icon = Icons.Filled.WifiTethering,
                isEnabled = settings.beaconLauncherEnabled,
                onToggle = onToggleBeaconLauncher
            ),
            Extension(
                id = "relay",
                title = "Cloud Relay",
                description = "Sync messages between devices using Firebase. Faster delivery than SMS.",
                icon = Icons.Filled.CloudSync,
                isEnabled = settings.firebaseMessagingEnabled,
                onToggle = onToggleFirebaseMessaging
            ),
            Extension(
                id = "email_backup",
                title = "Email Backup",
                description = "Automatically send critical alerts via email when SMS fails.",
                icon = Icons.Filled.Email,
                isEnabled = settings.emailFallbackEnabled,
                onToggle = onToggleEmailFallback
            ),
            Extension(
                id = "web",
                title = "Web Access",
                description = "Manage your messages and settings from a computer browser.",
                icon = Icons.Filled.Laptop,
                isEnabled = settings.remoteWebAccessEnabled,
                onToggle = onToggleRemoteWebAccess
            ),
            Extension(
                id = "otp",
                title = "Smart OTP Cleanup",
                description = "Automatically delete one-time password messages after 24 hours.",
                icon = Icons.Filled.DeleteSweep,
                isEnabled = settings.otpCleanupEnabled,
                onToggle = onToggleOtpCleanup
            ),
            Extension(
                id = "ai",
                title = "AI Assistant",
                description = "Smart summaries, urgency detection, and reply suggestions.",
                icon = Icons.Filled.SmartToy,
                isEnabled = settings.aiSummariesEnabled,
                onToggle = onToggleAiSummaries
            ),
            Extension(
                id = "crash",
                title = "Crash Detection",
                description = "Automatically alert contacts if a vehicle crash is detected.",
                icon = Icons.Filled.CarCrash,
                isEnabled = settings.crashDetectionEnabled,
                onToggle = onToggleCrashDetection,
                isAvailable = false // Temporarily unavailable as per existing code
            )
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Extensions Store") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Customize your experience",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Enable only the features you need to keep PulseLink simple and fast.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
            }

            items(extensions) { extension ->
                ExtensionCard(extension)
            }
        }
    }
}

@Composable
fun ExtensionCard(extension: Extension) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (extension.isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = extension.icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (extension.isEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = extension.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (extension.isEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
                Switch(
                    checked = extension.isEnabled,
                    onCheckedChange = extension.onToggle,
                    enabled = extension.isAvailable
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = extension.description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (extension.isEnabled) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!extension.isAvailable) {
                 Text(
                    text = "Temporarily unavailable",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
