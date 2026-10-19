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

    private fun key(landscape: Boolean) = if (landscape) "layout_landscape" else "layout_portrait"

    /** First run: one page with every v1 widget. */
    private fun defaultLayout(landscape: Boolean, cols: Int, rows: Int): DashboardLayout {
        val spots = if (landscape) listOf(
            "clock" to intArrayOf(0, 0, 3, 2), "weather" to intArrayOf(3, 0, 3, 2),
            "battery" to intArrayOf(0, 2, 2, 1), "latency" to intArrayOf(0, 3, 2, 1),
            "reading" to intArrayOf(2, 2, 2, 2), "notes" to intArrayOf(4, 2, 2, 1),
            "pomodoro" to intArrayOf(4, 3, 2, 1),
        ) else listOf(
            "clock" to intArrayOf(0, 0, 4, 2), "weather" to intArrayOf(0, 2, 2, 2),
            "battery" to intArrayOf(2, 2, 2, 1), "latency" to intArrayOf(2, 3, 2, 1),
            "reading" to intArrayOf(0, 4, 2, 2), "notes" to intArrayOf(2, 4, 2, 1),
            "pomodoro" to intArrayOf(2, 5, 2, 1),
        )
        val page = Page(cols, rows)
        spots.forEachIndexed { i, (type, g) -> page.tryUpdate(Placement(i + 1L, type, g[0], g[1], g[2], g[3])) }
        return DashboardLayout(cols, rows, mutableListOf(page))
    }
}
