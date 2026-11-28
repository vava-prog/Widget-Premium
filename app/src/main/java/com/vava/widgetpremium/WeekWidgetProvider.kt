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
        /** Dưới độ cao này (dp) widget dùng cỡ chữ gọn hơn để khối thời tiết và dải giờ không bị cắt. */
        private const val TIGHT_HEIGHT_DP = 168

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
        private val DIVIDER = intArrayOf(R.id.wk_div0, R.id.wk_div1, R.id.wk_div2, R.id.wk_div3, R.id.wk_div4, R.id.wk_div5)
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
            val views = RemoteViews(
                context.packageName, WidgetLayouts.week(StyleStore.load(context).fontFamily, StyleStore.load(context).fontWeight)
            )
            WidgetBackground.apply(context, views, R.id.week_bg, w, h)

            val opts = StyleStore.load(context)
            // Widget thấp (dưới 168 dp) thu nhỏ chữ của khối thời tiết và dải giờ để dải dưới không bị cắt.
            val tight = h < TIGHT_HEIGHT_DP * opts.fontScalePercent / 100
            val cols = WidgetFont.columns(w)
            val sp = android.util.TypedValue.COMPLEX_UNIT_SP
            WidgetBackground.setShown(views, R.id.week_date, opts.showSolarDate)
            WidgetBackground.setShown(views, R.id.week_lunar_row, opts.showLunar)
            WidgetBackground.setShown(views, R.id.week_weather_box, opts.showWeather)
            val today = LocalDate.now()
            val cachedWeather = WeatherStore.cached(context)
            val forecast = cachedWeather?.days.orEmpty()
            val hourCells = if (opts.weekMode == WeekMode.HOURLY) WeekStrip.hourly(cachedWeather, System.currentTimeMillis()) else emptyList()
            val l = VietCalendar.lunarOf(today)
            val lunarRes = if (l.isLeap) R.string.week_lunar_leap_fmt else R.string.week_lunar_fmt
            views.setTextViewText(R.id.week_lunar, context.getString(lunarRes, l.day, l.month))

            // Widget hẹp chỉ hiện 5 cột; dải ngày tuần chọn cửa sổ 5 ngày có chứa hôm nay.
            val allDays = VietCalendar.weekDays(today)
            val start = if (cols >= 7) 0 else minOf(allDays.indexOf(today).coerceAtLeast(0), 7 - cols)
            for (c in 0 until 7) {
                views.setViewVisibility(CELL[c], if (c < cols) android.view.View.VISIBLE else android.view.View.GONE)
                if (c > 0) views.setViewVisibility(DIVIDER[c - 1], if (c < cols) android.view.View.VISIBLE else android.view.View.GONE)
            }
            allDays.drop(start).take(cols).forEachIndexed { i, date ->
                if (hourCells.isNotEmpty()) {
                    // Dự báo theo giờ: giờ ở trên, biểu tượng ở giữa, nhiệt độ ở dưới; ô đầu là giờ hiện tại.
                    val cell = hourCells.getOrNull(i)
                    views.setTextViewText(NAME[i], cell?.label.orEmpty())
                    WidgetFont.size(context, views, NAME[i], if (tight) 10f else 11f)
                    views.setTextViewText(DAY[i], cell?.icon.orEmpty())
                    WidgetFont.size(context, views, DAY[i], if (tight) 15f else 19f)
                    views.setTextViewText(LUNAR[i], cell?.temp.orEmpty())
                    WidgetFont.size(context, views, LUNAR[i], if (tight) 11f else 12f)
                    views.setInt(CELL[i], "setBackgroundResource", if (i == 0) TODAY_BG[opts.todayColorIndex.coerceIn(0, TODAY_BG.size - 1)] else 0)
                    WidgetTap.bind(context, views, CELL[i], TapTarget.HOME)
                    return@forEachIndexed
                }
                views.setTextViewText(NAME[i], context.getString(NAME_RES[start + i]))
                WidgetFont.size(context, views, NAME[i], 11f)
                if (opts.weekMode == WeekMode.WEATHER) {
                    // Như app mẫu: biểu tượng thời tiết thay cho số ngày, bên dưới là "thấp° cao°".
                    val icon = WeekStrip.icon(date, forecast)
                    views.setTextViewText(DAY[i], icon.ifEmpty { date.dayOfMonth.toString() })
                    WidgetFont.size(context, views, DAY[i], if (icon.isEmpty()) 16f else 15f)
                    views.setTextViewText(LUNAR[i], WeekStrip.range(date, forecast))
                    WidgetFont.size(context, views, LUNAR[i], 9f)
                } else {
                    views.setTextViewText(DAY[i], date.dayOfMonth.toString())
                    WidgetFont.size(context, views, DAY[i], 16f)
                    views.setTextViewText(LUNAR[i], VietCalendar.weekLunarLabel(date))
                    WidgetFont.size(context, views, LUNAR[i], 10f)
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

            WidgetFont.size(context, views, R.id.week_temp, if (tight) 26f else 32f)
            WidgetFont.size(context, views, R.id.week_icon, if (tight) 22f else 28f)
            WidgetFont.size(context, views, R.id.week_cond, if (tight) 13f else 15f)
            WidgetFont.size(context, views, R.id.week_range, if (tight) 10f else 12f)
            WidgetFont.size(context, views, R.id.week_city, if (tight) 11f else 13f)
            WidgetFont.size(context, views, R.id.week_date, if (tight) 12f else 14f)
            WidgetFont.size(context, views, R.id.week_lunar, if (tight) 11f else 13f)

            WidgetBackground.applyText(
                context, views,
                listOf(R.id.week_time, R.id.week_temp) + DAY.toList(),
                listOf(R.id.week_sec, R.id.week_date, R.id.week_lunar, R.id.week_city, R.id.week_cond, R.id.week_range) + NAME.toList() + LUNAR.toList()
            )
            WidgetBackground.applyIconTint(context, views, listOf(R.id.week_lunar_icon))
            // Khối giờ chiếm tối đa nửa chiều rộng; cỡ chữ lớn thì tự co để không đè sang khối thời tiết.
            WidgetBackground.applyClockSize(context, views, R.id.week_time, if (tight) 34f else 40f, splitSeconds = true, availDp = w * 0.5f - 12f)
            WidgetBackground.applySeconds(context, views, R.id.week_sec, if (tight) 40f else 48f)
            WidgetTap.bind(context, views, R.id.week_time, TapTarget.ALARMS)
            WidgetTap.bind(context, views, R.id.week_date, TapTarget.CALENDAR)
            WidgetTap.bind(context, views, R.id.week_lunar_row, TapTarget.CALENDAR)
            WidgetTap.bind(context, views, R.id.week_temp, TapTarget.HOME)
            WidgetTap.bind(context, views, R.id.week_cond, TapTarget.HOME)
            return views
        }
    }
}
