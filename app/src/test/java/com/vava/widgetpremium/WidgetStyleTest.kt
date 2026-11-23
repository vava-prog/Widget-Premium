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

    @Test fun clockScaleClampedAndApplied() {
        assertEquals(WidgetPalette.MIN_CLOCK_SCALE, WidgetStyle(clockScalePercent = 5).sanitized().clockScalePercent)
        assertEquals(WidgetPalette.MAX_CLOCK_SCALE, WidgetStyle(clockScalePercent = 999).sanitized().clockScalePercent)
        assertEquals(56f, WidgetPalette.clockSizeSp(56f, 100), 0.001f)
        assertEquals(33.6f, WidgetPalette.clockSizeSp(56f, 50), 0.001f) // dưới mức tối thiểu bị nâng lên 60%
        assertEquals(33.6f, WidgetPalette.clockSizeSp(56f, 60), 0.001f)
    }

    @Test fun borderZeroKeepsThemeDefaultAndPositiveOverrides() {
        assertEquals(0f, WidgetPalette.palette(WidgetStyle(theme = WidgetTheme.STANDARD), true).strokeDp, 0f)
        assertEquals(1.5f, WidgetPalette.palette(WidgetStyle(theme = WidgetTheme.GLASS), true).strokeDp, 0f)
        val custom = WidgetPalette.palette(WidgetStyle(theme = WidgetTheme.STANDARD, borderDp = 3), true)
        assertEquals(3f, custom.strokeDp, 0f)
        assertTrue((custom.stroke ushr 24) > 0)
        assertEquals(3f, WidgetPalette.palette(WidgetStyle(theme = WidgetTheme.GLASS, borderDp = 3), false).strokeDp, 0f)
        assertEquals(WidgetPalette.MAX_BORDER_DP, WidgetStyle(borderDp = 99).sanitized().borderDp)
        assertEquals(0, WidgetStyle(borderDp = -2).sanitized().borderDp)
    }

    @Test fun secondsFormatAndShrink() {
        assertEquals("h:mm" to "HH:mm", WidgetPalette.timeFormats(false))
        assertEquals("h:mm:ss" to "HH:mm:ss", WidgetPalette.timeFormats(true))
        assertFalse(WidgetStyle().showSeconds)
        assertEquals(56f, WidgetPalette.clockSizeSp(56f, 100, false), 0.001f)
        assertEquals(56f * WidgetPalette.SECONDS_SHRINK, WidgetPalette.clockSizeSp(56f, 100, true), 0.001f)
    }

    @Test fun labelIsCleanedAndLimited() {
        assertEquals("Nhà của Vân", WidgetPalette.cleanLabel("  Nhà   của\nVân  "))
        assertEquals("", WidgetPalette.cleanLabel("   "))
        assertEquals(WidgetPalette.MAX_LABEL_LENGTH, WidgetPalette.cleanLabel("a".repeat(100)).length)
        assertEquals("abc", WidgetStyle(label = " abc ").sanitized().label)
    }
}
