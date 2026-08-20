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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Palette for the approved SoText splash artwork: a near-white page lit by a soft
// blue bloom behind the app mark, faint line-art messaging icons drifting across the
// top and bottom, and a navy-to-blue icon tile with a solid blue call-to-action.
private val SplashPageBase = Color(0xFFEFF6FC)
private val SplashBloom = Color(0xFF8FC9EC)
private val SplashWatermark = Color(0xFFD4E6F4)
private val SplashTileTop = Color(0xFF2E6DB4)
private val SplashTileBottom = Color(0xFF122A60)
private val SplashWordmarkLight = Color(0xFF3A8FD0)
private val SplashWordmarkDeep = Color(0xFF2166AE)
private val SplashTagline = Color(0xFF1B4F86)
private val SplashButtonTop = Color(0xFF2F86CE)
private val SplashButtonBottom = Color(0xFF1B62AC)
private val SplashFootnote = Color(0xFF8FA6BC)
private val SplashBubbleDot = Color(0xFFBBD9F2)
private val SplashBadgeSurface = Color(0xFFDCEBF8)

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
            .drawWithCache {
                // The bloom is anchored on the app mark rather than the geometric
                // centre, so the light reads as coming *from* the logo.
                val bloomCenter = Offset(size.width * 0.5f, size.height * 0.42f)
                val bloom = Brush.radialGradient(
                    colors = listOf(
                        SplashBloom.copy(alpha = 0.60f),
                        SplashBloom.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = bloomCenter,
                    radius = size.width * 0.95f
                )
                val core = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.95f), Color.Transparent),
                    center = bloomCenter,
                    radius = size.width * 0.45f
                )
                onDrawBehind {
                    drawRect(SplashPageBase)
                    drawRect(bloom)
                    drawRect(core)
                }
            }
    ) {
        MessagingWatermarks(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 32.dp, vertical = 24.dp)
                .semantics { contentDescription = "SoText splash screen" },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            AppIconTile(isUnifiedMode = isUnifiedMode)

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "$brandName.",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 54.sp,
                    lineHeight = 60.sp,
                    fontWeight = FontWeight.Bold,
                    brush = Brush.verticalGradient(
                        listOf(SplashWordmarkLight, SplashWordmarkDeep)
                    )
                )
            )

            if (badgeText != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = badgeText.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp
                    ),
                    color = if (usePremiumBranding) SplashWordmarkDeep else SplashTagline,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(SplashBadgeSurface)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = tagline,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 26.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                ),
                color = SplashTagline
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Get Started".uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp,
                    textAlign = TextAlign.Center
                ),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.verticalGradient(listOf(SplashButtonTop, SplashButtonBottom))
                    )
                    .clickable(onClick = onGetStartedClick)
                    .padding(vertical = 19.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "© $brandName. All rights reserved.",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = SplashFootnote
            )
        }
    }
}

/** The rounded app-icon tile: an "S" beside a speech bubble that stands in for the "o". */
@Composable
private fun AppIconTile(isUnifiedMode: Boolean) {
    Box(contentAlignment = Alignment.Center) {
        // Soft drop shadow so the tile lifts off the pale background.
        Box(
            modifier = Modifier
                .size(124.dp)
                .offset(y = 12.dp)
                .blur(24.dp)
                .clip(RoundedCornerShape(34.dp))
                .background(SplashTileBottom.copy(alpha = 0.28f))
        )

        Box(
            modifier = Modifier
                .size(136.dp)
                .clip(RoundedCornerShape(38.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(SplashTileTop, SplashTileBottom),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // The mark is artwork, not body copy: pin it to dp so an accessibility
            // font scale can never push the "S" outside the fixed-size tile.
            val markSize = with(LocalDensity.current) { 72.dp.toSp() }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.offset(y = (-2).dp)
            ) {
                Text(
                    text = "S",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = markSize,
                        lineHeight = markSize,
                        fontWeight = FontWeight.Black
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(2.dp))
                SpeechBubbleGlyph(modifier = Modifier.size(58.dp))
            }

            if (isUnifiedMode) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-12).dp, y = (-12).dp)
                        .clip(CircleShape)
                        .background(SplashBubbleDot)
                )
            }
        }
    }
}

/** White speech bubble with a down-left tail and three pale-blue typing dots. */
@Composable
private fun SpeechBubbleGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val bubbleRadius = size.width * 0.42f
        val bubbleCenter = Offset(size.width * 0.5f, size.height * 0.44f)

        val tail = Path().apply {
            moveTo(size.width * 0.30f, size.height * 0.76f)
            lineTo(size.width * 0.13f, size.height * 0.99f)
            lineTo(size.width * 0.50f, size.height * 0.88f)
            close()
        }
        drawPath(tail, color = Color.White)
        drawCircle(color = Color.White, radius = bubbleRadius, center = bubbleCenter)

        val dotRadius = size.width * 0.062f
        listOf(0.28f, 0.50f, 0.72f).forEach { fraction ->
            drawCircle(
                color = SplashBubbleDot,
                radius = dotRadius,
                center = Offset(size.width * fraction, bubbleCenter.y)
            )
        }
    }
}

private enum class WatermarkKind { BUBBLE_LINES, BUBBLE_DOTS, BUBBLE_EMPTY, PERSON, PERSON_HEADSET }

/**
 * Placement of one line-art watermark, expressed in fractions of the screen so the
 * scatter keeps its composition across phone sizes.
 */
private data class Watermark(
    val kind: WatermarkKind,
    val x: Float,
    val y: Float,
    val widthFraction: Float,
    val flipped: Boolean = false
)

