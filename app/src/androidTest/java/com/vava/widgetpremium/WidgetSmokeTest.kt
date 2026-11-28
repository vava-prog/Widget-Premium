package com.vava.widgetpremium

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicReference
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Chạy trên máy ảo Android thật: dựng RemoteViews của cả ba widget cho nhiều kiểu thời tiết, ngày/đêm, giao diện,
 * màu chữ và kích thước, rồi `apply` vào một khung, đo, xếp và vẽ ra bitmap như launcher làm.
 * Bắt các lỗi chỉ xảy ra khi chạy thật (RemoteViews không hỗ trợ phương thức, tài nguyên lỗi, vẽ Canvas lỗi...).
 */
@RunWith(AndroidJUnit4::class)
class WidgetSmokeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    private fun forecast(code: Int): String {
        val now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0)
        val hourFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        val hours = (-1..46).map { now.plusHours(it.toLong()) }
        val days = (0..6).map { now.toLocalDate().plusDays(it.toLong()) }
        fun list(items: List<Any>) = JSONArray(items)
        return JSONObject()
            .put("utc_offset_seconds", 25200)
            .put(
                "current",
                JSONObject().put("temperature_2m", 23.0).put("relative_humidity_2m", 90)
                    .put("weather_code", code).put("apparent_temperature", 24.5)
            )
            .put(
                "daily",
                JSONObject()
                    .put("time", list(days.map { it.toString() }))
                    .put("weather_code", list(days.map { code }))
                    .put("temperature_2m_max", list(days.map { 24.0 }))
                    .put("temperature_2m_min", list(days.map { 22.0 }))
                    .put("sunrise", list(days.map { "${it}T06:00" }))
                    .put("sunset", list(days.map { "${it}T18:00" }))
                    .put("precipitation_probability_max", list(days.map { 80 }))
            )
            .put(
                "hourly",
                JSONObject()
                    .put("time", list(hours.map { it.format(hourFormat) }))
                    .put("temperature_2m", list(hours.map { 23.0 }))
                    .put("weather_code", list(hours.map { code }))
            )
            .toString()
    }

    private fun seed(code: Int, style: WidgetStyle) {
        WeatherStore.saveCity(context, City("Đà Nẵng", "", "Việt Nam", 16.0678, 108.2208))
        WeatherStore.saveForecast(context, forecast(code), System.currentTimeMillis())
        StyleStore.save(context, style)
    }

    /** Dựng, apply, đo, xếp và vẽ một widget; trả về mô tả lỗi (kèm vài dòng stack) hoặc null nếu ổn. */
    private fun exercise(label: String, wDp: Int, hDp: Int, build: () -> RemoteViews): String? {
        val error = AtomicReference<Throwable?>()
        instrumentation.runOnMainSync {
            try {
                val views = build()
                val frame = FrameLayout(context)
                val density = context.resources.displayMetrics.density
                val wPx = (wDp * density).toInt()
                val hPx = (hDp * density).toInt()
                frame.addView(views.apply(context, frame))
                frame.measure(
                    View.MeasureSpec.makeMeasureSpec(wPx, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(hPx, View.MeasureSpec.EXACTLY)
                )
                frame.layout(0, 0, wPx, hPx)
                val bitmap = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
                frame.draw(Canvas(bitmap))
                bitmap.recycle()
            } catch (t: Throwable) {
                error.set(t)
            }
        }
        val t = error.get() ?: return null
        return "$label -> ${t.javaClass.name}: ${t.message}\n" + t.stackTraceToString().lines().drop(1).take(12).joinToString("\n")
    }

    @Test
    fun allWidgetsBuildApplyAndDrawForEveryWeatherThemeAndSize() {
        val base = WidgetStyle()
        val styles = listOf(
            "mặc định" to base,
            "tắt chuyển động" to base.copy(weatherMotion = false),
            "giây+nhãn+mờ40+vuông+ẩn vị trí" to base.copy(
                showSeconds = true, label = "Nhãn", opacityPercent = 40, cornerDp = 0, showLocation = false, showWeatherDetail = true
            ),
            "chữ tuỳ chọn" to base.copy(textTone = TextTone.CUSTOM, customTextColor = 0xFFFFD54F.toInt()),
            "chữ sáng" to base.copy(textTone = TextTone.LIGHT, glassFrostPercent = 60)
        )
        val failures = ArrayList<String>()
        var runs = 0

        fun all(tag: String, style: WidgetStyle, week: WeekMode) {
            val s = style.copy(weekMode = week)
            StyleStore.save(context, s)
            for ((w, h) in listOf(250 to 110, 340 to 380)) {
                runs++
                exercise("$tag clock ${w}x$h", w, h) { ClockWidgetProvider.buildViews(context, w, h) }?.let { failures += it }
            }
            for ((w, h) in listOf(250 to 200, 340 to 260)) {
                runs++
                exercise("$tag info ${w}x$h", w, h) { InfoWidgetProvider.buildViews(context, w, h) }?.let { failures += it }
            }
            for ((w, h) in listOf(250 to 130, 340 to 176, 340 to 200)) {
                runs++
                exercise("$tag week/$week ${w}x$h", w, h) { WeekWidgetProvider.buildViews(context, w, h) }?.let { failures += it }
            }
        }

        for (code in listOf(0, 3, 45, 61, 71, 95)) {
            for (mode in listOf(DayMode.DAY, DayMode.NIGHT)) {
                for ((name, style) in styles) {
                    seed(code, style.copy(theme = WidgetTheme.WEATHER, mode = mode))
                    for (week in WeekMode.values()) all("WEATHER code=$code $mode [$name]", style.copy(theme = WidgetTheme.WEATHER, mode = mode), week)
                }
            }
        }
        for (theme in listOf(WidgetTheme.STANDARD, WidgetTheme.GLASS)) {
            for ((name, style) in styles) {
                seed(61, style.copy(theme = theme))
                for (week in WeekMode.values()) all("$theme [$name]", style.copy(theme = theme), week)
            }
        }

        // Gom lỗi giống nhau (cùng dòng đầu) để log ngắn: in số lần và một ví dụ đầy đủ.
        val grouped = failures.groupBy { it.lines().first().substringAfter("-> ") }
        val report = grouped.entries.joinToString("\n\n") { (key, list) ->
            "[${list.size} lần] $key\nVí dụ: ${list.first()}\nCác ca lỗi: " + list.take(6).joinToString(" | ") { it.substringBefore(" ->") }
        }
        println("WidgetSmokeTest: $runs lần dựng, ${failures.size} lỗi, ${grouped.size} loại lỗi")
        assertTrue("\n$runs lần dựng, ${failures.size} lỗi:\n$report", failures.isEmpty())
    }
}
