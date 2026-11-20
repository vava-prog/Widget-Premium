package com.vava.widgetpremium

import android.content.Context
import org.json.JSONException

/** Lưu thành phố đã chọn và bản dự báo gần nhất trên máy, để dùng khi mất mạng. */
object WeatherStore {
    private const val PREFS = "weather"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun city(context: Context): City? {
        val p = prefs(context)
        if (!p.contains("lat") || !p.contains("lon")) return null
        return City(
            name = p.getString("name", "") ?: "",
            admin = p.getString("admin", "") ?: "",
            country = p.getString("country", "") ?: "",
            latitude = java.lang.Double.longBitsToDouble(p.getLong("lat", 0L)),
            longitude = java.lang.Double.longBitsToDouble(p.getLong("lon", 0L))
        )
    }

    fun saveCity(context: Context, city: City) {
        prefs(context).edit()
            .putString("name", city.name)
            .putString("admin", city.admin)
            .putString("country", city.country)
            .putLong("lat", java.lang.Double.doubleToLongBits(city.latitude))
            .putLong("lon", java.lang.Double.doubleToLongBits(city.longitude))
            .remove("forecast")
            .remove("fetched_at")
            .apply()
    }

    fun saveForecast(context: Context, json: String, nowMs: Long) {
        prefs(context).edit().putString("forecast", json).putLong("fetched_at", nowMs).apply()
    }

    fun fetchedAtMs(context: Context): Long = prefs(context).getLong("fetched_at", 0L)

    fun cached(context: Context): Weather? {
        val json = prefs(context).getString("forecast", null) ?: return null
        return try {
            WeatherParsers.parseWeather(json)
        } catch (e: JSONException) {
            null
        }
    }
}
