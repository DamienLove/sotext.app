package com.pulselink.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.ThemePreferences
import com.pulselink.util.parseColorOr

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewMessageScreen(
    contacts: List<Contact>,
    onBack: () -> Unit,
    onCreateConversation: (List<Contact>) -> Unit,
    onManualInput: (String) -> Unit,
    theme: ThemePreferences
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedContacts by remember { mutableStateOf(emptyList<Contact>()) }

    val filteredContacts = remember(searchQuery, contacts, selectedContacts) {
        val candidates = if (searchQuery.isBlank()) contacts else contacts.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
            it.phoneNumber.contains(searchQuery)
        }
        candidates.filter { c -> selectedContacts.none { it.id == c.id } }
    }

    val primaryColor = parseColorOr(MaterialTheme.colorScheme.primary, theme.primaryColor)
    val onPrimaryColor = parseColorOr(MaterialTheme.colorScheme.onPrimary, theme.onBubbleOutgoing)

    Scaffold(
        containerColor = parseColorOr(MaterialTheme.colorScheme.background, theme.backgroundColor),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("New Message", color = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onTopBarColor)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onTopBarColor))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = parseColorOr(MaterialTheme.colorScheme.surface, theme.topBarColor)
                )
            )
        },
        floatingActionButton = {
            if (selectedContacts.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { onCreateConversation(selectedContacts) },
                    containerColor = primaryColor,
                    contentColor = onPrimaryColor
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Create")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Selected Contacts Chips
            if (selectedContacts.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedContacts.forEach { contact ->
                        InputChip(
                            selected = true,
                            onClick = {
                                selectedContacts = selectedContacts - contact
                            },
                            label = { Text(contact.displayName) },
                            trailingIcon = {
                                Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                            },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = primaryColor.copy(alpha = 0.2f),
                                selectedLabelColor = primaryColor
                            )
                        )
                    }
                }
            }

            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("To: Name or number") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )

            // Manual Entry Option if search is numeric
            if (searchQuery.isNotBlank() && searchQuery.any { it.isDigit() }) {
                 Row(
                     modifier = Modifier
                         .fillMaxWidth()
                         .clickable {
                             // If manual input, we treat it as a direct navigation for now
                             // Or we could create a temp Contact.
                             // For simplicity/safety per user request, just navigate directly.
                             onManualInput(searchQuery)
                         }
                         .padding(16.dp),
                     verticalAlignment = Alignment.CenterVertically
                 ) {
                     Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.padding(end = 16.dp))
                     Text("Send to $searchQuery", style = MaterialTheme.typography.bodyLarge)
                 }
            }

            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp), // Space for FAB
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredContacts) { contact ->
                    ContactRow(contact, theme) {
                        // Add to selection
                        selectedContacts = selectedContacts + contact
                        searchQuery = "" // Reset search
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(contact: Contact, theme: ThemePreferences, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Avatar placeholder
        Box(
            modifier = Modifier
                .padding(4.dp)
        ) {
            // We can reuse AvatarCircle from Inbox if we make it public or duplicate logic
            Text(
                text = contact.displayName.firstOrNull()?.toString() ?: "#",
                style = MaterialTheme.typography.titleLarge,
                color = parseColorOr(MaterialTheme.colorScheme.primary, theme.primaryColor)
            )
        }

        Column {
            Text(
                text = contact.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onBackground)
            )
            if (contact.phoneNumber.isNotBlank()) {
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = parseColorOr(MaterialTheme.colorScheme.onSurfaceVariant, theme.onBackground).copy(alpha = 0.7f)
                )
            }
        }
    }
}
