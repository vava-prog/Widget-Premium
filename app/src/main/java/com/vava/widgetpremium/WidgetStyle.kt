package com.vava.widgetpremium

import android.content.Context

enum class WidgetTheme { STANDARD, GLASS }

/** AUTO: theo giờ mặt trời mọc/lặn thật; DAY và NIGHT: ép cố định. */
enum class DayMode { AUTO, DAY, NIGHT }

data class WidgetStyle(
    val theme: WidgetTheme = WidgetTheme.STANDARD,
    val accentIndex: Int = 0,
    val opacityPercent: Int = 100,
    val mode: DayMode = DayMode.AUTO
) {
    fun sanitized(): WidgetStyle = copy(
        accentIndex = accentIndex.coerceIn(0, WidgetPalette.ACCENTS.size - 1),
        opacityPercent = opacityPercent.coerceIn(WidgetPalette.MIN_OPACITY, 100)
    )
}

/** Màu nền một widget, dạng ARGB. strokeDp = 0 nghĩa là không viền. */
data class Palette(val top: Int, val bottom: Int, val sheen: Int, val stroke: Int, val strokeDp: Float)

/** Tính màu nền cho hai phong cách. Thuần Kotlin, có test; việc vẽ nằm ở WidgetBackground. */
object WidgetPalette {
    val ACCENTS = intArrayOf(0xFF2F7BE0.toInt(), 0xFF14A89A.toInt(), 0xFF7E57C2.toInt(), 0xFFD6457A.toInt(), 0xFFE8803A.toInt())
    const val MIN_OPACITY = 20

    fun withAlpha(argb: Int, alpha: Int): Int = (alpha.coerceIn(0, 255) shl 24) or (argb and 0x00FFFFFF)

    /** Trộn màu với trắng hoặc đen: amount 0 giữ nguyên màu gốc, 1 thành màu đích. Kết quả không trong suốt. */
    fun mix(argb: Int, target: Int, amount: Float): Int {
        fun ch(shift: Int): Int {
            val a = (argb shr shift) and 0xFF
            val b = (target shr shift) and 0xFF
            return (a + (b - a) * amount).toInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    private fun scale(argb: Int, percent: Int): Int {
        val alpha = ((argb ushr 24) * percent / 100f).toInt()
        return withAlpha(argb, alpha)
    }

    fun resolveDay(mode: DayMode, sunSaysDay: Boolean): Boolean = when (mode) {
        DayMode.AUTO -> sunSaysDay
        DayMode.DAY -> true
        DayMode.NIGHT -> false
    }

    fun palette(style: WidgetStyle, isDay: Boolean): Palette {
        val s = style.sanitized()
        val accent = ACCENTS[s.accentIndex]
        val white = 0xFFFFFFFF.toInt()
        val black = 0xFF000000.toInt()
        val raw = when (s.theme) {
            WidgetTheme.STANDARD ->
                if (isDay) Palette(withAlpha(mix(accent, white, 0.35f), 0xE6), withAlpha(accent, 0xE6), 0, 0, 0f)
                else Palette(withAlpha(mix(accent, black, 0.75f), 0xE6), withAlpha(mix(accent, black, 0.88f), 0xE6), 0, 0, 0f)
            WidgetTheme.GLASS ->
                if (isDay) Palette(withAlpha(mix(accent, white, 0.55f), 0x80), withAlpha(accent, 0x4D), withAlpha(white, 0x50), withAlpha(white, 0xA0), 1.5f)
                else Palette(withAlpha(mix(accent, black, 0.6f), 0x99), withAlpha(mix(accent, black, 0.85f), 0x66), withAlpha(white, 0x30), withAlpha(white, 0x66), 1.5f)
        }
        val p = s.opacityPercent
        return raw.copy(top = scale(raw.top, p), bottom = scale(raw.bottom, p), sheen = scale(raw.sheen, p), stroke = scale(raw.stroke, p))
    }
}

/** Lưu lựa chọn giao diện trên máy (SharedPreferences "style"). */
object StyleStore {
    private const val PREFS = "style"

    fun load(context: Context): WidgetStyle {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return WidgetStyle(
            theme = runCatching { WidgetTheme.valueOf(p.getString("theme", "STANDARD")!!) }.getOrDefault(WidgetTheme.STANDARD),
            accentIndex = p.getInt("accent", 0),
            opacityPercent = p.getInt("opacity", 100),
            mode = runCatching { DayMode.valueOf(p.getString("mode", "AUTO")!!) }.getOrDefault(DayMode.AUTO)
        ).sanitized()
    }

    fun save(context: Context, style: WidgetStyle) {
        val s = style.sanitized()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("theme", s.theme.name).putInt("accent", s.accentIndex)
            .putInt("opacity", s.opacityPercent).putString("mode", s.mode.name).apply()
    }
}
