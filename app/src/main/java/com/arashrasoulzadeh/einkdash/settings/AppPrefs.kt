package com.arashrasoulzadeh.einkdash.settings

import android.content.Context
import kotlin.random.Random

/** App-wide preferences. */
class AppPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("app", Context.MODE_PRIVATE)

    /** "system", "en" or "fa". */
    var language: String
        get() = prefs.getString("language", "system")!!
        set(v) = prefs.edit().putString("language", v).apply()

    /** false = battery saver: show the sleep face, then let the device sleep (e-ink keeps the image). */
    var keepScreenOn: Boolean
        get() = prefs.getBoolean("keepScreenOn", true)
        set(v) = prefs.edit().putBoolean("keepScreenOn", v).apply()

    /** Minutes without a touch before the frontlight goes dark; 0 = never. */
    var dimAfterMin: Int
        get() = prefs.getInt("dimAfterMin", 20)
        set(v) = prefs.edit().putInt("dimAfterMin", v).apply()

    /** Show the sleep face (clock, notes, weather) when idle. */
    var sleepFace: Boolean
        get() = prefs.getBoolean("sleepFace", true)
        set(v) = prefs.edit().putBoolean("sleepFace", v).apply()

    /** Hour of the morning summary; -1 = off. */
    var summaryHour: Int
        get() = prefs.getInt("summaryHour", 7)
        set(v) = prefs.edit().putInt("summaryHour", v).apply()

    /** Day (yyyyMMdd) the summary was last shown, so it appears once a day. */
    var summaryShownDay: Int
        get() = prefs.getInt("summaryShownDay", 0)
        set(v) = prefs.edit().putInt("summaryShownDay", v).apply()

    /** Night window start hour; -1 = night theme off. Ends at [nightEnd]. */
    var nightStart: Int
        get() = prefs.getInt("nightStart", 22)
        set(v) = prefs.edit().putInt("nightStart", v).apply()

    var nightEnd: Int
        get() = prefs.getInt("nightEnd", 7)
        set(v) = prefs.edit().putInt("nightEnd", v).apply()

    /** At night while charging, show a single giant clock. */
    var nightClock: Boolean
        get() = prefs.getBoolean("nightClock", true)
        set(v) = prefs.edit().putBoolean("nightClock", v).apply()

    var noteServer: Boolean
        get() = prefs.getBoolean("noteServer", false)
        set(v) = prefs.edit().putBoolean("noteServer", v).apply()

    /** PIN the note page asks for, so others on the Wi-Fi can't add notes. Created on first use. */
    val noteServerPin: String
        get() = prefs.getString("noteServerPin", null) ?: Random.nextInt(1000, 10000).toString().also {
            prefs.edit().putString("noteServerPin", it).apply()
        }

    /** "auto", "portrait" or "landscape". */
    var orientation: String
        get() = prefs.getString("orientation", "auto")!!
        set(v) = prefs.edit().putString("orientation", v).apply()

    /** Chosen brightness 0..1; negative = leave it to the system. */
    var brightness: Float
        get() = prefs.getFloat("brightness", -1f)
        set(v) = prefs.edit().putFloat("brightness", v).apply()

    /** Large print: bigger text and its own simpler layout of four large spots. */
    var largePrint: Boolean
        get() = prefs.getBoolean("largePrint", false)
        set(v) = prefs.edit().putBoolean("largePrint", v).apply()

    companion object {
        val DIM_CHOICES = intArrayOf(0, 1, 2, 5, 10, 20, 30)
        val SUMMARY_CHOICES = intArrayOf(-1, 6, 7, 8, 9)
        val NIGHT_CHOICES = intArrayOf(-1, 21, 22, 23)
    }
}
