package com.vava.widgetpremium

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import java.util.Random

/**
 * Vẽ bầu trời trên nền widget bằng Canvas, tự thiết kế (không dùng ảnh có sẵn): quầng sáng mặt trời
 * nhiều lớp, mây bông có đỉnh sáng và đáy xám xanh, mây ti mỏng, sương chân trời, mưa, tuyết, sao, trăng.
 * Vị trí cố định theo hạt giống để mỗi lần vẽ giống nhau. `unit` là số điểm ảnh mỗi dp;
 * `alpha` theo độ đậm nền người dùng chọn.
 */
object SkyArt {
    private const val WHITE = 0xFFFFFFFF.toInt()

    private fun paint(color: Int, alpha: Float, blurPx: Float = 0f) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = (color and 0x00FFFFFF) or ((((color ushr 24) * alpha).toInt().coerceIn(0, 255)) shl 24)
        if (blurPx > 0f) maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
    }

    /** Quầng sáng tròn, mờ dần ra rìa: không có mép cứng. */
    private fun glow(c: Canvas, cx: Float, cy: Float, r: Float, color: Int, alpha: Float) {
        val a = (alpha * 255).toInt().coerceIn(0, 255)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, r, intArrayOf((a shl 24) or (color and 0x00FFFFFF), ((a * 0.35f).toInt() shl 24) or (color and 0x00FFFFFF), color and 0x00FFFFFF),
                floatArrayOf(0f, 0.4f, 1f), Shader.TileMode.CLAMP
            )
        }
        c.drawCircle(cx, cy, r, p)
    }

    /** Mặt trời: quầng rộng ấm, quầng giữa sáng, lõi trắng rất mềm. */
    private fun sun(c: Canvas, cx: Float, cy: Float, h: Float, alpha: Float) {
        glow(c, cx, cy, h * 1.5f, 0xFFFFE9A8.toInt(), alpha * 0.40f)
        glow(c, cx, cy, h * 0.7f, 0xFFFFF4C8.toInt(), alpha * 0.65f)
        glow(c, cx, cy, h * 0.26f, WHITE, alpha * 0.95f)
    }

    /** Sương sáng dần về chân trời, làm trời bớt "phẳng". */
    private fun haze(c: Canvas, w: Float, h: Float, alpha: Float, strength: Float) {
        val a = (alpha * strength * 255).toInt().coerceIn(0, 255)
        val p = Paint().apply {
            shader = LinearGradient(0f, h * 0.35f, 0f, h, 0x00FFFFFF, (a shl 24) or 0x00FFFFFF, Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, w, h, p)
    }

    /** Cụm mây bông: nhiều búp chồng nhau, lớp bóng xám xanh phía dưới và lớp sáng phía trên. */
    private fun cumulus(c: Canvas, cx: Float, cy: Float, size: Float, alpha: Float, top: Int, shade: Int, seed: Long) {
        val rnd = Random(seed)
        val n = 12
        val xs = FloatArray(n); val ys = FloatArray(n); val rs = FloatArray(n)
        for (i in 0 until n) {
            val t = i / (n - 1f)
            xs[i] = cx + (t - 0.5f) * size * 1.7f + (rnd.nextFloat() - 0.5f) * size * 0.2f
            val dome = 1f - Math.abs(t - 0.5f) * 2f
            ys[i] = cy - dome * size * 0.28f + (rnd.nextFloat() - 0.3f) * size * 0.12f
            rs[i] = size * (0.13f + 0.17f * dome + rnd.nextFloat() * 0.07f)
        }
        for (i in 0 until n) c.drawCircle(xs[i], ys[i] + rs[i] * 0.45f, rs[i] * 1.05f, paint(shade, alpha * 0.55f, rs[i] * 0.45f))
        for (i in 0 until n) c.drawCircle(xs[i], ys[i] - rs[i] * 0.12f, rs[i], paint(top, alpha * 0.62f, rs[i] * 0.4f))
        for (i in 0 until n) c.drawCircle(xs[i] - rs[i] * 0.1f, ys[i] - rs[i] * 0.3f, rs[i] * 0.6f, paint(WHITE, alpha * 0.35f, rs[i] * 0.35f))
    }

    /** Mây ti mỏng kéo dài, hơi nghiêng. */
    private fun wisp(c: Canvas, cx: Float, cy: Float, len: Float, thick: Float, angle: Float, alpha: Float) {
        c.save()
        c.translate(cx, cy)
        c.rotate(angle)
        c.drawOval(RectF(-len / 2, -thick / 2, len / 2, thick / 2), paint(WHITE, alpha, thick * 0.9f))
        c.restore()
    }

    private fun rain(c: Canvas, w: Float, h: Float, unit: Float, alpha: Float, count: Int) {
        val rnd = Random(11)
        val p = paint(0xFFDCE9F7.toInt(), alpha * 0.5f).apply { strokeWidth = 1.1f * unit; strokeCap = Paint.Cap.ROUND }
        repeat(count) {
            val x = rnd.nextFloat() * (w + 20 * unit)
            val y = rnd.nextFloat() * h
            val len = (10 + rnd.nextFloat() * 8) * unit
            c.drawLine(x, y, x - len * 0.35f, y + len, p)
        }
    }

    private fun stars(c: Canvas, w: Float, h: Float, unit: Float, alpha: Float, rnd: Random, count: Int) {
        repeat(count) {
            val p = paint(WHITE, alpha * (0.35f + rnd.nextFloat() * 0.55f), 0.8f * unit)
            c.drawCircle(rnd.nextFloat() * w, rnd.nextFloat() * h * 0.8f, (0.5f + rnd.nextFloat() * 1.0f) * unit, p)
        }
    }

    private fun moon(c: Canvas, w: Float, h: Float, unit: Float, alpha: Float) {
        val cx = w * 0.84f; val cy = h * 0.22f; val r = 11 * unit
        glow(c, cx, cy, r * 5f, 0xFFDCE6FF.toInt(), alpha * 0.35f)
        glow(c, cx, cy, r * 2.2f, 0xFFEAF0FF.toInt(), alpha * 0.5f)
        c.drawCircle(cx, cy, r, paint(0xFFF6F3E8.toInt(), alpha * 0.95f, 1.2f * unit))
    }

    fun draw(c: Canvas, w: Float, h: Float, unit: Float, sky: Sky, alpha: Float) {
        val rnd = Random(7)
        val lightTop = 0xFFFFFFFF.toInt()
        val lightShade = 0xFF9DB3CC.toInt()
        val greyTop = 0xFFE4EAF0.toInt()
        val greyShade = 0xFF7C8B9B.toInt()
        val darkTop = 0xFF74818F.toInt()
        val darkShade = 0xFF283240.toInt()
        val nightTop = 0xFF7F8CAA.toInt()
        val nightShade = 0xFF1C2742.toInt()
        when (sky) {
            Sky.CLEAR_DAY -> {
                haze(c, w, h, alpha, 0.35f)
                wisp(c, w * 0.3f, h * 0.3f, w * 0.7f, h * 0.06f, -8f, alpha * 0.30f)
                wisp(c, w * 0.55f, h * 0.55f, w * 0.8f, h * 0.05f, -5f, alpha * 0.22f)
                wisp(c, w * 0.25f, h * 0.75f, w * 0.6f, h * 0.05f, -10f, alpha * 0.2f)
                sun(c, w * 0.82f, h * 0.16f, h, alpha)
            }
            Sky.CLEAR_NIGHT -> {
                stars(c, w, h, unit, alpha, rnd, 30)
                moon(c, w, h, unit, alpha)
            }
            Sky.PARTLY_DAY -> {
                haze(c, w, h, alpha, 0.3f)
                sun(c, w * 0.8f, h * 0.16f, h, alpha)
                wisp(c, w * 0.35f, h * 0.22f, w * 0.7f, h * 0.05f, -7f, alpha * 0.25f)
                cumulus(c, w * 0.28f, h * 0.55f, h * 0.7f, alpha, lightTop, lightShade, 3)
                cumulus(c, w * 0.78f, h * 0.8f, h * 0.55f, alpha, lightTop, lightShade, 5)
            }
            Sky.PARTLY_NIGHT -> {
                stars(c, w, h, unit, alpha, rnd, 16)
                moon(c, w, h, unit, alpha)
                cumulus(c, w * 0.3f, h * 0.6f, h * 0.65f, alpha * 0.8f, nightTop, nightShade, 3)
                cumulus(c, w * 0.78f, h * 0.82f, h * 0.5f, alpha * 0.8f, nightTop, nightShade, 5)
            }
            Sky.CLOUDY -> {
                haze(c, w, h, alpha, 0.2f)
                cumulus(c, w * 0.22f, h * 0.35f, h * 0.8f, alpha, greyTop, greyShade, 1)
                cumulus(c, w * 0.7f, h * 0.5f, h * 0.9f, alpha, greyTop, greyShade, 2)
                cumulus(c, w * 0.45f, h * 0.88f, h * 0.7f, alpha, greyTop, greyShade, 4)
            }
            Sky.FOG -> {
                for (i in 0 until 4) {
                    val y = h * (0.12f + 0.24f * i)
                    c.drawRoundRect(-w * 0.1f, y, w * 1.1f, y + h * 0.18f, h * 0.09f, h * 0.09f, paint(WHITE, alpha * 0.3f, h * 0.08f))
                }
                haze(c, w, h, alpha, 0.25f)
            }
            Sky.RAIN -> {
                cumulus(c, w * 0.25f, h * 0.2f, h * 0.85f, alpha, darkTop, darkShade, 6)
                cumulus(c, w * 0.72f, h * 0.15f, h * 0.9f, alpha, darkTop, darkShade, 8)
                rain(c, w, h, unit, alpha, (w / unit / 4).toInt().coerceIn(20, 90))
            }
            Sky.THUNDER -> {
                cumulus(c, w * 0.25f, h * 0.2f, h * 0.9f, alpha, 0xFF565466.toInt(), 0xFF14141F.toInt(), 6)
                cumulus(c, w * 0.72f, h * 0.15f, h * 0.95f, alpha, 0xFF565466.toInt(), 0xFF14141F.toInt(), 8)
                rain(c, w, h, unit, alpha, (w / unit / 5).toInt().coerceIn(16, 70))
                val bolt = Path().apply {
                    val x = w * 0.58f; val y = h * 0.25f; val s = h * 0.5f
                    moveTo(x, y); lineTo(x - s * 0.22f, y + s * 0.55f); lineTo(x - s * 0.02f, y + s * 0.55f)
                    lineTo(x - s * 0.2f, y + s); lineTo(x + s * 0.2f, y + s * 0.4f); lineTo(x, y + s * 0.4f); close()
                }
                c.drawPath(bolt, paint(0xFFFFF3A8.toInt(), alpha * 0.5f, 2.5f * unit))
            }
            Sky.SNOW -> {
                cumulus(c, w * 0.3f, h * 0.2f, h * 0.75f, alpha, greyTop, greyShade, 9)
                cumulus(c, w * 0.78f, h * 0.15f, h * 0.7f, alpha, greyTop, greyShade, 10)
                val p = paint(WHITE, alpha * 0.85f, 0.6f * unit)
                repeat(34) { c.drawCircle(rnd.nextFloat() * w, rnd.nextFloat() * h, (1f + rnd.nextFloat() * 2f) * unit, p) }
            }
        }
    }
}
