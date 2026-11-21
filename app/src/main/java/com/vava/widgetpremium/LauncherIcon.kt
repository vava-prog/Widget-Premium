package com.vava.widgetpremium

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Đổi biểu tượng ứng dụng giữa bản ban ngày và ban đêm bằng hai activity-alias (LauncherDay, LauncherNight).
 * Một số launcher có thể làm icon nhấp nháy hoặc đổi vị trí khi đổi, nên có công tắc tắt trong ứng dụng.
 */
object LauncherIcon {
    private const val PREFS = "settings"
    private const val KEY_ENABLED = "icon_day_night"
    private const val KEY_APPLIED = "icon_applied"

    fun wantNight(isDay: Boolean, featureEnabled: Boolean): Boolean = featureEnabled && !isDay

    fun isFeatureEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true)

    fun setFeatureEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
        apply(context, WidgetUpdater.isDay(context))
    }

    /** Chỉ gọi hệ thống khi trạng thái cần đổi, để tránh làm launcher vẽ lại không cần thiết. */
    fun apply(context: Context, isDay: Boolean) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val night = wantNight(isDay, isFeatureEnabled(context))
        val desired = if (night) "night" else "day"
        if (prefs.getString(KEY_APPLIED, "day") == desired) return
        val pm = context.packageManager
        val day = ComponentName(context, "com.vava.widgetpremium.LauncherDay")
        val nightComponent = ComponentName(context, "com.vava.widgetpremium.LauncherNight")
        val on = if (night) nightComponent else day
        val off = if (night) day else nightComponent
        pm.setComponentEnabledSetting(on, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        pm.setComponentEnabledSetting(off, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        prefs.edit().putString(KEY_APPLIED, desired).apply()
    }
}
