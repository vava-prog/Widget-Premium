package com.vava.widgetpremium

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherParsersTest {
    private val forecast = """
        {"latitude":21.0,"longitude":105.8,
         "current":{"time":"2026-11-20T17:00","interval":900,"temperature_2m":27.4,"relative_humidity_2m":71,"weather_code":3},
         "daily":{"time":["2026-11-20","2026-11-21"],"weather_code":[3,61],
                  "temperature_2m_max":[28.1,25.0],"temperature_2m_min":[21.2,19.5]}}
    """.trimIndent()

    @Test
    fun parsesCurrentAndDaily() {
        val w = WeatherParsers.parseWeather(forecast)
        assertNotNull(w)
        assertEquals(27.4, w!!.tempC, 0.001)
        assertEquals(71, w.humidity)
        assertEquals(3, w.code)
        assertEquals(2, w.days.size)
        assertEquals("2026-11-21", w.days[1].date)
        assertEquals(61, w.days[1].code)
        assertEquals(19.5, w.days[1].minC, 0.001)
    }

    @Test
    fun missingCurrentReturnsNull() {
        assertNull(WeatherParsers.parseWeather("""{"daily":{}}"""))
    }

    @Test
    fun parsesCities() {
        val json = """{"results":[
            {"name":"Hà Nội","latitude":21.0245,"longitude":105.84117,"country":"Việt Nam","admin1":"Hà Nội"},
            {"name":"Hanoi","latitude":20.0,"longitude":106.0}]}"""
        val cities = WeatherParsers.parseCities(json)
        assertEquals(2, cities.size)
        assertEquals("Hà Nội, Việt Nam", cities[0].label)
        assertEquals(21.0245, cities[0].latitude, 0.00001)
        assertEquals("Hanoi", cities[1].label)
    }

    @Test
    fun noResultsKeyMeansEmptyList() {
        assertTrue(WeatherParsers.parseCities("""{"generationtime_ms":0.5}""").isEmpty())
    }

    @Test
    fun wmoCodesMapToConditions() {
        assertEquals(Condition.CLEAR, Condition.fromWmo(0))
        assertEquals(Condition.PARTLY, Condition.fromWmo(2))
        assertEquals(Condition.CLOUDY, Condition.fromWmo(3))
        assertEquals(Condition.FOG, Condition.fromWmo(45))
        assertEquals(Condition.DRIZZLE, Condition.fromWmo(53))
        assertEquals(Condition.RAIN, Condition.fromWmo(63))
        assertEquals(Condition.RAIN, Condition.fromWmo(81))
        assertEquals(Condition.SNOW, Condition.fromWmo(73))
        assertEquals(Condition.THUNDER, Condition.fromWmo(95))
        assertEquals(Condition.UNKNOWN, Condition.fromWmo(-1))
    }

    @Test
    fun shortDateFormats() {
        assertEquals("20/11", Formats.shortDate("2026-11-20"))
        assertEquals("abc", Formats.shortDate("abc"))
    }

    @Test
    fun parsesFeelsLikeAndHourlyWhenPresent() {
        val json = """
            {"utc_offset_seconds":25200,
             "current":{"temperature_2m":24.0,"apparent_temperature":26.3,"relative_humidity_2m":80,"weather_code":2},
             "hourly":{"time":["2026-11-22T05:00","2026-11-22T06:00","2026-11-22T07:00"],
                       "temperature_2m":[23.0,null,25.5],"weather_code":[1,2,3]},
             "daily":{"time":["2026-11-22"],"weather_code":[2],"temperature_2m_max":[30.0],"temperature_2m_min":[22.0]}}
        """.trimIndent()
        val w = WeatherParsers.parseWeather(json)!!
        assertEquals(26.3, w.feelsC!!, 0.001)
        assertEquals(2, w.hourly.size) // giờ có giá trị null bị bỏ qua
        assertEquals("2026-11-22T07:00", w.hourly[1].time)
        assertEquals(25.5, w.hourly[1].tempC, 0.001)
        assertEquals(3, w.hourly[1].code)
    }

    @Test
    fun oldCachedDataHasNoFeelsLikeOrHourly() {
        val w = WeatherParsers.parseWeather(forecast)!!
        assertNull(w.feelsC)
        assertTrue(w.hourly.isEmpty())
    }

    @Test fun parsesRainChanceAndDetail() {
        val json = """{"utc_offset_seconds":25200,"current":{"temperature_2m":28.0,"weather_code":0},
            "daily":{"time":["2026-11-23","2026-11-24"],"weather_code":[0,61],"temperature_2m_max":[29.4,28.6],
            "temperature_2m_min":[22.0,23.0],"precipitation_probability_max":[10,null]}}"""
        val w = WeatherParsers.parseWeather(json)!!
        assertEquals(10, w.days[0].rainPercent)
        assertEquals(null, w.days[1].rainPercent)
        val d = WeatherDetail.of(w.days[0])!!
        assertEquals(22, d.minC)
        assertEquals(29, d.maxC)
        assertEquals(10, d.rainPercent)
        assertEquals(null, WeatherDetail.of(null))
    }

    @Test fun oldCachedForecastWithoutRainStillParses() {
        val json = """{"current":{"temperature_2m":28.0,"weather_code":0},
            "daily":{"time":["2026-11-23"],"weather_code":[0],"temperature_2m_max":[29.0],"temperature_2m_min":[22.0]}}"""
        assertEquals(null, WeatherParsers.parseWeather(json)!!.days[0].rainPercent)
    }
}
