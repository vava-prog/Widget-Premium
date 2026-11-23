package com.vava.widgetpremium

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import java.time.LocalDate

/**
 * Widget 4x2 "Lịch tuần": giờ, ngày dương, ngày âm, thời tiết đã lưu và dải bảy ngày của tuần
 * (thứ Hai đến Chủ nhật) kèm ngày âm; hôm nay được tô nền. Giờ do TextClock tự nhảy. Dải tuần
 * cập nhật khi đổi ngày, đổi giờ hoặc múi giờ và theo lịch hệ thống (khoảng 30 phút).
 */
class WeekWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) update(context, appWidgetManager, id)
        DayNightScheduler.scheduleNext(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle
    ) {
        update(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in REFRESH_ACTIONS) updateAll(context)
    }

    companion object {
        private val REFRESH_ACTIONS = setOf(
            Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED
        )
        private val NAME_RES = intArrayOf(
            R.string.cal_week_mon, R.string.cal_week_tue, R.string.cal_week_wed, R.string.cal_week_thu,
            R.string.cal_week_fri, R.string.cal_week_sat, R.string.cal_week_sun
        )
        private val CELL = intArrayOf(R.id.wk_cell0, R.id.wk_cell1, R.id.wk_cell2, R.id.wk_cell3, R.id.wk_cell4, R.id.wk_cell5, R.id.wk_cell6)
        private val NAME = intArrayOf(R.id.wk_name0, R.id.wk_name1, R.id.wk_name2, R.id.wk_name3, R.id.wk_name4, R.id.wk_name5, R.id.wk_name6)
        private val DAY = intArrayOf(R.id.wk_day0, R.id.wk_day1, R.id.wk_day2, R.id.wk_day3, R.id.wk_day4, R.id.wk_day5, R.id.wk_day6)
        private val LUNAR = intArrayOf(R.id.wk_lunar0, R.id.wk_lunar1, R.id.wk_lunar2, R.id.wk_lunar3, R.id.wk_lunar4, R.id.wk_lunar5, R.id.wk_lunar6)

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WeekWidgetProvider::class.java))
            for (id in ids) update(context, manager, id)
        }

        private fun update(context: Context, manager: AppWidgetManager, id: Int) {
            manager.updateAppWidget(id, buildViews(context, manager, id))
        }

        private fun buildViews(context: Context, manager: AppWidgetManager, id: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_week)
            val (w, h) = WidgetBackground.sizeDp(manager, id, 250, 110)
            WidgetBackground.apply(context, views, R.id.week_bg, w, h)

            val today = LocalDate.now()
            val l = VietCalendar.lunarOf(today)
            val lunarRes = if (l.isLeap) R.string.week_lunar_leap_fmt else R.string.week_lunar_fmt
            views.setTextViewText(R.id.week_lunar, context.getString(lunarRes, l.day, l.month))

            VietCalendar.weekDays(today).forEachIndexed { i, date ->
                views.setTextViewText(NAME[i], context.getString(NAME_RES[i]))
                views.setTextViewText(DAY[i], date.dayOfMonth.toString())
                views.setTextViewText(LUNAR[i], VietCalendar.weekLunarLabel(date))
                views.setInt(CELL[i], "setBackgroundResource", if (date == today) R.drawable.week_today_bg else 0)
                views.setOnClickPendingIntent(CELL[i], WidgetTap.pending(context, TapTarget.CALENDAR))
            }

            val city = WeatherStore.city(context)
            val weather = if (city != null) WeatherStore.cached(context) else null
            if (weather != null) {
                val condition = Condition.fromWmo(weather.code)
                val day = WidgetUpdater.widgetIsDay(context)
                views.setTextViewText(R.id.week_temp, Math.round(weather.tempC).toString() + "°")
                views.setTextViewText(R.id.week_cond, condition.emoji(day) + " " + context.getString(condition.labelRes()))
                val today0 = weather.days.firstOrNull()
                views.setTextViewText(
                    R.id.week_range,
                    if (today0 != null) "${Math.round(today0.minC)}°–${Math.round(today0.maxC)}°" else city!!.name
                )
            } else {
                views.setTextViewText(R.id.week_temp, "")
                views.setTextViewText(
                    R.id.week_cond,
                    context.getString(if (city == null) R.string.week_pick_city else R.string.week_no_data)
                )
                views.setTextViewText(R.id.week_range, "")
            }

            WidgetBackground.applyText(
                context, views,
                listOf(R.id.week_time, R.id.week_temp) + DAY.toList(),
                listOf(R.id.week_date, R.id.week_lunar, R.id.week_cond, R.id.week_range) + NAME.toList() + LUNAR.toList()
            )
            WidgetBackground.applyClockSize(context, views, R.id.week_time, 40f)
            views.setOnClickPendingIntent(R.id.week_time, WidgetTap.pending(context, TapTarget.ALARMS))
            views.setOnClickPendingIntent(R.id.week_date, WidgetTap.pending(context, TapTarget.CALENDAR))
            views.setOnClickPendingIntent(R.id.week_lunar, WidgetTap.pending(context, TapTarget.CALENDAR))
            views.setOnClickPendingIntent(R.id.week_temp, WidgetTap.pending(context, TapTarget.HOME))
            views.setOnClickPendingIntent(R.id.week_cond, WidgetTap.pending(context, TapTarget.HOME))
            return views
        }
    }
}
