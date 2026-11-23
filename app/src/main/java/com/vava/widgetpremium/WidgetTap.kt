package com.vava.widgetpremium

import android.app.Activity
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings

/** Các vùng chạm trên widget. Mỗi vùng mở một màn hình hệ thống hoặc một tab trong ứng dụng. */
enum class TapTarget {
    HOME, CALENDAR, ALARMS, BATTERY, MEMORY, STORAGE;

    companion object {
        fun parse(name: String?): TapTarget = values().firstOrNull { it.name == name } ?: HOME
    }
}

object WidgetTap {
    const val EXTRA_TARGET = "tap_target"

    /**
     * Danh sách hành động hệ thống thử lần lượt (nhiều hãng máy bỏ một số màn hình).
     * Rỗng nghĩa là mở ứng dụng. Cuối cùng luôn quay về Cài đặt chung rồi tới ứng dụng.
     */
    fun actionsFor(target: TapTarget): List<String> = when (target) {
        TapTarget.ALARMS -> listOf("android.intent.action.SHOW_ALARMS")
        TapTarget.BATTERY -> listOf("android.intent.action.POWER_USAGE_SUMMARY", "android.settings.BATTERY_SAVER_SETTINGS")
        TapTarget.MEMORY -> listOf("android.settings.DEVICE_INFO_SETTINGS")
        TapTarget.STORAGE -> listOf("android.settings.INTERNAL_STORAGE_SETTINGS", "android.settings.MEMORY_CARD_SETTINGS")
        TapTarget.HOME, TapTarget.CALENDAR -> emptyList()
    }

    /** Gắn thao tác chạm cho một view; người dùng tắt "chạm để mở" thì gỡ thao tác. */
    fun bind(context: Context, views: android.widget.RemoteViews, viewId: Int, target: TapTarget) {
        views.setOnClickPendingIntent(viewId, if (StyleStore.load(context).tapEnabled) pending(context, target) else null)
    }

    fun pending(context: Context, target: TapTarget): PendingIntent {
        val intent = Intent(context, TapActivity::class.java)
            .putExtra(EXTRA_TARGET, target.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
        return PendingIntent.getActivity(
            context, 100 + target.ordinal, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

/**
 * Cầu nối không giao diện: widget chỉ gắn được một Intent cố định, nên activity này thử từng màn hình
 * hệ thống và quay về phương án dự phòng nếu máy không có màn hình đó.
 */
class TapActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = TapTarget.parse(intent?.getStringExtra(WidgetTap.EXTRA_TARGET))
        val opened = WidgetTap.actionsFor(target).any { tryStart(Intent(it)) }
        if (!opened) {
            if (target == TapTarget.HOME || target == TapTarget.CALENDAR) {
                val tab = if (target == TapTarget.CALENDAR) 1 else 0
                startActivity(
                    Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_TAB, tab)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )
            } else if (!tryStart(Intent(Settings.ACTION_SETTINGS))) {
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        finish()
    }

    private fun tryStart(intent: Intent): Boolean = try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }
}
