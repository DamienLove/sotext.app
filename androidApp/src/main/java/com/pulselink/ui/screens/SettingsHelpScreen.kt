package com.pulselink.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.pulselink.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHelpScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(id = R.string.settings_help_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.action_back))
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HelpSection(
                title = "How do I add contacts to PulseLink?",
                body = "Adding contacts is simple. Select the 'add trusted contact' button and you will then be able to add a contact by name and phone/email or select the \"Import from contacts\" button below it. There is an option to allow remote alert changes just above the 'Save' button in the bottom right of the screen."
            )
            HelpSection(
                title = "What happens when I press the 'I am safe' button?",
                body = "When you press the main 'I am safe' button, all your chosen contacts instantly receive a customized alert with your message. They can send you messages, or call for help. The alert goes through PulseLink's secure cloud first, and if contacts are offline or SMS-only."
            )
            HelpSection(
                title = "Can I customize how I receive notifications?",
                body = "Yes. PulseLink uses smart notifications that can reach you via text, call, or app notification—whichever works best. In Settings > Notifications, you can choose different alert tones and vibration patterns for incoming alerts. You can also enable Do Not Disturb exceptions so critical alerts always get through when you need them most."
            )
            HelpSection(
                title = "Does PulseLink drain my battery?",
                body = "PulseLink is optimized for minimal battery drain. It only activates location sharing when an alert is sent or when you press the 'I am safe' button. For best results, allow Battery Optimization mode in Settings. This balances background responsiveness with power efficiency, so you get alerts."
            )
            HelpSection(
                title = "What's the difference between an alert and a check-in?",
                body = "An alert is sent when you press the 'Emergency Alert' button—it immediately notifies all your contacts with your location and marks the situation as active. A check-in is a wellness message that confirms you're safe without urgency. Check-ins are perfect for letting people know you made it home or arrived safely."
            )
            HelpSection(
                title = "How do I remove a contact or stop sharing with them?",
                body = "To remove someone from your PulseLink circle, select the contact you want to remove from the list at the bottom of the home screen, find their name, and then select the settings icon. Scroll down to the bottom of that screen and at the bottom is the 'Delete Contact' button."
            )
            HelpSection(
                title = "What's SMS-only mode and when would I use it?",
                body = "SMS-only mode lets you send and receive alerts via text message without an internet connection. It's perfect for contacts who don't have PulseLink installed yet or prefer text-based communication. Authenticated mode (with a PulseLink account) provides encrypted communication, real-time location sharing, and all premium features. You can use both simultaneously."
            )
            HelpSection(
                title = "How is my location data kept private?",
                body = "Your location is encrypted in transit and at rest on our secure servers. Only the contacts you explicitly choose can see your location. PulseLink never sells your data to third parties or tracks you without permission. You control exactly who sees what—disable location sharing anytime in Settings > Privacy. Your safety and privacy come first."
            )
            HelpSection(
                title = "Why aren't my contacts receiving my alerts?",
                body = "First, check that push notifications are enabled in your phone's Settings. Make sure PulseLink has permission to access your location and send notifications. Verify your internet connection is stable—alerts go through the secure cloud first. If a contact is SMS-only, ensure they have your number saved correctly. Restart the app if alerts suddenly stopped working."
            )
            HelpSection(
                title = "What features are in PulseLink Pro?",
                body = "PulseLink Pro includes advanced customization features like custom alert profiles, priority cloud processing, integration with emergency services, detailed location history, and priority support. You also get early access to beta features as they're developed. Pro users help shape PulseLink's future while getting the most powerful safety tools available."
            )
    }
}

@Composable
private fun HelpSection(
    title: String,
    body: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