/**
 * Faint outlined chat bubbles and contact avatars drifting across the top and bottom
 * of the page, framing the app mark without competing with it.
 */
@Composable
private fun MessagingWatermarks(modifier: Modifier = Modifier) {
    val watermarks = remember {
        listOf(
            // Top band.
            Watermark(WatermarkKind.BUBBLE_LINES, 0.030f, 0.035f, 0.26f),
            Watermark(WatermarkKind.BUBBLE_DOTS, 0.110f, 0.128f, 0.19f),
            Watermark(WatermarkKind.BUBBLE_LINES, 0.560f, 0.025f, 0.26f, flipped = true),
            Watermark(WatermarkKind.BUBBLE_EMPTY, 0.585f, 0.098f, 0.125f, flipped = true),
            Watermark(WatermarkKind.PERSON, 0.700f, 0.155f, 0.135f),
            Watermark(WatermarkKind.PERSON, 0.845f, 0.150f, 0.135f),
            // Bottom band.
            Watermark(WatermarkKind.BUBBLE_LINES, 0.030f, 0.735f, 0.27f),
            Watermark(WatermarkKind.BUBBLE_LINES, 0.055f, 0.825f, 0.235f),
            Watermark(WatermarkKind.BUBBLE_LINES, 0.420f, 0.775f, 0.25f, flipped = true),
            Watermark(WatermarkKind.PERSON_HEADSET, 0.800f, 0.795f, 0.145f),
            Watermark(WatermarkKind.BUBBLE_DOTS, 0.460f, 0.880f, 0.20f)
        )
    }

    Canvas(modifier = modifier) {
        watermarks.forEach { drawWatermark(it) }
    }
}

private fun DrawScope.drawWatermark(mark: Watermark) {
    val width = size.width * mark.widthFraction
    val origin = Offset(size.width * mark.x, size.height * mark.y)
    val stroke = Stroke(width = (width * 0.045f).coerceIn(2f, 6f), cap = StrokeCap.Round)
    when (mark.kind) {
        WatermarkKind.BUBBLE_LINES -> drawBubble(origin, width, stroke, lines = 3, flipped = mark.flipped)
        WatermarkKind.BUBBLE_DOTS -> drawBubble(origin, width, stroke, lines = 0, dots = true, flipped = mark.flipped)
        WatermarkKind.BUBBLE_EMPTY -> drawBubble(origin, width, stroke, lines = 0, flipped = mark.flipped)
        WatermarkKind.PERSON -> drawPerson(origin, width, stroke, headset = false)
        WatermarkKind.PERSON_HEADSET -> drawPerson(origin, width, stroke, headset = true)
    }
}

/** Rounded speech bubble outline with an optional stack of message lines or typing dots. */
private fun DrawScope.drawBubble(
    origin: Offset,
    width: Float,
    stroke: Stroke,
    lines: Int,
    dots: Boolean = false,
    flipped: Boolean
) {
    val height = width * 0.72f
    val corner = CornerRadius(width * 0.22f, width * 0.22f)
    drawRoundRect(
        color = SplashWatermark,
        topLeft = origin,
        size = Size(width, height),
        cornerRadius = corner,
        style = stroke
    )

    // Tail hangs from the lower-left edge, or mirrors to the right when flipped.
    val sign = if (flipped) -1f else 1f
    val tailRoot = if (flipped) origin.x + width * 0.84f else origin.x + width * 0.16f
    val baseline = origin.y + height - stroke.width * 0.5f
    val tail = Path().apply {
        moveTo(tailRoot, baseline)
        lineTo(tailRoot - sign * width * 0.09f, origin.y + height + height * 0.30f)
        lineTo(tailRoot + sign * width * 0.15f, baseline)
    }
    drawPath(tail, color = SplashWatermark, style = stroke)

    if (dots) {
        val dotRadius = width * 0.055f
        listOf(0.30f, 0.50f, 0.70f).forEach { fraction ->
            drawCircle(
                color = SplashWatermark,
                radius = dotRadius,
                center = Offset(origin.x + width * fraction, origin.y + height * 0.5f)
            )
        }
        return
    }

    repeat(lines) { index ->
        // Last line is short, the way a wrapped message tapers off.
        val lineWidth = if (index == lines - 1) width * 0.42f else width * 0.62f
        val y = origin.y + height * (0.30f + index * 0.20f)
        drawLine(
            color = SplashWatermark,
            start = Offset(origin.x + width * 0.19f, y),
            end = Offset(origin.x + width * 0.19f + lineWidth, y),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round
        )
    }
}

/** Contact avatar: a circle head over shoulders, optionally wearing a support headset. */
private fun DrawScope.drawPerson(origin: Offset, width: Float, stroke: Stroke, headset: Boolean) {
    val center = Offset(origin.x + width * 0.5f, origin.y + width * 0.5f)
    drawCircle(color = SplashWatermark, radius = width * 0.46f, center = center, style = stroke)
    drawCircle(color = SplashWatermark, radius = width * 0.17f, center = Offset(center.x, center.y - width * 0.13f), style = stroke)

    // Shoulders, clipped by the surrounding circle so they read as an avatar crop.
    drawArc(
        color = SplashWatermark,
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(center.x - width * 0.30f, center.y + width * 0.10f),
        size = Size(width * 0.60f, width * 0.52f),
        style = stroke
    )

    if (headset) {
        drawArc(
            color = SplashWatermark,
            startAngle = 190f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(center.x - width * 0.30f, center.y - width * 0.42f),
            size = Size(width * 0.60f, width * 0.52f),
            style = stroke
        )
    }
}
