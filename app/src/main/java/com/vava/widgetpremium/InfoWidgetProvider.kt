package com.vava.widgetpremium

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildViews(context))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in REFRESH_ACTIONS) updateAll(context)
    }

    companion object {
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
            for (id in ids) manager.updateAppWidget(id, buildViews(context))
        }

        private fun weatherLine(context: Context): String? {
            val city = WeatherStore.city(context) ?: return context.getString(R.string.weather_pick_city)
            val weather = WeatherStore.cached(context) ?: return null
            val condition = Condition.fromWmo(weather.code)
            var text = context.getString(
                R.string.weather_widget_fmt,
                condition.emoji,
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

        private fun buildViews(context: Context): RemoteViews {
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

            val weatherText = weatherLine(context)
            if (weatherText != null) {
                views.setTextViewText(R.id.info_weather, weatherText)
                views.setViewVisibility(R.id.info_weather, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.info_weather, View.GONE)
            }

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
            return views
        }
    }
}
