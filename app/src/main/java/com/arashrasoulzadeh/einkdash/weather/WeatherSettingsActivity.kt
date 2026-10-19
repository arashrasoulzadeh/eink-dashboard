package com.arashrasoulzadeh.einkdash.weather

import android.Manifest
import android.app.Activity
import com.arashrasoulzadeh.einkdash.util.BaseActivity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.eink.Eink
import com.arashrasoulzadeh.einkdash.util.Bg
import com.arashrasoulzadeh.einkdash.util.Ui

/** Pick device location (network-based) or search a city. */
class WeatherSettingsActivity : BaseActivity() {

    private lateinit var store: WeatherStore
    private lateinit var results: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = WeatherStore(this)
        val query = Ui.input(this, R.string.weather_city_hint).apply { setSingleLine() }
        results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val root = Ui.screen(this, R.string.widget_weather)
        root.addView(Ui.text(this, 16f).apply {
            text = getString(R.string.weather_current, store.placeName ?: getString(R.string.weather_none))
        })
        root.addView(Ui.gap(this, 16f))
        root.addView(Ui.button(this, R.string.weather_use_device) { useDevice() })
        root.addView(Ui.gap(this, 24f))
        root.addView(Ui.label(this, R.string.weather_search))
        root.addView(Ui.gap(this, 8f))
        root.addView(LinearLayout(this).apply {
            gravity = android.view.Gravity.CENTER_VERTICAL
            addView(query, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(Ui.button(context, R.string.weather_search) { search(query.text.toString()) },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = Ui.dp(context, 8f) })
        })
        root.addView(Ui.gap(this, 12f))
        root.addView(results)
    }

    private fun search(q: String) {
        if (q.isBlank()) return
        val lang = if (resources.configuration.locale.language == "fa") "fa" else "en"
        Bg.run({ WeatherStore.search(q.trim(), lang) }) { r ->
            results.removeAllViews()
            val places = r.getOrElse { toast(R.string.network_error); return@run }
            if (places.isEmpty()) toast(R.string.weather_no_results)
            places.forEach { p ->
                results.addView(Ui.button(this, p.label) { store.setCity(p.label, p.lat, p.lon); finish() })
                results.addView(Ui.gap(this, 6f))
            }
            Eink.fullRefresh(window.decorView)
        }
    }

    private fun useDevice() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            return
        }
        if (store.deviceLocation() == null) toast(R.string.weather_no_location)
        else { store.setUseDevice(getString(R.string.weather_device)); finish() }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) useDevice()
        else toast(R.string.weather_no_location)
    }

    private fun toast(res: Int) = Toast.makeText(this, res, Toast.LENGTH_LONG).show()
}
