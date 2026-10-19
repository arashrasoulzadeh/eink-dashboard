package com.arashrasoulzadeh.einkdash.reading

import android.content.Context
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Minutes read per day, recorded when a focus session finishes. */
class ReadingStats(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("readingStats", Context.MODE_PRIVATE)
    private val fmt = SimpleDateFormat("yyyyMMdd", Locale.US)

    private fun days(): JSONObject = runCatching { JSONObject(prefs.getString("days", "{}")!!) }.getOrDefault(JSONObject())

    fun add(minutes: Int, cal: Calendar = Calendar.getInstance()) {
        val d = days()
        val key = fmt.format(cal.time)
        d.put(key, d.optInt(key) + minutes)
        prefs.edit().putString("days", d.toString()).apply()
    }

    /** Last 7 days, oldest first, ending today. */
    fun week(today: Calendar = Calendar.getInstance()): List<Int> {
        val d = days()
        return (6 downTo 0).map { back ->
            val c = today.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, -back)
            d.optInt(fmt.format(c.time))
        }
    }

    /** Consecutive days with reading, counting back from today (or yesterday if today has none yet). */
    fun streak(today: Calendar = Calendar.getInstance()): Int {
        val d = days()
        val c = today.clone() as Calendar
        if (d.optInt(fmt.format(c.time)) == 0) c.add(Calendar.DAY_OF_YEAR, -1)
        var n = 0
        while (d.optInt(fmt.format(c.time)) > 0) { n++; c.add(Calendar.DAY_OF_YEAR, -1) }
        return n
    }
}
