package com.sotext.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

// Palette matching the approved SoText splash artwork: a navy-to-royal-blue field
// with violet/magenta glow blooming from the top-right and bottom-left corners,
// and a cyan-to-violet glass "S" mark at the center.
private val SplashDeepBlue = Color(0xFF0A1550)
private val SplashRoyalBlue = Color(0xFF1E49D6)
private val SplashMidBlue = Color(0xFF23409E)
private val SplashViolet = Color(0xFF8A3FD6)
private val SplashMagenta = Color(0xFFC24FDB)
private val SplashCyan = Color(0xFF5EE8D8)
private val SplashSkyBlue = Color(0xFF4FC3F7)

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    usePremiumBranding: Boolean = false,
    brandName: String = "SoText",
    badgeText: String? = null,
    isUnifiedMode: Boolean = false,
    tagline: String = "Messaging Reimagined.\nFluid. Instant. Connective.",
    onGetStartedClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val glowRadius = kotlin.math.max(size.width, size.height) * 0.6f
                val baseBrush = Brush.linearGradient(
                    colors = listOf(SplashDeepBlue, SplashMidBlue, SplashRoyalBlue),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )
                val topRightGlow = Brush.radialGradient(
                    colors = listOf(SplashMagenta.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(size.width * 0.95f, size.height * 0.02f),
                    radius = glowRadius
                )
                val bottomLeftGlow = Brush.radialGradient(
                    colors = listOf(SplashViolet.copy(alpha = 0.5f), Color.Transparent),
                    center = Offset(size.width * 0.02f, size.height * 0.96f),
                    radius = glowRadius
                )
                onDrawBehind {
                    drawRect(baseBrush)
                    drawRect(topRightGlow)
                    drawRect(bottomLeftGlow)
                }
            }
    ) {
        ConstellationBackdrop(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 32.dp, vertical = 24.dp)
                .semantics { contentDescription = "SoText splash screen" },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            GlassLogoMark(isUnifiedMode = isUnifiedMode)

            Spacer(modifier = Modifier.height(20.dp))

            val wordmarkShadow = Shadow(
                color = Color.Black.copy(alpha = 0.35f),
                offset = Offset(0f, 6f),
                blurRadius = 14f
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "So.",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        shadow = wordmarkShadow
                    )
                )
                Text(
                    text = "Text.",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        brush = Brush.linearGradient(listOf(SplashCyan, SplashSkyBlue)),
                        shadow = wordmarkShadow
                    )
                )
            }

            if (badgeText != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = badgeText.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp
                    ),
                    color = if (usePremiumBranding) SplashCyan else Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = tagline,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                ),
                color = Color.White.copy(alpha = 0.92f)
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Get Started",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.16f),
                                SplashMidBlue.copy(alpha = 0.35f)
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(50))
                    .clickable(onClick = onGetStartedClick)
                    .padding(horizontal = 40.dp, vertical = 14.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Copyright © ${currentYear()} $brandName Inc.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
private fun GlassLogoMark(isUnifiedMode: Boolean) {
    val ringBrush = remember {
        Brush.linearGradient(listOf(SplashCyan, SplashSkyBlue, SplashViolet, SplashMagenta))
    }
    Box(contentAlignment = Alignment.Center) {
        // Soft drop shadow, offset down-right, giving the bubble a sense of elevation.
        Box(
            modifier = Modifier
                .size(150.dp)
                .offset(x = 10.dp, y = 14.dp)
                .blur(28.dp)
                .clip(CircleShape)
                .background(SplashDeepBlue.copy(alpha = 0.55f))
        )

        // Soft outer glow.
        Box(
            modifier = Modifier
                .size(190.dp)
                .blur(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(SplashCyan.copy(alpha = 0.55f), Color.Transparent)
                    )
                )
        )

        // Speech-bubble tail.
        Canvas(modifier = Modifier.size(140.dp)) {
            val tail = Path().apply {
                moveTo(size.width * 0.58f, size.height * 0.92f)
                lineTo(size.width * 0.74f, size.height * 1.08f)
                lineTo(size.width * 0.68f, size.height * 0.80f)
                close()
            }
            drawPath(tail, brush = ringBrush, alpha = 0.85f)
        }

        // Frosted glass bubble.
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.20f),
                            SplashCyan.copy(alpha = 0.18f),
                            SplashViolet.copy(alpha = 0.22f)
                        )
                    )
                )
                .border(2.dp, ringBrush, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            SwirlGlyph(modifier = Modifier.size(68.dp))

            // Glass rim highlight, arcing across the top of the bubble.
            Canvas(modifier = Modifier.size(120.dp)) {
                drawArc(
                    color = Color.White.copy(alpha = 0.5f),
                    startAngle = -160f,
                    sweepAngle = 80f,
                    useCenter = false,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Specular highlight.
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .blur(6.dp)
                    .align(Alignment.TopStart)
                    .padding(top = 14.dp, start = 14.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.55f))
            )
        }

        if (isUnifiedMode) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(SplashMagenta)
                    .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
            )
        }
    }
}

/**
 * A continuous ribbon "S" formed from two mirrored arcs so the ends read as a
 * single flowing swirl (rather than two separate strokes), capped with small
 * flourish dots at each terminus for a glossy, hand-finished look.
 */
