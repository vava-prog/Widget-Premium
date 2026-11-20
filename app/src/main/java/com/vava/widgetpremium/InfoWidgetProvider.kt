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
 * Widget thông tin: giờ, ngày dương, ngày âm, pin, nhiệt độ pin, RAM, bộ nhớ trong.
 * Làm mới theo lịch của hệ thống (khoảng 30 phút) và khi có sự kiện hệ thống cho phép
 * (đổi ngày, đổi giờ, cắm hoặc rút sạc, pin yếu).
 */
class InfoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildViews(context))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in REFRESH_ACTIONS) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, InfoWidgetProvider::class.java))
            if (ids.isNotEmpty()) onUpdate(context, manager, ids)
        }
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

    private companion object {
        val REFRESH_ACTIONS = setOf(
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED,
            Intent.ACTION_BATTERY_LOW,
            Intent.ACTION_BATTERY_OKAY
        )
    }
}
