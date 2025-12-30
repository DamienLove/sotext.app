package com.pulselink.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CarCrash
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulselink.R
import com.pulselink.domain.model.PulseLinkSettings

data class Extension(
    val id: String,
    val titleRes: Int,
    val descriptionRes: Int,
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
                titleRes = R.string.extension_beacon_title,
                descriptionRes = R.string.extension_beacon_desc,
                icon = Icons.Filled.WifiTethering,
                isEnabled = settings.beaconLauncherEnabled,
                onToggle = onToggleBeaconLauncher
            ),
            Extension(
                id = "relay",
                titleRes = R.string.extension_relay_title,
                descriptionRes = R.string.extension_relay_desc,
                icon = Icons.Filled.CloudSync,
                isEnabled = settings.firebaseMessagingEnabled,
                onToggle = onToggleFirebaseMessaging
            ),
            Extension(
                id = "email_backup",
                titleRes = R.string.extension_email_backup_title,
                descriptionRes = R.string.extension_email_backup_desc,
                icon = Icons.Filled.Email,
                isEnabled = settings.emailFallbackEnabled,
                onToggle = onToggleEmailFallback
            ),
            Extension(
                id = "web",
                titleRes = R.string.extension_web_title,
                descriptionRes = R.string.extension_web_desc,
                icon = Icons.Filled.Laptop,
                isEnabled = settings.remoteWebAccessEnabled,
                onToggle = onToggleRemoteWebAccess
            ),
            Extension(
                id = "otp",
                titleRes = R.string.extension_otp_title,
                descriptionRes = R.string.extension_otp_desc,
                icon = Icons.Filled.DeleteSweep,
                isEnabled = settings.otpCleanupEnabled,
                onToggle = onToggleOtpCleanup
            ),
            Extension(
                id = "ai",
                titleRes = R.string.extension_ai_title,
                descriptionRes = R.string.extension_ai_desc,
                icon = Icons.Filled.SmartToy,
                isEnabled = settings.aiSummariesEnabled,
                onToggle = onToggleAiSummaries
            ),
            Extension(
                id = "crash",
                titleRes = R.string.extension_crash_title,
                descriptionRes = R.string.extension_crash_desc,
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
                title = { Text(stringResource(R.string.extensions_store_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.extensions_store_header),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.extensions_store_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            extensions.forEach { extension ->
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
                text = stringResource(extension.titleRes),
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
        text = stringResource(extension.descriptionRes),
                style = MaterialTheme.typography.bodyMedium,
                color = if (extension.isEnabled) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!extension.isAvailable) {
        Text(
            text = stringResource(R.string.extensions_store_unavailable),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
