package com.sotext.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Palette matching the new light-themed SoText splash artwork
private val SplashBgLight = Color(0xFFF8FAFF)
private val SplashBrandingBlue = Color(0xFF0056D2)
private val SplashLogoGradientStart = Color(0xFF1E88E5)
private val SplashLogoGradientEnd = Color(0xFF1565C0)
private val SplashTextGrey = Color(0xFF455A64)
private val SplashIconLight = Color(0xFFE3E9F5)

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    usePremiumBranding: Boolean = false,
    brandName: String = "SoText",
    badgeText: String? = null,
    isUnifiedMode: Boolean = false,
    tagline: String = "Simple. Fast. Reliable SMS.",
    onGetStartedClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SplashBgLight)
    ) {
        FaintChatBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 32.dp, vertical = 24.dp)
                .semantics { contentDescription = "SoText splash screen" },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1.2f))

            ModernLogoMark()

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "$brandName.",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = SplashBrandingBlue,
                    letterSpacing = (-1).sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = tagline,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = SplashBrandingBlue.copy(alpha = 0.9f)
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "GET STARTED",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.verticalGradient(
                            listOf(SplashLogoGradientStart, SplashLogoGradientEnd)
                        )
                    )
                    .clickable(onClick = onGetStartedClick)
                    .padding(horizontal = 48.dp, vertical = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "© $brandName. All rights reserved.",
                style = MaterialTheme.typography.labelMedium,
                color = SplashTextGrey.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ModernLogoMark() {
    Box(
        modifier = Modifier
            .size(160.dp)
            .shadow(12.dp, RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.verticalGradient(
                    listOf(SplashLogoGradientStart, SplashLogoGradientEnd)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(100.dp)) {
            val w = size.width
            val h = size.height

            // The "S" mark
            val sPath = Path().apply {
                moveTo(w * 0.25f, h * 0.3f)
                cubicTo(w * 0.25f, h * 0.15f, w * 0.75f, h * 0.15f, w * 0.75f, h * 0.35f)
                cubicTo(w * 0.75f, h * 0.45f, w * 0.25f, h * 0.55f, w * 0.25f, h * 0.65f)
                cubicTo(w * 0.25f, h * 0.85f, w * 0.75f, h * 0.85f, w * 0.75f, h * 0.7f)
            }
            drawPath(
                sPath,
                color = Color.White,
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
            )

            // Chat bubble icon overlapping the S
            val bubbleRect = androidx.compose.ui.geometry.Rect(w * 0.55f, h * 0.35f, w * 0.95f, h * 0.65f)
            val bubblePath = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        bubbleRect,
                        androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                    )
                )
                // Tail
                moveTo(w * 0.8f, h * 0.65f)
                lineTo(w * 0.9f, h * 0.75f)
                lineTo(w * 0.9f, h * 0.6f)
            }
            drawPath(bubblePath, color = SplashIconLight.copy(alpha = 0.9f))
            
            // Three dots in bubble
            val dotR = 1.5.dp.toPx()
            drawCircle(SplashLogoGradientEnd, dotR, Offset(w * 0.68f, h * 0.5f))
            drawCircle(SplashLogoGradientEnd, dotR, Offset(w * 0.75f, h * 0.5f))
            drawCircle(SplashLogoGradientEnd, dotR, Offset(w * 0.82f, h * 0.5f))
        }
    }
}

@Composable
private fun FaintChatBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val color = SplashIconLight
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)

        // Top-left cluster
        drawChatBubble(Offset(50.dp.toPx(), 80.dp.toPx()), 60.dp.toPx(), color, stroke)
        drawChatBubble(Offset(140.dp.toPx(), 40.dp.toPx()), 40.dp.toPx(), color, stroke)

        // Top-right cluster
        drawChatBubble(Offset(size.width - 120.dp.toPx(), 60.dp.toPx()), 80.dp.toPx(), color, stroke)
        drawChatBubble(Offset(size.width - 60.dp.toPx(), 150.dp.toPx()), 50.dp.toPx(), color, stroke)

        // Bottom-left cluster
        drawChatBubble(Offset(80.dp.toPx(), size.height - 200.dp.toPx()), 100.dp.toPx(), color, stroke)
        drawChatBubble(Offset(40.dp.toPx(), size.height - 80.dp.toPx()), 60.dp.toPx(), color, stroke)

        // Bottom-right cluster
        drawChatBubble(Offset(size.width - 150.dp.toPx(), size.height - 150.dp.toPx()), 120.dp.toPx(), color, stroke)
    }
}

private fun DrawScope.drawChatBubble(
    center: Offset,
    width: Float,
    color: Color,
    style: Stroke
) {
    val height = width * 0.7f
    val rect = androidx.compose.ui.geometry.Rect(
        center.x - width / 2,
        center.y - height / 2,
        center.x + width / 2,
        center.y + height / 2
    )
    drawRoundRect(
        color = color,
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
        style = style
    )
    // Three lines inside
    val lineW = width * 0.6f
    val startX = center.x - lineW / 2
    drawLine(color, Offset(startX, center.y - 6.dp.toPx()), Offset(startX + lineW, center.y - 6.dp.toPx()), style.width)
    drawLine(color, Offset(startX, center.y), Offset(startX + lineW, center.y), style.width)
    drawLine(color, Offset(startX, center.y + 6.dp.toPx()), Offset(startX + lineW * 0.6f, center.y + 6.dp.toPx()), style.width)
}
