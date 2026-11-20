package com.vava.widgetpremium

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs

data class BatteryInfo(val percent: Int, val charging: Boolean, val tempC: Float?)
data class UsageInfo(val usedBytes: Long, val totalBytes: Long)

/** Đọc thông số máy. Mục nào máy không cung cấp thì trả null để widget ẩn đi, không hiện số giả. */
object DeviceStats {
    fun battery(context: Context): BatteryInfo? {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return null
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val rawTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val temp = if (rawTemp > 0) rawTemp / 10f else null
        return BatteryInfo(level * 100 / scale, charging, temp)
    }

    fun ram(context: Context): UsageInfo? {
        val manager = context.getSystemService(ActivityManager::class.java) ?: return null
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        if (info.totalMem <= 0) return null
        return UsageInfo(info.totalMem - info.availMem, info.totalMem)
    }

    fun storage(): UsageInfo? = try {
        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.totalBytes
        if (total <= 0) null else UsageInfo(total - stat.availableBytes, total)
    } catch (e: IllegalArgumentException) {
        null
    }
}
