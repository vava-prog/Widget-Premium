package com.vava.widgetpremium

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

object WidgetUpdater {
    /** Giờ mọc/lặn đang dùng: của thành phố đã chọn nếu có dữ liệu, không thì 6:00 đến 18:00. */
    fun currentSun(context: Context, nowMs: Long): SunTimes {
        val weather = WeatherStore.cached(context)
        return (weather?.let { DayNight.sunTimesFor(it, nowMs) }) ?: DayNight.fallback(nowMs)
    }

    fun isDay(context: Context, nowMs: Long = System.currentTimeMillis()): Boolean =
        DayNight.isDay(nowMs, currentSun(context, nowMs))

    /** Sáng hay tối của widget: theo mặt trời hoặc ép cố định theo lựa chọn của người dùng. */
    fun widgetIsDay(context: Context): Boolean =
        WidgetPalette.resolveDay(StyleStore.load(context).mode, isDay(context))

    /** Vẽ lại mọi widget (không đụng tới biểu tượng ứng dụng). */
    fun updateWidgets(context: Context) {
        ClockWidgetProvider.updateAll(context)
        InfoWidgetProvider.updateAll(context)
    }

    fun updateAll(context: Context) {
        updateWidgets(context)
        LauncherIcon.apply(context, isDay(context))
    }
}

/** Đặt lịch đổi giao diện ngày/đêm đúng lúc mặt trời mọc hoặc lặn. */
object DayNightScheduler {
    private const val UNIQUE_NAME = "daynight_next"

    fun scheduleNext(context: Context) {
        val now = System.currentTimeMillis()
        val next = DayNight.nextTransitionMs(now, WidgetUpdater.currentSun(context, now))
        val delayMs = maxOf(next - now, 60_000L) + 5_000L
        val request = OneTimeWorkRequestBuilder<DayNightWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.REPLACE, request)
    }
}

class DayNightWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        WidgetUpdater.updateAll(applicationContext)
        DayNightScheduler.scheduleNext(applicationContext)
        return Result.success()
    }
}
