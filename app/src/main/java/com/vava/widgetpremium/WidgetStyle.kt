package com.vava.widgetpremium

import android.content.Context

/** WEATHER: nền là bầu trời theo thời tiết đã lưu; chưa có dữ liệu thì dùng nền Standard. */
enum class WidgetTheme { STANDARD, GLASS, WEATHER }

/** Màu chữ trên widget: AUTO giữ màu mặc định (chữ sáng), LIGHT luôn chữ trắng, DARK luôn chữ tối. */
enum class TextTone { AUTO, LIGHT, DARK }

/** AUTO: theo giờ mặt trời mọc/lặn thật; DAY và NIGHT: ép cố định. */
enum class DayMode { AUTO, DAY, NIGHT }

data class WidgetStyle(
    val theme: WidgetTheme = WidgetTheme.STANDARD,
    val accentIndex: Int = 0,
    val opacityPercent: Int = 100,
    val mode: DayMode = DayMode.AUTO,
    val cornerDp: Int = WidgetPalette.DEFAULT_CORNER_DP,
    val textTone: TextTone = TextTone.AUTO,
    val clockScalePercent: Int = WidgetPalette.DEFAULT_CLOCK_SCALE,
    /** Độ rộng viền (dp). 0 = tự động theo giao diện (Kính mờ có viền mảnh, còn lại không viền). */
    val borderDp: Int = 0,
    /** Hiện giây trên đồng hồ widget. Tắt mặc định vì TextClock phải vẽ lại mỗi giây. */
    val showSeconds: Boolean = false
) {
    fun sanitized(): WidgetStyle = copy(
        accentIndex = accentIndex.coerceIn(0, WidgetPalette.ACCENTS.size - 1),
        opacityPercent = opacityPercent.coerceIn(WidgetPalette.MIN_OPACITY, 100),
        cornerDp = cornerDp.coerceIn(0, WidgetPalette.MAX_CORNER_DP),
        clockScalePercent = clockScalePercent.coerceIn(WidgetPalette.MIN_CLOCK_SCALE, WidgetPalette.MAX_CLOCK_SCALE),
        borderDp = borderDp.coerceIn(0, WidgetPalette.MAX_BORDER_DP)
    )
}

/** Màu nền một widget, dạng ARGB. strokeDp = 0 nghĩa là không viền. */
data class Palette(val top: Int, val bottom: Int, val sheen: Int, val stroke: Int, val strokeDp: Float)

/** Tính màu nền cho hai phong cách. Thuần Kotlin, có test; việc vẽ nằm ở WidgetBackground. */
object WidgetPalette {
    val ACCENTS = intArrayOf(0xFF2F7BE0.toInt(), 0xFF14A89A.toInt(), 0xFF7E57C2.toInt(), 0xFFD6457A.toInt(), 0xFFE8803A.toInt())
    const val MIN_OPACITY = 20
    const val DEFAULT_CORNER_DP = 26
    const val MAX_CORNER_DP = 40
    const val MAX_BORDER_DP = 6
    const val MIN_CLOCK_SCALE = 60
    const val DEFAULT_CLOCK_SCALE = 100
    const val MAX_CLOCK_SCALE = 130

    /** Cỡ chữ (sp) của giờ sau khi nhân tỉ lệ người dùng chọn. */
    fun clockSizeSp(baseSp: Float, scalePercent: Int, showSeconds: Boolean = false): Float =
        baseSp * scalePercent.coerceIn(MIN_CLOCK_SCALE, MAX_CLOCK_SCALE) / 100f * (if (showSeconds) SECONDS_SHRINK else 1f)

    /** Khi hiện giây, chuỗi giờ dài hơn nên thu chữ lại để vẫn vừa widget. */
    const val SECONDS_SHRINK = 0.72f

    /** Định dạng TextClock (kiểu 12 giờ, kiểu 24 giờ) cho đồng hồ widget. */
    fun timeFormats(showSeconds: Boolean): Pair<String, String> =
        if (showSeconds) "h:mm:ss" to "HH:mm:ss" else "h:mm" to "HH:mm"

