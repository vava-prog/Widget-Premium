package com.vava.widgetpremium

import java.util.Locale

object Formats {
    /** Đổi byte sang GB thập phân: từ 10 GB trở lên làm tròn số nguyên, dưới đó giữ một chữ số lẻ. */
    fun gb(bytes: Long, locale: Locale = Locale.getDefault()): String {
        val value = bytes / 1_000_000_000.0
        return if (value >= 10) String.format(locale, "%.0f", value) else String.format(locale, "%.1f", value)
    }

    /** "2026-11-20" thành "20/11". Chuỗi lạ thì trả nguyên. */
    fun shortDate(iso: String): String {
        val parts = iso.split("-")
        return if (parts.size == 3) parts[2] + "/" + parts[1] else iso
    }

    fun clockTime(epochMs: Long): String =
        java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date(epochMs))

    fun celsius(value: Float, locale: Locale = Locale.getDefault()): String = String.format(locale, "%.1f", value)
}
