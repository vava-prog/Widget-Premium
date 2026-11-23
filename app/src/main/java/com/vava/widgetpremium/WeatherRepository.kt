package com.vava.widgetpremium

import android.content.Context
import java.io.IOException
import java.util.concurrent.TimeUnit
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException

object WeatherRepository {
    /**
     * Tải dự báo mới cho thành phố đã chọn rồi làm mới widget.
     * Trả true nếu thành công hoặc chưa chọn thành phố (không có gì phải làm), false nếu lỗi mạng hoặc dữ liệu hỏng.
     */
    suspend fun refresh(context: Context): Boolean {
        val city = WeatherStore.city(context) ?: return true
        val ok = withContext(Dispatchers.IO) {
            try {
                val json = WeatherApi.fetchForecastJson(city)
                if (WeatherParsers.parseWeather(json) == null) {
                    false
                } else {
                    WeatherStore.saveForecast(context, json, System.currentTimeMillis())
                    true
                }
            } catch (e: IOException) {
                false
            } catch (e: JSONException) {
                false
            }
        }
        WidgetUpdater.updateAll(context)
        DayNightScheduler.scheduleNext(context)
        return ok
    }
}

class WeatherWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result =
        if (WeatherRepository.refresh(applicationContext)) Result.success() else Result.retry()
}

object WeatherScheduler {
    private const val UNIQUE_NAME = "weather_periodic"

    /** Tải lại ngay một lần (khi người dùng chạm widget); cần mạng, chạy khi có mạng. */
    fun refreshNow(context: Context) {
        val request = androidx.work.OneTimeWorkRequestBuilder<WeatherWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        androidx.work.WorkManager.getInstance(context)
            .enqueueUniqueWork("weather_now", androidx.work.ExistingWorkPolicy.REPLACE, request)
    }

    /** Làm mới thời tiết mỗi 30 phút khi có mạng. Chỉ gọi khi đã chọn thành phố. */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<WeatherWorker>(30, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        androidx.work.WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
