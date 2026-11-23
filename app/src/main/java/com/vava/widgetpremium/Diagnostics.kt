package com.vava.widgetpremium

import android.content.Context
import android.os.PowerManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** Khoảng thời gian đã trôi qua, tách thành đơn vị và số để giao diện chọn chuỗi theo ngôn ngữ. */
data class Age(val span: Span, val amount: Int) {
    enum class Span { NOW, MINUTES, HOURS, DAYS }

    companion object {
        /** Dưới 1 phút là NOW; làm tròn xuống; một số âm (đồng hồ bị chỉnh) coi như NOW. */
        fun of(diffMs: Long): Age {
            val minutes = diffMs / 60_000L
            return when {
                minutes < 1 -> Age(Span.NOW, 0)
                minutes < 60 -> Age(Span.MINUTES, minutes.toInt())
                minutes < 24 * 60 -> Age(Span.HOURS, (minutes / 60).toInt())
                else -> Age(Span.DAYS, (minutes / (24 * 60)).toInt())
            }
        }
    }
}

/** Ghi lại lần cập nhật widget gần nhất để hiện ở màn hình số liệu; chỉ lưu một mốc thời gian trên máy. */
object Diagnostics {
    private const val PREFS = "diag"

    fun markWidgetUpdate(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong("widget_update", System.currentTimeMillis()).apply()
    }

    fun lastWidgetUpdateMs(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("widget_update", 0L)

    fun ignoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }
}

@Composable
private fun ageText(thenMs: Long): String {
    if (thenMs <= 0L) return stringResource(R.string.diag_never)
    val age = Age.of(System.currentTimeMillis() - thenMs)
    return when (age.span) {
        Age.Span.NOW -> stringResource(R.string.diag_ago_now)
        Age.Span.MINUTES -> stringResource(R.string.diag_ago_min, age.amount)
        Age.Span.HOURS -> stringResource(R.string.diag_ago_hour, age.amount)
        Age.Span.DAYS -> stringResource(R.string.diag_ago_day, age.amount)
    }
}

@Composable
private fun yesNo(value: Boolean): String = stringResource(if (value) R.string.diag_yes else R.string.diag_no)

/** Màn hình số liệu: đọc trực tiếp từ máy mỗi lần bấm làm mới, không gửi đi đâu. */
@Composable
fun DiagnosticsCard() {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    val battery = remember(tick) { DeviceStats.battery(context) }
    val ram = remember(tick) { DeviceStats.ram(context) }
    val storage = remember(tick) { DeviceStats.storage() }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.diag_title), style = MaterialTheme.typography.titleMedium)
        if (battery != null) {
            val state = stringResource(if (battery.charging) R.string.diag_charging else R.string.diag_not_charging)
            Text(stringResource(R.string.diag_battery_fmt, battery.percent, state))
            battery.tempC?.let { Text(stringResource(R.string.diag_temp_fmt, Formats.celsius(it))) }
        }
        ram?.let { Text(stringResource(R.string.diag_ram_fmt, Formats.gb(it.usedBytes), Formats.gb(it.totalBytes))) }
        storage?.let { Text(stringResource(R.string.diag_storage_fmt, Formats.gb(it.usedBytes), Formats.gb(it.totalBytes))) }
        Text(stringResource(R.string.diag_widget_update_fmt, ageText(Diagnostics.lastWidgetUpdateMs(context))))
        Text(stringResource(R.string.diag_weather_fmt, ageText(WeatherStore.fetchedAtMs(context))))

        Text(stringResource(R.string.diag_reminders_title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.diag_notif_fmt, yesNo(ReminderScheduler.hasNotificationPermission(context))))
        Text(stringResource(R.string.diag_exact_fmt, yesNo(ReminderScheduler.canExact(context))))
        Text(stringResource(R.string.diag_batteryopt_fmt, yesNo(Diagnostics.ignoringBatteryOptimizations(context))))
        Text(
            stringResource(R.string.diag_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = { tick++ }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.diag_refresh))
        }
    }
}
