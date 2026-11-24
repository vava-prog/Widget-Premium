package com.vava.widgetpremium

import android.content.Context

/** WEATHER: nền là bầu trời theo thời tiết đã lưu; chưa có dữ liệu thì dùng nền Standard. */
enum class WidgetTheme { STANDARD, GLASS, WEATHER }

/** Màu chữ trên widget: AUTO giữ màu mặc định (chữ sáng), LIGHT luôn chữ trắng, DARK luôn chữ tối. */
/** Kiểu giờ: theo hệ thống, ép 24 giờ hoặc ép 12 giờ. */
enum class HourFormat { SYSTEM, H24, H12 }

/** Hàng thứ ba của dải tuần: ngày âm hoặc thời tiết từng ngày. */
enum class WeekMode { LUNAR, WEATHER }

enum class TextTone { AUTO, LIGHT, DARK, CUSTOM }

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
    val showSeconds: Boolean = false,
    /** Dòng tên tự nhập hiện trên widget; rỗng thì ẩn. */
    val label: String = "",
    /** Màu chữ khi chọn TextTone.CUSTOM (ARGB, luôn đục). */
    val customTextColor: Int = WidgetPalette.CUSTOM_TEXT_COLORS[0],
    val showSolarDate: Boolean = true,
    val showLunar: Boolean = true,
    val showWeather: Boolean = true,
    val hourFormat: HourFormat = HourFormat.SYSTEM,
    /** Tắt thì chạm vào widget không mở gì. */
    val tapEnabled: Boolean = true,
    val weekMode: WeekMode = WeekMode.LUNAR,
    /** Thêm dòng thấp/cao và khả năng mưa dưới dòng thời tiết của widget thông tin. */
    val showWeatherDetail: Boolean = false,
    /** Chỉ số ô màu nổi của ngày hôm nay trên dải tuần (0 = trắng mờ mặc định). */
    val todayColorIndex: Int = 0,
    /** Độ mờ kính 0..100: lớp sương trắng phủ lên mặt kính (chỉ giao diện Kính mờ). */
    val glassFrostPercent: Int = 0,
    /** Độ nổi khối kính 0..100: viền và mép sáng dày hơn, rõ hơn (chỉ giao diện Kính mờ). */
    val glassReliefPercent: Int = WidgetPalette.DEFAULT_GLASS_RELIEF,
    /** Hiện tên thành phố cạnh nhiệt độ trên widget Lịch tuần. */
    val showLocation: Boolean = true,
    /** Hiệu ứng mưa/nắng/mây... chạy vài giây rồi dừng trên nền thời tiết (chỉ giao diện Thời tiết). */
    val weatherMotion: Boolean = true
) {
    fun sanitized(): WidgetStyle = copy(
        accentIndex = accentIndex.coerceIn(0, WidgetPalette.ACCENTS.size - 1),
        opacityPercent = opacityPercent.coerceIn(WidgetPalette.MIN_OPACITY, 100),
        cornerDp = cornerDp.coerceIn(0, WidgetPalette.MAX_CORNER_DP),
        clockScalePercent = clockScalePercent.coerceIn(WidgetPalette.MIN_CLOCK_SCALE, WidgetPalette.MAX_CLOCK_SCALE),
        borderDp = borderDp.coerceIn(0, WidgetPalette.MAX_BORDER_DP),
        label = WidgetPalette.cleanLabel(label),
        todayColorIndex = todayColorIndex.coerceIn(0, WidgetPalette.TODAY_SWATCHES.size - 1),
        glassFrostPercent = glassFrostPercent.coerceIn(0, 100),
        glassReliefPercent = glassReliefPercent.coerceIn(0, 100),
        customTextColor = customTextColor or (0xFF shl 24)
    )
}

/** Màu nền một widget, dạng ARGB. strokeDp = 0 nghĩa là không viền. */
data class Palette(
    val top: Int, val bottom: Int, val sheen: Int, val stroke: Int, val strokeDp: Float,
    /** Lớp sương trắng phủ lên mặt kính (ARGB); 0 = không phủ. */
    val frost: Int = 0
)

/** Tính màu nền cho hai phong cách. Thuần Kotlin, có test; việc vẽ nằm ở WidgetBackground. */
object WidgetPalette {
    val ACCENTS = intArrayOf(0xFF2F7BE0.toInt(), 0xFF14A89A.toInt(), 0xFF7E57C2.toInt(), 0xFFD6457A.toInt(), 0xFFE8803A.toInt())
    const val MIN_OPACITY = 20
    const val DEFAULT_CORNER_DP = 26
    const val MAX_CORNER_DP = 40
    const val MAX_BORDER_DP = 6
    const val MAX_LABEL_LENGTH = 24
    const val DEFAULT_GLASS_RELIEF = 60

