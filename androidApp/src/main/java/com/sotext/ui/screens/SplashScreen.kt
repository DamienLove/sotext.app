package com.sotext.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

// Color palette matching the approved SoText splash artwork
private val SplashBgStart = Color(0xFFEBF5FF)
private val SplashBgCenter = Color(0xFFE1F0FF)
private val SplashBgEnd = Color(0xFFF4F8FC)
private val SplashBluePrimary = Color(0xFF1565C0)
private val SplashBlueGradientStart = Color(0xFF1976D2)
private val SplashBlueGradientEnd = Color(0xFF0D47A1)
private val SplashTaglineDark = Color(0xFF1E293B)
private val SplashFooterGrey = Color(0xFF64748B)
private val SplashIllustrationColor = Color(0xFF1565C0).copy(alpha = 0.16f)

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
            .background(
                Brush.verticalGradient(
                    colors = listOf(SplashBgStart, SplashBgCenter, SplashBgEnd)
                )
            )
    ) {
        // Soft central glow effect behind the logo
        Box(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.Center)
                .offset(y = (-40).dp)
                .blur(80.dp)
                .clip(CircleShape)
                .background(Color(0xFF90CAF9).copy(alpha = 0.35f))
        )

        // Vector outline chat illustrations in corners
        CornerChatIllustrations(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 32.dp, vertical = 24.dp)
                .semantics { contentDescription = "SoText splash screen" },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            SoTextIconMark(isUnifiedMode = isUnifiedMode)

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "SoText.",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = SplashBluePrimary,
                    fontSize = 40.sp
                )
            )

            if (badgeText != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = badgeText.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = if (usePremiumBranding) Color(0xFFB58A29) else SplashBluePrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (usePremiumBranding) Color(0xFFFFF8E1) else SplashBluePrimary.copy(alpha = 0.1f)
                        )
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = tagline,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    fontSize = 19.sp,
                    color = SplashTaglineDark
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            // Pill-shaped CTA button matching the artwork
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(50),
                        ambientColor = SplashBlueGradientEnd.copy(alpha = 0.4f),
                        spotColor = SplashBlueGradientEnd.copy(alpha = 0.4f)
                    )
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            listOf(SplashBlueGradientStart, SplashBlueGradientEnd)
                        )
                    )
                    .clickable(onClick = onGetStartedClick)
                    .padding(horizontal = 56.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "GET STARTED",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 17.sp,
                        letterSpacing = 1.2.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "© $brandName. All rights reserved.",
                style = MaterialTheme.typography.labelMedium,
                color = SplashFooterGrey,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Modern SoText Icon Mark:
 * Blue gradient squircle card containing white 'S' and speech bubble 'o' with three blue dots ('...').
 */
@Composable
private fun SoTextIconMark(isUnifiedMode: Boolean) {
    Box(contentAlignment = Alignment.Center) {
        // Soft drop shadow
        Box(
            modifier = Modifier
                .size(120.dp)
                .offset(y = 8.dp)
                .blur(20.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(SplashBlueGradientEnd.copy(alpha = 0.35f))
        )

        // Main squircle card
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(SplashBlueGradientStart, SplashBlueGradientEnd),
                        start = Offset(0f, 0f),
                        end = Offset(300f, 300f)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.offset(x = (-2).dp)
            ) {
                Text(
                    text = "S",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 58.sp
                    )
                )

                Spacer(modifier = Modifier.width(3.dp))

                // Speech bubble 'o' mark with 3 dots inside
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .offset(y = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height * 0.85f
                        val bubblePath = Path().apply {
                            addRoundRect(
                                androidx.compose.ui.geometry.RoundRect(
                                    rect = Rect(0f, 0f, w, h),
                                    cornerRadius = CornerRadius(w * 0.48f)
                                )
                            )
                            // Tail pointing down-left
                            moveTo(w * 0.30f, h)
                            lineTo(w * 0.15f, size.height)
                            lineTo(w * 0.50f, h * 0.92f)
                            close()
                        }
                        drawPath(bubblePath, color = Color.White)

                        // 3 blue dots inside speech bubble
                        val dotRadius = w * 0.07f
                        val centerY = h * 0.50f
                        val startX = w * 0.30f
                        val spacing = w * 0.20f
                        for (i in 0..2) {
                            drawCircle(
                                color = SplashBluePrimary,
                                radius = dotRadius,
                                center = Offset(startX + i * spacing, centerY)
                            )
                        }
                    }
                }
            }
        }

        if (isUnifiedMode) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color(0xFF00E676))
            )
        }
    }
}

