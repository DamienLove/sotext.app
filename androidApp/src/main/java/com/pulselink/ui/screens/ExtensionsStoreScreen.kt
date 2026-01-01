package com.pulselink.ui.screens

import com.pulselink.BuildConfig
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pulselink.R
import com.pulselink.domain.model.PulseLinkSettings

data class FeatureToggle(
    val id: String,
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector,
    val isEnabled: Boolean,
    val onToggle: (Boolean) -> Unit,
    val isAvailable: Boolean = true,
    val requiresPremium: Boolean = false,
    val plannedPriceLabel: String? = null
)

private enum class StoreFilter {
    ALL, INSTALLED, PREMIUM
}

private const val SHOW_PRICE_PREVIEW = false

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
    onToggleThirdPartyExtensions: (Boolean) -> Unit,
    onToggleMergedExperience: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val premiumActive = remember(settings) {
        BuildConfig.PREMIUM_FEATURES || settings.premiumUnlocked
    }

    val features = remember(settings) {
        listOf(
            FeatureToggle(
                id = "beacon",
                titleRes = R.string.extension_beacon_title,
                descriptionRes = R.string.extension_beacon_desc,
                icon = Icons.Filled.WifiTethering,
                isEnabled = settings.beaconLauncherEnabled,
                onToggle = onToggleBeaconLauncher
            ),
            FeatureToggle(
                id = "web",
                titleRes = R.string.extension_web_title,
                descriptionRes = R.string.extension_web_desc,
                icon = Icons.Filled.Laptop,
                isEnabled = settings.remoteWebAccessEnabled,
                onToggle = onToggleRemoteWebAccess,
                requiresPremium = true,
                plannedPriceLabel = "$4.99/mo (planned)"
            ),
            FeatureToggle(
                id = "ai",
                titleRes = R.string.extension_ai_title,
                descriptionRes = R.string.extension_ai_desc,
                icon = Icons.Filled.SmartToy,
                isEnabled = settings.aiSummariesEnabled,
                onToggle = onToggleAiSummaries,
                requiresPremium = true,
                plannedPriceLabel = "$4.99/mo (planned)"
            ),
            FeatureToggle(
                id = "relay",
                titleRes = R.string.extension_relay_title,
                descriptionRes = R.string.extension_relay_desc,
                icon = Icons.Filled.CloudSync,
                isEnabled = settings.firebaseMessagingEnabled,
                onToggle = onToggleFirebaseMessaging
            ),
            FeatureToggle(
                id = "otp",
                titleRes = R.string.extension_otp_title,
                descriptionRes = R.string.extension_otp_desc,
                icon = Icons.Filled.DeleteSweep,
                isEnabled = settings.otpCleanupEnabled,
                onToggle = onToggleOtpCleanup
            ),
            FeatureToggle(
                id = "email_backup",
                titleRes = R.string.extension_email_backup_title,
                descriptionRes = R.string.extension_email_backup_desc,
                icon = Icons.Filled.Email,
                isEnabled = settings.emailFallbackEnabled,
                onToggle = onToggleEmailFallback
            ),
            FeatureToggle(
                id = "third_party_extensions",
                titleRes = R.string.extension_third_party_title,
                descriptionRes = R.string.extension_third_party_desc,
                icon = Icons.Filled.Extension,
                isEnabled = settings.thirdPartyExtensionsEnabled,
                onToggle = onToggleThirdPartyExtensions,
                requiresPremium = true,
                plannedPriceLabel = "$1.99/mo (planned)"
            ),
            FeatureToggle(
                id = "merged_experience",
                titleRes = R.string.extension_merged_title,
                descriptionRes = R.string.extension_merged_desc,
                icon = Icons.Filled.Layers,
                isEnabled = settings.mergedExperienceEnabled,
                onToggle = onToggleMergedExperience,
                requiresPremium = true
            ),
            FeatureToggle(
                id = "crash",
                titleRes = R.string.extension_crash_title,
                descriptionRes = R.string.extension_crash_desc,
                icon = Icons.Filled.CarCrash,
                isEnabled = settings.crashDetectionEnabled,
                onToggle = onToggleCrashDetection,
                isAvailable = true,
                requiresPremium = true
            )
        )
    }

    var activeFilter by remember { mutableStateOf(StoreFilter.ALL) }

    val filteredFeatures = remember(features, activeFilter) {
        when (activeFilter) {
            StoreFilter.ALL -> features
            StoreFilter.INSTALLED -> features.filter { it.isEnabled }
            StoreFilter.PREMIUM -> features.filter { it.requiresPremium }
        }
    }

    // Preset Logic
    fun applyEssentials() {
        onToggleBeaconLauncher(true)
        onToggleOtpCleanup(true)
        onToggleFirebaseMessaging(true)

        // Disable heavy features
        onToggleAiSummaries(false)
        onToggleRemoteWebAccess(false)
        onToggleCrashDetection(false)
        onToggleThirdPartyExtensions(false)
    }

    fun applyPowerUser() {
        onToggleBeaconLauncher(true)
        onToggleOtpCleanup(true)
        onToggleFirebaseMessaging(true)

        // Try enabling premium features if possible, or at least toggle them on (the UI handles locking)
        if (premiumActive) {
            onToggleAiSummaries(true)
            onToggleRemoteWebAccess(true)
            onToggleCrashDetection(true)
            onToggleThirdPartyExtensions(true)
            onToggleEmailFallback(true)
        }
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
                .padding(padding)
        ) {
            // Presets Header
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Quick Setup",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { applyEssentials() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.store_preset_basic))
                        }
                        Button(
                            onClick = { applyPowerUser() },
                            modifier = Modifier.weight(1f),
                            enabled = premiumActive
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.store_preset_pro))
                        }
                    }
                    if (!premiumActive) {
                         Text(
                            text = "Upgrade to Pro to unlock Power User preset",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeFilter == StoreFilter.ALL,
                    onClick = { activeFilter = StoreFilter.ALL },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = activeFilter == StoreFilter.INSTALLED,
                    onClick = { activeFilter = StoreFilter.INSTALLED },
                    label = { Text("Installed") }
                )
                FilterChip(
                    selected = activeFilter == StoreFilter.PREMIUM,
                    onClick = { activeFilter = StoreFilter.PREMIUM },
                    label = { Text("Premium") }
                )
            }

            // Store Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredFeatures) { feature ->
                    StoreItemCard(feature, premiumActive)
                }

                item {
                    ExtensionCTA()
                }
            }
        }
    }
}

@Composable
fun StoreItemCard(feature: FeatureToggle, premiumActive: Boolean) {
    val locked = feature.requiresPremium && !premiumActive

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (feature.isEnabled)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (feature.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = feature.icon,
                                contentDescription = null,
                                tint = if (feature.isEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(feature.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(feature.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    minLines = 2
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Area
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (feature.requiresPremium) {
                    Text(
                        text = "PREMIUM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.extraSmall)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                if (feature.isEnabled) {
                    OutlinedButton(
                        onClick = { feature.onToggle(false) },
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("Remove")
                    }
                } else {
                    Button(
                        onClick = { feature.onToggle(true) },
                        enabled = feature.isAvailable && !locked,
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text(if (locked) "Locked" else "Get")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtensionCTA() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.extensions_store_external_header),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.extensions_store_external_action),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