    /** Độ đậm (0..255) của mép sáng phía trên mặt kính; 60% là giá trị gốc. */
    fun glassSheenAlpha(isDay: Boolean, reliefPercent: Int): Int =
        ((if (isDay) 0x50 else 0x30) * reliefPercent.coerceIn(0, 100) / DEFAULT_GLASS_RELIEF).coerceIn(0, 0xFF)

    /** Độ đậm (0..255) của viền kính. */
    fun glassStrokeAlpha(isDay: Boolean, reliefPercent: Int): Int =
        ((if (isDay) 0xA0 else 0x66) * reliefPercent.coerceIn(0, 100) / DEFAULT_GLASS_RELIEF).coerceIn(0, 0xFF)

    /** Độ dày viền kính (dp): 1,5 dp ở mức gốc, mảnh hơn khi giảm, dày hơn khi tăng. */
    fun glassStrokeDp(reliefPercent: Int): Float =
        1.5f * (40f + reliefPercent.coerceIn(0, 100) * 60f / DEFAULT_GLASS_RELIEF) / 100f

    /** Độ đậm (0..255) của lớp sương trắng theo độ mờ kính. */
    fun glassFrostAlpha(frostPercent: Int): Int = (0x70 * frostPercent.coerceIn(0, 100) / 100)

