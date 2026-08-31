package com.sotext.ui.theme

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.random.Random

private data class Star(
    val xFraction: Float,
    val yFraction: Float,
    val radiusDp: Float,
    val alpha: Float
)

/**
 * Scatters a fixed, deterministic field of small dots over whatever this modifier is chained
 * after (a gradient background, typically), for themes like Dark Cosmic that want a starfield.
 * A no-op (returns [this] unchanged, no allocation) when [enabled] is false, so it's safe to
 * chain unconditionally at every background render site - only a theme that actually turns it
 * on pays for it.
 *
 * The field uses a fixed seed rather than [kotlin.random.Random.Default] so it's stable across
 * recompositions and re-renders (a preset shouldn't visibly reshuffle its stars every frame),
 * and static rather than twinkling/animated so it's cheap enough to draw on every theme-card
 * preview in the Visual Settings grid, not just the one full-screen background in use.
 */
fun Modifier.starfieldOverlay(
    enabled: Boolean,
    starColor: Color = Color.White,
    starCount: Int = 90
): Modifier = composed {
    if (!enabled) return@composed this
    val stars = remember(starCount) {
        val random = Random(20260830L xor starCount.toLong())
        List(starCount) {
            Star(
                xFraction = random.nextFloat(),
                yFraction = random.nextFloat(),
                radiusDp = 0.5f + random.nextFloat() * 1.4f,
                alpha = 0.25f + random.nextFloat() * 0.65f
            )
        }
    }
    drawWithContent {
        drawContent()
        stars.forEach { star ->
            drawCircle(
                color = starColor.copy(alpha = star.alpha),
                radius = star.radiusDp.dp.toPx(),
                center = Offset(star.xFraction * size.width, star.yFraction * size.height)
            )
        }
    }
}
