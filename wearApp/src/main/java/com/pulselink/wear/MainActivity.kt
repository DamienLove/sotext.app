package com.pulselink.wear

import android.os.Bundle
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PulseLinkWearApp() }
    }
}

private const val ACTION_PATH = "/pulselink/action"
private const val PAYLOAD_EMERGENCY = "EMERGENCY"
private const val PAYLOAD_CANCEL = "CANCEL"
private const val PAYLOAD_CHECKIN = "CHECKIN"

@Composable
fun PulseLinkWearApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var emergencyActive by remember { mutableStateOf(false) }
    var contactStates by remember { mutableStateOf(dummyStatuses()) }

    val messageClient = remember { Wearable.getMessageClient(context) }
    val vibrator = remember {
        val vm = context.getSystemService(VibratorManager::class.java)
        vm?.defaultVibrator
    }

    fun sendCommand(payload: String) {
        scope.launch {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            nodes.forEach { node ->
                messageClient.sendMessage(node.id, ACTION_PATH, payload.toByteArray())
            }
        }
    }

    fun pulse(hertz: Int = 120) {
        vibrator?.vibrate(VibrationEffect.createOneShot(80, hertz))
    }

    val background = Brush.radialGradient(
        colors = listOf(Color(0xFF111118), Color(0xFF0A0A0F)),
        center = Offset.Zero,
        radius = 900f
    )

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
        ) {
            GoldenRing(statuses = contactStates, modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EmergencyPill(active = emergencyActive)

                EmergencyButton(
                    active = emergencyActive,
                    onLongPress = {
                        emergencyActive = !emergencyActive
                        val payload = if (emergencyActive) PAYLOAD_EMERGENCY else PAYLOAD_CANCEL
                        sendCommand(payload)
                        pulse(if (emergencyActive) 180 else 120)
                    }
                )

                CheckInButton {
                    sendCommand(PAYLOAD_CHECKIN)
                    pulse(120)
                }
            }
        }
    }
}

@Composable
private fun EmergencyPill(active: Boolean) {
    val bg = if (active) Color(0xFFB71C1C) else Color(0xFFD32F2F)
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg.copy(alpha = 0.9f))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("EMERGENCY", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("PULSELINK PRO", color = Color.White, fontSize = 10.sp)
        }
    }
}

@Composable
private fun EmergencyButton(active: Boolean, onLongPress: () -> Unit) {
    val centerColor = if (active) Color(0xFFFF7043) else Color(0xFFD50000)
    val outerColor = if (active) Color(0xFFFFA726) else Color(0xFFFF5252)
    Box(
        modifier = Modifier
            .size(180.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(centerColor, outerColor)))
            .pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress() }) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (active) "HOLD TO CANCEL" else "HOLD FOR EMERGENCY",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun CheckInButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B1C28)),
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
    ) {
        Text(text = "✓  Check-in", color = Color(0xFFFFD54F), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun GoldenRing(statuses: List<ContactStatus>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.padding(8.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2.2f
        val ringBrush = Brush.radialGradient(listOf(Color(0xFFE7B55A), Color(0xFFC08A2F)))
        drawCircle(brush = ringBrush, radius = radius + 10f, center = center)
        drawCircle(color = Color(0xFF0B0E16), radius = radius - 10f, center = center)

        val dots = statuses.take(12)
        val dotRadius = 12f
        dots.forEachIndexed { index, status ->
            val angle = Math.toRadians((index * 30.0) - 90)
            val x = center.x + radius * cos(angle).toFloat()
            val y = center.y + radius * sin(angle).toFloat()
            val dotColor = when (status.state) {
                ContactState.ONLINE -> Color(0xFF66E0A3)
                ContactState.UNTRUSTED -> Color(0xFFFFC857)
                ContactState.OFFLINE -> Color(0xFFB0BEC5)
            }
            drawCircle(color = dotColor, radius = dotRadius, center = Offset(x, y))
            drawCircle(color = Color(0xFFFAF3DD), radius = dotRadius / 2.4f, center = Offset(x, y))
        }
    }
}

enum class ContactState { ONLINE, OFFLINE, UNTRUSTED }

data class ContactStatus(val id: Int, val state: ContactState)

private fun dummyStatuses(): List<ContactStatus> =
    List(12) { idx ->
        val state = when (idx % 3) {
            0 -> ContactState.ONLINE
            1 -> ContactState.UNTRUSTED
            else -> ContactState.OFFLINE
        }
        ContactStatus(idx, state)
    }

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
    return suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result -> cont.resume(result) {} }
        addOnFailureListener { error -> cont.resumeWithException(error) }
    }
}
