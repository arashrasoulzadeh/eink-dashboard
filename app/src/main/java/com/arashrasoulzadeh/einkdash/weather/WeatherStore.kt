package com.arashrasoulzadeh.einkdash.weather

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import com.arashrasoulzadeh.einkdash.util.Http
import org.json.JSONObject

/** Location choice plus the last Open-Meteo forecast, cached so the widget works offline. */
class WeatherStore(context: Context) {
    private val ctx = context.applicationContext
    private val prefs = ctx.getSharedPreferences("weather", Context.MODE_PRIVATE)

    val hasLocation get() = prefs.contains("lat") || useDevice
    val useDevice get() = prefs.getBoolean("device", false)
    val placeName: String? get() = prefs.getString("name", null)
    val cache: String? get() = prefs.getString("cache", null)
    val fetchedAt get() = prefs.getLong("fetchedAt", 0)
    val attemptedAt get() = prefs.getLong("attemptedAt", 0)

    fun setCity(name: String, lat: Double, lon: Double) {
        prefs.edit().putString("name", name).putFloat("lat", lat.toFloat()).putFloat("lon", lon.toFloat())
            .putBoolean("device", false).remove("cache").putLong("fetchedAt", 0).putLong("attemptedAt", 0).apply()
    }

    fun setUseDevice(name: String) {
        prefs.edit().putBoolean("device", true).putString("name", name).remove("cache")
            .putLong("fetchedAt", 0).putLong("attemptedAt", 0).apply()
    }

    /** Due for a fetch: 2 h after success, or 15 min after a failed attempt. */
    fun isStale(now: Long) = now - fetchedAt > 2 * 3600_000L && now - attemptedAt > 15 * 60_000L

    /** Blocking; call from [com.arashrasoulzadeh.einkdash.util.Bg]. */
    fun fetch() {
        prefs.edit().putLong("attemptedAt", System.currentTimeMillis()).apply()
        if (useDevice) deviceLocation()?.let { (lat, lon) ->
            prefs.edit().putFloat("lat", lat.toFloat()).putFloat("lon", lon.toFloat()).apply()
        }
        if (!prefs.contains("lat")) error("no location")
        val lat = prefs.getFloat("lat", 0f)
        val lon = prefs.getFloat("lon", 0f)
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,weather_code&daily=weather_code,temperature_2m_max,temperature_2m_min" +
            "&timezone=auto&forecast_days=3"
        val r = Http.getAllowingHttpFallback(url)
        if (r.code != 200) error("HTTP ${r.code}")
        JSONObject(r.body) // validate before caching
        prefs.edit().putString("cache", r.body).putLong("fetchedAt", System.currentTimeMillis()).apply()
    }

    @SuppressLint("MissingPermission")
    fun deviceLocation(): Pair<Double, Double>? {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val loc = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time } ?: return null
        return loc.latitude to loc.longitude
    }

    companion object {
        class Place(val label: String, val lat: Double, val lon: Double)

        /** Blocking city search via Open-Meteo geocoding. */
        fun search(query: String, language: String): List<Place> {
            val q = java.net.URLEncoder.encode(query, "UTF-8")
            val r = Http.getAllowingHttpFallback("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=8&language=$language")
            val results = JSONObject(r.body).optJSONArray("results") ?: return emptyList()
            return List(results.length()) { i ->
                val o = results.getJSONObject(i)
                val label = listOf(o.optString("name"), o.optString("admin1"), o.optString("country"))
                    .filter { it.isNotEmpty() }.distinct().joinToString(", ")
                Place(label, o.getDouble("latitude"), o.getDouble("longitude"))
            }
        }
    }
}
