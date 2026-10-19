package com.arashrasoulzadeh.einkdash.settings

import android.content.Context

/** App-wide preferences: language override, always-on display, frontlight dimming. */
class AppPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("app", Context.MODE_PRIVATE)

    /** "system", "en" or "fa". */
    var language: String
        get() = prefs.getString("language", "system")!!
        set(v) = prefs.edit().putString("language", v).apply()

    var keepScreenOn: Boolean
        get() = prefs.getBoolean("keepScreenOn", true)
        set(v) = prefs.edit().putBoolean("keepScreenOn", v).apply()

    /** Minutes without a touch before the frontlight goes dark; 0 = never. */
    var dimAfterMin: Int
        get() = prefs.getInt("dimAfterMin", 2)
        set(v) = prefs.edit().putInt("dimAfterMin", v).apply()

    companion object {
        val DIM_CHOICES = intArrayOf(0, 1, 2, 5, 10)
    }
}
