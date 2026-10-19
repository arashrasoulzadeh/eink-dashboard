package com.arashrasoulzadeh.einkdash.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import com.arashrasoulzadeh.einkdash.settings.AppPrefs

/** Brightness, rotation and Wi-Fi, done the most direct way each Android version allows. */
object DeviceControls {

    /** Applies the saved brightness to this window and, when permitted, to the whole device (the frontlight). */
    fun applyBrightness(activity: Activity) {
        val b = AppPrefs(activity).brightness
        activity.window.attributes = activity.window.attributes.apply {
            screenBrightness = if (b < 0) WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE else b.coerceAtLeast(0.01f)
        }
        if (b >= 0 && canWriteSystem(activity)) runCatching {
            val cr = activity.contentResolver
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, (b * 255).toInt().coerceIn(1, 255))
        }
    }

    fun stepBrightness(activity: Activity, delta: Float) {
        val p = AppPrefs(activity)
        val cur = if (p.brightness < 0) 0.5f else p.brightness
        p.brightness = ((Math.round((cur + delta) * 10)) / 10f).coerceIn(0f, 1f)
        applyBrightness(activity)
    }

    private fun canWriteSystem(ctx: Context) = Build.VERSION.SDK_INT < 23 || Settings.System.canWrite(ctx)

    /** On Android 6+ changing the device brightness needs a one-time grant. */
    fun requestSystemWrite(activity: Activity) {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.System.canWrite(activity)) runCatching {
            activity.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, android.net.Uri.parse("package:" + activity.packageName)))
        }
    }

    fun applyOrientation(activity: Activity) {
        activity.requestedOrientation = when (AppPrefs(activity).orientation) {
            "portrait" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "landscape" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    /** Flips between portrait and landscape and remembers the choice. */
    fun rotate(activity: Activity) {
        val landscape = activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        AppPrefs(activity).orientation = if (landscape) "portrait" else "landscape"
        applyOrientation(activity)
    }

    @Suppress("DEPRECATION")
    fun wifiOn(ctx: Context) = (ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled

    /** Android 10+ no longer lets apps switch Wi-Fi; there it opens the system Wi-Fi panel. */
    fun toggleWifi(activity: Activity) {
        if (Build.VERSION.SDK_INT >= 29) {
            activity.startActivity(Intent(Settings.Panel.ACTION_WIFI))
            return
        }
        @Suppress("DEPRECATION")
        (activity.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled = !wifiOn(activity)
    }

    /** Skip network work when there is no connection: it would only fail and wake the radio. */
    fun online(ctx: Context): Boolean {
        @Suppress("DEPRECATION")
        return (ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo?.isConnected == true
    }

    /** System screen-off timeout in ms (what the device does on its own when we don't keep it on). */
    fun screenTimeoutMs(ctx: Context): Long =
        runCatching { Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT).toLong() }.getOrDefault(60_000L)
}
