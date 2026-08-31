package com.sotext.util

import androidx.compose.ui.graphics.Color
import com.sotext.domain.model.ThemePreferences
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ColorUtilsTest {

    @Before
    fun setup() {
        // android.graphics.Color.parseColor isn't available under plain JVM unit tests;
        // stub it with a real hex parse so assertions below check actual color values.
        mockkStatic(android.graphics.Color::class)
        every { android.graphics.Color.parseColor(any()) } answers {
            val hex = firstArg<String>().removePrefix("#")
            val argb = if (hex.length == 6) "FF$hex" else hex
            argb.toLong(16).toInt()
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(android.graphics.Color::class)
    }

    @Test
    fun `flat background (no gradient) returns null`() {
        assertNull(themeGradientColors(ThemePreferences()))
    }

    @Test
    fun `a start with no end is not treated as a gradient`() {
        val theme = ThemePreferences(appBackgroundGradientStart = "#10B981")
        assertNull(themeGradientColors(theme))
    }

    @Test
    fun `two-stop gradient returns exactly start then end`() {
        // Sunset Fade's actual stops.
        val theme = ThemePreferences(
            appBackgroundGradientStart = "#FF5F6D",
            appBackgroundGradientEnd = "#FFC371"
        )
        val colors = themeGradientColors(theme)
        assertEquals(
            listOf(parseColorOr(Color.White, "#FF5F6D"), parseColorOr(Color.White, "#FFC371")),
            colors
        )
    }

    @Test
    fun `three-stop gradient returns start then mid then end, in order`() {
        // Prism Drift's actual stops - the regression this test guards: a mid stop must
        // survive rendering, and must land between start and end, not get dropped or reordered.
        val theme = ThemePreferences(
            appBackgroundGradientStart = "#10B981",
            appBackgroundGradientMid = "#2563EB",
            appBackgroundGradientEnd = "#7C3AED"
        )
        val colors = themeGradientColors(theme)
        assertEquals(
            listOf(
                parseColorOr(Color.White, "#10B981"),
                parseColorOr(Color.White, "#2563EB"),
                parseColorOr(Color.White, "#7C3AED")
            ),
            colors
        )
    }

    @Test
    fun `applies the requested alpha to every stop including mid`() {
        // Opal Bloom's actual stops.
        val theme = ThemePreferences(
            appBackgroundGradientStart = "#EC4899",
            appBackgroundGradientMid = "#A855F7",
            appBackgroundGradientEnd = "#6366F1"
        )
        val colors = themeGradientColors(theme, alpha = 0.35f)
        assertEquals(3, colors?.size)
        // Color packs alpha into a bit-limited internal representation, so compare with a
        // tolerance rather than expecting an exact float round-trip.
        colors?.forEach { assertEquals(0.35f, it.alpha, 0.01f) }
    }

    @Test
    fun `explicit stops list returns all four in order, ignoring start-mid-end`() {
        // Blood Moon's actual stops: near-black, oxblood, deep crimson (peak), back down.
        val theme = ThemePreferences(
            appBackgroundGradientStops = listOf("#0A0405", "#4A0E13", "#8B1A1A", "#200608"),
            // Should be ignored entirely once appBackgroundGradientStops is present.
            appBackgroundGradientStart = "#FFFFFF",
            appBackgroundGradientEnd = "#000000"
        )
        val colors = themeGradientColors(theme)
        assertEquals(
            listOf(
                parseColorOr(Color.White, "#0A0405"),
                parseColorOr(Color.White, "#4A0E13"),
                parseColorOr(Color.White, "#8B1A1A"),
                parseColorOr(Color.White, "#200608")
            ),
            colors
        )
    }

    @Test
    fun `a stops list with fewer than two entries falls back to start-end`() {
        val theme = ThemePreferences(
            appBackgroundGradientStops = listOf("#111111"),
            appBackgroundGradientStart = "#FF5F6D",
            appBackgroundGradientEnd = "#FFC371"
        )
        val colors = themeGradientColors(theme)
        assertEquals(
            listOf(parseColorOr(Color.White, "#FF5F6D"), parseColorOr(Color.White, "#FFC371")),
            colors
        )
    }
}
