package com.pulselink.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactCreateScreen(
    initialName: String?,
    initialPhone: String?,
    initialEmail: String?,
    initialAvatarUrl: String?,
    profileLoading: Boolean,
    onSave: (String, String, String?, String?) -> Unit,
    onBack: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName.orEmpty()) }
    var phone by rememberSaveable { mutableStateOf(initialPhone.orEmpty()) }
    var email by rememberSaveable { mutableStateOf(initialEmail.orEmpty()) }
    var avatarUrl by rememberSaveable { mutableStateOf(initialAvatarUrl.orEmpty()) }
    var nameTouched by rememberSaveable { mutableStateOf(false) }
    var phoneTouched by rememberSaveable { mutableStateOf(false) }
    var emailTouched by rememberSaveable { mutableStateOf(false) }
    var avatarTouched by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initialName) {
        if (!nameTouched && !initialName.isNullOrBlank()) {
            name = initialName
        }
    }
    LaunchedEffect(initialPhone) {
        if (!phoneTouched && !initialPhone.isNullOrBlank()) {
            phone = initialPhone
        }
    }
    LaunchedEffect(initialEmail) {
        if (!emailTouched && !initialEmail.isNullOrBlank()) {
            email = initialEmail
        }
    }
    LaunchedEffect(initialAvatarUrl) {
        if (!avatarTouched && !initialAvatarUrl.isNullOrBlank()) {
            avatarUrl = initialAvatarUrl
        }
    }

    val canSave = name.isNotBlank() && phone.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create contact") },
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
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val initial = name.firstOrNull()?.uppercase() ?: "?"
                val avatarBackground = MaterialTheme.colorScheme.primaryContainer
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(avatarBackground),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(avatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = initial.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (profileLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Looking up profile...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    nameTouched = true
                    name = it
                },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phoneTouched = true
                    phone = it
                },
                label = { Text("Phone number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email,
                onValueChange = {
                    emailTouched = true
                    email = it
                },
                label = { Text("Email (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = avatarUrl,
                onValueChange = {
                    avatarTouched = true
                    avatarUrl = it
                },
                label = { Text("Avatar URL (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onSave(name.trim(), phone.trim(), email.trim().takeIf { it.isNotBlank() }, avatarUrl.trim().takeIf { it.isNotBlank() }) },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save contact")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
