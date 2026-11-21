package com.vava.widgetpremium

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Đồng hồ 12 canh giờ: Tý ở trên cùng, đi theo chiều kim đồng hồ. Ô màu đậm là giờ Hoàng đạo,
 * vòng sáng bên ngoài đánh dấu canh giờ hiện tại (currentChi = -1 nếu không phải hôm nay).
 */
@Composable
fun HourDial(good: List<Int>, currentChi: Int, centerLines: List<String>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier.size(264.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 42.dp.toPx()
            val radius = size.minDimension / 2 - stroke / 2 - 8.dp.toPx()
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)
            val labelPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 14.dp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
            for (i in 0..11) {
                val isGood = i in good
                drawArc(
                    color = if (isGood) scheme.primary else scheme.surfaceVariant,
                    startAngle = -90f + 30f * i - 13.5f,
                    sweepAngle = 27f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke)
                )
                val angle = Math.toRadians(-90.0 + 30.0 * i)
                val x = center.x + radius * cos(angle).toFloat()
                val y = center.y + radius * sin(angle).toFloat()
                labelPaint.color = (if (isGood) scheme.onPrimary else scheme.onSurfaceVariant).toArgb()
                drawContext.canvas.nativeCanvas.drawText(VietCalendar.CHI[i], x, y + labelPaint.textSize / 3f, labelPaint)
            }
            if (currentChi in 0..11) {
                val outer = radius + stroke / 2 + 5.dp.toPx()
                drawArc(
                    color = scheme.tertiary,
                    startAngle = -90f + 30f * currentChi - 15f,
                    sweepAngle = 30f,
                    useCenter = false,
                    topLeft = Offset(center.x - outer, center.y - outer),
                    size = Size(outer * 2, outer * 2),
                    style = Stroke(width = 4.dp.toPx())
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            centerLines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}
