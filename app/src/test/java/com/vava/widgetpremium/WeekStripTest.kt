package com.vava.widgetpremium

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WeekStripTest {
    private val days = listOf(
        DayForecast("2026-11-23", 0, 29.4, 22.0),
        DayForecast("2026-11-24", 61, 28.6, 23.0)
    )

    @Test fun iconAndRangeFollowForecast() {
        assertEquals(Condition.CLEAR.emoji, WeekStrip.icon(LocalDate.of(2026, 11, 23), days))
        assertEquals("22° 29°", WeekStrip.range(LocalDate.of(2026, 11, 23), days))
        assertEquals(Condition.RAIN.emoji, WeekStrip.icon(LocalDate.of(2026, 11, 24), days))
        assertEquals("23° 29°", WeekStrip.range(LocalDate.of(2026, 11, 24), days))
    }

    private val weather = Weather(
        tempC = 27.0, humidity = 80, code = 61,
        days = listOf(
            DayForecast("2026-11-24", 61, 28.0, 23.0, "2026-11-24T05:50", "2026-11-24T17:20"),
            DayForecast("2026-11-25", 3, 27.0, 23.0, "2026-11-25T05:51", "2026-11-25T17:20")
        ),
        utcOffsetSec = 25200,
        hourly = (0 until 48).map { h ->
            HourForecast("2026-11-%02dT%02d:00".format(24 + h / 24, h % 24), 20.0 + h % 24, if (h % 24 >= 18) 0 else 61)
        }
    )

    @Test fun hourlyStartsAtTheCurrentCityHour() {
        val now = java.time.Instant.parse("2026-11-24T11:20:00Z").toEpochMilli() // 18:20 giờ thành phố
        val cells = WeekStrip.hourly(weather, now, 7)
        assertEquals(7, cells.size)
        assertEquals("18:00", cells.first().label)
        assertEquals("00:00", cells.last().label)
        assertEquals("38°", cells.first().temp)
        assertEquals("🌙", cells.first().icon) // trời quang sau giờ lặn là trăng
    }

    @Test fun hourlyDaytimeRainKeepsRainIcon() {
        val now = java.time.Instant.parse("2026-11-24T02:30:00Z").toEpochMilli() // 09:30 giờ thành phố
        val first = WeekStrip.hourly(weather, now, 7).first()
        assertEquals("09:00", first.label)
        assertEquals(Condition.RAIN.emoji, first.icon)
    }

    @Test fun hourlyRunsOutAtTheEndOfTheData() {
        val now = java.time.Instant.parse("2026-11-25T15:30:00Z").toEpochMilli() // 22:30 giờ thành phố
        assertEquals(listOf("22:00", "23:00"), WeekStrip.hourly(weather, now, 7).map { it.label })
    }

    @Test fun hourlyEmptyWithoutDataAndNeverInventsTemperature() {
        val now = java.time.Instant.parse("2026-11-24T11:20:00Z").toEpochMilli()
        assertEquals(emptyList<HourCell>(), WeekStrip.hourly(null, now))
        assertEquals(emptyList<HourCell>(), WeekStrip.hourly(weather.copy(hourly = emptyList()), now))
        val nan = weather.copy(hourly = listOf(HourForecast("2026-11-24T18:00", Double.NaN, 0)))
        assertEquals("", WeekStrip.hourly(nan, now).single().temp)
    }

    @Test fun missingForecastGivesEmpty() {
        assertEquals("", WeekStrip.icon(LocalDate.of(2026, 11, 29), days))
        assertEquals("", WeekStrip.range(LocalDate.of(2026, 11, 29), days))
        assertEquals("", WeekStrip.range(LocalDate.of(2026, 11, 23), emptyList()))
    }
}
