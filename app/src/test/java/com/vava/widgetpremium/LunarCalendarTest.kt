package com.vava.widgetpremium

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LunarCalendarTest {
    private fun check(d: Int, m: Int, y: Int, day: Int, month: Int, year: Int, leap: Boolean = false) {
        val l = LunarCalendar.fromSolar(d, m, y)
        assertEquals("$d/$m/$y ngày", day, l.day)
        assertEquals("$d/$m/$y tháng", month, l.month)
        assertEquals("$d/$m/$y năm", year, l.year)
        assertEquals("$d/$m/$y nhuận", leap, l.isLeap)
    }

    @Test
    fun tetDays() {
        check(10, 2, 2024, 1, 1, 2024)
        check(29, 1, 2025, 1, 1, 2025)
        check(17, 2, 2026, 1, 1, 2026)
    }

    @Test
    fun leapMonthSixOf2025() {
        check(24, 7, 2025, 30, 6, 2025, leap = false)
        check(25, 7, 2025, 1, 6, 2025, leap = true)
    }

    @Test
    fun beforeTetBelongsToPreviousLunarYear() {
        check(1, 1, 2025, 2, 12, 2024)
    }

    @Test
    fun knownDay() {
        check(20, 11, 2026, 12, 10, 2026)
    }

    @Test
    fun yearNames() {
        assertEquals("Giáp Thìn", LunarCalendar.yearName(2024))
        assertEquals("Ất Tỵ", LunarCalendar.yearName(2025))
        assertEquals("Bính Ngọ", LunarCalendar.yearName(2026))
    }

    @Test
    fun leapFlagOnlyOnLeapMonth() {
        assertTrue(LunarCalendar.fromSolar(25, 7, 2025).isLeap)
        assertFalse(LunarCalendar.fromSolar(25, 8, 2025).isLeap)
    }
}
