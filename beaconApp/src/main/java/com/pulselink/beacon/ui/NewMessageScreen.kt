package com.pulselink.beacon.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pulselink.beacon.ui.theme.ThemePalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMessageScreen(
    theme: ThemePalette,
    onBack: () -> Unit,
    onStartConversation: (String) -> Unit,
) {
    var phoneNumber by remember { mutableStateOf("") }
    val isValid = phoneNumber.isNotBlank()
        var messageText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Message") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.frameColor,
                    titleContentColor = theme.textColor,
                    navigationIconContentColor = theme.textColor
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onStartConversation(phoneNumber.trim()) },
                containerColor = theme.accentColor,
                enabled = isValid
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Start conversation"
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        containerColor = theme.inboxBackgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter phone number") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Phone"
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.accentColor,
                    cursorColor = theme.accentColor
                )
            )

                            Spacer(modifier = Modifier.height(16.dp))

                                            OutlinedTextField(
                                                                    value = messageText,
                                                                                        onValueChange = { messageText = it },
                                                                                                            modifier = Modifier.fillMaxWidth(),
                                                                                                                                label = { Text("Message") },
                                                                                                                                                    placeholder = { Text("Enter message") },
                                                                                                                                                                        minLines = 3,
                                                                                                                                                                                            maxLines = 5,
                                                                                                                                                                                                                colors = OutlinedTextFieldDefaults.colors(
                                                                                                                                                                                                                                            focusedBorderColor = theme.accentColor,
                                                                                                                                                                                                                                                                    cursorColor = theme.accentColor
                                                                                                                                                                                                                                                                                        )
                                                                                                                                                                                                                                                                                                        )
        }
    }
}
