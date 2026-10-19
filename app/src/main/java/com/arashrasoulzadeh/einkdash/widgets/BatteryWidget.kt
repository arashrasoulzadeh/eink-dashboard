package com.arashrasoulzadeh.einkdash.widgets

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.BatteryManager
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.util.FitText
import com.arashrasoulzadeh.einkdash.util.Ui

class BatteryWidget : DashboardWidget {
    override val type = "battery"
    override val titleRes = R.string.widget_battery
    override val defaultW = 2
    override val defaultH = 1

    override fun createView(context: Context): WidgetView = object : WidgetView(context, titleRes) {
        val battery = fill(FitText(context, Ui.light, 64f))
        val wifi = line(14f)

        override fun refresh() {
            // Sticky broadcast: reading it needs no receiver to stay registered.
            val b = context.applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = b?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = b?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val status = b?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val pct = if (level >= 0) "${level * 100 / scale}%" else "?"
            battery.text = num(pct) + if (charging) " · " + context.getString(R.string.charging) else ""

            @Suppress("DEPRECATION")
            val net = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo
            @Suppress("DEPRECATION")
            val onWifi = net != null && net.isConnected && net.type == ConnectivityManager.TYPE_WIFI
            wifi.setText(if (onWifi) R.string.wifi_on else R.string.wifi_off)
        }
    }
}