    /** Màu nổi cho ngày hôm nay trên dải tuần (hiện ở ô chọn; nền thật là bản mờ của màu này). */
    val TODAY_SWATCHES = intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFE066.toInt(), 0xFF7EE0A0.toInt(), 0xFF7FD8FF.toInt(), 0xFFFF9EC4.toInt(), 0xFFFFB36B.toInt())

    /** Gộp xuống một dòng, bỏ khoảng trắng thừa, cắt tối đa MAX_LABEL_LENGTH ký tự. */
    fun cleanLabel(raw: String): String =
        raw.replace(Regex("\\s+"), " ").trim().take(MAX_LABEL_LENGTH).trim()
    const val MIN_CLOCK_SCALE = 60
    const val DEFAULT_CLOCK_SCALE = 100
    const val MAX_CLOCK_SCALE = 130

    /** Cỡ chữ (sp) của giờ sau khi nhân tỉ lệ người dùng chọn. */
    fun clockSizeSp(baseSp: Float, scalePercent: Int, showSeconds: Boolean = false): Float =
        baseSp * scalePercent.coerceIn(MIN_CLOCK_SCALE, MAX_CLOCK_SCALE) / 100f * (if (showSeconds) SECONDS_SHRINK else 1f)

    /** Khi hiện giây, chuỗi giờ dài hơn nên thu chữ lại để vẫn vừa widget. */
    const val SECONDS_SHRINK = 0.72f

    /**
     * Định dạng TextClock (kiểu 12 giờ, kiểu 24 giờ) cho đồng hồ widget. TextClock chọn một trong hai
     * theo cài đặt hệ thống, nên muốn ép một kiểu thì đặt cả hai thành cùng một mẫu.
     */
    fun timeFormats(showSeconds: Boolean, hourFormat: HourFormat = HourFormat.SYSTEM): Pair<String, String> {
        val sec = if (showSeconds) ":ss" else ""
        val f12 = "h:mm$sec"
        val f24 = "HH:mm$sec"
        return when (hourFormat) {
            HourFormat.SYSTEM -> f12 to f24
            HourFormat.H24 -> f24 to f24
            HourFormat.H12 -> f12 to f12
        }
    }

    /** Màu chữ chính và phụ (ARGB) khi người dùng ép tông chữ; null nghĩa là giữ màu mặc định của widget. */
    /** Các ô màu chữ gợi ý cho chế độ tùy chỉnh. */
    val CUSTOM_TEXT_COLORS = intArrayOf(
        0xFFFFF3B0.toInt(), 0xFFB8F0D0.toInt(), 0xFF9FE3FF.toInt(), 0xFFFFC2D9.toInt(),
        0xFFFFD2A3.toInt(), 0xFFE0D2FF.toInt(), 0xFF0D2B52.toInt(), 0xFF4A1F2E.toInt()
    )

    fun textColors(tone: TextTone, custom: Int = CUSTOM_TEXT_COLORS[0]): Pair<Int, Int>? = when (tone) {
        TextTone.CUSTOM -> (custom or (0xFF shl 24)) to withAlpha(custom, 0xCC)
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
            WidgetTheme.GLASS -> {
                val sheen = withAlpha(white, glassSheenAlpha(isDay, s.glassReliefPercent))
                val stroke = withAlpha(white, glassStrokeAlpha(isDay, s.glassReliefPercent))
                val frost = withAlpha(white, glassFrostAlpha(s.glassFrostPercent))
                val dp = glassStrokeDp(s.glassReliefPercent)
                if (isDay) Palette(withAlpha(mix(accent, white, 0.55f), 0x80), withAlpha(accent, 0x4D), sheen, stroke, dp, frost)
                else Palette(withAlpha(mix(accent, black, 0.6f), 0x99), withAlpha(mix(accent, black, 0.85f), 0x66), sheen, stroke, dp, frost)
            }
        }
        val p = s.opacityPercent
        val scaled = raw.copy(top = scale(raw.top, p), bottom = scale(raw.bottom, p), sheen = scale(raw.sheen, p), stroke = scale(raw.stroke, p), frost = scale(raw.frost, p))
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
        val theme = runCatching { WidgetTheme.valueOf(p.getString("theme", "STANDARD")!!) }.getOrDefault(WidgetTheme.STANDARD)
        return WidgetStyle(
            theme = theme,
            accentIndex = p.getInt("accent", 0),
            opacityPercent = p.getInt("opacity", 100),
            mode = runCatching { DayMode.valueOf(p.getString("mode", "AUTO")!!) }.getOrDefault(DayMode.AUTO),
            cornerDp = p.getInt("corner", WidgetPalette.DEFAULT_CORNER_DP),
            textTone = textFor(p, theme).first,
            clockScalePercent = p.getInt("clock_scale", WidgetPalette.DEFAULT_CLOCK_SCALE),
            borderDp = p.getInt("border", 0),
            showSeconds = p.getBoolean("seconds", false),
            label = p.getString("label", "") ?: "",
            customTextColor = textFor(p, theme).second,
            showSolarDate = p.getBoolean("show_solar", true),
            showLunar = p.getBoolean("show_lunar", true),
            showWeather = p.getBoolean("show_weather", true),
            hourFormat = enumOrDefault(p.getString("hour_format", null), HourFormat.SYSTEM),
            tapEnabled = p.getBoolean("tap_enabled", true),
            weekMode = enumOrDefault(p.getString("week_mode", null), WeekMode.LUNAR),
            showWeatherDetail = p.getBoolean("weather_detail", false),
            todayColorIndex = p.getInt("today_color", 0),
            glassFrostPercent = p.getInt("glass_frost", 0),
            glassReliefPercent = p.getInt("glass_relief", WidgetPalette.DEFAULT_GLASS_RELIEF),
            showLocation = p.getBoolean("show_location", true),
            weatherMotion = p.getBoolean("weather_motion", true)
        ).sanitized()
    }

    /** Khóa lưu riêng cho từng giao diện, ví dụ "text_GLASS". Thuần, có test. */
    fun textKey(base: String, theme: WidgetTheme): String = base + "_" + theme.name

    /** Màu chữ đã lưu của một giao diện; chưa có thì dùng giá trị chung cũ rồi mới đến mặc định. */
    private fun textFor(p: android.content.SharedPreferences, theme: WidgetTheme): Pair<TextTone, Int> {
        val tone = p.getString(textKey("text", theme), null) ?: p.getString("text", "AUTO")
        val color = if (p.contains(textKey("text_custom", theme))) p.getInt(textKey("text_custom", theme), 0)
        else p.getInt("text_custom", WidgetPalette.CUSTOM_TEXT_COLORS[0])
        return (runCatching { TextTone.valueOf(tone!!) }.getOrDefault(TextTone.AUTO)) to color
    }

    /** Đổi giao diện và nạp màu chữ riêng của giao diện đó (mỗi giao diện nhớ màu chữ của mình). */
    fun withTheme(context: Context, current: WidgetStyle, theme: WidgetTheme): WidgetStyle {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val (tone, color) = textFor(p, theme)
        return current.copy(theme = theme, textTone = tone, customTextColor = color)
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
            .putString(textKey("text", s.theme), s.textTone.name).putInt(textKey("text_custom", s.theme), s.customTextColor)
            .putInt("clock_scale", s.clockScalePercent).putInt("border", s.borderDp).putBoolean("seconds", s.showSeconds).putString("label", s.label).putInt("text_custom", s.customTextColor)
            .putBoolean("show_solar", s.showSolarDate).putBoolean("show_lunar", s.showLunar)
            .putBoolean("show_weather", s.showWeather).putString("hour_format", s.hourFormat.name)
            .putBoolean("tap_enabled", s.tapEnabled).putString("week_mode", s.weekMode.name)
            .putBoolean("weather_detail", s.showWeatherDetail).putInt("today_color", s.todayColorIndex)
            .putInt("glass_frost", s.glassFrostPercent).putInt("glass_relief", s.glassReliefPercent).putBoolean("show_location", s.showLocation).putBoolean("weather_motion", s.weatherMotion).apply()
    }
}

private inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: default
