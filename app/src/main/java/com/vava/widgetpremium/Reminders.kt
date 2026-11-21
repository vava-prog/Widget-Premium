package com.vava.widgetpremium

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.app.Notification
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Đặt và hủy báo thức nhắc. Dùng báo thức chính xác khi hệ thống cho phép;
 * nếu Android không cấp quyền báo thức chính xác thì dùng báo thức gần đúng (có thể lệch vài phút).
 */
object ReminderScheduler {
    const val ACTION = "com.vava.widgetpremium.NOTE_REMINDER"
    const val EXTRA_ID = "note_id"
    const val EXTRA_TEXT = "note_text"

    private fun intentFor(context: Context, id: Long): Intent =
        Intent(context, ReminderReceiver::class.java).setAction(ACTION).putExtra(EXTRA_ID, id)

    fun schedule(context: Context, note: Note) {
        val at = note.remindAtMs ?: return
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = PendingIntent.getBroadcast(
            context, note.id.toInt(),
            intentFor(context, note.id).putExtra(EXTRA_TEXT, note.text),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        try {
            if (exact) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        } catch (e: SecurityException) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }

    fun cancel(context: Context, id: Long) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = PendingIntent.getBroadcast(
            context, id.toInt(), intentFor(context, id),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarms.cancel(pending)
        pending.cancel()
    }

    fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

object ReminderNotifier {
    private const val CHANNEL = "note_reminders"

    fun show(context: Context, id: Long, text: String) {
        if (!ReminderScheduler.hasNotificationPermission(context)) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.reminder_channel_name), NotificationManager.IMPORTANCE_HIGH)
                .apply { description = context.getString(R.string.reminder_channel_desc) }
        )
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_TAB, 1)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(id.toInt(), notification)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION) return
        val id = intent.getLongExtra(ReminderScheduler.EXTRA_ID, 0L)
        val text = intent.getStringExtra(ReminderScheduler.EXTRA_TEXT).orEmpty()
        ReminderNotifier.show(context, id, text)
    }
}

/** Đặt lại các báo thức sau khi khởi động máy hoặc cập nhật ứng dụng (Android xóa báo thức ở các thời điểm này). */
class ReminderRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                NoteRepository.rescheduleAll(app)
            } finally {
                pending.finish()
            }
        }
    }
}
