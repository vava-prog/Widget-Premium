package com.vava.widgetpremium

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import java.util.Locale
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Logic thuần (có test) cho vị trí của máy. */
object LocationRules {
    /** Làm tròn 2 chữ số thập phân (khoảng 1 km): đủ cho dự báo, không lộ vị trí chính xác. */
    fun round2(value: Double): Double = Math.round(value * 100.0) / 100.0

    fun isValid(lat: Double, lon: Double): Boolean =
        lat in -90.0..90.0 && lon in -180.0..180.0 && !(lat == 0.0 && lon == 0.0)
}

/**
 * Lấy vị trí gần đúng MỘT LẦN khi người dùng bấm nút. Không theo dõi vị trí, không chạy nền,
 * không dùng quyền vị trí chính xác. Cần quyền ACCESS_COARSE_LOCATION do người dùng cấp.
 */
object DeviceLocation {
    private const val TIMEOUT_MS = 15_000L

    fun hasPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Trả về thành phố ứng với vị trí máy, hoặc null nếu tắt định vị, hết thời gian hoặc không lấy được. */
    suspend fun currentCity(context: Context): City? {
        val loc = withTimeoutOrNull(TIMEOUT_MS) { fetch(context) } ?: return null
        val lat = LocationRules.round2(loc.latitude)
        val lon = LocationRules.round2(loc.longitude)
        if (!LocationRules.isValid(lat, lon)) return null
        val (name, admin, country) = withContext(Dispatchers.IO) { placeName(context, lat, lon) }
        return City(name = name, admin = admin, country = country, latitude = lat, longitude = lon)
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetch(context: Context): Location? {
        if (!hasPermission(context)) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val provider = when {
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            manager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER) -> LocationManager.PASSIVE_PROVIDER
            else -> return null
        }
        val last = runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        if (last != null && System.currentTimeMillis() - last.time < 30 * 60 * 1000L) return last
        return suspendCancellableCoroutine { cont ->
            val signal = CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                manager.getCurrentLocation(provider, signal, Executors.newSingleThreadExecutor()) { result ->
                    if (cont.isActive) cont.resume(result ?: last)
                }
            } else {
                @Suppress("DEPRECATION")
                manager.requestSingleUpdate(provider, { location -> if (cont.isActive) cont.resume(location) }, null)
            }
        }
    }

    /** Tên nơi từ bộ dịch tọa độ của hệ thống nếu có; không có thì dùng tên mặc định. Không gọi máy chủ của dự án. */
    @Suppress("DEPRECATION")
    private fun placeName(context: Context, lat: Double, lon: Double): Triple<String, String, String> {
        val fallback = Triple(context.getString(R.string.location_default_name), "", "")
        if (!Geocoder.isPresent()) return fallback
        return try {
            val a = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lon, 1)?.firstOrNull() ?: return fallback
            val name = a.locality ?: a.subAdminArea ?: a.adminArea ?: return fallback
            Triple(name, a.adminArea.orEmpty().takeIf { it != name }.orEmpty(), a.countryName.orEmpty())
        } catch (e: Exception) {
            fallback
        }
    }
}
