package com.vava.widgetpremium

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay

private fun moonNameRes(p: MoonPhase): Int = when (p) {
    MoonPhase.NEW -> R.string.moon_new
    MoonPhase.WAXING_CRESCENT -> R.string.moon_waxing_crescent
    MoonPhase.FIRST_QUARTER -> R.string.moon_first_quarter
    MoonPhase.WAXING_GIBBOUS -> R.string.moon_waxing_gibbous
    MoonPhase.FULL -> R.string.moon_full
    MoonPhase.WANING_GIBBOUS -> R.string.moon_waning_gibbous
    MoonPhase.LAST_QUARTER -> R.string.moon_last_quarter
    MoonPhase.WANING_CRESCENT -> R.string.moon_waning_crescent
}

/** Bảng tổng quan ở Trang chủ: ngày dương/âm, giờ, thời tiết, thiên văn, dự báo 24 giờ. Chỉ dùng dữ liệu thật. */
@Composable
fun Dashboard() {
    val context = LocalContext.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var weather by remember { mutableStateOf(WeatherStore.cached(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            weather = WeatherStore.cached(context)
            delay(60_000L - System.currentTimeMillis() % 60_000L + 200L)
        }
    }
    val today = now.toLocalDate()
    val lunar = remember(today) { VietCalendar.lunarOf(today) }
    val city = WeatherStore.city(context)
    val w = weather
    val cityIso = w?.let { Astro.cityLocalIso(System.currentTimeMillis(), it.utcOffsetSec) }
    val todayForecast = if (w != null && cityIso != null) w.days.firstOrNull { cityIso.startsWith(it.date) } else null
    val sunrise = Astro.clockOf(todayForecast?.sunrise)
    val sunset = Astro.clockOf(todayForecast?.sunset)
    val dayLength = Astro.dayLengthMinutes(todayForecast?.sunrise, todayForecast?.sunset)
    val moon = Astro.moonPhase(lunar.day)
    val weekday = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase(Locale.getDefault())
    val isDay = WidgetUpdater.isDay(context)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$weekday · ${today.dayOfMonth}/${today.monthValue}/${today.year}",
                        Modifier.weight(1f), style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        stringResource(R.string.dash_today),
                        Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge
                    )
                }
                Text(
                    stringResource(R.string.dash_date_big, today.dayOfMonth, today.monthValue),
                    fontSize = 34.sp, fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.dash_lunar_big, lunar.day, lunar.month),
                    style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(
                        R.string.dash_year_month_day,
                        LunarCalendar.yearName(lunar.year), VietCalendar.lunarMonthName(lunar), VietCalendar.dayName(today)
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.dash_time), style = MaterialTheme.typography.labelMedium)
                        Text("%02d:%02d".format(now.hour, now.minute), fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.dash_weather), style = MaterialTheme.typography.labelMedium)
                        if (w != null && city != null) {
                            val condition = Condition.fromWmo(w.code)
                            Text(
                                condition.emoji(isDay) + " " + Math.round(w.tempC) + "°C",
                                fontSize = 32.sp, fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(condition.labelRes()) + " · " + city.name,
                                style = MaterialTheme.typography.bodySmall
                            )
                            w.feelsC?.let {
                                Text(
                                    stringResource(R.string.dash_feels_like, Math.round(it).toInt()),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        } else {
                            Text(stringResource(R.string.dash_weather_none), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.dash_astro_title), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AstroTile("🌅", stringResource(R.string.dash_sunrise), sunrise ?: "–", Modifier.weight(1f))
                    AstroTile("🌇", stringResource(R.string.dash_sunset), sunset ?: "–", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AstroTile(moon.emoji, stringResource(R.string.dash_moon), stringResource(moonNameRes(moon)), Modifier.weight(1f))
                    AstroTile(
                        "⏳", stringResource(R.string.dash_day_length),
                        dayLength?.let { stringResource(R.string.dash_day_length_fmt, it / 60, it % 60) } ?: "–",
                        Modifier.weight(1f)
                    )
                }
                Text(
                    stringResource(R.string.dash_moon_note),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.dash_hourly_title), style = MaterialTheme.typography.labelLarge)
                val hours = if (w != null && cityIso != null) Astro.nextHours(w.hourly, cityIso) else emptyList()
                if (hours.isEmpty()) {
                    Text(
                        stringResource(R.string.dash_hourly_empty),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        hours.forEach { h ->
                            Column(Modifier.width(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(h.time.substring(11, 13) + "h", style = MaterialTheme.typography.labelMedium)
                                Text(Condition.fromWmo(h.code).emoji(Astro.isDayAt(h.time, w!!.days)), fontSize = 26.sp)
                                Text(Math.round(h.tempC).toString() + "°", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AstroTile(emoji: String, label: String, value: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text("$emoji $label", style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
