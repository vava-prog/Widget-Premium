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

    @Test fun solarTermsMatchKnownDates() {
        fun start(y: Int, m: Int, d: Int) = VietCalendar.solarTermStartingOn(LocalDate.of(y, m, d))?.let { VietCalendar.TERMS[it] }
        assertEquals("Lập xuân", start(2026, 2, 4))
        assertEquals("Thanh minh", start(2026, 4, 5))
        assertEquals("Xuân phân", start(2026, 3, 20))
        assertEquals("Hạ chí", start(2026, 6, 21))
        assertEquals("Thu phân", start(2026, 9, 23))
        assertEquals("Lập đông", start(2026, 11, 7))
        assertEquals("Đông chí", start(2026, 12, 22))
        assertNull(start(2026, 11, 8))
    }

    @Test fun termMonthChiMapping() {
        assertEquals(2, VietCalendar.termMonthChi(21)) // Lập xuân: tháng Dần
        assertEquals(2, VietCalendar.termMonthChi(22)) // Vũ thủy vẫn tháng Dần
        assertEquals(3, VietCalendar.termMonthChi(23)) // Kinh trập: Mão
        assertEquals(3, VietCalendar.termMonthChi(0))  // Xuân phân: Mão
        assertEquals(0, VietCalendar.termMonthChi(17)) // Đại tuyết: Tý
        assertEquals(0, VietCalendar.termMonthChi(18)) // Đông chí: Tý
        assertEquals(1, VietCalendar.termMonthChi(19)) // Tiểu hàn: Sửu
    }

    @Test fun trucAdvancesDailyAndRepeatsOnTermDay() {
        var date = LocalDate.of(2026, 1, 1)
        repeat(400) {
            val next = date.plusDays(1)
            val a = VietCalendar.trucIndex(date)
            val b = VietCalendar.trucIndex(next)
            if (VietCalendar.solarTermStartingOn(next)?.let { it % 2 == 1 } == true) {
                assertEquals("repeat on $next", a, b)
            } else {
                assertEquals("advance on $next", (a + 1) % 12, b)
            }
            date = next
        }
    }

    @Test fun trucKienWhenDayChiEqualsMonthChi() {
        // Cuối ngày Lập xuân 2026 vào tháng Dần; ngày chi Dần đầu tiên sau đó là Kiến
        var d = LocalDate.of(2026, 2, 4)
        while (VietCalendar.dayChiIndex(d) != 2) d = d.plusDays(1)
        assertEquals(0, VietCalendar.trucIndex(d))
        assertEquals("Kiến", VietCalendar.TRUC[VietCalendar.trucIndex(d)])
    }

    @Test fun tabooDays() {
        fun lunar(day: Int) = LunarDate(day, 5, 2026, false)
        assertTrue(VietCalendar.isNguyetKy(lunar(14)))
        assertFalse(VietCalendar.isNguyetKy(lunar(15)))
        assertTrue(VietCalendar.isTamNuong(lunar(27)))
        assertFalse(VietCalendar.isTamNuong(lunar(28)))
    }

    @Test fun hyThanTaiThanByDayCan() {
        // Ngày Giáp Thân và Mậu Tuất khớp quantrimang.com; các ngày còn lại theo bảng của Đặng Xuân Xuyến.
        fun dateWithCan(can: Int): LocalDate {
            var d = LocalDate.of(2026, 1, 1)
            while (VietCalendar.dayCanIndex(d) != can) d = d.plusDays(1)
            return d
        }
        val hy = listOf(Direction.NE, Direction.NW, Direction.SW, Direction.S, Direction.SE, Direction.NE, Direction.NW, Direction.SW, Direction.S, Direction.SE)
        val tai = listOf(Direction.SE, Direction.SE, Direction.E, Direction.E, Direction.N, Direction.S, Direction.SW, Direction.SW, Direction.W, Direction.NW)
        for (can in 0..9) {
            assertEquals(hy[can], VietCalendar.hyThan(dateWithCan(can)))
            assertEquals(tai[can], VietCalendar.taiThan(dateWithCan(can)))
        }
        // Giáp ngày 5 ngày sau đó là Kỷ: cùng Hỷ thần Đông Bắc, khác Tài thần
        val giap = dateWithCan(0)
        assertEquals(VietCalendar.hyThan(giap), VietCalendar.hyThan(giap.plusDays(5)))
        assertTrue(VietCalendar.taiThan(giap) != VietCalendar.taiThan(giap.plusDays(5)))
    }
}
