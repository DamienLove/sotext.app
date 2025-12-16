package com.pulselink.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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
                title = stringResource(id = R.string.settings_help_battery_title),
                body = stringResource(id = R.string.settings_help_battery_body)
            )
            HelpSection(
                title = stringResource(id = R.string.settings_help_unused_title),
                body = stringResource(id = R.string.settings_help_unused_body)
            )
            HelpSection(
                title = stringResource(id = R.string.settings_help_default_sms_title),
                body = stringResource(id = R.string.settings_help_default_sms_body)
            )
            HelpSection(
                title = stringResource(id = R.string.settings_help_web_title),
                body = stringResource(id = R.string.settings_help_web_body)
            )
            HelpSection(
                title = stringResource(id = R.string.settings_help_pin_title),
                body = stringResource(id = R.string.settings_help_pin_body)
            )
            HelpSection(
                title = stringResource(id = R.string.settings_help_support_title),
                body = stringResource(id = R.string.settings_help_support_body)
            )

            // Visual separator before FAQ section
            Spacer(Modifier.height(24.dp))
            
            // FAQ section subtitle
            Text(
                text = "Frequently Asked Questions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Spacer(Modifier.height(8.dp))
            FaqList()
        }
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

@Composable
private fun FaqList() {
    Text(
        text = stringResource(id = R.string.settings_faq_section_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 4.dp)
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val items = listOf(
            R.string.faq_question_1 to R.string.faq_answer_1,
            R.string.faq_question_2 to R.string.faq_answer_2,
            R.string.faq_question_3 to R.string.faq_answer_3,
            R.string.faq_question_4 to R.string.faq_answer_4,
            R.string.faq_question_5 to R.string.faq_answer_5,
            R.string.faq_question_6 to R.string.faq_answer_6
        )
        items.forEach { (qRes, aRes) ->
            FaqItem(
                question = stringResource(id = qRes),
                answer = stringResource(id = aRes)
            )
        }
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "faqRotation")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = question,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Text(
                    text = answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
