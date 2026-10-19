package com.arashrasoulzadeh.einkdash.widgets

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.util.Bg
import com.arashrasoulzadeh.einkdash.util.FitText
import com.arashrasoulzadeh.einkdash.util.Ui
import com.arashrasoulzadeh.einkdash.weather.WeatherCodes
import com.arashrasoulzadeh.einkdash.weather.WeatherSettingsActivity
import com.arashrasoulzadeh.einkdash.weather.WeatherStore
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

/** Big current temperature with condition and range beside it, then three day columns. */
class WeatherWidget : DashboardWidget {
    override val type = "weather"
    override val titleRes = R.string.widget_weather
    override val defaultW = 2
    override val defaultH = 2

    override fun createView(context: Context): WidgetView = object : WidgetView(context, titleRes) {
        val store = WeatherStore(context)
        val temp = FitText(context, Ui.light).apply { gravity = Gravity.CENTER_VERTICAL }
        val cond = Ui.text(context, 14f, Ui.bold).apply { maxLines = 2 }
        val range = Ui.text(context, 13f).apply { setSingleLine() }
        val days = LinearLayout(context)
        var fetching = false

        init {
            setOnClickListener { open(context) }
            fill(LinearLayout(context).apply {
                addView(temp, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding((8 * dp).toInt(), 0, 0, 0)
                    addView(cond); addView(range)
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
            }, 1.3f)
            addView(Ui.rule(context))
            fill(days, 1f)
        }

        override fun refresh() {
            label(R.string.widget_weather, store.placeName?.substringBefore(","))
            if (!store.hasLocation) {
                temp.visibility = GONE; cond.setText(R.string.weather_tap_setup); range.text = ""; days.removeAllViews()
                return
            }
            temp.visibility = VISIBLE
            render(store.cache)
            if (!fetching && store.isStale(System.currentTimeMillis()) && com.arashrasoulzadeh.einkdash.util.DeviceControls.online(context)) {
                fetching = true
                Bg.run({ store.fetch() }) { fetching = false; changed() }
            }
        }

        private fun render(json: String?) {
            val o = json?.let { runCatching { JSONObject(it) }.getOrNull() }
            if (o == null) { temp.text = "…"; cond.text = ""; range.text = ""; return }
            val cur = o.getJSONObject("current")
            val d = o.getJSONObject("daily")
            temp.text = num("${cur.getDouble("temperature_2m").roundToInt()}°")
            cond.setText(WeatherCodes.label(cur.getInt("weather_code")))
            val hi = d.getJSONArray("temperature_2m_max"); val lo = d.getJSONArray("temperature_2m_min")
            range.text = num("${hi.getDouble(0).roundToInt()}° / ${lo.getDouble(0).roundToInt()}°")

            val locale = context.resources.configuration.locale
            val parse = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dayName = SimpleDateFormat("EEE", locale)
            val times = d.getJSONArray("time")
            days.removeAllViews()
            for (i in 0 until times.length()) {
                val name = runCatching { dayName.format(parse.parse(times.getString(i))!!) }.getOrDefault("")
                days.addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    addView(Ui.text(context, 12f, Ui.bold).apply { text = name.uppercase(locale) })
                    addView(Ui.text(context, 14f).apply {
                        text = num("${hi.getDouble(i).roundToInt()}° ${lo.getDouble(i).roundToInt()}°")
                    })
                    addView(Ui.text(context, 11f).apply {
                        setText(WeatherCodes.label(d.getJSONArray("weather_code").getInt(i)))
                        setSingleLine(); ellipsize = android.text.TextUtils.TruncateAt.END
                        gravity = Gravity.CENTER
                    })
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
            }
        }
    }

    override fun configure(activity: Activity): Boolean { open(activity); return true }

    private fun open(context: Context) = context.startActivity(Intent(context, WeatherSettingsActivity::class.java))
}
