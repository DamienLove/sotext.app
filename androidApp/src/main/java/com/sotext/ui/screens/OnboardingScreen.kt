package com.sotext.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sotext.R
import com.sotext.domain.model.ThemePreferences
import androidx.compose.material.icons.filled.VpnKey

data class OnboardingPermissionState(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val granted: Boolean,
    val manualHelp: String? = null,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val emphasis: String? = null
)

/**
 * Design: "starting look" theme presets offered during onboarding.
 * Values mirror the presets in VisualSettingsScreen / the SoText prototype.
 */
data class StartingLook(
    val name: String,
    val swatchStart: Color,
    val swatchEnd: Color,
    val theme: ThemePreferences
)

val startingLooks: List<StartingLook> = listOf(
    StartingLook(
        name = "Midnight OLED",
        swatchStart = Color(0xFF38BDF8),
        swatchEnd = Color(0xFF38BDF8),
        theme = ThemePreferences(
            fontStyle = "Default", bubbleCornerRadius = 14,
            backgroundColor = "#0B0B0F", onBackground = "#F1F5F9",
            topBarColor = "#111827", onTopBarColor = "#F8FAFC",
            bubbleOutgoing = "#1F2937", onBubbleOutgoing = "#F8FAFC",
            bubbleIncoming = "#0F172A", onBubbleIncoming = "#E2E8F0",
            primaryColor = "#38BDF8", secondaryColor = "#22D3EE",
            timestampColor = "#94A3B8", dividerColor = "#1F2937",
            inboxIconVariant = "midnight_oled"
        )
    ),
    StartingLook(
        name = "Aurora",
        swatchStart = Color(0xFF0F766E),
        swatchEnd = Color(0xFF6366F1),
        theme = ThemePreferences(
            fontStyle = "Default", bubbleCornerRadius = 20,
            appBackgroundGradientStart = "#0F766E", appBackgroundGradientEnd = "#6366F1",
            onBackground = "#F8FAFC",
            topBarColor = "#0F766E", onTopBarColor = "#F8FAFC",
            bubbleOutgoing = "#6366F1", onBubbleOutgoing = "#FFFFFF",
            bubbleIncoming = "#14B8A6", onBubbleIncoming = "#FFFFFF",
            primaryColor = "#14B8A6", secondaryColor = "#6366F1",
            dividerColor = "#5EEAD4", inboxIconVariant = "aurora",
            iconSizeFactor = 1.15f
        )
    ),
    StartingLook(
        name = "Lavender Haze",
        swatchStart = Color(0xFF7C3AED),
        swatchEnd = Color(0xFF7C3AED),
        theme = ThemePreferences(
            fontStyle = "Cursive", bubbleCornerRadius = 16,
            backgroundColor = "#F5F3FF", onBackground = "#4C1D95",
            topBarColor = "#EDE9FE", onTopBarColor = "#4C1D95",
            bubbleOutgoing = "#C4B5FD", onBubbleOutgoing = "#312E81",
            bubbleIncoming = "#EDE9FE", onBubbleIncoming = "#4C1D95",
            primaryColor = "#7C3AED", secondaryColor = "#A78BFA",
            dividerColor = "#DDD6FE", inboxIconVariant = "lavender_haze",
            iconSizeFactor = 1.1f
        )
    ),
    StartingLook(
        name = "Sunset Fade",
        swatchStart = Color(0xFFFF5F6D),
        swatchEnd = Color(0xFFFFC371),
        theme = ThemePreferences(
            fontStyle = "Default", bubbleCornerRadius = 24,
            appBackgroundGradientStart = "#FF5F6D", appBackgroundGradientEnd = "#FFC371",
            onBackground = "#FFFFFF",
            topBarColor = "#FF5F6D", onTopBarColor = "#FFFFFF",
            bubbleOutgoing = "#FFFFFF", onBubbleOutgoing = "#FF5F6D",
            bubbleIncoming = "#FFF7ED", onBubbleIncoming = "#C2410C",
            primaryColor = "#FF5F6D", secondaryColor = "#F97316",
            dividerColor = "#FED7AA", inboxIconVariant = "sunset_fade",
            bubbleCornerRadiusTopStart = 0, bubbleCornerRadiusBottomEnd = 0
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingIntroScreen(
    modifier: Modifier = Modifier,
    ownerName: String,
    onOwnerNameChange: (String) -> Unit,
    onContinue: () -> Unit,
    onApplyTheme: ((ThemePreferences) -> Unit)? = null,
    currentTheme: ThemePreferences? = null
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF10131F), Color(0xFF0B0D16))
    )
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .background(gradient)
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .padding(horizontal = 22.dp, vertical = 26.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Design: gradient logo tile.
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF38BDF8), Color(0xFF22D3EE))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = "SoText logo",
                    modifier = Modifier.size(32.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Make texting\nlook like you.",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium),
                    lineHeight = MaterialTheme.typography.headlineMedium.fontSize * 1.15,
                    color = Color.White
                )
                Text(
                    text = "So.Text is your default SMS app. Pick a starting look now — every color, corner and font stays editable later.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.72f)
                )
            }

            OnboardingSectionLabel("Your name")
            OutlinedTextField(
                value = ownerName,
                onValueChange = onOwnerNameChange,
                placeholder = { Text("Your name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = "We include this name when contacting your trusted partners so they know it's you.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFCD34D)
            )

            if (onApplyTheme != null) {
                // Design: starting-look preset chips.
                OnboardingSectionLabel("Starting look")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    startingLooks.forEach { look ->
                        val selected = currentTheme != null &&
                            currentTheme.primaryColor == look.theme.primaryColor &&
                            currentTheme.bubbleOutgoing == look.theme.bubbleOutgoing
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                                .border(
                                    1.dp,
                                    if (selected) Color.Transparent else Color.White.copy(alpha = 0.18f),
                                    RoundedCornerShape(100.dp)
                                )
                                .clickable { onApplyTheme(look.theme) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(look.swatchStart, look.swatchEnd)
                                        )
                                    )
                            )
                            Text(
                                text = look.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) Color(0xFF03151F) else Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.material3.Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = ownerName.isNotBlank()
            ) {
                Text(text = "Continue to permissions")
            }
        }
    }
}

