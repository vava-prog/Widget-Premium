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

/** Chọn giao diện widget (Standard hoặc Kính mờ), màu chủ đạo, độ đậm của nền và chế độ sáng/tối. */
@Composable
fun StyleSection() {
    val context = LocalContext.current
    var style by remember { mutableStateOf(StyleStore.load(context)) }
    var opacity by remember { mutableStateOf(style.opacityPercent.toFloat()) }
    var corner by remember { mutableStateOf(style.cornerDp.toFloat()) }
    var clockScale by remember { mutableStateOf(style.clockScalePercent.toFloat()) }

    fun change(new: WidgetStyle) {
        style = new.sanitized()
        StyleStore.save(context, style)
        WidgetUpdater.updateWidgets(context)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.style_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        StylePreview(style)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = style.theme == WidgetTheme.STANDARD,
                onClick = { change(style.copy(theme = WidgetTheme.STANDARD)) },
                label = { Text(stringResource(R.string.style_theme_standard)) }
            )
            FilterChip(
                selected = style.theme == WidgetTheme.GLASS,
                onClick = { change(style.copy(theme = WidgetTheme.GLASS)) },
                label = { Text(stringResource(R.string.style_theme_glass)) }
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
        Text(stringResource(R.string.style_clock_size, clockScale.toInt()), style = MaterialTheme.typography.labelLarge)
        Slider(
            value = clockScale,
            onValueChange = { clockScale = it },
            valueRange = WidgetPalette.MIN_CLOCK_SCALE.toFloat()..WidgetPalette.MAX_CLOCK_SCALE.toFloat(),
            onValueChangeFinished = { change(style.copy(clockScalePercent = clockScale.toInt())) }
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
        Text(stringResource(R.string.style_mode), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = style.mode == DayMode.AUTO, onClick = { change(style.copy(mode = DayMode.AUTO)) },
                label = { Text(stringResource(R.string.style_mode_auto)) })
            FilterChip(selected = style.mode == DayMode.DAY, onClick = { change(style.copy(mode = DayMode.DAY)) },
                label = { Text(stringResource(R.string.style_mode_day)) })
            FilterChip(selected = style.mode == DayMode.NIGHT, onClick = { change(style.copy(mode = DayMode.NIGHT)) },
                label = { Text(stringResource(R.string.style_mode_night)) })
        }
        OutlinedButton(onClick = {
            StyleStore.reset(context)
            val fresh = StyleStore.load(context)
            opacity = fresh.opacityPercent.toFloat()
            corner = fresh.cornerDp.toFloat()
            clockScale = fresh.clockScalePercent.toFloat()
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
private fun StylePreview(style: WidgetStyle) {
    val isDay = WidgetPalette.resolveDay(style.mode, WidgetUpdater.isDay(LocalContext.current))
    val palette = WidgetPalette.palette(style, isDay)
    val shape = RoundedCornerShape(style.cornerDp.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF3A8D5C), Color(0xFFE9B949), Color(0xFFD9534F)))),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.85f)
                .height(88.dp)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color(palette.top), Color(palette.bottom))))
                .then(
                    if (palette.strokeDp > 0f) Modifier.border(BorderStroke(palette.strokeDp.dp, Color(palette.stroke)), shape)
                    else Modifier
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            val textColor = WidgetPalette.textColors(style.textTone)?.first?.let { Color(it) } ?: Color.White
            Text("09:41", color = textColor, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        }
    }
}
