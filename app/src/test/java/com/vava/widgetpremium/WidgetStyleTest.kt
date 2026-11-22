package com.vava.widgetpremium

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetStyleTest {
    private fun alpha(argb: Int) = argb ushr 24

    @Test fun sanitizeClampsValues() {
        val s = WidgetStyle(accentIndex = 99, opacityPercent = 5).sanitized()
        assertEquals(WidgetPalette.ACCENTS.size - 1, s.accentIndex)
        assertEquals(WidgetPalette.MIN_OPACITY, s.opacityPercent)
        assertEquals(100, WidgetStyle(opacityPercent = 500).sanitized().opacityPercent)
        assertEquals(0, WidgetStyle(accentIndex = -3).sanitized().accentIndex)
    }

    @Test fun resolveDayByMode() {
        assertTrue(WidgetPalette.resolveDay(DayMode.AUTO, true))
        assertFalse(WidgetPalette.resolveDay(DayMode.AUTO, false))
        assertTrue(WidgetPalette.resolveDay(DayMode.DAY, false))
        assertFalse(WidgetPalette.resolveDay(DayMode.NIGHT, true))
    }

    @Test fun mixKeepsEndpoints() {
        val c = 0xFF2F7BE0.toInt()
        assertEquals(c, WidgetPalette.mix(c, 0xFFFFFFFF.toInt(), 0f))
        assertEquals(0xFFFFFFFF.toInt(), WidgetPalette.mix(c, 0xFFFFFFFF.toInt(), 1f))
        assertEquals(0xFF000000.toInt(), WidgetPalette.mix(c, 0xFF000000.toInt(), 1f))
    }

    @Test fun standardIsMoreOpaqueThanGlassAndHasNoStroke() {
        val std = WidgetPalette.palette(WidgetStyle(WidgetTheme.STANDARD), true)
        val glass = WidgetPalette.palette(WidgetStyle(WidgetTheme.GLASS), true)
        assertEquals(0f, std.strokeDp, 0f)
        assertEquals(0, alpha(std.sheen))
        assertTrue(alpha(std.bottom) > alpha(glass.bottom))
        assertTrue(glass.strokeDp > 0f)
        assertTrue(alpha(glass.stroke) > 0)
        assertTrue(alpha(glass.sheen) > 0)
    }

    @Test fun dayAndNightDiffer() {
        for (theme in WidgetTheme.values()) {
            val style = WidgetStyle(theme)
            assertNotEquals(WidgetPalette.palette(style, true).top, WidgetPalette.palette(style, false).top)
        }
    }

    @Test fun opacityScalesAlpha() {
        val full = WidgetPalette.palette(WidgetStyle(WidgetTheme.GLASS, opacityPercent = 100), false)
        val half = WidgetPalette.palette(WidgetStyle(WidgetTheme.GLASS, opacityPercent = 50), false)
        assertEquals(alpha(full.top) / 2, alpha(half.top))
        assertEquals(alpha(full.stroke) / 2, alpha(half.stroke))
        assertEquals(full.top and 0x00FFFFFF, half.top and 0x00FFFFFF)
    }

    @Test fun accentChangesColor() {
        val a = WidgetPalette.palette(WidgetStyle(accentIndex = 0), true)
        val b = WidgetPalette.palette(WidgetStyle(accentIndex = 3), true)
        assertNotEquals(a.bottom, b.bottom)
    }

    @Test fun cornerIsClampedAndDefaultsKept() {
        assertEquals(WidgetPalette.MAX_CORNER_DP, WidgetStyle(cornerDp = 999).sanitized().cornerDp)
        assertEquals(0, WidgetStyle(cornerDp = -5).sanitized().cornerDp)
        assertEquals(WidgetPalette.DEFAULT_CORNER_DP, WidgetStyle().cornerDp)
    }

    @Test fun textColorsByTone() {
        assertEquals(null, WidgetPalette.textColors(TextTone.AUTO))
        val light = WidgetPalette.textColors(TextTone.LIGHT)!!
        val dark = WidgetPalette.textColors(TextTone.DARK)!!
        assertEquals(0xFFFFFFFF.toInt(), light.first)
        assertTrue(alpha(light.second) < 255 && alpha(light.second) > 0)
        assertEquals(0xFF, alpha(dark.first))
        assertNotEquals(light.first, dark.first)
    }
}