@Composable
private fun SwirlGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.16f
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        val topArcBrush = Brush.linearGradient(listOf(SplashCyan, SplashSkyBlue))
        val bottomArcBrush = Brush.linearGradient(listOf(SplashViolet, SplashMagenta))

        val topArcTopLeft = Offset(size.width * 0.04f, 0f)
        val topArcSize = Size(size.width * 0.92f, size.height * 0.56f)
        val bottomArcTopLeft = Offset(size.width * 0.04f, size.height * 0.44f)
        val bottomArcSize = Size(size.width * 0.92f, size.height * 0.56f)

        drawArc(
            brush = topArcBrush,
            startAngle = -160f,
            sweepAngle = 200f,
            useCenter = false,
            topLeft = topArcTopLeft,
            size = topArcSize,
            style = stroke
        )
        drawArc(
            brush = bottomArcBrush,
            startAngle = 20f,
            sweepAngle = 200f,
            useCenter = false,
            topLeft = bottomArcTopLeft,
            size = bottomArcSize,
            style = stroke
        )

        // Glossy highlight riding along the top arc.
        drawArc(
            color = Color.White.copy(alpha = 0.35f),
            startAngle = -150f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = topArcTopLeft,
            size = topArcSize,
            style = Stroke(width = strokeWidth * 0.3f, cap = StrokeCap.Round)
        )

        // Terminus flourish dots, echoing the swirl's curled ends.
        val topEndAngleRad = Math.toRadians(40.0)
        val topCenter = Offset(topArcTopLeft.x + topArcSize.width / 2f, topArcTopLeft.y + topArcSize.height / 2f)
        val topRadius = topArcSize.width / 2f
        val topDot = Offset(
            x = topCenter.x + (topRadius * kotlin.math.cos(topEndAngleRad)).toFloat(),
            y = topCenter.y + (topRadius * kotlin.math.sin(topEndAngleRad)).toFloat()
        )
        drawCircle(color = SplashSkyBlue, radius = strokeWidth * 0.55f, center = topDot)

        val bottomEndAngleRad = Math.toRadians(220.0)
        val bottomCenter = Offset(
            bottomArcTopLeft.x + bottomArcSize.width / 2f,
            bottomArcTopLeft.y + bottomArcSize.height / 2f
        )
        val bottomRadius = bottomArcSize.width / 2f
        val bottomDot = Offset(
            x = bottomCenter.x + (bottomRadius * kotlin.math.cos(bottomEndAngleRad)).toFloat(),
            y = bottomCenter.y + (bottomRadius * kotlin.math.sin(bottomEndAngleRad)).toFloat()
        )
        drawCircle(color = SplashMagenta, radius = strokeWidth * 0.55f, center = bottomDot)
    }
}

/**
 * Faint constellation of dots and connecting lines in the top-right and
 * bottom-left corners, plus flowing cyan "current" lines threading across
 * the bottom of the screen, echoing the "connective" line in the tagline.
 */
@Composable
private fun ConstellationBackdrop(modifier: Modifier = Modifier) {
    val topRightNodes = remember {
        listOf(
            0.62f to 0.02f, 0.75f to 0.06f, 0.88f to 0.03f, 0.95f to 0.12f,
            0.80f to 0.16f, 0.68f to 0.13f, 0.92f to 0.24f, 0.72f to 0.24f
        )
    }
    val topRightEdges = remember {
        listOf(0 to 1, 1 to 2, 2 to 3, 3 to 6, 6 to 4, 4 to 5, 5 to 0, 4 to 7, 7 to 1)
    }
    val bottomLeftNodes = remember {
        listOf(
            0.03f to 0.82f, 0.14f to 0.88f, 0.02f to 0.95f, 0.24f to 0.93f,
            0.12f to 0.99f, 0.34f to 0.90f, 0.50f to 0.96f, 0.64f to 0.90f,
            0.78f to 0.97f, 0.90f to 0.92f
        )
    }
    val bottomLeftEdges = remember {
        listOf(0 to 1, 1 to 3, 3 to 5, 5 to 6, 6 to 7, 7 to 8, 8 to 9, 1 to 2, 3 to 4)
    }

    Canvas(modifier = modifier) {
        drawFlowingCurrents()
        drawConstellationCluster(topRightNodes, topRightEdges)
        drawConstellationCluster(bottomLeftNodes, bottomLeftEdges)
    }
}

/** Gentle cyan-to-blue current lines that sweep across the lower third of the screen. */
private fun DrawScope.drawFlowingCurrents() {
    val brush = Brush.linearGradient(
        listOf(SplashCyan.copy(alpha = 0.32f), SplashSkyBlue.copy(alpha = 0.12f))
    )
    val baselines = listOf(0.80f, 0.87f, 0.94f)
    baselines.forEachIndexed { index, baseline ->
        val path = Path().apply {
            moveTo(0f, size.height * (baseline - 0.05f))
            cubicTo(
                size.width * 0.28f, size.height * (baseline + 0.04f),
                size.width * 0.55f, size.height * (baseline - 0.06f),
                size.width * 0.82f, size.height * baseline
            )
            cubicTo(
                size.width * 0.92f, size.height * (baseline + 0.02f),
                size.width * 0.97f, size.height * (baseline - 0.02f),
                size.width, size.height * (baseline - 0.04f)
            )
        }
        drawPath(
            path,
            brush = brush,
            style = Stroke(width = (1.2f - index * 0.2f).dp.toPx())
        )
    }
}

private fun DrawScope.drawConstellationCluster(
    nodes: List<Pair<Float, Float>>,
    edges: List<Pair<Int, Int>>
) {
    val lineColor = Color.White.copy(alpha = 0.20f)
    val dotColor = Color.White.copy(alpha = 0.55f)
    val points = nodes.map { (fx, fy) -> Offset(fx * size.width, fy * size.height) }
    edges.forEach { (a, b) ->
        drawLine(lineColor, points[a], points[b], strokeWidth = 1.dp.toPx())
    }
    points.forEachIndexed { index, point ->
        drawCircle(dotColor, radius = if (index % 3 == 0) 3.dp.toPx() else 1.6.dp.toPx(), center = point)
    }
}

private fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)
