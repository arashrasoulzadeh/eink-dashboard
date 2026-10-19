package com.arashrasoulzadeh.einkdash.widgets

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.util.Bg
import com.arashrasoulzadeh.einkdash.weather.WeatherCodes
import com.arashrasoulzadeh.einkdash.weather.WeatherSettingsActivity
import com.arashrasoulzadeh.einkdash.weather.WeatherStore
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

class WeatherWidget : DashboardWidget {
    override val type = "weather"
    override val titleRes = R.string.widget_weather
    override val defaultW = 2
    override val defaultH = 2

    override fun createView(context: Context): WidgetView = object : WidgetView(context, titleRes) {
        val store = WeatherStore(context)
        val now = figure(40f)
        val place = line(13f)
        val days = line(14f).apply { setLineSpacing(0f, 1.3f); setPadding(0, (10 * dp).toInt(), 0, 0) }
        var fetching = false

        init { setOnClickListener { open(context) } }

        override fun refresh() {
            if (!store.hasLocation) {
                place.show(null); now.show(null); days.show(context.getString(R.string.weather_tap_setup))
                return
            }
            place.show(store.placeName)
            render(store.cache)
            if (!fetching && store.isStale(System.currentTimeMillis())) {
                fetching = true
                Bg.run({ store.fetch() }) { fetching = false; changed() }
            }
        }

        private fun render(json: String?) {
            val o = json?.let { runCatching { JSONObject(it) }.getOrNull() }
            if (o == null) { now.text = "…"; days.text = ""; return }
            val cur = o.getJSONObject("current")
            now.text = num("${cur.getDouble("temperature_2m").roundToInt()}°") + "  " +
                context.getString(WeatherCodes.label(cur.getInt("weather_code")))
            val d = o.getJSONObject("daily")
            val locale = context.resources.configuration.locale
            val parse = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dayName = SimpleDateFormat("EEE", locale)
            val times = d.getJSONArray("time")
            days.text = (0 until times.length()).joinToString("\n") { i ->
                val name = runCatching { dayName.format(parse.parse(times.getString(i))!!) }.getOrDefault("")
                val hi = d.getJSONArray("temperature_2m_max").getDouble(i).roundToInt()
                val lo = d.getJSONArray("temperature_2m_min").getDouble(i).roundToInt()
                "$name  " + num("$hi° / $lo°") + "  " + context.getString(WeatherCodes.label(d.getJSONArray("weather_code").getInt(i)))
            }
        }
    }

    override fun configure(activity: Activity): Boolean { open(activity); return true }

    private fun open(context: Context) = context.startActivity(Intent(context, WeatherSettingsActivity::class.java))
}
