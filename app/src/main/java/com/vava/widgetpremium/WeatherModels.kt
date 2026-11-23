package com.vava.widgetpremium

import org.json.JSONObject

data class City(
    val name: String,
    val admin: String,
    val country: String,
    val latitude: Double,
    val longitude: Double
) {
    /** Nhãn hiển thị, ví dụ "Hà Nội, Việt Nam". */
    val label: String
        get() = listOf(name, admin, country).filter { it.isNotBlank() }.distinct().joinToString(", ")
}

data class DayForecast(
    val date: String,
    val code: Int,
    val maxC: Double,
    val minC: Double,
    val sunrise: String? = null,
    val sunset: String? = null,
    /** Khả năng mưa lớn nhất trong ngày (%), null nếu nguồn không có. */
    val rainPercent: Int? = null
)

data class HourForecast(val time: String, val tempC: Double, val code: Int)

data class Weather(
    val tempC: Double,
    val humidity: Int,
    val code: Int,
    val days: List<DayForecast>,
    /** Độ lệch múi giờ của thành phố so với UTC (giây). */
    val utcOffsetSec: Int = 0,
    /** Dự báo từng giờ (giờ địa phương của thành phố). Rỗng với dữ liệu lưu từ bản cũ. */
    val hourly: List<HourForecast> = emptyList(),
    /** Nhiệt độ cảm nhận (°C); null với dữ liệu lưu từ bản cũ. */
    val feelsC: Double? = null
)

enum class Condition(val emoji: String) {
    CLEAR("☀️"), PARTLY("⛅"), CLOUDY("☁️"), FOG("🌫️"), DRIZZLE("🌦️"),
    RAIN("🌧️"), SNOW("❄️"), THUNDER("⛈️"), UNKNOWN("❔");

    /** Biểu tượng theo ban ngày hoặc ban đêm: trời quang ban đêm là trăng. */
    fun emoji(isDay: Boolean): String = when {
        isDay -> emoji
        this == CLEAR -> "🌙"
        this == PARTLY -> "☁️"
        else -> emoji
    }

    companion object {
        /** Đổi mã thời tiết WMO của Open-Meteo sang nhóm tình trạng. */
        fun fromWmo(code: Int): Condition = when (code) {
            0, 1 -> CLEAR
            2 -> PARTLY
            3 -> CLOUDY
            45, 48 -> FOG
            in 51..57 -> DRIZZLE
            in 61..67, in 80..82 -> RAIN
            in 71..77, 85, 86 -> SNOW
            in 95..99 -> THUNDER
            else -> UNKNOWN
        }
    }
}

object WeatherParsers {
    fun parseCities(json: String): List<City> {
        val results = JSONObject(json).optJSONArray("results") ?: return emptyList()
        val list = ArrayList<City>()
        for (i in 0 until results.length()) {
            val o = results.optJSONObject(i) ?: continue
            if (!o.has("latitude") || !o.has("longitude")) continue
            list.add(
                City(
                    name = o.optString("name"),
                    admin = o.optString("admin1"),
                    country = o.optString("country"),
                    latitude = o.getDouble("latitude"),
                    longitude = o.getDouble("longitude")
                )
            )
        }
        return list
    }

    /** Trả null nếu dữ liệu thiếu phần thời tiết hiện tại. */
    fun parseWeather(json: String): Weather? {
        val root = JSONObject(json)
        val current = root.optJSONObject("current") ?: return null
        if (!current.has("temperature_2m")) return null
        val days = ArrayList<DayForecast>()
        val daily = root.optJSONObject("daily")
        if (daily != null) {
            val time = daily.optJSONArray("time")
            val codes = daily.optJSONArray("weather_code")
            val max = daily.optJSONArray("temperature_2m_max")
            val min = daily.optJSONArray("temperature_2m_min")
            val sunrise = daily.optJSONArray("sunrise")
            val sunset = daily.optJSONArray("sunset")
            val rain = daily.optJSONArray("precipitation_probability_max")
            if (time != null && codes != null && max != null && min != null) {
                val n = minOf(time.length(), codes.length(), max.length(), min.length())
                for (i in 0 until n) {
                    days.add(
                        DayForecast(
                            time.getString(i), codes.optInt(i, -1), max.optDouble(i), min.optDouble(i),
                            sunrise?.optString(i)?.takeIf { it.isNotEmpty() },
                            sunset?.optString(i)?.takeIf { it.isNotEmpty() },
                            rainPercent = if (rain == null || rain.isNull(i)) null else rain.optInt(i).coerceIn(0, 100)
                        )
                    )
                }
            }
        }
        val hours = ArrayList<HourForecast>()
        val hourly = root.optJSONObject("hourly")
        if (hourly != null) {
            val time = hourly.optJSONArray("time")
            val temps = hourly.optJSONArray("temperature_2m")
            val codes = hourly.optJSONArray("weather_code")
            if (time != null && temps != null && codes != null) {
                val n = minOf(time.length(), temps.length(), codes.length())
                for (i in 0 until n) {
                    if (temps.isNull(i)) continue
                    hours.add(HourForecast(time.getString(i), temps.getDouble(i), codes.optInt(i, -1)))
                }
            }
        }
        return Weather(
            tempC = current.getDouble("temperature_2m"),
            humidity = current.optInt("relative_humidity_2m", -1),
            code = current.optInt("weather_code", -1),
            days = days,
            utcOffsetSec = root.optInt("utc_offset_seconds", 0),
            hourly = hours,
            feelsC = current.optDouble("apparent_temperature", Double.NaN).takeIf { !it.isNaN() }
        )
    }
}

/** Dòng chi tiết thời tiết hôm nay: thấp, cao (làm tròn) và khả năng mưa nếu có. */
object WeatherDetail {
    data class Parts(val minC: Int, val maxC: Int, val rainPercent: Int?)

    fun of(day: DayForecast?): Parts? {
        if (day == null || day.minC.isNaN() || day.maxC.isNaN()) return null
        return Parts(Math.round(day.minC).toInt(), Math.round(day.maxC).toInt(), day.rainPercent)
    }
}
