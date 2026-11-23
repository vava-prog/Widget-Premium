package com.vava.widgetpremium

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WeekStripTest {
    private val days = listOf(
        DayForecast("2026-11-23", 0, 29.4, 22.0),
        DayForecast("2026-11-24", 61, 28.6, 23.0)
    )

    @Test fun labelShowsConditionAndRoundedMax() {
        assertEquals(Condition.CLEAR.emoji + "29°", WeekStrip.weatherLabel(LocalDate.of(2026, 11, 23), days))
        assertEquals(Condition.RAIN.emoji + "29°", WeekStrip.weatherLabel(LocalDate.of(2026, 11, 24), days))
    }

    @Test fun missingForecastGivesEmptyLabel() {
        assertEquals("", WeekStrip.weatherLabel(LocalDate.of(2026, 11, 29), days))
        assertEquals("", WeekStrip.weatherLabel(LocalDate.of(2026, 11, 23), emptyList()))
    }
}
