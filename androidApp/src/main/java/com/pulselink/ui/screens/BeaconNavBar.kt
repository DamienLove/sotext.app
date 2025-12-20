package com.pulselink.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.pulselink.domain.model.ThemePreferences
import com.pulselink.util.parseColorOr

@Composable
fun BeaconNavBar(
    currentRoute: BeaconNavRoute,
    onNavigate: (BeaconNavRoute) -> Unit,
    theme: ThemePreferences
) {
    NavigationBar(
        containerColor = parseColorOr(MaterialTheme.colorScheme.surface, theme.topBarColor),
        contentColor = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onTopBarColor)
    ) {
        NavigationBarItem(
            selected = currentRoute == BeaconNavRoute.Inbox,
            onClick = { onNavigate(BeaconNavRoute.Inbox) },
            icon = { Icon(Icons.Filled.History, contentDescription = "Recent") },
            label = { Text("All") }
        )
        NavigationBarItem(
            selected = currentRoute == BeaconNavRoute.Otp,
            onClick = { onNavigate(BeaconNavRoute.Otp) },
            icon = { Icon(Icons.Filled.VpnKey, contentDescription = "2-step") },
            label = { Text("2-step") }
        )
        NavigationBarItem(
            selected = currentRoute == BeaconNavRoute.Trusted,
            onClick = { onNavigate(BeaconNavRoute.Trusted) },
            icon = { Icon(Icons.Filled.VerifiedUser, contentDescription = "Trusted") },
            label = { Text("Trusted") }
        )
        NavigationBarItem(
            selected = currentRoute == BeaconNavRoute.Favorites,
            onClick = { onNavigate(BeaconNavRoute.Favorites) },
            icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorites") },
            label = { Text("Favorites") }
        )
        NavigationBarItem(
            selected = currentRoute == BeaconNavRoute.Private,
            onClick = { onNavigate(BeaconNavRoute.Private) },
            icon = { Icon(Icons.Filled.Lock, contentDescription = "Private") },
            label = { Text("Private") }
        )
    }
}

enum class BeaconNavRoute {
    Inbox,
    Otp,
    Trusted,
    Favorites,
    Private
}