@Composable
private fun OnboardingSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
        color = Color.White.copy(alpha = 0.55f)
    )
}

@Composable
private fun IntroBullet(text: String) {
    val secondaryTextColor = Color(0xFFDEE2FF)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF67DBA0)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = secondaryTextColor
        )
    }
}

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    permissions: List<OnboardingPermissionState>,
    focusedPermission: OnboardingPermissionState? = null,
    isReadyToFinish: Boolean,
    onGrantPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onBack: () -> Unit,
    extraSection: @Composable (() -> Unit)? = null
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF10131F), Color(0xFF0B0D16))
    )
    val manualHelp = when {
        focusedPermission != null && !focusedPermission.granted && !focusedPermission.manualHelp.isNullOrBlank() -> focusedPermission.manualHelp
        else -> permissions.firstOrNull { it.manualHelp != null && !it.granted }?.manualHelp
    }
    val activeFocus = focusedPermission?.takeIf { !it.granted }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .background(gradient)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .clip(RoundedCornerShape(28.dp)),
            color = Color.White.copy(alpha = 0.04f)
        ) {}

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "SoText logo",
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Enable critical permissions",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Grant these permissions so SoText can protect you even when your phone is silenced.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFCBD5F5),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (activeFocus != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.Top
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (!activeFocus.emphasis.isNullOrBlank()) {
                            Text(
                                text = activeFocus.emphasis,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFCD34D),
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        PermissionCard(state = activeFocus)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(permissions) { card ->
                            PermissionCard(state = card)
                        }
                    }
                }
            }
            extraSection?.invoke()
            if (!manualHelp.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = manualHelp,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFCD34D)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onOpenAppSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open app settings")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.Button(
                onClick = onGrantPermissions,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (isReadyToFinish) "Finish setup" else "Grant permissions")
            }
        }
    }
}

@Composable
private fun PermissionCard(state: OnboardingPermissionState) {
    val statusColor = if (state.granted) MaterialTheme.colorScheme.primary else Color(0xFFF59E0B)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = statusColor.copy(alpha = 0.18f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = state.icon,
                        contentDescription = state.title,
                        tint = statusColor,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = state.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5F5)
                    )
                }
                // Design: pill-shaped status — outlined once granted, filled while pending.
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = if (state.granted) Color.Transparent else statusColor,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (state.granted) Color.White.copy(alpha = 0.18f) else Color.Transparent
                    )
                ) {
                    Text(
                        text = if (state.granted) "Granted" else "Allow",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.granted) Color.White.copy(alpha = 0.8f) else Color(0xFF1A1200),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            if (!state.emphasis.isNullOrBlank() && !state.granted) {
                Text(
                    text = state.emphasis,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFF87171)
                )
            }
            if (!state.granted && state.actionLabel != null && state.onAction != null) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.manualHelp ?: "Tap to grant",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFCD34D),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = state.onAction) {
                        Text(text = state.actionLabel)
                    }
                }
            }
        }
    }
}

