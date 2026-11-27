package com.vava.widgetpremium

import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.RemoteViews
import kotlin.math.ceil

/** Hiệu ứng chuyển động ngắn phủ lên nền bầu trời của widget. Mỗi loại ứng với một tài nguyên `fx_<tên>`. */
enum class Motion { SUN, STARS, CLOUDS, RAIN, STORM, SNOW, FOG }

/**
 * Logic thuần (có test) cho hiệu ứng "bán động": RemoteViews không phát được GIF hay chạy mã, nên mỗi hiệu ứng
 * là một AnimatedVectorDrawable hữu hạn gắn vào ProgressBar. Launcher chạy hoạt ảnh khi view hiện ra (mở màn hình
 * chính, bật màn hình, widget được cập nhật), chạy hết thời lượng rồi đứng yên ở khung hình tĩnh, không tốn thêm pin.
 * Mưa kính và chớp sáng chạy một lượt [RAIN_MS] (4 giây); các hiệu ứng còn lại chạy [LONG_MS] (1 phút).
 * Launcher không báo cho app biết khi người dùng vuốt giữa các trang, nên không thể chạy "đúng lúc vuốt tới và dừng lại":
 * hiệu ứng chạy lại mỗi khi view được gắn lại hoặc widget được vẽ lại.
 */
object WeatherMotion {
    /** Thời lượng một lượt của mưa kính và chớp sáng (giọt rơi, vệt nước rồi đứng yên). */
    const val RAIN_MS = 4_000

    /** Thời lượng mỗi lần chạy của các hiệu ứng còn lại (nắng, sao, mây, tuyết, sương). */
    const val LONG_MS = 60_000

    /** Thời lượng của lớp `fx_<tên>`; mọi animator của lớp đó đều đúng bằng giá trị này (có test). */
    fun durationMs(layer: String): Int = if (layer == "rain" || layer == "flash") RAIN_MS else LONG_MS

    /** Loại hiệu ứng cho bầu trời hiện tại; null nghĩa là không chạy gì. */
    fun choose(sky: Sky?, enabled: Boolean, powerSave: Boolean, animationsOn: Boolean): Motion? {
        if (sky == null || !enabled || powerSave || !animationsOn) return null
        return when (sky) {
            Sky.CLEAR_DAY, Sky.PARTLY_DAY -> Motion.SUN
            Sky.CLEAR_NIGHT, Sky.PARTLY_NIGHT -> Motion.STARS
            Sky.CLOUDY -> Motion.CLOUDS
            Sky.FOG -> Motion.FOG
            Sky.RAIN -> Motion.RAIN
            Sky.THUNDER -> Motion.STORM
            Sky.SNOW -> Motion.SNOW
        }
    }

    /** Các lớp hiệu ứng (tên tài nguyên `fx_<tên>`) hiện cùng lúc cho một loại; giông là mưa kính cộng chớp sáng. */
    fun layers(motion: Motion): List<String> = when (motion) {
        Motion.STORM -> listOf("rain", "flash")
        else -> listOf(motion.name.lowercase())
    }

    /**
     * Lề (dp) để lớp hiệu ứng nằm gọn trong hình chữ nhật bo góc của nền: góc vuông cách mép `inset`
     * vẫn nằm trong cung bo khi inset >= r * (1 - 1/√2) ≈ 0,293 r. Dùng 0,3 r làm tròn lên.
     */
    fun insetDp(cornerDp: Int): Int = ceil(0.3 * cornerDp.coerceAtLeast(0)).toInt()
}

/** Gắn lớp hiệu ứng vào RemoteViews: hiện đúng một ProgressBar theo thời tiết, ẩn các cái còn lại. */
object WidgetMotion {
    private val LAYER_IDS = mapOf(
        "sun" to R.id.fx_sun, "stars" to R.id.fx_stars, "clouds" to R.id.fx_clouds, "rain" to R.id.fx_rain,
        "flash" to R.id.fx_flash, "snow" to R.id.fx_snow, "fog" to R.id.fx_fog
    )

    /** Hiệu ứng sẽ chạy cho bầu trời này (null nếu tắt, tiết kiệm pin hoặc hệ thống tắt hoạt ảnh). */
    fun resolve(context: Context, style: WidgetStyle, sky: Sky?): Motion? {
        val powerSave = (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode == true
        val animationScale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        return WeatherMotion.choose(sky, style.weatherMotion, powerSave, animationScale > 0f)
    }

    fun apply(context: Context, views: RemoteViews, style: WidgetStyle, motion: Motion?) {
        val inset = (WeatherMotion.insetDp(style.cornerDp) * context.resources.displayMetrics.density).toInt()
        views.setViewPadding(R.id.fx_box, inset, inset, inset, inset)
        views.setViewVisibility(R.id.fx_box, if (motion != null) View.VISIBLE else View.GONE)
        val active = motion?.let { m -> WeatherMotion.layers(m).mapNotNull { LAYER_IDS[it] } }.orEmpty()
        for (id in LAYER_IDS.values) {
            // Ẩn rồi hiện lại ngay trong cùng một lần cập nhật để hoạt ảnh chạy lại từ đầu mỗi khi widget được vẽ lại.
            if (id in active) views.setViewVisibility(id, View.GONE)
            views.setViewVisibility(id, if (id in active) View.VISIBLE else View.GONE)
        }
    }
}
