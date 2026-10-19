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

    // v7: notes first, reading stats, quick settings; bumping the key moves existing installs onto them.
    private fun key(landscape: Boolean) = if (landscape) "layout8_landscape" else "layout7_portrait"

    /** First run: one page; spots below the clock are stacks you swipe through. */
    private fun defaultLayout(landscape: Boolean, cols: Int, rows: Int): DashboardLayout {
        val spots = if (landscape) listOf(
            // Landscape is short: the clock gets a full-height column so its figure can stay large.
            listOf("clock") to intArrayOf(0, 0, 2, 4), listOf("notes", "latency") to intArrayOf(2, 0, 4, 2),
            listOf("weather", "readingStats", "reading") to intArrayOf(2, 2, 2, 2), listOf("pomodoro", "controls", "battery") to intArrayOf(4, 2, 2, 2),
        ) else listOf(
            listOf("clock") to intArrayOf(0, 0, 4, 2), listOf("notes", "latency") to intArrayOf(0, 2, 4, 2),
            listOf("weather", "readingStats", "reading") to intArrayOf(0, 4, 2, 2), listOf("pomodoro", "controls", "battery") to intArrayOf(2, 4, 2, 2),
        )
        val page = Page(cols, rows)
        spots.forEachIndexed { i, (stack, g) -> page.tryUpdate(Placement(i + 1L, stack, g[0], g[1], g[2], g[3])) }
        return DashboardLayout(cols, rows, mutableListOf(page))
    }
}
