package com.vava.widgetpremium

import java.time.Instant
import java.time.ZoneOffset
import java.util.TimeZone

/** Giờ mặt trời mọc và lặn, tính bằng phút trong ngày theo giờ địa phương. [offsetMs] là độ lệch múi giờ tính bằng mili giây. */
data class SunTimes(val sunriseMin: Int, val sunsetMin: Int, val offsetMs: Long)

object DayNight {
    const val DEFAULT_SUNRISE_MIN = 6 * 60
    const val DEFAULT_SUNSET_MIN = 18 * 60
    private const val DAY_MIN = 24 * 60
    private const val MIN_MS = 60_000L

    /** Mặc định khi chưa có thời tiết: 6:00 đến 18:00 theo múi giờ của máy. */
    fun fallback(nowMs: Long): SunTimes =
        SunTimes(DEFAULT_SUNRISE_MIN, DEFAULT_SUNSET_MIN, TimeZone.getDefault().getOffset(nowMs).toLong())

    private fun minuteOfDay(nowMs: Long, offsetMs: Long): Int =
        Math.floorMod((nowMs + offsetMs) / MIN_MS, DAY_MIN.toLong()).toInt()

    fun isDay(nowMs: Long, sun: SunTimes): Boolean {
        val m = minuteOfDay(nowMs, sun.offsetMs)
        return m >= sun.sunriseMin && m < sun.sunsetMin
    }

    /** Thời điểm (epoch ms) của lần đổi ngày-đêm kế tiếp, luôn sau [nowMs]. */
    fun nextTransitionMs(nowMs: Long, sun: SunTimes): Long {
        val m = minuteOfDay(nowMs, sun.offsetMs)
        val nowMinuteStart = nowMs - ((nowMs + sun.offsetMs) % MIN_MS + MIN_MS) % MIN_MS
        var best = Long.MAX_VALUE
        for (target in intArrayOf(sun.sunriseMin, sun.sunsetMin)) {
            var delta = target - m
            if (delta <= 0) delta += DAY_MIN
            best = minOf(best, nowMinuteStart + delta * MIN_MS)
        }
        return best
    }

    /** "2026-11-20T05:58" thành số phút trong ngày (5*60+58). Sai định dạng thì trả null. */
    fun parseMinutes(isoLocal: String?): Int? {
        if (isoLocal == null || isoLocal.length < 16 || isoLocal[10] != 'T') return null
        val h = isoLocal.substring(11, 13).toIntOrNull() ?: return null
        val min = isoLocal.substring(14, 16).toIntOrNull() ?: return null
        return if (h in 0..23 && min in 0..59) h * 60 + min else null
    }

    /** Giờ mọc/lặn của hôm nay tại thành phố trong dữ liệu thời tiết, hoặc null nếu thiếu. */
    fun sunTimesFor(weather: Weather, nowMs: Long): SunTimes? {
        val offsetMs = weather.utcOffsetSec * 1000L
        val localDate = Instant.ofEpochMilli(nowMs + offsetMs).atZone(ZoneOffset.UTC).toLocalDate().toString()
        val day = weather.days.firstOrNull { it.date == localDate } ?: weather.days.firstOrNull() ?: return null
        val rise = parseMinutes(day.sunrise) ?: return null
        val set = parseMinutes(day.sunset) ?: return null
        if (rise >= set) return null
        return SunTimes(rise, set, offsetMs)
    }
}
