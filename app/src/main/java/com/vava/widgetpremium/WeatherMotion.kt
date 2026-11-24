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
 * chính, bật màn hình, widget được cập nhật), chạy [TOTAL_MS] rồi đứng yên ở khung hình tĩnh, không tốn thêm pin.
 * Launcher không báo cho app biết khi người dùng vuốt giữa các trang, nên thời lượng đủ dài (khoảng 1 phút) để
 * vẫn thấy chuyển động sau vài lần vuốt.
 */
object WeatherMotion {
    /** Thời lượng mỗi lần chạy; mọi animator trong res/animator/fx_* đều đúng bằng giá trị này (có test). */
    const val TOTAL_MS = 60_000

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

    /**
     * Lề (dp) để lớp hiệu ứng nằm gọn trong hình chữ nhật bo góc của nền: góc vuông cách mép `inset`
     * vẫn nằm trong cung bo khi inset >= r * (1 - 1/√2) ≈ 0,293 r. Dùng 0,3 r làm tròn lên.
     */
    fun insetDp(cornerDp: Int): Int = ceil(0.3 * cornerDp.coerceAtLeast(0)).toInt()
}

/** Gắn lớp hiệu ứng vào RemoteViews: hiện đúng một ProgressBar theo thời tiết, ẩn các cái còn lại. */
object WidgetMotion {
    private fun viewId(m: Motion): Int = when (m) {
        Motion.SUN -> R.id.fx_sun
        Motion.STARS -> R.id.fx_stars
        Motion.CLOUDS -> R.id.fx_clouds
        Motion.RAIN -> R.id.fx_rain
        Motion.STORM -> R.id.fx_storm
        Motion.SNOW -> R.id.fx_snow
        Motion.FOG -> R.id.fx_fog
    }

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
        for (m in Motion.values()) {
            val id = viewId(m)
            // Ẩn rồi hiện lại ngay trong cùng một lần cập nhật để hoạt ảnh chạy lại từ đầu mỗi khi widget được vẽ lại.
            if (m == motion) views.setViewVisibility(id, View.GONE)
            views.setViewVisibility(id, if (m == motion) View.VISIBLE else View.GONE)
        }
    }
}
