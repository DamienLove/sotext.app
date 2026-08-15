package com.sotext.wear

import android.os.Bundle
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.ZoneId
import java.time.ZonedDateTime
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

private data class Marker(val position: Int, val color: Color)

@Composable
fun PulseLinkWearApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var emergencyActive by remember { mutableStateOf(false) }

    val messageClient = remember { Wearable.getMessageClient(context) }
    val vibrator = remember { context.getSystemService(VibratorManager::class.java)?.defaultVibrator }

    fun pulse(pattern: LongArray) {
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    fun sendCommand(payload: String) {
        scope.launch {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            nodes.forEach { node -> messageClient.sendMessage(node.id, ACTION_PATH, payload.toByteArray()) }
        }
    }

    fun toggleEmergency() {
        emergencyActive = !emergencyActive
        val payload = if (emergencyActive) PAYLOAD_EMERGENCY else PAYLOAD_CANCEL
        sendCommand(payload)
        pulse(if (emergencyActive) longArrayOf(0, 160, 80, 200, 80, 240) else longArrayOf(0, 80, 80, 80))
    }

    val now by produceState(initialValue = ZonedDateTime.now()) {
        while (true) {
            value = ZonedDateTime.now(ZoneId.systemDefault())
            delay(200)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080A0F)),
        contentAlignment = Alignment.Center
    ) {
        WatchFaceCanvas(time = now, emergency = emergencyActive)

        CenterShield(
            modifier = Modifier
                .size(128.dp)
                .pointerInput(emergencyActive) { detectTapGestures(onTap = { toggleEmergency() }) },
            emergency = emergencyActive
        )

        CheckInPill(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            onClick = {
                sendCommand(PAYLOAD_CHECKIN)
                pulse(longArrayOf(0, 60, 60, 60))
            }
        )
    }
}

@Composable
private fun WatchFaceCanvas(time: ZonedDateTime, emergency: Boolean) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2.1f

        // Outer bezel
        drawCircle(
            brush = Brush.sweepGradient(
                listOf(
                    Color(0xFFF2D48D),
                    Color(0xFFD5A84D),
                    Color(0xFFF7E4AF),
                    Color(0xFFD5A84D),
                    Color(0xFFF2D48D)
                )
            ),
            radius = radius,
            center = center
        )

        // Inner face
        drawCircle(
            color = Color(0xFF0C0F16),
            radius = radius * 0.90f,
            center = center
        )

        // Concentric rings
        val ringPaint = Stroke(width = radius * 0.0025f)
        val ringAlpha = 0.18f
        listOf(0.34f, 0.48f, 0.64f).forEach { scale ->
            drawCircle(
                color = Color.White.copy(alpha = ringAlpha),
                radius = radius * scale,
                center = center,
                style = ringPaint
            )
        }

        // Markers
        drawMarkers(center, radius * 0.82f)

        // Hands
        drawHands(center, radius, time)

        // Emergency glow
        if (emergency) {
            drawCircle(
                color = Color(0xFFFF5E5B).copy(alpha = 0.12f),
                radius = radius * 0.88f,
                center = center
            )
        }
    }
}

private fun DrawScope.drawMarkers(center: Offset, radius: Float) {
    val pattern = listOf(
        Color(0xFFECE7D5), // 12
        Color(0xFF00C7B1),
        Color(0xFFECE7D5),
        Color(0xFF00C7B1),
        Color(0xFFECE7D5),
        Color(0xFFE45757),
        Color(0xFF00C7B1),
        Color(0xFFE45757),
        Color(0xFF00C7B1),
        Color(0xFFECE7D5),
        Color(0xFF00C7B1),
        Color(0xFFECE7D5)
    )

    val diamondSize = radius * 0.08f
    val paint = Paint()
    pattern.forEachIndexed { idx, color ->
        val angle = Math.toRadians((idx * 30.0) - 90.0)
        val x = center.x + radius * cos(angle).toFloat()
        val y = center.y + radius * sin(angle).toFloat()
        val path = Path().apply {
            moveTo(x, y - diamondSize / 2)
            lineTo(x + diamondSize / 2, y)
            lineTo(x, y + diamondSize / 2)
            lineTo(x - diamondSize / 2, y)
            close()
        }
        paint.color = color
        drawPath(path = path, color = color)
    }
}

private fun DrawScope.drawHands(center: Offset, radius: Float, time: ZonedDateTime) {
    val millis = time.nano / 1_000_000
    val second = time.second + millis / 1000f
    val minute = time.minute + second / 60f
    val hour = (time.hour % 12) + minute / 60f

    val hourColor = Color(0xFFD8B063)
    val minuteColor = Color(0xFFEDE4CE)

    fun hand(lengthScale: Float, width: Float, angleDeg: Float, color: Color) {
        rotate(angleDeg - 90, center) {
            drawLine(
                color = color,
                start = center,
                end = Offset(center.x + radius * lengthScale, center.y),
                strokeWidth = width,
                cap = Stroke.DefaultCap
            )
        }
    }

    hand(0.46f, radius * 0.014f, hour * 30f, hourColor)
    hand(0.64f, radius * 0.008f, minute * 6f, minuteColor)

    // Second hand (thin, subtle)
    hand(0.70f, radius * 0.004f, second * 6f, Color(0xFFECE7D5).copy(alpha = 0.5f))

    // Center cap and small gear mimic
    drawCircle(color = Color(0xFFECE7D5), radius = radius * 0.018f, center = center)
    drawCircle(color = Color(0xFF0C0F16), radius = radius * 0.009f, center = center)
}

@Composable
private fun CenterShield(modifier: Modifier = Modifier, emergency: Boolean) {
    Box(
        modifier = modifier
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (emergency) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0xFFFF5E5B).copy(alpha = 0.12f), shape = CircleShape)
            )
        }
        Image(
            painter = painterResource(id = R.drawable.ic_wear_logo),
            contentDescription = "SoText",
            modifier = Modifier.size(116.dp)
        )
    }
}

@Composable
private fun CheckInPill(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFF12141F).copy(alpha = 0.8f))
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) }
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFFD6B060),
            modifier = Modifier
                .padding(end = 6.dp)
                .size(14.dp)
        )
        Text(
            text = "CHECK IN",
            color = Color(0xFFD6B060),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
    return suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result -> cont.resume(result) {} }
        addOnFailureListener { error -> cont.resumeWithException(error) }
    }
}
