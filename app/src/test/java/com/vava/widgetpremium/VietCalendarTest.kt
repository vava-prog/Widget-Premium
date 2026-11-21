package com.vava.widgetpremium

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VietCalendarTest {
    @Test fun dayNameKnownDates() {
        assertEquals("Mậu Ngọ", VietCalendar.dayName(LocalDate.of(2000, 1, 1)))
        assertEquals(2451545L, VietCalendar.julianDay(LocalDate.of(2000, 1, 1)))
    }

    @Test fun dayNameAdvancesByOne() {
        val a = LocalDate.of(2026, 11, 21)
        assertEquals((VietCalendar.dayChiIndex(a) + 1) % 12, VietCalendar.dayChiIndex(a.plusDays(1)))
        assertEquals((VietCalendar.dayCanIndex(a) + 1) % 10, VietCalendar.dayCanIndex(a.plusDays(1)))
    }

    @Test fun hoangDaoHasSixGoodDaysInTwelve() {
        for (month in 1..12) {
            assertEquals(6, (0..11).count { VietCalendar.isHoangDao(month, it) })
        }
    }

    @Test fun hoangDaoThanhLongStartsAtMonthStartChi() {
        assertTrue(VietCalendar.isHoangDao(1, 0))   // tháng 1: Thanh Long ở ngày Tý
        assertTrue(VietCalendar.isHoangDao(2, 2))   // tháng 2: Dần
        assertFalse(VietCalendar.isHoangDao(1, 2))  // Thiên Hình
        assertTrue(VietCalendar.isHoangDao(7, 0))   // tháng 7 giống tháng 1
    }

    @Test fun goodHoursAreSixDistinctAndShared() {
        for (chi in 0..11) {
            val h = VietCalendar.goodHours(chi)
            assertEquals(6, h.toSet().size)
            assertEquals(h, VietCalendar.goodHours((chi + 6) % 12))
        }
        assertEquals(listOf(0, 1, 3, 6, 8, 9), VietCalendar.goodHours(0))
    }

    @Test fun hourRangeAndChi() {
        assertEquals(23 to 1, VietCalendar.hourRange(0))
        assertEquals(1 to 3, VietCalendar.hourRange(1))
        assertEquals(21 to 23, VietCalendar.hourRange(11))
        assertEquals(0, VietCalendar.hourChi(23))
        assertEquals(0, VietCalendar.hourChi(0))
        assertEquals(1, VietCalendar.hourChi(1))
        assertEquals(6, VietCalendar.hourChi(12))
        assertEquals(11, VietCalendar.hourChi(22))
    }

    @Test fun holidaysSolarAndLunar() {
        val tet = LocalDate.of(2026, 2, 17)
        assertEquals(listOf(Holiday.TET), VietCalendar.holidays(tet, VietCalendar.lunarOf(tet)))
        val national = LocalDate.of(2026, 9, 2)
        assertTrue(Holiday.NATIONAL in VietCalendar.holidays(national, VietCalendar.lunarOf(national)))
        val none = LocalDate.of(2026, 11, 21)
        assertTrue(VietCalendar.holidays(none, VietCalendar.lunarOf(none)).isEmpty())
    }

    @Test fun leapMonthHasNoLunarHoliday() {
        val d = LocalDate.of(2025, 7, 25) // mùng 1 tháng 6 nhuận
        val lunar = VietCalendar.lunarOf(d)
        assertTrue(lunar.isLeap)
        assertTrue(VietCalendar.holidays(d, lunar).none { it == Holiday.TET })
    }

    @Test fun monthGridIsWholeWeeksStartingMonday() {
        val cells = VietCalendar.monthCells(YearMonth.of(2026, 11)) // 1/11/2026 là Chủ nhật
        assertEquals(0, cells.size % 7)
        assertNull(cells[5])
        assertEquals(LocalDate.of(2026, 11, 1), cells[6])
        assertEquals(30, cells.filterNotNull().size)
        val feb = VietCalendar.monthCells(YearMonth.of(2027, 2)) // 1/2/2027 là Thứ Hai
        assertEquals(LocalDate.of(2027, 2, 1), feb[0])
        assertEquals(28, feb.size)
    }

    @Test fun monthEventsSortedAndIncludeLunar() {
        val events = VietCalendar.monthEvents(YearMonth.of(2026, 2))
        assertTrue(events.any { it.second == Holiday.TET && it.first == LocalDate.of(2026, 2, 17) })
        assertEquals(events.map { it.first }, events.map { it.first }.sorted())
    }

    @Test fun reminderRules() {
        assertTrue(ReminderRules.isValid(2000L, 1000L))
        assertFalse(ReminderRules.isValid(1000L, 1000L))
        val now = LocalDateTime.of(2026, 11, 21, 10, 30, 45)
        assertEquals(LocalDateTime.of(2026, 11, 21, 11, 30), ReminderRules.defaultTime(now.toLocalDate(), now))
        assertEquals(LocalDateTime.of(2026, 11, 22, 8, 0), ReminderRules.defaultTime(LocalDate.of(2026, 11, 22), now))
        assertEquals("2026-11-21", ReminderRules.dateKey(LocalDate.of(2026, 11, 21)))
    }
}
