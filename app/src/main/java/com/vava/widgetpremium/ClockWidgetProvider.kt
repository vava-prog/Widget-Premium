package com.vava.widgetpremium

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews

/**
 * Widget đồng hồ và ngày. Giờ và ngày do chính hệ thống vẽ (TextClock), nên tự nhảy đúng
 * mà không cần dịch vụ nền. Nền theo giao diện người dùng chọn (Standard hoặc Kính mờ) và
 * đổi giữa ban ngày và ban đêm theo giờ mặt trời mọc/lặn thật. Chạm vào widget để mở ứng dụng.
 */
class ClockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) update(context, appWidgetManager, id)
        DayNightScheduler.scheduleNext(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle
    ) {
        update(context, appWidgetManager, appWidgetId)
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ClockWidgetProvider::class.java))
            for (id in ids) update(context, manager, id)
        }

        private fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val (w, h) = WidgetBackground.sizeDp(manager, id, 250, 110)
            manager.updateAppWidget(id, buildViews(context, w, h))
        }

        /** Dựng widget theo kích thước (dp) cho trước; dùng chung cho widget thật và khung xem trước trong app. */
        fun buildViews(context: Context, w: Int, h: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_clock)
            WidgetBackground.apply(context, views, R.id.clock_bg, w, h)
            WidgetBackground.applyText(context, views, listOf(R.id.clock_time), listOf(R.id.clock_sec, R.id.clock_date, R.id.clock_label))
            WidgetBackground.applyClockSize(context, views, R.id.clock_time, 56f, splitSeconds = true)
            WidgetBackground.applySeconds(context, views, R.id.clock_sec, 56f)
            WidgetBackground.applyLabel(context, views, R.id.clock_label)
            WidgetBackground.setShown(views, R.id.clock_date, StyleStore.load(context).showSolarDate)
            val open = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pending = PendingIntent.getActivity(
                context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pending)
            WidgetTap.bind(context, views, R.id.clock_time, TapTarget.ALARMS)
            WidgetTap.bind(context, views, R.id.clock_date, TapTarget.CALENDAR)
            return views
        }
    }
}
