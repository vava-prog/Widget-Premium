package com.vava.widgetpremium

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/**
 * Logo sống: cùng bố cục với biểu tượng ứng dụng nhưng mọi con số là dữ liệu thật
 * (giờ, ngày dương/âm, thời tiết, pin, RAM, bộ nhớ). Nền đổi theo ban ngày hoặc ban đêm thật.
 */
@Composable
fun LiveLogo(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var battery by remember { mutableStateOf(DeviceStats.battery(context)) }
    var ram by remember { mutableStateOf(DeviceStats.ram(context)) }
    var storage by remember { mutableStateOf(DeviceStats.storage()) }
    var weather by remember { mutableStateOf(WeatherStore.cached(context)) }
    var isDay by remember { mutableStateOf(WidgetUpdater.isDay(context)) }

    // Giờ: cập nhật đúng lúc sang phút mới
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            isDay = WidgetUpdater.isDay(context, now)
            delay(60_000L - now % 60_000L + 50L)
        }
    }
    // Số liệu máy và thời tiết: mỗi 30 giây
    LaunchedEffect(Unit) {
        while (true) {
            battery = DeviceStats.battery(context)
            ram = DeviceStats.ram(context)
            storage = DeviceStats.storage()
            weather = WeatherStore.cached(context)
            delay(30_000L)
        }
    }

    val calendar = Calendar.getInstance().apply { timeInMillis = now }
    val lunar = LunarCalendar.fromSolar(
        calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR)
    )
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)

    val colors = if (isDay) {
        listOf(Color(0xFFFF4FA3), Color(0xFFA23DFF), Color(0xFF2A6BFF))
    } else {
        listOf(Color(0xFF3A1F7A), Color(0xFF1B1F6B), Color(0xFF0E2A66))
    }
    val panelShape = RoundedCornerShape(22.dp)
    val panelColor = Color(0x99140B3F)
    val edge = Brush.linearGradient(listOf(Color(0xFFFFD1EF), Color(0xFFB99CFF), Color(0xFF7FB4FF)))

    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(40.dp))
            .background(Brush.linearGradient(colors))
            .padding(14.dp)
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Đồng hồ
            Box(
                Modifier.fillMaxWidth().weight(1.1f).clip(panelShape).background(panelColor).border(2.dp, edge, panelShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    String.format("%02d:%02d", hour, minute),
                    color = Color.White, fontSize = 58.sp, fontWeight = FontWeight.Bold
                )
            }
            Row(Modifier.fillMaxWidth().weight(1.3f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Lịch âm dương
                Row(
                    Modifier.weight(1f).fillMaxSize().clip(panelShape).background(panelColor).border(2.dp, edge, panelShape),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DateColumn("🌙", stringResource(R.string.logo_lunar), lunar.day.toString(), Modifier.weight(1f))
                    DateColumn("🌒", stringResource(R.string.logo_solar), calendar.get(Calendar.DAY_OF_MONTH).toString(), Modifier.weight(1f))
                }
                // Thời tiết
                val w = weather
                Column(
                    Modifier.weight(1f).fillMaxSize().clip(panelShape)
                        .background(Brush.linearGradient(listOf(Color(0x994A5BFF), Color(0x997A3CFF))))
                        .border(2.dp, edge, panelShape),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (w != null) {
                        Text(Condition.fromWmo(w.code).emoji(isDay), fontSize = 34.sp)
                        Text("${w.tempC.roundToInt()}°C", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text(if (isDay) "☀️" else "🌙", fontSize = 34.sp)
                    }
                }
            }
            // Thông số máy
            Row(
                Modifier.fillMaxWidth().weight(0.9f).clip(panelShape).background(panelColor).border(2.dp, edge, panelShape),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val b = battery
                Stat("🔋", if (b != null) "${b.percent}%" + if (b.charging) "⚡" else "" else "-", Modifier.weight(1f))
                val r = ram
                Stat("RAM", if (r != null) Formats.gb(r.usedBytes) + "/" + Formats.gb(r.totalBytes) else "-", Modifier.weight(1f))
                val s = storage
                Stat("💾", if (s != null) Formats.gb(s.usedBytes) + "/" + Formats.gb(s.totalBytes) else "-", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DateColumn(moon: String, label: String, day: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(moon, fontSize = 22.sp)
        Text(label, color = Color(0xFFFFD877), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(day, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Stat(icon: String, value: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, color = Color(0xFF9AD0FF), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}
