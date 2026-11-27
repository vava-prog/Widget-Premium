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
        val sky = if (style.theme == WidgetTheme.WEATHER) WeatherSky.current(context, day) else null
        val palette = WidgetPalette.palette(style, day, sky)
        val motion = WidgetMotion.resolve(context, style, sky)
        views.setImageViewBitmap(
            imageId,
            // Có lớp hiệu ứng chuyển động thì nền chỉ vẽ ít hạt mưa, tuyết, sao để không bị dày đặc chồng nhau.
            render(context, widthDp, heightDp, palette, style.cornerDp, sky, style.opacityPercent / 100f, animated = motion != null, isDay = day)
        )
        WidgetMotion.apply(context, views, style, motion)
    }

    /** Ép màu chữ theo lựa chọn; AUTO giữ nguyên màu trong layout. `primary` là chữ lớn, `secondary` là các dòng phụ. */
    fun applyText(context: Context, views: RemoteViews, primary: List<Int>, secondary: List<Int>) {
        val style = StyleStore.load(context)
        val colors = WidgetPalette.textColors(style.textTone, style.customTextColor) ?: return
        primary.forEach { views.setTextColor(it, colors.first) }
        secondary.forEach { views.setTextColor(it, colors.second) }
    }

    /** Tô màu biểu tượng nhỏ (ví dụ icon lịch cạnh ngày âm) theo màu chữ phụ khi người dùng ép màu chữ; AUTO giữ nguyên. */
    fun applyIconTint(context: Context, views: RemoteViews, iconIds: List<Int>) {
        val style = StyleStore.load(context)
        val colors = WidgetPalette.textColors(style.textTone, style.customTextColor) ?: return
        iconIds.forEach { views.setInt(it, "setColorFilter", colors.second) }
    }

    /** Ẩn hoặc hiện một view theo tùy chọn hiển thị. */
    fun setShown(views: RemoteViews, viewId: Int, shown: Boolean) {
        views.setViewVisibility(viewId, if (shown) android.view.View.VISIBLE else android.view.View.GONE)
    }

    /** Giây là một TextClock nhỏ riêng cạnh giờ: hiện theo tùy chọn, cỡ bằng một phần ba chữ giờ. */
    fun applySeconds(context: Context, views: RemoteViews, secId: Int, baseTimeSp: Float) {
        val style = StyleStore.load(context)
        setShown(views, secId, style.showSeconds)
        val sp = WidgetPalette.clockSizeSp(baseTimeSp / 3f, style.clockScalePercent)
        views.setTextViewTextSize(secId, android.util.TypedValue.COMPLEX_UNIT_SP, sp)
    }

    /** Hiện dòng tên tự nhập; rỗng thì ẩn dòng đó. */
    fun applyLabel(context: Context, views: RemoteViews, labelId: Int) {
        val label = StyleStore.load(context).label
        views.setViewVisibility(labelId, if (label.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE)
        if (label.isNotEmpty()) views.setTextViewText(labelId, label)
    }

    /** Đổi cỡ chữ giờ theo tỉ lệ người dùng chọn; baseSp là cỡ mặc định trong layout. */
    fun applyClockSize(context: Context, views: RemoteViews, timeId: Int, baseSp: Float, splitSeconds: Boolean = false) {
        val style = StyleStore.load(context)
        // splitSeconds: giây là một TextClock nhỏ riêng cạnh giờ, nên giờ giữ nguyên cỡ và không kèm giây.
        val withSeconds = style.showSeconds && !splitSeconds
        val sp = WidgetPalette.clockSizeSp(baseSp, style.clockScalePercent, withSeconds)
        val (f12, f24) = WidgetPalette.timeFormats(withSeconds, style.hourFormat)
        views.setCharSequence(timeId, "setFormat12Hour", f12)
        views.setCharSequence(timeId, "setFormat24Hour", f24)
        views.setTextViewTextSize(timeId, android.util.TypedValue.COMPLEX_UNIT_SP, sp)
    }

    /**
     * Mép kính cho nền thời tiết: ánh sáng sau lưng kính làm sáng hai góc chéo (trên trái, dưới phải), vệt sáng
     * mờ chạy dọc mép trong như khúc xạ và một lớp bóng phản chiếu nhạt phía trên. Độ đậm theo độ mờ nền người dùng chọn.
     */
    private fun glassRim(canvas: Canvas, rect: RectF, radius: Float, unit: Float, alpha: Float) {
        fun white(a: Float) = (((a * alpha) * 255).toInt().coerceIn(0, 255) shl 24) or 0x00FFFFFF
        val gloss = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, rect.top, 0f, rect.top + rect.height() * 0.45f, white(0.16f), white(0f), Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(rect, radius, radius, gloss)
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f * unit
            color = white(0.10f)
            maskFilter = android.graphics.BlurMaskFilter(2.2f * unit, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
        val inner = RectF(rect.left + 2f * unit, rect.top + 2f * unit, rect.right - 2f * unit, rect.bottom - 2f * unit)
        canvas.drawRoundRect(inner, maxOf(0f, radius - 2f * unit), maxOf(0f, radius - 2f * unit), glow)
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.3f * unit
            shader = LinearGradient(
                rect.left, rect.top, rect.right, rect.bottom,
                intArrayOf(white(0.70f), white(0.16f), white(0.16f), white(0.42f)), floatArrayOf(0f, 0.35f, 0.65f, 1f), Shader.TileMode.CLAMP
            )
        }
        val half = 0.65f * unit
        canvas.drawRoundRect(RectF(rect.left + half, rect.top + half, rect.right - half, rect.bottom - half), radius, radius, edge)
    }

    fun render(
        context: Context, widthDp: Int, heightDp: Int, palette: Palette, cornerDp: Int,
        sky: Sky? = null, artAlpha: Float = 1f, animated: Boolean = false, isDay: Boolean = true
    ): Bitmap {
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

        if (sky != null) {
            canvas.save()
            val clip = android.graphics.Path().apply { addRoundRect(rect, radius, radius, android.graphics.Path.Direction.CW) }
            canvas.clipPath(clip)
            SkyArt.draw(canvas, bitmap.width.toFloat(), bitmap.height.toFloat(), density * k, sky, artAlpha, animated, night = !isDay)
            canvas.restore()
        }

        if (sky != null) glassRim(canvas, rect, radius, density * k, artAlpha)
        if ((palette.frost ushr 24) > 0) {
            val frost = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.frost }
            canvas.drawRoundRect(rect, radius, radius, frost)
        }
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
