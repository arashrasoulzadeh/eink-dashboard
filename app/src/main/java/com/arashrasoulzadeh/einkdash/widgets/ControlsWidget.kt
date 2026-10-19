package com.arashrasoulzadeh.einkdash.widgets

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.LinearLayout
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.eink.Eink
import com.arashrasoulzadeh.einkdash.settings.AppPrefs
import com.arashrasoulzadeh.einkdash.util.DeviceControls
import com.arashrasoulzadeh.einkdash.util.Ui

/** Quick settings: frontlight, Wi-Fi, rotation, screen mode and a way into Android settings. */
class ControlsWidget : DashboardWidget {
    override val type = "controls"
    override val titleRes = R.string.widget_controls
    override val defaultW = 2
    override val defaultH = 2

    override fun createView(context: Context): WidgetView = object : WidgetView(context, titleRes) {
        val activity = context as Activity
        val prefs = AppPrefs(context)
        val light = Ui.text(context, 14f, Ui.bold).apply { gravity = android.view.Gravity.CENTER }
        val wifi = Ui.button(context, "") { DeviceControls.toggleWifi(activity); later() }
        val screen = Ui.button(context, "") { prefs.keepScreenOn = !prefs.keepScreenOn; activity.recreate() }

        init {
            fun row(vararg v: android.view.View) = fill(Ui.row(context, *v, gapDp = 6f).apply {
                (0 until childCount).forEach {
                    (getChildAt(it).layoutParams as LinearLayout.LayoutParams).height = LinearLayout.LayoutParams.MATCH_PARENT
                    (getChildAt(it) as? android.widget.TextView)?.apply { textSize = 13f; setSingleLine(); minHeight = 0; minimumHeight = 0 }
                }
                setPadding(0, (3 * dp).toInt(), 0, (3 * dp).toInt())
            })
            row(Ui.button(context, "−") { step(-0.1f) }, light, Ui.button(context, "+") { step(0.1f) })
            row(wifi, Ui.button(context, R.string.ctl_rotate) { DeviceControls.rotate(activity) })
            row(screen, Ui.button(context, R.string.ctl_system) { context.startActivity(Intent(Settings.ACTION_SETTINGS)) })
        }

        private fun step(d: Float) {
            DeviceControls.requestSystemWrite(activity)
            DeviceControls.stepBrightness(activity, d)
            changed()
        }

        /** Wi-Fi state changes a moment after the request. */
        private fun later() = postDelayed({ changed() }, 1500)

        override fun refresh() {
            val b = prefs.brightness
            light.text = if (b < 0) context.getString(R.string.ctl_light_auto) else context.getString(R.string.ctl_light, num("${Math.round(b * 100)}%"))
            wifi.text = context.getString(if (DeviceControls.wifiOn(context)) R.string.ctl_wifi_on else R.string.ctl_wifi_off)
            screen.setText(if (prefs.keepScreenOn) R.string.ctl_always_on else R.string.ctl_saver)
        }
    }
}
