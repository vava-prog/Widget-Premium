package com.vava.widgetpremium

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * Widget đồng hồ và ngày. Giờ và ngày do chính hệ thống vẽ (TextClock), nên tự nhảy đúng
 * mà không cần dịch vụ nền. Nền đổi giữa ban ngày và ban đêm theo giờ mặt trời mọc/lặn thật.
 * Chạm vào widget để mở ứng dụng.
 */
class ClockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildViews(context))
        }
        DayNightScheduler.scheduleNext(context)
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ClockWidgetProvider::class.java))
            for (id in ids) manager.updateAppWidget(id, buildViews(context))
        }

        private fun buildViews(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_clock)
            val background = if (WidgetUpdater.isDay(context)) R.drawable.widget_bg_day else R.drawable.widget_bg
            views.setInt(R.id.widget_root, "setBackgroundResource", background)
            val open = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pending = PendingIntent.getActivity(
                context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pending)
            return views
        }
    }
}
