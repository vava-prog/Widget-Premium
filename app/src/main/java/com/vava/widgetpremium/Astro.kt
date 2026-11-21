package com.vava.widgetpremium

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

enum class MoonPhase(val emoji: String) {
    NEW("🌑"), WAXING_CRESCENT("🌒"), FIRST_QUARTER("🌓"), WAXING_GIBBOUS("🌔"),
    FULL("🌕"), WANING_GIBBOUS("🌖"), LAST_QUARTER("🌗"), WANING_CRESCENT("🌘")
}

/** Tính toán thiên văn và dự báo giờ (thuần Kotlin, có test). */
object Astro {
    private val ISO_MINUTE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

    /** Pha trăng suy từ ngày âm lịch (mùng 1 là trăng mới, 15 là trăng tròn). Chính xác trong khoảng một ngày. */
    fun moonPhase(lunarDay: Int): MoonPhase = when (lunarDay) {
        in 2..6 -> MoonPhase.WAXING_CRESCENT
        in 7..9 -> MoonPhase.FIRST_QUARTER
        in 10..13 -> MoonPhase.WAXING_GIBBOUS
        in 14..16 -> MoonPhase.FULL
        in 17..21 -> MoonPhase.WANING_GIBBOUS
        in 22..24 -> MoonPhase.LAST_QUARTER
        in 25..29 -> MoonPhase.WANING_CRESCENT
        else -> MoonPhase.NEW
    }

    /** Giờ hiện tại ở thành phố (theo độ lệch UTC), dạng yyyy-MM-ddTHH:mm như dữ liệu Open-Meteo. */
    fun cityLocalIso(nowMs: Long, utcOffsetSec: Int): String =
        LocalDateTime.ofEpochSecond(nowMs / 1000 + utcOffsetSec, 0, ZoneOffset.UTC).format(ISO_MINUTE)

    /** Lấy các mốc dự báo cách nhau `step` giờ, bắt đầu từ giờ hiện tại. */
    fun nextHours(hourly: List<HourForecast>, nowIso: String, step: Int = 3, count: Int = 8): List<HourForecast> {
        val hourStart = nowIso.substring(0, 13) + ":00"
        val first = hourly.indexOfFirst { it.time >= hourStart }
        if (first < 0) return emptyList()
        return (0 until count).mapNotNull { hourly.getOrNull(first + it * step) }
    }

    /** Thời điểm đó là ban ngày không: theo giờ mọc/lặn của ngày đó nếu có, không thì 6 giờ đến 18 giờ. */
    fun isDayAt(iso: String, days: List<DayForecast>): Boolean {
        val d = days.firstOrNull { iso.startsWith(it.date) }
        val rise = d?.sunrise
        val set = d?.sunset
        if (rise != null && set != null) return iso >= rise && iso < set
        val hour = iso.substring(11, 13).toIntOrNull() ?: return true
        return hour in 6..17
    }

    private fun minutesOf(iso: String): Int? {
        if (iso.length < 16) return null
        val h = iso.substring(11, 13).toIntOrNull() ?: return null
        val m = iso.substring(14, 16).toIntOrNull() ?: return null
        return h * 60 + m
    }

    fun dayLengthMinutes(sunrise: String?, sunset: String?): Int? {
        val a = minutesOf(sunrise ?: return null) ?: return null
        val b = minutesOf(sunset ?: return null) ?: return null
        return (b - a).takeIf { it > 0 }
    }

    /** "05:52" từ "2026-11-22T05:52". */
    fun clockOf(iso: String?): String? = iso?.takeIf { it.length >= 16 }?.substring(11, 16)
}
