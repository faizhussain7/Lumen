package com.mfhapps.lumen

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColourTest {
    private val accents = listOf(Color(0xFF1A3CFF), Color(0xFF0000FF), Color(0xFF00C853), Color(0xFFFFD600), Color(0xFFE91E63), Color(0xFF7C4DFF))

    private fun lstar(c: Color): Float {
        val y = c.luminance()
        return if (y <= 0.008856f) y * 903.3f else 116f * Math.cbrt(y.toDouble()).toFloat() - 16f
    }

    private fun hue(c: Color): Double {
        val ok = c.convert(ColorSpaces.Oklab)
        return Math.toDegrees(kotlin.math.atan2(ok.blue.toDouble(), ok.green.toDouble()))
    }

    @Test fun toneHitsTarget() {
        for (a in accents) for (t in listOf(10f, 20f, 30f, 40f, 80f, 90f)) {
            assertEquals("tone $t of $a", t, lstar(tone(a, t)), 1.5f)
        }
    }

    @Test fun blueStaysBlue() {
        val blue = Color(0xFF1A3CFF)
        val base = hue(blue)
        for (t in listOf(30f, 80f, 90f)) assertEquals("hue at tone $t", base, hue(tone(blue, t)), 12.0)
    }

    @Test fun pairsAreReadable() {
        val base = androidx.compose.material3.darkColorScheme()
        for (dark in listOf(true, false)) for (a in accents) {
            val s = legible(accentScheme(if (dark) base else androidx.compose.material3.lightColorScheme(), a, dark))
            assertTrue("onPrimary $a", contrast(s.onPrimary, s.primary) >= 4.5f)
            assertTrue("onPrimaryContainer $a", contrast(s.onPrimaryContainer, s.primaryContainer) >= 4.5f)
            assertTrue("onSecondaryContainer $a", contrast(s.onSecondaryContainer, s.secondaryContainer) >= 4.5f)
            assertTrue("onTertiaryContainer $a", contrast(s.onTertiaryContainer, s.tertiaryContainer) >= 4.5f)
        }
    }

    @Test fun readableFallsBack() {
        val navy = Color(0xFF0A1450)
        assertTrue(contrast(readable(Color(0xFF101830), navy), navy) >= 4.5f)
    }
}
