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

    @Test
    fun everyAnimatorRunsExactlyTotalAndEndsOnItsFirstFrame() {
        val files = File(resDir(), "animator").listFiles { f -> f.name.startsWith("fx_") }!!.toList()
        assertTrue(files.isNotEmpty())
        for (f in files) {
            val tags = tag.findAll(f.readText()).map { it.value }.toList()
            assertTrue("${f.name}: không có objectAnimator", tags.isNotEmpty())
            for (t in tags) {
                val duration = attr(t, "duration")!!.toInt()
                val passes = (attr(t, "repeatCount")?.toInt() ?: 0) + 1
                assertEquals("${f.name}: tổng thời gian", WeatherMotion.TOTAL_MS, duration * passes)
                if (attr(t, "repeatMode") == "reverse") {
                    assertEquals("${f.name}: reverse cần số lượt chẵn", 0, passes % 2)
                }
            }
        }
    }
}
