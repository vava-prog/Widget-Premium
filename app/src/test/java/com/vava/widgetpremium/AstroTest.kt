package com.vava.widgetpremium

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AstroTest {
    @Test fun moonPhaseByLunarDay() {
        assertEquals(MoonPhase.NEW, Astro.moonPhase(1))
        assertEquals(MoonPhase.NEW, Astro.moonPhase(30))
        assertEquals(MoonPhase.FIRST_QUARTER, Astro.moonPhase(8))
        assertEquals(MoonPhase.FULL, Astro.moonPhase(15))
        assertEquals(MoonPhase.LAST_QUARTER, Astro.moonPhase(23))
        assertEquals(MoonPhase.WAXING_GIBBOUS, Astro.moonPhase(13))
        assertEquals(MoonPhase.WANING_CRESCENT, Astro.moonPhase(27))
        for (d in 1..30) assertNotNull(Astro.moonPhase(d))
    }

    @Test fun cityLocalIsoAppliesOffset() {
        // 2026-11-21T22:30:00Z là 05:30 ngày 22 ở UTC+7
        val ms = java.time.Instant.parse("2026-11-21T22:30:00Z").toEpochMilli()
        assertEquals("2026-11-22T05:30", Astro.cityLocalIso(ms, 7 * 3600))
        assertEquals("2026-11-21T22:30", Astro.cityLocalIso(ms, 0))
    }

    private val hourly = (0 until 48).map {
        val day = if (it < 24) "2026-11-22" else "2026-11-23"
        HourForecast("%sT%02d:00".format(day, it % 24), 20.0 + it, 0)
    }

    @Test fun nextHoursStartsAtCurrentHourWithStep() {
        val picked = Astro.nextHours(hourly, "2026-11-22T05:33")
        assertEquals(8, picked.size)
        assertEquals("2026-11-22T05:00", picked[0].time)
        assertEquals("2026-11-22T08:00", picked[1].time)
        assertEquals("2026-11-23T02:00", picked[7].time)
    }

    @Test fun nextHoursHandlesShortOrMissingData() {
        assertTrue(Astro.nextHours(emptyList(), "2026-11-22T05:33").isEmpty())
        assertTrue(Astro.nextHours(hourly, "2030-01-01T00:00").isEmpty())
        assertEquals(2, Astro.nextHours(hourly, "2026-11-23T20:10").size)
    }

    @Test fun isDayUsesSunTimesOrFallback() {
        val days = listOf(DayForecast("2026-11-22", 0, 30.0, 20.0, "2026-11-22T05:52", "2026-11-22T17:20"))
        assertFalse(Astro.isDayAt("2026-11-22T05:00", days))
        assertTrue(Astro.isDayAt("2026-11-22T06:00", days))
        assertFalse(Astro.isDayAt("2026-11-22T17:20", days))
        assertTrue(Astro.isDayAt("2026-11-23T07:00", emptyList()))
        assertFalse(Astro.isDayAt("2026-11-23T20:00", emptyList()))
    }

    @Test fun dayLengthAndClock() {
        assertEquals(11 * 60 + 28, Astro.dayLengthMinutes("2026-11-22T05:52", "2026-11-22T17:20"))
        assertNull(Astro.dayLengthMinutes(null, "2026-11-22T17:20"))
        assertNull(Astro.dayLengthMinutes("2026-11-22T18:00", "2026-11-22T17:20"))
        assertEquals("05:52", Astro.clockOf("2026-11-22T05:52"))
        assertNull(Astro.clockOf(null))
    }

    @Test fun lunarMonthNameAndHourName() {
        assertEquals("Kỷ Hợi", VietCalendar.lunarMonthName(LunarDate(13, 10, 2026, false)))
        assertEquals("Bính Dần", VietCalendar.lunarMonthName(LunarDate(1, 1, 2024, false)))
        // Ngày Giáp: giờ Tý là Giáp Tý; ngày Kỷ Hợi (can Kỷ): giờ Tý là Giáp Tý
        val kyHoi = LocalDate.of(2026, 11, 21)
        assertEquals("Kỷ Hợi", VietCalendar.dayName(kyHoi))
        assertEquals("Giáp Tý", VietCalendar.hourName(kyHoi, 0))
        assertEquals("Ất Sửu", VietCalendar.hourName(kyHoi, 1))
    }

    @Test fun nextTermAfterLapDong() {
        val next = VietCalendar.nextTerm(LocalDate.of(2026, 11, 8))
        assertNotNull(next)
        assertEquals("Tiểu tuyết", VietCalendar.TERMS[next!!.first])
        assertTrue(next.second.isAfter(LocalDate.of(2026, 11, 20)) && next.second.isBefore(LocalDate.of(2026, 11, 24)))
    }
}