/**
 * Helper alias for androidx.compose.ui.geometry.Rect to avoid ambiguity
 */
private typealias Rect = androidx.compose.ui.geometry.Rect

/**
 * Faint vector outline illustrations of chat bubbles, connected nodes, and people icons
 * drawn in the 4 corners of the screen matching the user's splash artwork.
 */
@Composable
private fun CornerChatIllustrations(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Top-Left Cluster: Chat bubbles with dots and background overlap
        drawTopLeftChatCluster(w, h)

        // Top-Right Cluster: Connected circular avatars / chat bubbles
        drawTopRightAvatarCluster(w, h)

        // Bottom-Left Cluster: Stacked message cards
        drawBottomLeftCardCluster(w, h)

        // Bottom-Right Cluster: Speech bubbles with person icons
        drawBottomRightPeopleCluster(w, h)
    }
}

private fun DrawScope.drawTopLeftChatCluster(w: Float, h: Float) {
    val stroke = Stroke(width = 2.dp.toPx())

    // Background bubble (top-left)
    drawRoundRect(
        color = SplashIllustrationColor,
        topLeft = Offset(w * 0.05f, h * 0.03f),
        size = Size(w * 0.32f, h * 0.08f),
        cornerRadius = CornerRadius(18.dp.toPx()),
        style = stroke
    )

    // Foreground bubble with 3 dots (offset)
    val frontX = w * 0.08f
    val frontY = h * 0.07f
    val frontW = w * 0.35f
    val frontH = h * 0.09f
    drawRoundRect(
        color = SplashIllustrationColor,
        topLeft = Offset(frontX, frontY),
        size = Size(frontW, frontH),
        cornerRadius = CornerRadius(20.dp.toPx()),
        style = stroke
    )

    // Tail for front bubble
    val tailPath = Path().apply {
        moveTo(frontX + 24.dp.toPx(), frontY + frontH)
        lineTo(frontX + 10.dp.toPx(), frontY + frontH + 14.dp.toPx())
        lineTo(frontX + 44.dp.toPx(), frontY + frontH)
        close()
    }
    drawPath(tailPath, color = SplashIllustrationColor, style = stroke)

    // 3 dots in front bubble
    val dotY = frontY + frontH * 0.5f
    val dotStartX = frontX + frontW * 0.35f
    val dotSpacing = frontW * 0.15f
    for (i in 0..2) {
        drawCircle(
            color = SplashIllustrationColor,
            radius = 3.5.dp.toPx(),
            center = Offset(dotStartX + i * dotSpacing, dotY)
        )
    }
}

private fun DrawScope.drawTopRightAvatarCluster(w: Float, h: Float) {
    val stroke = Stroke(width = 2.dp.toPx())

    val r1 = 30.dp.toPx()
    val c1 = Offset(w * 0.72f, h * 0.10f)

    val r2 = 32.dp.toPx()
    val c2 = Offset(w * 0.88f, h * 0.15f)

    // Connecting line between avatar bubbles
    drawLine(
        color = SplashIllustrationColor,
        start = c1,
        end = c2,
        strokeWidth = 2.dp.toPx()
    )

    // Avatar bubble 1
    drawCircle(color = SplashIllustrationColor, radius = r1, center = c1, style = stroke)
    drawPersonOutline(c1, r1 * 0.7f)

    // Avatar bubble 2
    drawCircle(color = SplashIllustrationColor, radius = r2, center = c2, style = stroke)
    drawPersonOutline(c2, r2 * 0.7f)
}

