package com.pulselink.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivatePinScreen(
    hasPin: Boolean,
    onSavePin: (String?) -> Unit,
    onBack: () -> Unit
) {
    val (pin, setPin) = remember { mutableStateOf("") }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (hasPin) "Change private PIN" else "Set private PIN") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Set a PIN to lock private contacts and chats.")
            OutlinedTextField(
                value = pin,
                onValueChange = setPin,
                label = { Text("PIN") },
                visualTransformation = PasswordVisualTransformation()
            )
            Button(onClick = { onSavePin(pin.takeIf { it.isNotBlank() }) }) {
                Text(if (hasPin) "Update PIN" else "Save PIN")
            }
            if (hasPin) {
                Button(onClick = { onSavePin(null) }) {
                    Text("Clear PIN")
                }
            }
        }
    }
}
