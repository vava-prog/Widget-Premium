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
        private val TODAY_BG = intArrayOf(
            R.drawable.week_today_bg, R.drawable.week_today_bg1, R.drawable.week_today_bg2,
            R.drawable.week_today_bg3, R.drawable.week_today_bg4, R.drawable.week_today_bg5
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
            val (w, h) = WidgetBackground.sizeDp(manager, id, 250, 110)
            manager.updateAppWidget(id, buildViews(context, w, h))
        }

        /** Dựng widget theo kích thước (dp) cho trước; dùng chung cho widget thật và khung xem trước trong app. */
        fun buildViews(context: Context, w: Int, h: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_week)
            WidgetBackground.apply(context, views, R.id.week_bg, w, h)

            val opts = StyleStore.load(context)
            WidgetBackground.setShown(views, R.id.week_date, opts.showSolarDate)
            WidgetBackground.setShown(views, R.id.week_lunar, opts.showLunar)
            WidgetBackground.setShown(views, R.id.week_weather_box, opts.showWeather)
            val today = LocalDate.now()
            val cachedWeather = WeatherStore.cached(context)
            val forecast = cachedWeather?.days.orEmpty()
            val hourCells = if (opts.weekMode == WeekMode.HOURLY) WeekStrip.hourly(cachedWeather, System.currentTimeMillis()) else emptyList()
            val l = VietCalendar.lunarOf(today)
            val lunarRes = if (l.isLeap) R.string.week_lunar_leap_fmt else R.string.week_lunar_fmt
            views.setTextViewText(R.id.week_lunar, context.getString(lunarRes, l.day, l.month))

            VietCalendar.weekDays(today).forEachIndexed { i, date ->
                if (hourCells.isNotEmpty()) {
                    // Dự báo theo giờ: giờ ở trên, biểu tượng ở giữa, nhiệt độ ở dưới; ô đầu là giờ hiện tại.
                    val cell = hourCells.getOrNull(i)
                    views.setTextViewText(NAME[i], cell?.label.orEmpty())
                    views.setTextViewTextSize(NAME[i], android.util.TypedValue.COMPLEX_UNIT_SP, 10f)
                    views.setTextViewText(DAY[i], cell?.icon.orEmpty())
                    views.setTextViewTextSize(DAY[i], android.util.TypedValue.COMPLEX_UNIT_SP, 15f)
                    views.setTextViewText(LUNAR[i], cell?.temp.orEmpty())
                    views.setTextViewTextSize(LUNAR[i], android.util.TypedValue.COMPLEX_UNIT_SP, 11f)
                    views.setInt(CELL[i], "setBackgroundResource", if (i == 0) TODAY_BG[opts.todayColorIndex.coerceIn(0, TODAY_BG.size - 1)] else 0)
                    WidgetTap.bind(context, views, CELL[i], TapTarget.HOME)
                    return@forEachIndexed
                }
                views.setTextViewText(NAME[i], context.getString(NAME_RES[i]))
                views.setTextViewTextSize(NAME[i], android.util.TypedValue.COMPLEX_UNIT_SP, 11f)
                if (opts.weekMode == WeekMode.WEATHER) {
                    // Như app mẫu: biểu tượng thời tiết thay cho số ngày, bên dưới là "thấp° cao°".
                    val icon = WeekStrip.icon(date, forecast)
                    views.setTextViewText(DAY[i], icon.ifEmpty { date.dayOfMonth.toString() })
                    views.setTextViewTextSize(DAY[i], android.util.TypedValue.COMPLEX_UNIT_SP, if (icon.isEmpty()) 16f else 15f)
                    views.setTextViewText(LUNAR[i], WeekStrip.range(date, forecast))
                    views.setTextViewTextSize(LUNAR[i], android.util.TypedValue.COMPLEX_UNIT_SP, 9f)
                } else {
                    views.setTextViewText(DAY[i], date.dayOfMonth.toString())
                    views.setTextViewTextSize(DAY[i], android.util.TypedValue.COMPLEX_UNIT_SP, 16f)
                    views.setTextViewText(LUNAR[i], VietCalendar.weekLunarLabel(date))
                    views.setTextViewTextSize(LUNAR[i], android.util.TypedValue.COMPLEX_UNIT_SP, 10f)
                }
                views.setInt(CELL[i], "setBackgroundResource", if (date == today) TODAY_BG[opts.todayColorIndex.coerceIn(0, TODAY_BG.size - 1)] else 0)
                WidgetTap.bind(context, views, CELL[i], TapTarget.CALENDAR)
            }

            val city = WeatherStore.city(context)
            val weather = if (city != null) WeatherStore.cached(context) else null
            views.setTextViewText(R.id.week_city, if (city != null && opts.showLocation) "📍 " + city.name else "")
            if (weather != null) {
                val condition = Condition.fromWmo(weather.code)
                val day = WidgetUpdater.widgetIsDay(context)
                views.setTextViewText(R.id.week_temp, Math.round(weather.tempC).toString() + "°")
                views.setTextViewText(R.id.week_icon, condition.emoji(day))
                views.setTextViewText(R.id.week_cond, context.getString(condition.labelRes()))
                val today0 = weather.days.firstOrNull()
                views.setTextViewText(
                    R.id.week_range,
                    if (today0 != null) "${Math.round(today0.minC)}°–${Math.round(today0.maxC)}°" else ""
                )
            } else {
                views.setTextViewText(R.id.week_temp, "")
                views.setTextViewText(R.id.week_icon, "")
                views.setTextViewText(
                    R.id.week_cond,
                    context.getString(if (city == null) R.string.week_pick_city else R.string.week_no_data)
                )
                views.setTextViewText(R.id.week_range, "")
            }

            WidgetBackground.applyText(
                context, views,
                listOf(R.id.week_time, R.id.week_temp) + DAY.toList(),
                listOf(R.id.week_sec, R.id.week_date, R.id.week_lunar, R.id.week_city, R.id.week_cond, R.id.week_range) + NAME.toList() + LUNAR.toList()
            )
            WidgetBackground.applyClockSize(context, views, R.id.week_time, 40f, splitSeconds = true)
            WidgetBackground.applySeconds(context, views, R.id.week_sec, 48f)
            WidgetTap.bind(context, views, R.id.week_time, TapTarget.ALARMS)
            WidgetTap.bind(context, views, R.id.week_date, TapTarget.CALENDAR)
            WidgetTap.bind(context, views, R.id.week_lunar, TapTarget.CALENDAR)
            WidgetTap.bind(context, views, R.id.week_temp, TapTarget.HOME)
            WidgetTap.bind(context, views, R.id.week_cond, TapTarget.HOME)
            return views
        }
    }
}
