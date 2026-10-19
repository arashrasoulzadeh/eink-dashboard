package com.arashrasoulzadeh.einkdash.face

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.arashrasoulzadeh.einkdash.settings.AppPrefs
import java.util.Calendar

/** Time-of-day theme: night window from settings, plus charging state for the bedside clock. */
object Night {
    fun isNight(ctx: Context): Boolean {
        val p = AppPrefs(ctx)
        val start = p.nightStart
        if (start < 0) return false
        return inWindow(Calendar.getInstance().get(Calendar.HOUR_OF_DAY), start, p.nightEnd)
    }

    /** Hour [h] inside [start, end), wrapping past midnight when start > end. */
    fun inWindow(h: Int, start: Int, end: Int) = if (start > end) h >= start || h < end else h in start until end

    /**
     * Which face should be on screen right now, if any: the morning summary (once a day,
     * marked shown here) or the bedside clock (night + charging).
     */
    fun dueFace(ctx: Context): String? {
        val p = AppPrefs(ctx)
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.YEAR) * 10000 + (cal.get(Calendar.MONTH) + 1) * 100 + cal.get(Calendar.DAY_OF_MONTH)
        if (p.summaryHour >= 0 && cal.get(Calendar.HOUR_OF_DAY) == p.summaryHour && p.summaryShownDay != day) {
            p.summaryShownDay = day
            return FaceActivity.SUMMARY
        }
        return if (p.nightClock && isNight(ctx) && isCharging(ctx)) FaceActivity.CLOCK else null
    }

    fun isCharging(ctx: Context): Boolean {
        val b = ctx.applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = b?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        return plugged != 0
    }

    /** Night text is larger for reading across the room. */
    const val FONT_SCALE = 1.2f

    /** Frontlight level at night when not dimmed (0..1). */
    const val BRIGHTNESS = 0.03f
}
