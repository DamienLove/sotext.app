package com.pulselink.ui.screens

import android.os.Bundle
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.pulselink.domain.model.EmergencyLocation
import com.pulselink.ui.state.EmergencyMapViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyMapScreen(
    onBackClick: () -> Unit,
    viewModel: EmergencyMapViewModel = hiltViewModel()
) {
    val locations by viewModel.locations.collectAsStateWithLifecycle()
    var selectedLocation by remember { mutableStateOf<EmergencyLocation?>(null) }
    val mapView = rememberMapViewWithLifecycle()
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Emergency Map") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .padding(horizontal = 16.dp)
            ) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { mapView },
                    modifier = Modifier.fillMaxSize()
                ) { view ->
                    if (googleMap == null) {
                        view.getMapAsync { map ->
                            googleMap = map.apply {
                                uiSettings.isMapToolbarEnabled = false
                                uiSettings.isMyLocationButtonEnabled = false
                                uiSettings.isCompassEnabled = true
                            }
                        }
                    }
                }
            }

            if (locations.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Place,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "No emergency locations yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(locations, key = { it.id }) { location ->
                        EmergencyLocationItem(
                            location = location,
                            isSelected = selectedLocation?.id == location.id,
                            onSelect = { selectedLocation = location },
                            onClear = { viewModel.clearLocation(location.id) }
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(locations, googleMap) {
        val map = googleMap ?: return@LaunchedEffect
        map.clear()
        if (locations.isEmpty()) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(39.5, -98.35), 3f))
            return@LaunchedEffect
        }
        val bounds = LatLngBounds.builder()
        locations.forEach { location ->
            val position = LatLng(location.lat, location.lng)
            map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(location.sourceName ?: "Emergency")
                    .snippet(DateUtils.getRelativeTimeSpanString(location.createdAt).toString())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
            bounds.include(position)
        }
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 80))
    }

    LaunchedEffect(selectedLocation, googleMap) {
        val map = googleMap ?: return@LaunchedEffect
        val target = selectedLocation ?: return@LaunchedEffect
        val position = LatLng(target.lat, target.lng)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 14f))
    }
}

@Composable
private fun EmergencyLocationItem(
    location: EmergencyLocation,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onClear: () -> Unit
) {
    val timestamp = DateUtils.getRelativeTimeSpanString(
        location.createdAt,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    ).toString()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val title = location.sourceName?.takeIf { it.isNotBlank() }
                        ?: location.sourcePhone?.takeIf { it.isNotBlank() }
                        ?: "Emergency alert"
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = timestamp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(onClick = onClear) {
                    Text("Clear")
                }
            }
            location.message?.let { message ->
                Text(
                    text = message.lineSequence().firstOrNull().orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onSelect, modifier = Modifier.fillMaxWidth()) {
                Text("View on map")
            }
        }
    }
}

@Composable
private fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context).apply { onCreate(Bundle()) } }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    return mapView
}
