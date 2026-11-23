package com.vava.widgetpremium

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import java.util.Random

/**
 * Vẽ mây, mưa, tuyết, sao, mặt trời, mặt trăng lên nền bầu trời của widget. Tự thiết kế bằng
 * Canvas (không dùng ảnh có sẵn). Vị trí cố định theo hạt giống để mỗi lần vẽ giống nhau.
 * `unit` là số điểm ảnh trên mỗi dp; `alpha` theo độ đậm nền người dùng chọn.
 */
object SkyArt {
    private fun paint(color: Int, alpha: Float, blurPx: Float = 0f) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = (color and 0x00FFFFFF) or ((((color ushr 24) * alpha).toInt().coerceIn(0, 255)) shl 24)
        if (blurPx > 0f) maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
    }

    private fun cloud(c: Canvas, cx: Float, cy: Float, size: Float, color: Int, alpha: Float) {
        val p = paint(color, alpha, size * 0.12f)
        c.drawCircle(cx - size * 0.55f, cy + size * 0.1f, size * 0.38f, p)
        c.drawCircle(cx - size * 0.1f, cy - size * 0.12f, size * 0.5f, p)
        c.drawCircle(cx + size * 0.45f, cy + size * 0.05f, size * 0.42f, p)
        c.drawCircle(cx + size * 0.05f, cy + size * 0.2f, size * 0.42f, p)
    }

    private fun glow(c: Canvas, cx: Float, cy: Float, r: Float, color: Int, alpha: Float) {
        val a = (alpha * 255).toInt().coerceIn(0, 255)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy, r, (a shl 24) or (color and 0x00FFFFFF), color and 0x00FFFFFF, Shader.TileMode.CLAMP)
        }
        c.drawCircle(cx, cy, r, p)
    }

    private fun rain(c: Canvas, w: Float, h: Float, unit: Float, alpha: Float, count: Int) {
        val rnd = Random(11)
        val p = paint(0xFFDCE9F7.toInt(), alpha * 0.55f).apply { strokeWidth = 1.2f * unit; strokeCap = Paint.Cap.ROUND }
        repeat(count) {
            val x = rnd.nextFloat() * (w + 20 * unit)
            val y = rnd.nextFloat() * h
            c.drawLine(x, y, x - 5 * unit, y + 14 * unit, p)
        }
    }

    fun draw(c: Canvas, w: Float, h: Float, unit: Float, sky: Sky, alpha: Float) {
        val white = 0xFFFFFFFF.toInt()
        val rnd = Random(7)
        when (sky) {
            Sky.CLEAR_DAY -> {
                glow(c, w * 0.82f, h * 0.2f, h * 0.9f, 0xFFFFF2B0.toInt(), alpha * 0.55f)
                c.drawCircle(w * 0.82f, h * 0.2f, 14 * unit, paint(0xFFFFF6C8.toInt(), alpha * 0.9f, 3 * unit))
            }
            Sky.CLEAR_NIGHT -> {
                stars(c, w, h, unit, alpha, rnd, 28)
                moon(c, w, h, unit, alpha)
            }
            Sky.PARTLY_DAY -> {
                glow(c, w * 0.8f, h * 0.2f, h * 0.8f, 0xFFFFF2B0.toInt(), alpha * 0.45f)
                cloud(c, w * 0.3f, h * 0.35f, h * 0.32f, white, alpha * 0.55f)
                cloud(c, w * 0.72f, h * 0.55f, h * 0.28f, white, alpha * 0.45f)
            }
            Sky.PARTLY_NIGHT -> {
                stars(c, w, h, unit, alpha, rnd, 16)
                moon(c, w, h, unit, alpha)
                cloud(c, w * 0.3f, h * 0.4f, h * 0.3f, 0xFFB8C4DC.toInt(), alpha * 0.35f)
                cloud(c, w * 0.72f, h * 0.6f, h * 0.26f, 0xFFB8C4DC.toInt(), alpha * 0.3f)
            }
            Sky.CLOUDY -> {
                cloud(c, w * 0.2f, h * 0.3f, h * 0.34f, white, alpha * 0.4f)
                cloud(c, w * 0.55f, h * 0.5f, h * 0.38f, white, alpha * 0.35f)
                cloud(c, w * 0.88f, h * 0.28f, h * 0.3f, white, alpha * 0.4f)
                cloud(c, w * 0.4f, h * 0.85f, h * 0.3f, white, alpha * 0.3f)
            }
            Sky.FOG -> {
                for (i in 0 until 4) {
                    val y = h * (0.15f + 0.22f * i)
                    c.drawRoundRect(-w * 0.1f, y, w * 1.1f, y + h * 0.16f, h * 0.08f, h * 0.08f, paint(white, alpha * 0.28f, h * 0.06f))
                }
            }
            Sky.RAIN -> {
                cloud(c, w * 0.25f, h * 0.15f, h * 0.36f, 0xFF2A3442.toInt(), alpha * 0.55f)
                cloud(c, w * 0.7f, h * 0.1f, h * 0.38f, 0xFF2A3442.toInt(), alpha * 0.55f)
                rain(c, w, h, unit, alpha, (w / unit / 4).toInt().coerceIn(20, 90))
            }
            Sky.THUNDER -> {
                cloud(c, w * 0.25f, h * 0.15f, h * 0.38f, 0xFF171727.toInt(), alpha * 0.6f)
                cloud(c, w * 0.72f, h * 0.1f, h * 0.4f, 0xFF171727.toInt(), alpha * 0.6f)
                rain(c, w, h, unit, alpha, (w / unit / 5).toInt().coerceIn(16, 70))
                val bolt = Path().apply {
                    val x = w * 0.58f; val y = h * 0.22f; val s = h * 0.5f
                    moveTo(x, y); lineTo(x - s * 0.22f, y + s * 0.55f); lineTo(x - s * 0.02f, y + s * 0.55f)
                    lineTo(x - s * 0.2f, y + s); lineTo(x + s * 0.2f, y + s * 0.4f); lineTo(x, y + s * 0.4f); close()
                }
                c.drawPath(bolt, paint(0xFFFFF3A8.toInt(), alpha * 0.5f, 2 * unit))
            }
            Sky.SNOW -> {
                cloud(c, w * 0.3f, h * 0.12f, h * 0.34f, white, alpha * 0.4f)
                cloud(c, w * 0.78f, h * 0.1f, h * 0.32f, white, alpha * 0.4f)
                val p = paint(white, alpha * 0.8f)
                repeat(34) { c.drawCircle(rnd.nextFloat() * w, rnd.nextFloat() * h, (1f + rnd.nextFloat() * 2f) * unit, p) }
            }
        }
    }

    private fun stars(c: Canvas, w: Float, h: Float, unit: Float, alpha: Float, rnd: Random, count: Int) {
        repeat(count) {
            val p = paint(0xFFFFFFFF.toInt(), alpha * (0.4f + rnd.nextFloat() * 0.5f))
            c.drawCircle(rnd.nextFloat() * w, rnd.nextFloat() * h * 0.8f, (0.6f + rnd.nextFloat() * 1.1f) * unit, p)
        }
    }

    private fun moon(c: Canvas, w: Float, h: Float, unit: Float, alpha: Float) {
        val cx = w * 0.84f; val cy = h * 0.22f; val r = 12 * unit
        glow(c, cx, cy, r * 3.5f, 0xFFDCE6FF.toInt(), alpha * 0.35f)
        c.drawCircle(cx, cy, r, paint(0xFFF4F1E6.toInt(), alpha * 0.95f))
    }
}
