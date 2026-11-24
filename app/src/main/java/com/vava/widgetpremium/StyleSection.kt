package com.vava.widgetpremium

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Các điều khiển giao diện widget. Trạng thái `style` do màn hình cha giữ để khung xem trước
 * có thể được ghim cố định phía trên trong lúc cuộn qua các thanh chỉnh.
 */
@Composable
fun StyleSection(style: WidgetStyle, onChange: (WidgetStyle) -> Unit) {
    val context = LocalContext.current
    var opacity by remember { mutableStateOf(style.opacityPercent.toFloat()) }
    var corner by remember { mutableStateOf(style.cornerDp.toFloat()) }
    var labelText by remember { mutableStateOf(style.label) }
    var frost by remember { mutableStateOf(style.glassFrostPercent.toFloat()) }
    var relief by remember { mutableStateOf(style.glassReliefPercent.toFloat()) }
    var border by remember { mutableStateOf(style.borderDp.toFloat()) }
    var clockScale by remember { mutableStateOf(style.clockScalePercent.toFloat()) }

    fun change(new: WidgetStyle) = onChange(new)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = style.theme == WidgetTheme.STANDARD,
                onClick = { change(StyleStore.withTheme(context, style, WidgetTheme.STANDARD)) },
                label = { Text(stringResource(R.string.style_theme_standard)) }
            )
            FilterChip(
                selected = style.theme == WidgetTheme.GLASS,
                onClick = { change(StyleStore.withTheme(context, style, WidgetTheme.GLASS)) },
                label = { Text(stringResource(R.string.style_theme_glass)) }
            )
            FilterChip(
                selected = style.theme == WidgetTheme.WEATHER,
                onClick = { change(StyleStore.withTheme(context, style, WidgetTheme.WEATHER)) },
                label = { Text(stringResource(R.string.style_theme_weather)) }
            )
        }
        if (style.theme == WidgetTheme.WEATHER) {
            Text(
                stringResource(R.string.style_weather_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(stringResource(R.string.style_accent), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WidgetPalette.ACCENTS.forEachIndexed { index, color ->
                val selected = index == style.accentIndex
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .border(
                            BorderStroke(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline),
                            CircleShape
                        )
                        .clickable { change(style.copy(accentIndex = index)) }
                )
            }
        }
        Text(stringResource(R.string.style_opacity, opacity.toInt()), style = MaterialTheme.typography.labelLarge)
        Slider(
            value = opacity,
            onValueChange = { opacity = it },
            valueRange = WidgetPalette.MIN_OPACITY.toFloat()..100f,
            onValueChangeFinished = { change(style.copy(opacityPercent = opacity.toInt())) }
        )
        Text(stringResource(R.string.style_corner, style.cornerDp), style = MaterialTheme.typography.labelLarge)
        Slider(
            value = corner,
            onValueChange = { corner = it },
            valueRange = 0f..WidgetPalette.MAX_CORNER_DP.toFloat(),
            onValueChangeFinished = { change(style.copy(cornerDp = corner.toInt())) }
        )
        if (style.theme == WidgetTheme.GLASS) {
            Text(stringResource(R.string.style_glass_frost, frost.toInt()), style = MaterialTheme.typography.labelLarge)
            Slider(
                value = frost,
                onValueChange = { frost = it },
                valueRange = 0f..100f,
                onValueChangeFinished = { change(style.copy(glassFrostPercent = frost.toInt())) }
            )
            Text(stringResource(R.string.style_glass_relief, relief.toInt()), style = MaterialTheme.typography.labelLarge)
            Slider(
                value = relief,
                onValueChange = { relief = it },
                valueRange = 0f..100f,
                onValueChangeFinished = { change(style.copy(glassReliefPercent = relief.toInt())) }
            )
            Text(
                stringResource(R.string.style_glass_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            if (border.toInt() == 0) stringResource(R.string.style_border_auto) else stringResource(R.string.style_border, border.toInt()),
            style = MaterialTheme.typography.labelLarge
        )
        Slider(
            value = border,
            onValueChange = { border = it },
            valueRange = 0f..WidgetPalette.MAX_BORDER_DP.toFloat(),
            onValueChangeFinished = { change(style.copy(borderDp = border.toInt())) }
        )
        Text(stringResource(R.string.style_clock_size, clockScale.toInt()), style = MaterialTheme.typography.labelLarge)
        Slider(
            value = clockScale,
            onValueChange = { clockScale = it },
            valueRange = WidgetPalette.MIN_CLOCK_SCALE.toFloat()..WidgetPalette.MAX_CLOCK_SCALE.toFloat(),
            onValueChangeFinished = { change(style.copy(clockScalePercent = clockScale.toInt())) }
        )
        OutlinedTextField(
            value = labelText,
            onValueChange = { labelText = it.take(WidgetPalette.MAX_LABEL_LENGTH) },
            label = { Text(stringResource(R.string.style_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { change(style.copy(label = labelText)); labelText = WidgetPalette.cleanLabel(labelText) }) {
                Text(stringResource(R.string.style_label_apply))
            }
            if (style.label.isNotEmpty()) {
                OutlinedButton(onClick = { labelText = ""; change(style.copy(label = "")) }) {
                    Text(stringResource(R.string.style_label_clear))
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.style_seconds), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            Switch(checked = style.showSeconds, onCheckedChange = { change(style.copy(showSeconds = it)) })
        }
        Text(
            stringResource(R.string.style_seconds_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(stringResource(R.string.style_text), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = style.textTone == TextTone.AUTO, onClick = { change(style.copy(textTone = TextTone.AUTO)) },
                label = { Text(stringResource(R.string.style_text_auto)) })
            FilterChip(selected = style.textTone == TextTone.LIGHT, onClick = { change(style.copy(textTone = TextTone.LIGHT)) },
                label = { Text(stringResource(R.string.style_text_light)) })
            FilterChip(selected = style.textTone == TextTone.DARK, onClick = { change(style.copy(textTone = TextTone.DARK)) },
                label = { Text(stringResource(R.string.style_text_dark)) })
        }
        FilterChip(selected = style.textTone == TextTone.CUSTOM, onClick = { change(style.copy(textTone = TextTone.CUSTOM)) },
            label = { Text(stringResource(R.string.style_text_custom)) })
        if (style.textTone == TextTone.CUSTOM) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WidgetPalette.CUSTOM_TEXT_COLORS.forEach { color ->
                    val selected = (color or (0xFF shl 24)) == style.customTextColor
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(color))
                            .border(
                                BorderStroke(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline),
                                CircleShape
                            )
                            .clickable { change(style.copy(customTextColor = color)) }
                    )
                }
            }
        }
        Text(stringResource(R.string.style_mode), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = style.mode == DayMode.AUTO, onClick = { change(style.copy(mode = DayMode.AUTO)) },
                label = { Text(stringResource(R.string.style_mode_auto)) })
            FilterChip(selected = style.mode == DayMode.DAY, onClick = { change(style.copy(mode = DayMode.DAY)) },
                label = { Text(stringResource(R.string.style_mode_day)) })
            FilterChip(selected = style.mode == DayMode.NIGHT, onClick = { change(style.copy(mode = DayMode.NIGHT)) },
                label = { Text(stringResource(R.string.style_mode_night)) })
        }
        Text(stringResource(R.string.disp_title), style = MaterialTheme.typography.titleMedium)
        @Composable
        fun toggle(labelRes: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(labelRes), modifier = Modifier.weight(1f))
                Switch(checked = checked, onCheckedChange = onChange)
            }
        }
        toggle(R.string.disp_solar, style.showSolarDate) { change(style.copy(showSolarDate = it)) }
        toggle(R.string.disp_lunar, style.showLunar) { change(style.copy(showLunar = it)) }
        toggle(R.string.disp_weather, style.showWeather) { change(style.copy(showWeather = it)) }
        toggle(R.string.disp_location, style.showLocation) { change(style.copy(showLocation = it)) }
        toggle(R.string.disp_weather_detail, style.showWeatherDetail) { change(style.copy(showWeatherDetail = it)) }
        toggle(R.string.disp_tap, style.tapEnabled) { change(style.copy(tapEnabled = it)) }
        Text(stringResource(R.string.disp_hour_format), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = style.hourFormat == HourFormat.SYSTEM, onClick = { change(style.copy(hourFormat = HourFormat.SYSTEM)) }, label = { Text(stringResource(R.string.disp_hour_system)) })
            FilterChip(selected = style.hourFormat == HourFormat.H24, onClick = { change(style.copy(hourFormat = HourFormat.H24)) }, label = { Text(stringResource(R.string.disp_hour_24)) })
            FilterChip(selected = style.hourFormat == HourFormat.H12, onClick = { change(style.copy(hourFormat = HourFormat.H12)) }, label = { Text(stringResource(R.string.disp_hour_12)) })
        }
        Text(stringResource(R.string.disp_today_color), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WidgetPalette.TODAY_SWATCHES.forEachIndexed { index, color ->
                val selected = index == style.todayColorIndex
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .border(
                            BorderStroke(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline),
                            CircleShape
                        )
                        .clickable { change(style.copy(todayColorIndex = index)) }
                )
            }
        }
        Text(stringResource(R.string.disp_week_mode), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = style.weekMode == WeekMode.LUNAR, onClick = { change(style.copy(weekMode = WeekMode.LUNAR)) }, label = { Text(stringResource(R.string.disp_week_lunar)) })
            FilterChip(selected = style.weekMode == WeekMode.WEATHER, onClick = { change(style.copy(weekMode = WeekMode.WEATHER)) }, label = { Text(stringResource(R.string.disp_week_weather)) })
        }
        OutlinedButton(onClick = {
            StyleStore.reset(context)
            val fresh = StyleStore.load(context)
            opacity = fresh.opacityPercent.toFloat()
            corner = fresh.cornerDp.toFloat()
            clockScale = fresh.clockScalePercent.toFloat()
            border = fresh.borderDp.toFloat()
            frost = fresh.glassFrostPercent.toFloat()
            relief = fresh.glassReliefPercent.toFloat()
            labelText = fresh.label
            change(fresh)
        }) { Text(stringResource(R.string.style_reset)) }
        Text(
            stringResource(R.string.style_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Xem trước nền widget trên một hình nền mẫu nhiều màu để thấy rõ độ trong suốt. */
@Composable
fun StylePreview(style: WidgetStyle) {
    val isDay = WidgetPalette.resolveDay(style.mode, WidgetUpdater.isDay(LocalContext.current))
    val sky = if (style.theme == WidgetTheme.WEATHER) WeatherSky.current(LocalContext.current, isDay) else null
    val palette = WidgetPalette.palette(style, isDay, sky)
    val shape = RoundedCornerShape(style.cornerDp.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(104.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF3A8D5C), Color(0xFFE9B949), Color(0xFFD9534F)))),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.85f)
                .height(76.dp)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color(palette.top), Color(palette.bottom))))
                .then(
                    if (palette.strokeDp > 0f) Modifier.border(BorderStroke(palette.strokeDp.dp, Color(palette.stroke)), shape)
                    else Modifier
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            val textColor = WidgetPalette.textColors(style.textTone, style.customTextColor)?.first?.let { Color(it) } ?: Color.White
            val now = java.time.LocalDateTime.now()
            val timeText = now.format(java.time.format.DateTimeFormatter.ofPattern(if (style.hourFormat == HourFormat.H12) "h:mm" else "HH:mm"))
            val dateText = now.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d/M", java.util.Locale.getDefault()))
            val soft = WidgetPalette.textColors(style.textTone, style.customTextColor)?.second?.let { Color(it) } ?: Color(0xCCFFFFFF)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(timeText, color = textColor, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text(dateText, color = soft, fontSize = 13.sp)
            }
        }
    }
}
