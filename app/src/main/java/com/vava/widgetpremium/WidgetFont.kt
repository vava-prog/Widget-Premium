package com.vava.widgetpremium

import android.content.Context
import android.util.TypedValue
import android.widget.RemoteViews
import kotlin.math.roundToInt

/**
 * Cỡ chữ, phông và độ đậm của widget. RemoteViews không đổi được phông lúc chạy, nên phông và độ đậm chọn
 * bộ layout đã sinh sẵn (tools/gen_font_layouts.py, bảng tra WidgetLayouts); cỡ chữ nhân theo tỉ lệ ở đây.
 * Phần thuần (snap, scaled, fitSp, columns) có test.
 */
object WidgetFont {
    const val MIN_SCALE = 80
    const val MAX_SCALE = 130
    const val STEP = 5
    const val DEFAULT_SCALE = 100

    /** Làm tròn về bước 5% trong khoảng cho phép. */
    fun snap(percent: Int): Int = ((percent.coerceIn(MIN_SCALE, MAX_SCALE) / STEP.toFloat()).roundToInt() * STEP).coerceIn(MIN_SCALE, MAX_SCALE)

    fun scaled(baseSp: Float, percent: Int): Float = baseSp * snap(percent) / 100f

    /**
     * Cỡ chữ lớn nhất (sp) để `chars` ký tự rộng ~0,56 lần cỡ chữ vẫn nằm gọn trong `availDp`; không vượt `wantedSp`.
     * Thay cho autoSizeTextType (RemoteViews không bật được): đồng hồ tự co khi khung hẹp hoặc cỡ chữ tăng.
     */
    fun fitSp(wantedSp: Float, availDp: Float, chars: Float): Float {
        if (availDp <= 0f || chars <= 0f) return wantedSp
        return minOf(wantedSp, availDp / (chars * 0.56f)).coerceAtLeast(10f)
    }

    /** Số cột dải dự báo: 7 khi đủ rộng, 5 khi widget hẹp (theo độ rộng tối thiểu launcher cấp). */
    fun columns(widthDp: Int): Int = if (widthDp < 300) 5 else 7

    fun size(context: Context, views: RemoteViews, id: Int, baseSp: Float) {
        views.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, scaled(baseSp, StyleStore.load(context).fontScalePercent))
    }

    fun sizes(context: Context, views: RemoteViews, bases: Map<Int, Float>) {
        val pct = StyleStore.load(context).fontScalePercent
        for ((id, sp) in bases) views.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, scaled(sp, pct))
    }
}
