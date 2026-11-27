package com.vava.widgetpremium

import java.io.File
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherMotionTest {
    @Test
    fun everySkyHasAMotion() {
        for (sky in Sky.values()) assertNotNull("$sky", WeatherMotion.choose(sky, true, false, true))
    }

    @Test
    fun mappingFollowsTheWeather() {
        fun m(s: Sky) = WeatherMotion.choose(s, true, false, true)
        assertEquals(Motion.SUN, m(Sky.CLEAR_DAY))
        assertEquals(Motion.SUN, m(Sky.PARTLY_DAY))
        assertEquals(Motion.STARS, m(Sky.CLEAR_NIGHT))
        assertEquals(Motion.STARS, m(Sky.PARTLY_NIGHT))
        assertEquals(Motion.CLOUDS, m(Sky.CLOUDY))
        assertEquals(Motion.FOG, m(Sky.FOG))
        assertEquals(Motion.RAIN, m(Sky.RAIN))
        assertEquals(Motion.STORM, m(Sky.THUNDER))
        assertEquals(Motion.SNOW, m(Sky.SNOW))
    }

    @Test
    fun offWhenDisabledSavingPowerOrAnimationsOffOrNoSky() {
        assertNull(WeatherMotion.choose(Sky.RAIN, false, false, true))
        assertNull(WeatherMotion.choose(Sky.RAIN, true, true, true))
        assertNull(WeatherMotion.choose(Sky.RAIN, true, false, false))
        assertNull(WeatherMotion.choose(null, true, false, true))
    }

    @Test
    fun insetKeepsTheOverlayInsideTheRoundedCorner() {
        assertEquals(0, WeatherMotion.insetDp(0))
        assertEquals(0, WeatherMotion.insetDp(-5))
        for (r in 1..WidgetPalette.MAX_CORNER_DP) {
            val inset = WeatherMotion.insetDp(r)
            // Góc vuông của lớp hiệu ứng phải nằm trong cung tròn tâm (r, r) bán kính r.
            val dist = (r - inset) * sqrt(2.0)
            assertTrue("r=$r inset=$inset", dist <= r + 1e-9)
        }
    }

    @Test
    fun styleDefaultsToMotionOn() {
        assertTrue(WidgetStyle().weatherMotion)
        assertTrue(WidgetStyle(weatherMotion = false).sanitized().weatherMotion.not())
    }

    // ---- Kiểm tra tài nguyên sinh tự động (tools/gen_weather_fx.py) ----

    private fun resDir(): File =
        listOf("src/main/res", "app/src/main/res").map { File(it) }.first { it.isDirectory }

    private val tag = Regex("<objectAnimator[^>]*>")

    private fun attr(text: String, name: String): String? =
        Regex("android:$name=\"([^\"]*)\"").find(text)?.groupValues?.get(1)

    private fun allLayers(): List<String> = Motion.values().flatMap { WeatherMotion.layers(it) }.distinct()

    @Test
    fun stormIsGlassRainPlusFlashOnTop() {
        assertEquals(listOf("rain", "flash"), WeatherMotion.layers(Motion.STORM))
        assertEquals(listOf("snow"), WeatherMotion.layers(Motion.SNOW))
    }

    @Test
    fun everyMotionHasItsResourcesAndViewId() {
        val res = resDir()
        val layout = File(res, "layout/widget_fx.xml").readText()
        for (n in allLayers()) {
            assertTrue("thiếu fx_$n.xml", File(res, "drawable/fx_$n.xml").isFile)
            assertTrue("thiếu fx_${n}_vec.xml", File(res, "drawable/fx_${n}_vec.xml").isFile)
            assertTrue("thiếu id fx_$n", layout.contains("@+id/fx_$n\""))
            assertTrue("fx_$n chưa trỏ đúng drawable", layout.contains("@drawable/fx_$n\""))
        }
    }

    @Test
    fun everyAvdTargetExistsAndHasAnAnimator() {
        val res = resDir()
        for (n in allLayers()) {
            val avd = File(res, "drawable/fx_$n.xml").readText()
            val vec = File(res, "drawable/fx_${n}_vec.xml").readText()
            val targets = Regex("<target\\s+android:name=\"([^\"]+)\"\\s+android:animation=\"@animator/([^\"]+)\"").findAll(avd).toList()
            assertTrue("fx_$n không có target", targets.isNotEmpty())
            for (t in targets) {
                assertTrue("fx_$n: không có '${t.groupValues[1]}' trong vector", vec.contains("android:name=\"${t.groupValues[1]}\""))
                assertTrue("thiếu animator ${t.groupValues[2]}", File(res, "animator/${t.groupValues[2]}.xml").isFile)
            }
        }
    }

    private val holder = Regex("<propertyValuesHolder\\s+android:propertyName=\"([^\"]+)\"[^>]*>(.*?)</propertyValuesHolder>", RegexOption.DOT_MATCHES_ALL)
    private val keyframe = Regex("<keyframe[^>]*/>", RegexOption.DOT_MATCHES_ALL)

    private fun animatorFiles(): List<File> =
        File(resDir(), "animator").listFiles { f -> f.name.startsWith("fx_") }!!.toList()

    /** Tên lớp hiệu ứng của một tệp animator: fx_rain_d1.xml -> rain. */
    private fun layerOf(f: File): String = f.name.removePrefix("fx_").substringBefore('.').substringBefore('_')

    @Test
    fun keyframesGoFromZeroToOneInStrictOrder() {
        var checked = 0
        for (f in animatorFiles()) {
            for (m in holder.findAll(f.readText())) {
                val fractions = keyframe.findAll(m.groupValues[2]).map { attr(it.value, "fraction")!!.toDouble() }.toList()
                assertTrue("${f.name}/${m.groupValues[1]}: cần ít nhất 2 keyframe", fractions.size >= 2)
                assertEquals("${f.name}/${m.groupValues[1]}: keyframe đầu", 0.0, fractions.first(), 0.0)
                assertEquals("${f.name}/${m.groupValues[1]}: keyframe cuối", 1.0, fractions.last(), 0.0)
                for (i in 1 until fractions.size) {
                    assertTrue("${f.name}/${m.groupValues[1]}: fraction phải tăng dần tại #$i", fractions[i] > fractions[i - 1])
                }
                checked++
            }
        }
        assertTrue(checked > 0)
    }

    @Test
    fun loopingScaleAnimatorsReturnToTheirFirstFrame() {
        // Hiệu ứng lặp (quầng nắng, trăng): nhịp phồng-xẹp đảo chiều với số lượt chẵn nên hết giờ về đúng kích thước ban đầu.
        // Mưa là ngoại lệ: chạy một lượt rồi giữ khung cuối (vệt nước ở lại như kính vừa ướt), xem test bên dưới.
        var checked = 0
        for (f in animatorFiles().filter { layerOf(it) != "rain" }) {
            for (t in tag.findAll(f.readText()).map { it.value }) {
                val prop = attr(t, "propertyName")
                if (prop != "scaleX" && prop != "scaleY") continue
                assertEquals("${f.name}: scale lặp phải đảo chiều", "reverse", attr(t, "repeatMode"))
                assertEquals("${f.name}: cần số lượt chẵn", 0, ((attr(t, "repeatCount")?.toInt() ?: 0) + 1) % 2)
                checked++
            }
        }
        assertTrue(checked > 0)
    }

    @Test
    fun rainRunsOnceAndKeepsTheFinalFrameWithinSaneScales() {
        var checked = 0
        for (f in animatorFiles().filter { layerOf(it) == "rain" && it.name.startsWith("fx_rain_d") }) {
            for (m in holder.findAll(f.readText())) {
                if (m.groupValues[1] != "scaleX" && m.groupValues[1] != "scaleY") continue
                val values = keyframe.findAll(m.groupValues[2]).map { attr(it.value, "value")!!.toDouble() }.toList()
                assertTrue("${f.name}: giọt phồng quá lớn", values.all { it in 0.0..1.6 })
                checked++
            }
        }
        assertTrue(checked > 0)
        assertEquals(4_000, WeatherMotion.RAIN_MS)
    }

    @Test
    fun everyReferencedInterpolatorResourceExists() {
        val res = resDir()
        val ref = Regex("@interpolator/([A-Za-z0-9_]+)")
        var used = 0
        for (f in animatorFiles()) {
            for (m in ref.findAll(f.readText())) {
                assertTrue("${f.name}: thiếu interpolator ${m.groupValues[1]}", File(res, "interpolator/${m.groupValues[1]}.xml").isFile)
                used++
            }
        }
        assertTrue("mưa phải dùng nội suy rơi/hãm/nảy riêng", used > 0)
    }

    @Test
    fun rainHasGravityTrailsAndManyDrops() {
        val res = resDir()
        val avd = File(res, "drawable/fx_rain.xml").readText()
        val drops = Regex("android:name=\"d\\d+\"").findAll(avd).count()
        val trails = Regex("android:name=\"t\\d+\"").findAll(avd).count()
        assertTrue("cần nhiều giọt, có $drops", drops >= 30)
        assertTrue("cần các vệt nước, có $trails", trails >= 6)
        // Ít nhất một giọt rơi nhanh dần: dùng nội suy trọng lực ở tịnh tiến dọc.
        val anyFall = animatorFiles().any { it.name.startsWith("fx_rain_d") && it.readText().contains("@interpolator/fx_fall") }
        assertTrue("không có giọt nào rơi nhanh dần", anyFall)
    }

    @Test
    fun everyAnimatorRunsExactlyItsLayerDuration() {
        val files = File(resDir(), "animator").listFiles { f -> f.name.startsWith("fx_") }!!.toList()
        assertTrue(files.isNotEmpty())
        for (f in files) {
            val tags = tag.findAll(f.readText()).map { it.value }.toList()
            assertTrue("${f.name}: không có objectAnimator", tags.isNotEmpty())
            for (t in tags) {
                val duration = attr(t, "duration")!!.toInt()
                val passes = (attr(t, "repeatCount")?.toInt() ?: 0) + 1
                assertEquals("${f.name}: tổng thời gian", WeatherMotion.durationMs(layerOf(f)), duration * passes)
                if (attr(t, "repeatMode") == "reverse") {
                    assertEquals("${f.name}: reverse cần số lượt chẵn", 0, passes % 2)
                }
            }
        }
    }
}
