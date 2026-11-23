package com.vava.widgetpremium

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/** Quy tắc cảnh báo thuần Kotlin (có test): ngưỡng và thời gian nghỉ giữa hai lần báo. */
object AlertRules {
    const val LOW_PERCENT = 15
    const val HOT_CELSIUS = 42f
    const val LOW_COOLDOWN_MS = 3 * 60 * 60 * 1000L
    const val HOT_COOLDOWN_MS = 2 * 60 * 60 * 1000L

    fun shouldWarnLow(percent: Int, charging: Boolean, lastMs: Long, nowMs: Long): Boolean =
        !charging && percent <= LOW_PERCENT && nowMs - lastMs >= LOW_COOLDOWN_MS

    fun shouldWarnHot(tempC: Float?, lastMs: Long, nowMs: Long): Boolean =
        tempC != null && tempC >= HOT_CELSIUS && nowMs - lastMs >= HOT_COOLDOWN_MS
}

/**
 * Cảnh báo pin yếu và máy nóng, mặc định tắt. Không có dịch vụ nền: ứng dụng chỉ kiểm tra khi widget thông tin
 * cập nhật (tín hiệu pin yếu của hệ thống, cắm hoặc rút sạc, và định kỳ khoảng 30 phút), nên cảnh báo nóng có thể trễ.
 */
object DeviceAlerts {
    private const val PREFS = "alerts"
    private const val CHANNEL = "device_alerts"
    private const val ID_LOW = 9001
    private const val ID_HOT = 9002

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("enabled", false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("enabled", enabled).apply()
    }

    fun check(context: Context, nowMs: Long = System.currentTimeMillis()) {
        if (!isEnabled(context) || !ReminderScheduler.hasNotificationPermission(context)) return
        val battery = DeviceStats.battery(context) ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (AlertRules.shouldWarnLow(battery.percent, battery.charging, prefs.getLong("last_low", 0L), nowMs)) {
            prefs.edit().putLong("last_low", nowMs).apply()
            notify(context, ID_LOW, context.getString(R.string.alert_low_title), context.getString(R.string.alert_low_text, battery.percent))
        }
        if (AlertRules.shouldWarnHot(battery.tempC, prefs.getLong("last_hot", 0L), nowMs)) {
            prefs.edit().putLong("last_hot", nowMs).apply()
            notify(context, ID_HOT, context.getString(R.string.alert_hot_title), context.getString(R.string.alert_hot_text, battery.tempC ?: 0f))
        }
    }

    private fun notify(context: Context, id: Int, title: String, text: String) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.alert_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = context.getString(R.string.alert_channel_desc) }
        )
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        manager.notify(
            id,
            Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
        )
    }
}
