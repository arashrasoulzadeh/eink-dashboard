package com.arashrasoulzadeh.einkdash.dashboard

import android.content.Context
import android.content.res.Configuration

/** Portrait (4x6) and landscape (6x4) keep separate layouts in SharedPreferences. */
class LayoutStore(context: Context) {
    private val prefs = context.getSharedPreferences("dashboard", Context.MODE_PRIVATE)

    fun load(orientation: Int): DashboardLayout {
        val landscape = orientation == Configuration.ORIENTATION_LANDSCAPE
        val (cols, rows) = if (landscape) 6 to 4 else 4 to 6
        return prefs.getString(key(landscape), null)
            ?.let { DashboardLayout.fromJson(it, cols, rows) }
            ?: defaultLayout(landscape, cols, rows)
    }

    fun save(orientation: Int, layout: DashboardLayout) {
        prefs.edit().putString(key(orientation == Configuration.ORIENTATION_LANDSCAPE), layout.toJson()).apply()
    }

    // v3: the instrument-panel defaults; bumping the key moves existing installs onto them.
    private fun key(landscape: Boolean) = if (landscape) "layout3_landscape" else "layout3_portrait"

    /** First run: page 1 is the panel (clock, latency, weather, pomodoro); page 2 is notes and reading (battery lives in the footer). */
    private fun defaultLayout(landscape: Boolean, cols: Int, rows: Int): DashboardLayout {
        val pages = if (landscape) listOf(
            listOf("clock" to intArrayOf(0, 0, 3, 2), "latency" to intArrayOf(3, 0, 3, 2),
                "weather" to intArrayOf(0, 2, 3, 2), "pomodoro" to intArrayOf(3, 2, 3, 2)),
            listOf("notes" to intArrayOf(0, 0, 4, 4), "reading" to intArrayOf(4, 0, 2, 4)),
        ) else listOf(
            listOf("clock" to intArrayOf(0, 0, 4, 2), "latency" to intArrayOf(0, 2, 4, 2),
                "weather" to intArrayOf(0, 4, 2, 2), "pomodoro" to intArrayOf(2, 4, 2, 2)),
            listOf("notes" to intArrayOf(0, 0, 4, 4), "reading" to intArrayOf(0, 4, 4, 2)),
        )
        var id = 1L
        return DashboardLayout(cols, rows, pages.map { spots ->
            Page(cols, rows).apply { spots.forEach { (type, g) -> tryUpdate(Placement(id++, type, g[0], g[1], g[2], g[3])) } }
        }.toMutableList())
    }
}
