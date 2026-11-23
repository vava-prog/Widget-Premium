package com.vava.widgetpremium

import android.content.Context

/** Bầu trời vẽ trên nền widget, chọn theo tình trạng thời tiết đã lưu và ban ngày hay ban đêm. */
enum class Sky { CLEAR_DAY, CLEAR_NIGHT, PARTLY_DAY, PARTLY_NIGHT, CLOUDY, FOG, RAIN, THUNDER, SNOW }

/** Logic thuần (có test): chọn loại bầu trời và hai màu nền. Việc vẽ nằm ở WidgetBackground. */
object WeatherSky {
    fun of(condition: Condition, isDay: Boolean): Sky = when (condition) {
        Condition.CLEAR -> if (isDay) Sky.CLEAR_DAY else Sky.CLEAR_NIGHT
        Condition.PARTLY -> if (isDay) Sky.PARTLY_DAY else Sky.PARTLY_NIGHT
        Condition.CLOUDY, Condition.UNKNOWN -> Sky.CLOUDY
        Condition.FOG -> Sky.FOG
        Condition.DRIZZLE, Condition.RAIN -> Sky.RAIN
        Condition.THUNDER -> Sky.THUNDER
        Condition.SNOW -> Sky.SNOW
    }

    /** Màu nền (ARGB, đục) từ trên xuống dưới. Ban đêm tối hơn ban ngày. */
    fun colors(sky: Sky, isDay: Boolean): Pair<Int, Int> {
        val black = 0xFF000000.toInt()
        val (top, bottom) = when (sky) {
            Sky.CLEAR_DAY -> 0xFF2F7FE6.toInt() to 0xFF8CC8F7.toInt()
            Sky.CLEAR_NIGHT -> 0xFF0A1433.toInt() to 0xFF2B4175.toInt()
            Sky.PARTLY_DAY -> 0xFF4A8FDE.toInt() to 0xFF9CC5E6.toInt()
            Sky.PARTLY_NIGHT -> 0xFF14213F.toInt() to 0xFF3A4D78.toInt()
            Sky.CLOUDY -> 0xFF63788C.toInt() to 0xFF9FB0BF.toInt()
            Sky.FOG -> 0xFF8793A0.toInt() to 0xFFB5BFC8.toInt()
            Sky.RAIN -> 0xFF39465A.toInt() to 0xFF627588.toInt()
            Sky.THUNDER -> 0xFF292840.toInt() to 0xFF4F4C6E.toInt()
            Sky.SNOW -> 0xFF7890A9.toInt() to 0xFFC2CFDB.toInt()
        }
        val dim = isDay || sky == Sky.CLEAR_NIGHT || sky == Sky.PARTLY_NIGHT
        return if (dim) top to bottom
        else WidgetPalette.mix(top, black, 0.45f) to WidgetPalette.mix(bottom, black, 0.45f)
    }

    /** Bầu trời hiện tại; null khi chưa chọn thành phố hoặc chưa có dữ liệu thời tiết. */
    fun current(context: Context, isDay: Boolean): Sky? {
        if (WeatherStore.city(context) == null) return null
        val weather = WeatherStore.cached(context) ?: return null
        return of(Condition.fromWmo(weather.code), isDay)
    }
}
