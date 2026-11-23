package com.vava.widgetpremium

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.widget.RemoteViews

/** Vẽ nền widget thành ảnh bo góc theo đúng kích thước widget, rồi gắn vào RemoteViews. */
object WidgetBackground {
    private const val MAX_SIDE_PX = 640f

    /** Kích thước widget (dp) theo thông tin launcher cấp; thiếu thì dùng giá trị mặc định. */
    fun sizeDp(manager: AppWidgetManager, id: Int, defaultW: Int, defaultH: Int): Pair<Int, Int> {
        val o = manager.getAppWidgetOptions(id)
        val w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0).takeIf { it > 0 } ?: defaultW
        val h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0).takeIf { it > 0 } ?: defaultH
        return w to h
    }

    fun apply(context: Context, views: RemoteViews, imageId: Int, widthDp: Int, heightDp: Int) {
        val style = StyleStore.load(context)
        val day = WidgetUpdater.widgetIsDay(context)
        views.setImageViewBitmap(imageId, render(context, widthDp, heightDp, WidgetPalette.palette(style, day), style.cornerDp))
    }

    /** Ép màu chữ theo lựa chọn; AUTO giữ nguyên màu trong layout. `primary` là chữ lớn, `secondary` là các dòng phụ. */
    fun applyText(context: Context, views: RemoteViews, primary: List<Int>, secondary: List<Int>) {
        val colors = WidgetPalette.textColors(StyleStore.load(context).textTone) ?: return
        primary.forEach { views.setTextColor(it, colors.first) }
        secondary.forEach { views.setTextColor(it, colors.second) }
    }

    /** Đổi cỡ chữ giờ theo tỉ lệ người dùng chọn; baseSp là cỡ mặc định trong layout. */
    fun applyClockSize(context: Context, views: RemoteViews, timeId: Int, baseSp: Float) {
        val sp = WidgetPalette.clockSizeSp(baseSp, StyleStore.load(context).clockScalePercent)
        views.setTextViewTextSize(timeId, android.util.TypedValue.COMPLEX_UNIT_SP, sp)
    }

    fun render(context: Context, widthDp: Int, heightDp: Int, palette: Palette, cornerDp: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        var w = widthDp * density
        var h = heightDp * density
        val k = minOf(1f, MAX_SIDE_PX / maxOf(w, h))
        w *= k
        h *= k
        val bitmap = Bitmap.createBitmap(maxOf(w.toInt(), 32), maxOf(h.toInt(), 32), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val strokePx = palette.strokeDp * density * k
        val inset = strokePx / 2f
        val rect = RectF(inset, inset, bitmap.width - inset, bitmap.height - inset)
        val radius = cornerDp * density * k

        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, bitmap.height.toFloat(), palette.top, palette.bottom, Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(rect, radius, radius, fill)

        if ((palette.sheen ushr 24) > 0) {
            val sheen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, 0f, 0f, bitmap.height * 0.5f, palette.sheen, 0x00FFFFFF, Shader.TileMode.CLAMP)
            }
            canvas.drawRoundRect(rect, radius, radius, sheen)
        }
        if (strokePx > 0f && (palette.stroke ushr 24) > 0) {
            val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokePx
                color = palette.stroke
            }
            canvas.drawRoundRect(rect, radius, radius, line)
        }
        return bitmap
    }
}