@Composable
fun OtpCleanupOnboardingCard(
    enabled: Boolean,
    days: Int,
    onToggle: (Boolean) -> Unit,
    onChangeDays: (Int) -> Unit
) {
    val retentionLabel = when (days) {
        1 -> "1 day"
        3 -> "3 days"
        7 -> "7 days"
        30 -> "30 days"
        else -> "$days days"
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.VpnKey,
                        contentDescription = "2-step cleanup",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "2-step code cleanup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = if (enabled) {
                            "Auto-delete 2-step messages after $retentionLabel."
                        } else {
                            "Keep 2-step messages unless you delete them."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5F5)
                    )
                }
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
            TextButton(
                onClick = {
                    val next = when (days) {
                        1 -> 3
                        3 -> 7
                        7 -> 30
                        30 -> 1
                        else -> 1
                    }
                    onChangeDays(next)
                },
                enabled = enabled
            ) {
                Text(text = "Change window")
            }
        }
    }
}
@Composable
fun BetaAgreementScreen(
    ownerName: String,
    agreementVersion: String,
    isSubmitting: Boolean,
    onViewFullAgreement: () -> Unit,
    onAgree: () -> Unit,
    onBack: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val gradient = if (isDarkTheme) {
        Brush.verticalGradient(colors = listOf(Color(0xFF10131F), Color(0xFF0B0D16)))
    } else {
        Brush.verticalGradient(colors = listOf(Color(0xFFE5E9FF), Color(0xFFF9FAFF)))
    }
    val panelColor = if (isDarkTheme) {
        Color.White.copy(alpha = 0.06f)
    } else {
        Color.White
    }
    val headingColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onBackground
    val bodyColor = if (isDarkTheme) Color(0xFFD6DCFF) else MaterialTheme.colorScheme.onSurfaceVariant
    val metaColor = if (isDarkTheme) Color(0xFFBACCFF) else MaterialTheme.colorScheme.primary
    val actionColor = if (isDarkTheme) Color(0xFF7FB2FF) else MaterialTheme.colorScheme.primary
    val dividerColor = if (isDarkTheme) {
        Color.White.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    }
    val displayName = if (ownerName.isBlank()) {
        stringResource(id = R.string.beta_agreement_tester_default)
    } else ownerName

    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    var actionHeightPx by remember { mutableIntStateOf(0) }
    val contentBottomPadding = if (actionHeightPx == 0) {
        128.dp
    } else {
        with(density) { actionHeightPx.toDp() } + 16.dp
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .background(gradient)
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(28.dp))
                .background(panelColor)
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = contentBottomPadding)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = headingColor
                            )
                        }
                        Text(
                            text = stringResource(
                                id = R.string.beta_agreement_tester_label,
                                displayName,
                                agreementVersion
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = metaColor,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                    }

                    Text(
                        text = stringResource(id = R.string.beta_agreement_summary_title),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = headingColor
                    )
                    Text(
                        text = stringResource(id = R.string.beta_agreement_summary_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = bodyColor
                    )
                    Text(
                        text = stringResource(id = R.string.beta_agreement_summary_points),
                        style = MaterialTheme.typography.bodyMedium,
                        color = bodyColor
                    )
                    Text(
                        text = stringResource(id = R.string.beta_agreement_summary_footer),
                        style = MaterialTheme.typography.bodyMedium,
                        color = bodyColor
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        actionHeightPx = coordinates.size.height
                    },
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalDivider(color = dividerColor)
                TextButton(
                    onClick = onViewFullAgreement,
                    colors = ButtonDefaults.textButtonColors(contentColor = actionColor)
                ) {
                    Text(
                        text = stringResource(id = R.string.beta_agreement_view_full),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Button(
                    onClick = onAgree,
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(text = stringResource(id = R.string.beta_agreement_agree_button))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BetaAgreementFullScreen(
    onBack: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val gradient = if (isDarkTheme) {
        Brush.verticalGradient(colors = listOf(Color(0xFF0B0D16), Color(0xFF05060B)))
    } else {
        Brush.verticalGradient(colors = listOf(Color(0xFFE7EBFF), Color(0xFFF9FAFF)))
    }
    val textColor = if (isDarkTheme) Color(0xFFE6EAFF) else MaterialTheme.colorScheme.onBackground
    val scrollState = rememberScrollState()
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = textColor,
                    navigationIconContentColor = textColor
                ),
                title = { Text(text = stringResource(id = R.string.beta_agreement_full_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.beta_agreement_full_text),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
    }
}
