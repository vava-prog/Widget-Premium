package com.vava.widgetpremium

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherSkyTest {
    @Test fun skyFollowsConditionAndDayNight() {
        assertEquals(Sky.CLEAR_DAY, WeatherSky.of(Condition.CLEAR, true))
        assertEquals(Sky.CLEAR_NIGHT, WeatherSky.of(Condition.CLEAR, false))
        assertEquals(Sky.PARTLY_NIGHT, WeatherSky.of(Condition.PARTLY, false))
        assertEquals(Sky.RAIN, WeatherSky.of(Condition.DRIZZLE, true))
        assertEquals(Sky.RAIN, WeatherSky.of(Condition.RAIN, false))
        assertEquals(Sky.THUNDER, WeatherSky.of(Condition.THUNDER, true))
        assertEquals(Sky.FOG, WeatherSky.of(Condition.FOG, true))
        assertEquals(Sky.SNOW, WeatherSky.of(Condition.SNOW, false))
        assertEquals(Sky.CLOUDY, WeatherSky.of(Condition.UNKNOWN, true))
    }

    @Test fun mapsWmoCodesToExpectedSky() {
        assertEquals(Sky.CLEAR_DAY, WeatherSky.of(Condition.fromWmo(0), true))
        assertEquals(Sky.FOG, WeatherSky.of(Condition.fromWmo(45), false))
        assertEquals(Sky.RAIN, WeatherSky.of(Condition.fromWmo(61), true))
        assertEquals(Sky.THUNDER, WeatherSky.of(Condition.fromWmo(95), true))
    }

    @Test fun everySkyHasOpaqueColorsAndNightIsDarker() {
        fun lum(c: Int) = ((c shr 16) and 0xFF) + ((c shr 8) and 0xFF) + (c and 0xFF)
        for (sky in Sky.values()) {
            val (top, bottom) = WeatherSky.colors(sky, true)
            assertEquals(0xFF, top ushr 24)
            assertEquals(0xFF, bottom ushr 24)
        }
        val day = WeatherSky.colors(Sky.RAIN, true)
        val night = WeatherSky.colors(Sky.RAIN, false)
        assertTrue(lum(night.first) < lum(day.first))
        assertNotEquals(WeatherSky.colors(Sky.CLEAR_DAY, true), WeatherSky.colors(Sky.CLEAR_NIGHT, false))
    }

    @Test fun weatherThemeUsesSkyColorsAndFallsBackToStandard() {
        val style = WidgetStyle(theme = WidgetTheme.WEATHER)
        val withSky = WidgetPalette.palette(style, true, Sky.CLEAR_DAY)
        assertEquals(WeatherSky.colors(Sky.CLEAR_DAY, true).first, withSky.top)
        assertEquals(0f, withSky.strokeDp, 0f)
        val without = WidgetPalette.palette(style, true, null)
        assertEquals(WidgetPalette.palette(WidgetStyle(theme = WidgetTheme.STANDARD), true).top, without.top)
    }

    @Test fun opacityScalesWeatherBackground() {
        val p = WidgetPalette.palette(WidgetStyle(theme = WidgetTheme.WEATHER, opacityPercent = 50), true, Sky.RAIN)
        assertEquals(0x7F, p.top ushr 24) // 255 * 50% làm tròn xuống
    }
}