    /** Màu chữ chính và phụ (ARGB) khi người dùng ép tông chữ; null nghĩa là giữ màu mặc định của widget. */
    fun textColors(tone: TextTone): Pair<Int, Int>? = when (tone) {
        TextTone.AUTO -> null
        TextTone.LIGHT -> 0xFFFFFFFF.toInt() to 0xCCFFFFFF.toInt()
        TextTone.DARK -> 0xFF101820.toInt() to 0xCC101820.toInt()
    }

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

    fun palette(style: WidgetStyle, isDay: Boolean, sky: Sky? = null): Palette {
        val s = style.sanitized()
        val accent = ACCENTS[s.accentIndex]
        val white = 0xFFFFFFFF.toInt()
        val black = 0xFF000000.toInt()
        val raw = when (s.theme) {
            WidgetTheme.WEATHER -> if (sky != null) {
                val (t, b) = WeatherSky.colors(sky, isDay)
                Palette(t, b, 0, 0, 0f)
            } else standard(accent, isDay)
            WidgetTheme.STANDARD -> standard(accent, isDay)
            WidgetTheme.GLASS ->
                if (isDay) Palette(withAlpha(mix(accent, white, 0.55f), 0x80), withAlpha(accent, 0x4D), withAlpha(white, 0x50), withAlpha(white, 0xA0), 1.5f)
                else Palette(withAlpha(mix(accent, black, 0.6f), 0x99), withAlpha(mix(accent, black, 0.85f), 0x66), withAlpha(white, 0x30), withAlpha(white, 0x66), 1.5f)
        }
        val p = s.opacityPercent
        val scaled = raw.copy(top = scale(raw.top, p), bottom = scale(raw.bottom, p), sheen = scale(raw.sheen, p), stroke = scale(raw.stroke, p))
        if (s.borderDp <= 0) return scaled
        // Người dùng chọn viền: dùng màu viền sẵn có của giao diện, hoặc trắng mờ nếu giao diện không có viền.
        val color = if ((raw.stroke ushr 24) > 0) scaled.stroke else scale(withAlpha(white, if (isDay) 0xB0 else 0x80), p)
        return scaled.copy(stroke = color, strokeDp = s.borderDp.toFloat())
    }

    private fun standard(accent: Int, isDay: Boolean): Palette {
        val white = 0xFFFFFFFF.toInt()
        val black = 0xFF000000.toInt()
        return if (isDay) Palette(withAlpha(mix(accent, white, 0.35f), 0xE6), withAlpha(accent, 0xE6), 0, 0, 0f)
        else Palette(withAlpha(mix(accent, black, 0.75f), 0xE6), withAlpha(mix(accent, black, 0.88f), 0xE6), 0, 0, 0f)
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
            mode = runCatching { DayMode.valueOf(p.getString("mode", "AUTO")!!) }.getOrDefault(DayMode.AUTO),
            cornerDp = p.getInt("corner", WidgetPalette.DEFAULT_CORNER_DP),
            textTone = runCatching { TextTone.valueOf(p.getString("text", "AUTO")!!) }.getOrDefault(TextTone.AUTO),
            clockScalePercent = p.getInt("clock_scale", WidgetPalette.DEFAULT_CLOCK_SCALE),
            borderDp = p.getInt("border", 0),
            showSeconds = p.getBoolean("seconds", false)
        ).sanitized()
    }

    fun reset(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun save(context: Context, style: WidgetStyle) {
        val s = style.sanitized()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("theme", s.theme.name).putInt("accent", s.accentIndex)
            .putInt("opacity", s.opacityPercent).putString("mode", s.mode.name)
            .putInt("corner", s.cornerDp).putString("text", s.textTone.name)
            .putInt("clock_scale", s.clockScalePercent).putInt("border", s.borderDp).putBoolean("seconds", s.showSeconds).apply()
    }
}
