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

data class DayForecast(val date: String, val code: Int, val maxC: Double, val minC: Double)

data class Weather(
    val tempC: Double,
    val humidity: Int,
    val code: Int,
    val days: List<DayForecast>
)

enum class Condition(val emoji: String) {
    CLEAR("☀️"), PARTLY("⛅"), CLOUDY("☁️"), FOG("🌫️"), DRIZZLE("🌦️"),
    RAIN("🌧️"), SNOW("❄️"), THUNDER("⛈️"), UNKNOWN("❔");

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
            if (time != null && codes != null && max != null && min != null) {
                val n = minOf(time.length(), codes.length(), max.length(), min.length())
                for (i in 0 until n) {
                    days.add(DayForecast(time.getString(i), codes.optInt(i, -1), max.optDouble(i), min.optDouble(i)))
                }
            }
        }
        return Weather(
            tempC = current.getDouble("temperature_2m"),
            humidity = current.optInt("relative_humidity_2m", -1),
            code = current.optInt("weather_code", -1),
            days = days
        )
    }
}
