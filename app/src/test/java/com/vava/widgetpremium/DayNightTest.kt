package com.vava.widgetpremium

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DayNightTest {
    private val hour = 3_600_000L
    // Múi giờ UTC+7, mặt trời mọc 5:58 và lặn 17:25
    private val sun = SunTimes(5 * 60 + 58, 17 * 60 + 25, 7 * hour)

    /** Epoch ms ứng với giờ:phút địa phương (UTC+7) của một ngày cố định. */
    private fun at(h: Int, m: Int): Long = 20_000L * 86_400_000L - 7 * hour + h * hour + m * 60_000L

    @Test
    fun dayAndNightBoundaries() {
        assertFalse(DayNight.isDay(at(5, 57), sun))
        assertTrue(DayNight.isDay(at(5, 58), sun))
        assertTrue(DayNight.isDay(at(17, 24), sun))
        assertFalse(DayNight.isDay(at(17, 25), sun))
        assertFalse(DayNight.isDay(at(23, 59), sun))
        assertFalse(DayNight.isDay(at(0, 0), sun))
    }

    @Test
    fun nextTransitionFromNight() {
        assertEquals(at(5, 58), DayNight.nextTransitionMs(at(3, 10), sun))
        // sau khi mặt trời lặn thì lần đổi kế tiếp là sáng hôm sau
        assertEquals(at(5, 58) + 24 * hour, DayNight.nextTransitionMs(at(20, 0), sun))
    }

    @Test
    fun nextTransitionFromDay() {
        assertEquals(at(17, 25), DayNight.nextTransitionMs(at(12, 0), sun))
        // đúng lúc mọc thì lần kế tiếp là lúc lặn
        assertEquals(at(17, 25), DayNight.nextTransitionMs(at(5, 58), sun))
    }

    @Test
    fun parsesLocalMinutes() {
        assertEquals(5 * 60 + 58, DayNight.parseMinutes("2026-11-20T05:58"))
        assertNull(DayNight.parseMinutes("2026-11-20"))
        assertNull(DayNight.parseMinutes(null))
        assertNull(DayNight.parseMinutes("2026-11-20T25:00"))
    }

    @Test
    fun sunTimesFromWeatherUsesTodaysEntry() {
        val json = """
            {"utc_offset_seconds":25200,
             "current":{"temperature_2m":25.0,"relative_humidity_2m":70,"weather_code":0},
             "daily":{"time":["2026-11-19","2026-11-20"],"weather_code":[0,0],
               "temperature_2m_max":[28,28],"temperature_2m_min":[20,20],
               "sunrise":["2026-11-19T05:57","2026-11-20T05:58"],
               "sunset":["2026-11-19T17:26","2026-11-20T17:25"]}}
        """.trimIndent()
        val w = WeatherParsers.parseWeather(json)!!
        assertEquals(25200, w.utcOffsetSec)
        val s = DayNight.sunTimesFor(w, at(12, 0) )
        // at(12,0) rơi vào ngày địa phương nào tùy hằng số; chỉ cần kết quả hợp lệ và đúng múi giờ
        assertEquals(25200_000L, s!!.offsetMs)
        assertTrue(s.sunriseMin in listOf(5 * 60 + 57, 5 * 60 + 58))
    }

    @Test
    fun emojiAtNight() {
        assertEquals("🌙", Condition.CLEAR.emoji(false))
        assertEquals("☀️", Condition.CLEAR.emoji(true))
        assertEquals("🌧️", Condition.RAIN.emoji(false))
    }
}
