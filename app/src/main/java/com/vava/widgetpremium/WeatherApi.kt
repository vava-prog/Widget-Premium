package com.vava.widgetpremium

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/** Gọi Open-Meteo qua HTTPS. Chỉ gửi tên thành phố (tìm kiếm) hoặc tọa độ (dự báo). Phải gọi ngoài luồng chính. */
object WeatherApi {
    private const val GEOCODING = "https://geocoding-api.open-meteo.com/v1/search"
    private const val FORECAST = "https://api.open-meteo.com/v1/forecast"

    @Throws(IOException::class)
    fun searchCities(query: String): List<City> {
        val name = URLEncoder.encode(query.trim(), "UTF-8")
        return WeatherParsers.parseCities(get("$GEOCODING?name=$name&count=5&language=vi&format=json"))
    }

    @Throws(IOException::class)
    fun fetchForecastJson(city: City): String {
        val lat = String.format(Locale.US, "%.4f", city.latitude)
        val lon = String.format(Locale.US, "%.4f", city.longitude)
        return get(
            "$FORECAST?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,relative_humidity_2m,weather_code" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
                "&timezone=auto&forecast_days=7"
        )
    }

    @Throws(IOException::class)
    private fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP " + connection.responseCode)
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