private fun DrawScope.drawBottomLeftCardCluster(w: Float, h: Float) {
    val stroke = Stroke(width = 2.dp.toPx())

    // Back card
    drawRoundRect(
        color = SplashIllustrationColor,
        topLeft = Offset(w * 0.04f, h * 0.78f),
        size = Size(w * 0.35f, h * 0.10f),
        cornerRadius = CornerRadius(16.dp.toPx()),
        style = stroke
    )

    // Front message card
    val frontX = w * 0.10f
    val frontY = h * 0.83f
    val frontW = w * 0.38f
    val frontH = h * 0.11f

    drawRoundRect(
        color = SplashIllustrationColor,
        topLeft = Offset(frontX, frontY),
        size = Size(frontW, frontH),
        cornerRadius = CornerRadius(18.dp.toPx()),
        style = stroke
    )

    // Faint text line outlines inside front message card
    drawLine(
        color = SplashIllustrationColor,
        start = Offset(frontX + 16.dp.toPx(), frontY + 20.dp.toPx()),
        end = Offset(frontX + frontW - 24.dp.toPx(), frontY + 20.dp.toPx()),
        strokeWidth = 2.5.dp.toPx()
    )
    drawLine(
        color = SplashIllustrationColor,
        start = Offset(frontX + 16.dp.toPx(), frontY + 36.dp.toPx()),
        end = Offset(frontX + frontW * 0.65f, frontY + 36.dp.toPx()),
        strokeWidth = 2.5.dp.toPx()
    )

    // Person profile circle bottom-left
    drawCircle(
        color = SplashIllustrationColor,
        radius = 22.dp.toPx(),
        center = Offset(w * 0.10f, h * 0.94f),
        style = stroke
    )
}

private fun DrawScope.drawBottomRightPeopleCluster(w: Float, h: Float) {
    val stroke = Stroke(width = 2.dp.toPx())

    // Large chat bubble
    val bX = w * 0.62f
    val bY = h * 0.79f
    val bW = w * 0.32f
    val bH = h * 0.12f

    drawRoundRect(
        color = SplashIllustrationColor,
        topLeft = Offset(bX, bY),
        size = Size(bW, bH),
        cornerRadius = CornerRadius(20.dp.toPx()),
        style = stroke
    )

    // Faint text lines inside
    drawLine(
        color = SplashIllustrationColor,
        start = Offset(bX + 18.dp.toPx(), bY + 22.dp.toPx()),
        end = Offset(bX + bW - 18.dp.toPx(), bY + 22.dp.toPx()),
        strokeWidth = 2.5.dp.toPx()
    )
    drawLine(
        color = SplashIllustrationColor,
        start = Offset(bX + 18.dp.toPx(), bY + 38.dp.toPx()),
        end = Offset(bX + bW * 0.7f, bY + 38.dp.toPx()),
        strokeWidth = 2.5.dp.toPx()
    )

    // Person profile circle bottom-right corner
    val pCenter = Offset(w * 0.90f, h * 0.92f)
    drawCircle(
        color = SplashIllustrationColor,
        radius = 26.dp.toPx(),
        center = pCenter,
        style = stroke
    )
    drawPersonOutline(pCenter, 26.dp.toPx() * 0.7f)
}

private fun DrawScope.drawPersonOutline(center: Offset, scale: Float) {
    val stroke = Stroke(width = 1.8.dp.toPx())
    // Head circle
    drawCircle(
        color = SplashIllustrationColor,
        radius = scale * 0.35f,
        center = Offset(center.x, center.y - scale * 0.25f),
        style = stroke
    )
    // Shoulders arc
    val shoulderPath = Path().apply {
        addArc(
            oval = Rect(
                center.x - scale * 0.6f,
                center.y + scale * 0.05f,
                center.x + scale * 0.6f,
                center.y + scale * 0.95f
            ),
            startAngleDegrees = 190f,
            sweepAngleDegrees = 160f
        )
    }
    drawPath(shoulderPath, color = SplashIllustrationColor, style = stroke)
}

private fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)
