package com.vava.widgetpremium

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.LruCache
import java.util.Random

/**
 * Vẽ bầu trời trên nền widget bằng Canvas, tự thiết kế (không dùng ảnh có sẵn): quầng sáng mặt trời
 * nhiều lớp, mây bông có đỉnh sáng và đáy xám xanh, mây ti mỏng, sương chân trời, tuyết, sao, trăng, và cửa kính
 * ướt mưa (hậu cảnh mờ cây xanh, toà nhà, ánh đèn ấm cùng các giọt nước thấu kính) cho mưa và giông.
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

    private fun argb(rgb: Int, alpha: Float): Int = (((alpha * 255).toInt().coerceIn(0, 255)) shl 24) or (rgb and 0x00FFFFFF)

    private fun solid(rgb: Int) = 0xFF000000.toInt() or rgb

    /** Cảnh cửa kính ướt vẽ một lần ở độ đậm đầy đủ rồi lưu lại: kéo thanh độ mờ không phải vẽ lại phần làm mờ tốn kém. */
    private val sceneCache = LruCache<String, Bitmap>(4)

    private fun rainScene(w: Int, h: Int, unit: Float, night: Boolean, animated: Boolean): Bitmap {
        val key = "$w:$h:${(unit * 100).toInt()}:$night:$animated"
        synchronized(sceneCache) { sceneCache.get(key)?.let { return it } }
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        rainWindow(Canvas(bitmap), w.toFloat(), h.toFloat(), unit, night, animated)
        synchronized(sceneCache) { sceneCache.put(key, bitmap) }
        return bitmap
    }

    private fun blobRect(c: Canvas, x0: Float, y0: Float, x1: Float, y1: Float, rgb: Int, alpha: Float, blur: Float) =
        c.drawRect(x0, y0, x1, y1, paint(solid(rgb), alpha, blur))

    private fun blobCircle(c: Canvas, x: Float, y: Float, r: Float, rgb: Int, alpha: Float, blur: Float) =
        c.drawCircle(x, y, r, paint(solid(rgb), alpha, blur))

    /**
     * Cửa kính ướt mưa: trời xanh xám, toà nhà và cây xanh nhòe, quầng sáng ấm ở góc dưới trái (đèn xe, biển hiệu),
     * một lớp tối nhẹ để chữ trắng luôn đọc được, rồi các giọt nước li ti bám kính. `animated`: lớp hiệu ứng chuyển động
     * vẽ các giọt lớn và vệt nước, nên nền chỉ vẽ giọt li ti; không có hiệu ứng thì nền tự vẽ vài vệt nước có giọt ở đầu.
     */
    private fun rainWindow(c: Canvas, w: Float, h: Float, unit: Float, night: Boolean, animated: Boolean) {
        val rnd = Random(41)
        val skyColors = if (night) intArrayOf(solid(0x32486B), solid(0x263A58), solid(0x18263C))
        else intArrayOf(solid(0x86A9D8), solid(0x6F93C2), solid(0x4F7399))
        c.drawRect(0f, 0f, w, h, Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, h, skyColors, floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        })

        // Toà nhà phía sau, nhòe.
        val dark = if (night) 0x27405F else 0x3F6797
        val light = if (night) 0x3A5679 else 0x6F97C8
        val blur = h * 0.05f
        blobRect(c, w * 0.60f, h * 0.20f, w * 0.80f, h * 1.1f, dark, 0.80f, blur)
        blobRect(c, w * 0.80f, h * 0.06f, w * 0.97f, h * 1.1f, light, 0.70f, blur)
        blobRect(c, w * 0.44f, h * 0.46f, w * 0.58f, h * 1.1f, dark, 0.60f, blur)
        blobRect(c, w * 0.30f, h * 0.58f, w * 0.42f, h * 1.1f, light, 0.45f, blur)
        repeat(14) { // ô cửa sáng nhỏ
            val x = w * (0.62f + rnd.nextFloat() * 0.33f)
            val y = h * (0.28f + rnd.nextFloat() * 0.5f)
            val s = (2f + rnd.nextFloat() * 3f) * unit
            blobRect(c, x, y, x + s, y + s * 1.3f, 0xDCEBFA, if (night) 0.30f else 0.22f, 2.2f * unit)
        }

        // Cây xanh nhòe: mép trái, góc trên trái, góc dưới phải.
        val greens = if (night) intArrayOf(0x1F4A33, 0x28603E, 0x1B3B2C) else intArrayOf(0x2C6B3A, 0x4C9444, 0x7DB45A, 0x235A45)
        val clusters = arrayOf(
            floatArrayOf(0.03f, 0.18f, 0.20f), floatArrayOf(0.14f, 0.06f, 0.16f), floatArrayOf(0.07f, 0.50f, 0.19f),
            floatArrayOf(0.20f, 0.32f, 0.13f), floatArrayOf(0.02f, 0.80f, 0.16f), floatArrayOf(0.92f, 0.34f, 0.10f),
            floatArrayOf(0.97f, 0.62f, 0.12f), floatArrayOf(0.84f, 0.90f, 0.20f), floatArrayOf(0.96f, 0.94f, 0.20f),
            floatArrayOf(0.30f, 0.02f, 0.11f)
        )
        for (cl in clusters) repeat(5) {
            val angle = rnd.nextFloat() * 6.283f
            val dist = rnd.nextFloat() * cl[2] * h * 0.6f
            blobCircle(
                c, w * cl[0] + Math.cos(angle.toDouble()).toFloat() * dist, h * cl[1] + Math.sin(angle.toDouble()).toFloat() * dist,
                h * cl[2] * (0.45f + rnd.nextFloat() * 0.5f), greens[rnd.nextInt(greens.size)], 0.78f, h * 0.07f
            )
        }

        // Quầng sáng ấm ở góc dưới trái và vài đốm bokeh.
        glow(c, w * 0.05f, h * 0.98f, h * 0.62f, solid(0xFFB84D), if (night) 0.6f else 0.78f)
        glow(c, w * 0.16f, h * 1.02f, h * 0.36f, solid(0xFFE2A8), if (night) 0.4f else 0.5f)
        for (i in 0 until 4) {
            blobCircle(c, w * (0.06f + i * 0.05f + rnd.nextFloat() * 0.03f), h * (0.78f + rnd.nextFloat() * 0.18f),
                (5f + rnd.nextFloat() * 5f) * unit, 0xFFE9BB, 0.28f, 2.5f * unit)
        }

        // Phủ tối nhẹ, đậm dần xuống dưới.
        c.drawRect(0f, 0f, w, h, Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, h, argb(0x102238, if (night) 0.30f else 0.18f), argb(0x0B1828, if (night) 0.38f else 0.30f), Shader.TileMode.CLAMP
            )
        })

        // Giọt li ti bám kính; một phần là giọt to hơn.
        repeat(if (animated) 84 else 120) {
            val x = rnd.nextFloat() * w
            val y = rnd.nextFloat() * h
            val big = if (rnd.nextFloat() < 0.14f) 1.8f else 1f
            microDrop(c, x, y, (0.8f + rnd.nextFloat() * rnd.nextFloat() * 2.2f) * unit * big)
        }
        if (!animated) {
            for (fx in floatArrayOf(0.12f, 0.31f, 0.52f, 0.70f, 0.88f)) {
                val x = w * (fx + (rnd.nextFloat() - 0.5f) * 0.06f)
                val top = h * (0.04f + rnd.nextFloat() * 0.12f)
                val len = h * (0.35f + rnd.nextFloat() * 0.3f)
                waterTrail(c, x, top, len, unit, rnd.nextFloat() * 6.28f)
                bigDrop(c, x, top + len, (2.6f + rnd.nextFloat() * 1.4f) * unit)
            }
        }
    }

    /** Giọt nước li ti như thấu kính: mép tối phía trên trái, mép sáng phía dưới phải, điểm sáng nhỏ. */
    private fun microDrop(c: Canvas, x: Float, y: Float, r: Float) {
        c.drawCircle(x, y, r, paint(WHITE, 0.07f))
        val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = maxOf(0.5f, r * 0.18f) }
        rim.color = argb(0x0B1828, 0.34f)
        c.drawArc(RectF(x - r, y - r, x + r, y + r), 195f, 115f, false, rim)
        rim.color = argb(0xFFFFFF, 0.62f)
        val inner = r * 0.82f
        c.drawArc(RectF(x - inner, y - inner, x + inner, y + inner), 20f, 105f, false, rim)
        c.drawCircle(x - r * 0.34f, y - r * 0.38f, maxOf(0.4f, r * 0.24f), paint(WHITE, 0.9f))
    }

    /** Giọt có đuôi nhọn hướng lên (giọt đang trượt dừng lại ở cuối vệt nước). */
    private fun bigDrop(c: Canvas, x: Float, y: Float, r: Float) {
        val body = Path().apply {
            moveTo(x, y - r * 1.7f)
            lineTo(x + r * 0.93f, y - r * 0.36f)
            arcTo(RectF(x - r, y - r, x + r, y + r), -21.8f, 223.6f, false)
            close()
        }
        c.drawPath(body, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(x, y - r, x, y + r * 1.1f, intArrayOf(argb(0x0B1828, 0.30f), argb(0xFFFFFF, 0.05f), argb(0xFFFFFF, 0.40f)), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        })
        c.drawPath(body, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = maxOf(0.6f, r * 0.12f); color = argb(0xFFFFFF, 0.45f) })
        c.save()
        c.rotate(-28f, x - r * 0.36f, y - r * 0.3f)
        c.drawOval(RectF(x - r * 0.62f, y - r * 0.46f, x - r * 0.10f, y - r * 0.14f), paint(WHITE, 0.95f))
        c.restore()
    }

    /** Vệt nước mảnh hơi uốn từ trên xuống, hai nét chồng nhau (viền mờ rộng và lõi sáng hẹp). */
    private fun waterTrail(c: Canvas, x: Float, top: Float, len: Float, unit: Float, phase: Float) {
        val path = Path()
        for (i in 0..24) {
            val f = i / 24f
            val xx = x + Math.sin((f * 6f + phase).toDouble()).toFloat() * unit * 0.5f
            if (i == 0) path.moveTo(xx, top) else path.lineTo(xx, top + len * f)
        }
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        line.color = argb(0xFFFFFF, 0.20f); line.strokeWidth = unit * 2.2f
        c.drawPath(path, line)
        line.color = argb(0xFFFFFF, 0.38f); line.strokeWidth = unit * 0.8f
        c.drawPath(path, line)
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

    /**
     * `animated`: widget đang có lớp hiệu ứng chuyển động phủ lên, nên nền chỉ vẽ ít hạt (mưa, tuyết, sao)
     * để hai lớp không chồng thành dày đặc và cứng. `night`: ban đêm, dùng màu tối hơn cho cảnh mưa.
     */
    fun draw(c: Canvas, w: Float, h: Float, unit: Float, sky: Sky, alpha: Float, animated: Boolean = false, night: Boolean = false) {
        val rnd = Random(7)
        val lightTop = 0xFFFFFFFF.toInt()
        val lightShade = 0xFF9DB3CC.toInt()
        val greyTop = 0xFFE4EAF0.toInt()
        val greyShade = 0xFF7C8B9B.toInt()
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
                stars(c, w, h, unit, alpha, rnd, if (animated) 12 else 30)
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
                stars(c, w, h, unit, alpha, rnd, if (animated) 7 else 16)
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
            Sky.RAIN, Sky.THUNDER -> {
                val dim = night || sky == Sky.THUNDER
                val scene = rainScene(Math.round(w), Math.round(h), unit, dim, animated)
                c.drawBitmap(scene, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG).apply { this.alpha = (alpha * 255).toInt().coerceIn(0, 255) })
                if (sky == Sky.THUNDER && !animated) { // tia sét do lớp hiệu ứng chớp vẽ khi có chuyển động
                    val bolt = Path().apply {
                        val x = w * 0.58f; val y = h * 0.25f; val s = h * 0.5f
                        moveTo(x, y); lineTo(x - s * 0.22f, y + s * 0.55f); lineTo(x - s * 0.02f, y + s * 0.55f)
                        lineTo(x - s * 0.2f, y + s); lineTo(x + s * 0.2f, y + s * 0.4f); lineTo(x, y + s * 0.4f); close()
                    }
                    c.drawPath(bolt, paint(0xFFFFF3A8.toInt(), alpha * 0.5f, 2.5f * unit))
                }
            }
            Sky.SNOW -> {
                cumulus(c, w * 0.3f, h * 0.2f, h * 0.75f, alpha, greyTop, greyShade, 9)
                cumulus(c, w * 0.78f, h * 0.15f, h * 0.7f, alpha, greyTop, greyShade, 10)
                val p = paint(WHITE, alpha * 0.85f, 0.6f * unit)
                repeat(if (animated) 12 else 34) { c.drawCircle(rnd.nextFloat() * w, rnd.nextFloat() * h, (1f + rnd.nextFloat() * 2f) * unit, p) }
            }
        }
    }
}
