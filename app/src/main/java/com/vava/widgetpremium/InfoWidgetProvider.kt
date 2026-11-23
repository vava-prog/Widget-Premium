package com.vava.widgetpremium

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import java.util.Calendar

/**
 * Widget thông tin: giờ, ngày dương, ngày âm, thời tiết, pin, nhiệt độ pin, RAM, bộ nhớ trong.
 * Làm mới theo lịch của hệ thống (khoảng 30 phút), theo lịch thời tiết (WorkManager) và khi có
 * sự kiện hệ thống cho phép (đổi ngày, đổi giờ, cắm hoặc rút sạc, pin yếu).
 */
class InfoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) update(context, appWidgetManager, id)
        DeviceAlerts.check(context)
        DayNightScheduler.scheduleNext(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle
    ) {
        update(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WEATHER) {
            if (WeatherStore.city(context) != null) {
                WeatherScheduler.refreshNow(context)
                android.widget.Toast.makeText(context, R.string.weather_refreshing, android.widget.Toast.LENGTH_SHORT).show()
            }
            return
        }
        if (intent.action in REFRESH_ACTIONS) {
            updateAll(context)
            DeviceAlerts.check(context)
        }
    }

    companion object {
        /** Hành động riêng: chạm dòng thời tiết trên widget để tải lại ngay. */
        private const val ACTION_REFRESH_WEATHER = "com.vava.widgetpremium.action.REFRESH_WEATHER"

        private val REFRESH_ACTIONS = setOf(
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED,
            Intent.ACTION_BATTERY_LOW,
            Intent.ACTION_BATTERY_OKAY
        )

        /** Quá ngưỡng này thì widget ghi thêm giờ cập nhật của dữ liệu thời tiết. */
        private const val STALE_MS = 90 * 60 * 1000L

        /** Làm mới mọi widget thông tin đang có trên màn hình chính. */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, InfoWidgetProvider::class.java))
            for (id in ids) update(context, manager, id)
        }

        private fun update(context: Context, manager: AppWidgetManager, id: Int) {
            manager.updateAppWidget(id, buildViews(context, manager, id))
            Diagnostics.markWidgetUpdate(context)
        }

        private fun weatherLine(context: Context, isDay: Boolean): String? {
            val city = WeatherStore.city(context) ?: return context.getString(R.string.weather_pick_city)
            val weather = WeatherStore.cached(context) ?: return null
            val condition = Condition.fromWmo(weather.code)
            var text = context.getString(
                R.string.weather_widget_fmt,
                condition.emoji(isDay),
                Math.round(weather.tempC).toString(),
                context.getString(condition.labelRes()),
                city.name
            )
            val fetched = WeatherStore.fetchedAtMs(context)
            if (fetched > 0 && System.currentTimeMillis() - fetched > STALE_MS) {
                text += " · " + context.getString(R.string.weather_updated_fmt, Formats.clockTime(fetched))
            }
            return text
        }

        private fun buildViews(context: Context, manager: AppWidgetManager, id: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_info)

            val now = Calendar.getInstance()
            val lunar = LunarCalendar.fromSolar(
                now.get(Calendar.DAY_OF_MONTH),
                now.get(Calendar.MONTH) + 1,
                now.get(Calendar.YEAR)
            )
            val lunarRes = if (lunar.isLeap) R.string.lunar_fmt_leap else R.string.lunar_fmt
            views.setTextViewText(
                R.id.info_lunar,
                context.getString(lunarRes, lunar.day, lunar.month, LunarCalendar.yearName(lunar.year))
            )

            val day = WidgetUpdater.widgetIsDay(context)
            val (w, h) = WidgetBackground.sizeDp(manager, id, 250, 200)
            WidgetBackground.apply(context, views, R.id.info_bg, w, h)
            WidgetBackground.applyText(
                context, views,
                listOf(R.id.info_time, R.id.stat_battery, R.id.stat_temp, R.id.stat_ram, R.id.stat_storage),
                listOf(R.id.info_sec, R.id.info_date, R.id.info_label, R.id.info_lunar, R.id.info_weather, R.id.info_detail)
            )
            WidgetBackground.applyClockSize(context, views, R.id.info_time, 60f, splitSeconds = true)
            WidgetBackground.applySeconds(context, views, R.id.info_sec, 60f)
            WidgetBackground.applyLabel(context, views, R.id.info_label)

            val opts = StyleStore.load(context)
            WidgetBackground.setShown(views, R.id.info_date, opts.showSolarDate)
            WidgetBackground.setShown(views, R.id.info_lunar, opts.showLunar)
            val weatherText = if (opts.showWeather) weatherLine(context, day) else null
            if (weatherText != null) {
                views.setTextViewText(R.id.info_weather, weatherText)
                views.setViewVisibility(R.id.info_weather, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.info_weather, View.GONE)
            }

            val detail = if (opts.showWeather && opts.showWeatherDetail && WeatherStore.city(context) != null)
                WeatherDetail.of(WeatherStore.cached(context)?.days?.firstOrNull()) else null
            if (detail != null) {
                val range = context.getString(R.string.weather_detail_range_fmt, detail.minC, detail.maxC)
                val text = if (detail.rainPercent != null) {
                    range + " · " + context.getString(R.string.weather_detail_rain_fmt, detail.rainPercent)
                } else range
                views.setTextViewText(R.id.info_detail, text)
            }
            WidgetBackground.setShown(views, R.id.info_detail, detail != null)

            val battery = DeviceStats.battery(context)
            if (battery != null) {
                val text = context.getString(R.string.stat_battery_fmt, battery.percent)
                views.setTextViewText(R.id.stat_battery, if (battery.charging) "$text ⚡" else text)
                views.setViewVisibility(R.id.stat_battery, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.stat_battery, View.GONE)
            }
            val temp = battery?.tempC
            if (temp != null) {
                views.setTextViewText(R.id.stat_temp, context.getString(R.string.stat_temp_fmt, Formats.celsius(temp)))
                views.setViewVisibility(R.id.stat_temp, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.stat_temp, View.GONE)
            }

            val ram = DeviceStats.ram(context)
            if (ram != null) {
                views.setTextViewText(
                    R.id.stat_ram,
                    context.getString(R.string.stat_ram_fmt, Formats.gb(ram.usedBytes), Formats.gb(ram.totalBytes))
                )
                views.setViewVisibility(R.id.stat_ram, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.stat_ram, View.GONE)
            }

            val storage = DeviceStats.storage()
            if (storage != null) {
                views.setTextViewText(
                    R.id.stat_storage,
                    context.getString(R.string.stat_storage_fmt, Formats.gb(storage.usedBytes), Formats.gb(storage.totalBytes))
                )
                views.setViewVisibility(R.id.stat_storage, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.stat_storage, View.GONE)
            }

            val open = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pending = PendingIntent.getActivity(
                context, 1, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.info_root, pending)
            WidgetTap.bind(context, views, R.id.info_time, TapTarget.ALARMS)
            val refresh = Intent(context, InfoWidgetProvider::class.java).setAction(ACTION_REFRESH_WEATHER)
            views.setOnClickPendingIntent(
                R.id.info_weather,
                PendingIntent.getBroadcast(context, 20, refresh, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )
            WidgetTap.bind(context, views, R.id.info_date, TapTarget.CALENDAR)
            WidgetTap.bind(context, views, R.id.info_lunar, TapTarget.CALENDAR)
            WidgetTap.bind(context, views, R.id.stat_battery, TapTarget.BATTERY)
            WidgetTap.bind(context, views, R.id.stat_temp, TapTarget.BATTERY)
            WidgetTap.bind(context, views, R.id.stat_ram, TapTarget.MEMORY)
            WidgetTap.bind(context, views, R.id.stat_storage, TapTarget.STORAGE)
            return views
        }
    }
}
