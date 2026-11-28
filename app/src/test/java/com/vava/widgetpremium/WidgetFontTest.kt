package com.vava.widgetpremium

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetFontTest {
    @Test
    fun snapKeepsStepOfFiveWithinRange() {
        assertEquals(80, WidgetFont.snap(10))
        assertEquals(130, WidgetFont.snap(500))
        assertEquals(100, WidgetFont.snap(100))
        assertEquals(105, WidgetFont.snap(106))
        assertEquals(95, WidgetFont.snap(93))
    }

    @Test
    fun scaledMultipliesBySnappedPercent() {
        assertEquals(13f, WidgetFont.scaled(10f, 130), 1e-4f)
        assertEquals(8f, WidgetFont.scaled(10f, 80), 1e-4f)
        assertEquals(10f, WidgetFont.scaled(10f, 100), 1e-4f)
    }

    @Test
    fun fitShrinksWhenTheBoxIsTooNarrowAndNeverGrows() {
        assertEquals(40f, WidgetFont.fitSp(40f, 400f, 4.6f), 1e-4f)
        val fitted = WidgetFont.fitSp(60f, 100f, 4.6f)
        assertTrue(fitted < 60f)
        assertTrue(fitted * 0.56f * 4.6f <= 100f + 1e-3f)
        assertTrue(WidgetFont.fitSp(60f, 5f, 4.6f) >= 10f)
    }

    @Test
    fun narrowWidgetsShowFiveColumns() {
        assertEquals(5, WidgetFont.columns(250))
        assertEquals(5, WidgetFont.columns(299))
        assertEquals(7, WidgetFont.columns(300))
    }

    @Test
    fun styleSanitizesFontScale() {
        assertEquals(130, WidgetStyle(fontScalePercent = 999).sanitized().fontScalePercent)
        assertEquals(80, WidgetStyle(fontScalePercent = -5).sanitized().fontScalePercent)
        assertEquals(FontWeightChoice.NORMAL, WidgetStyle().fontWeight)
    }

    @Test
    fun everyFontLayoutVariantExistsAndHasTheSameIdsAsItsBase() {
        val dir = listOf("src/main/res/layout", "app/src/main/res/layout").map { File(it) }.first { it.isDirectory }
        val idRe = Regex("@\\+id/(\\w+)")
        for (base in listOf("clock", "info", "week")) {
            val ids = idRe.findAll(File(dir, "widget_$base.xml").readText()).map { it.groupValues[1] }.toSet()
            val variants = dir.listFiles { f -> f.name.startsWith("widget_${base}_") }!!
            assertEquals("số biến thể của $base", 14, variants.size)
            for (v in variants) assertEquals(v.name, ids, idRe.findAll(v.readText()).map { it.groupValues[1] }.toSet())
        }
    }
}
