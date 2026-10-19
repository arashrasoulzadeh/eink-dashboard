package com.arashrasoulzadeh.einkdash.dashboard

import android.content.Context
import android.content.res.Configuration
import com.arashrasoulzadeh.einkdash.settings.AppPrefs

/**
 * Portrait (4x6) and landscape (6x4) keep separate layouts in SharedPreferences.
 * Large print has its own pair, so switching it on and off never disturbs the normal layout.
 */
class LayoutStore(context: Context) {
    private val prefs = context.getSharedPreferences("dashboard", Context.MODE_PRIVATE)
    private val large = AppPrefs(context).largePrint

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

    // v9: calendar and quote join the stacks; bumping the key moves existing installs onto the new defaults.
    private fun key(landscape: Boolean) = (if (large) "large1_" else "layout9_") + if (landscape) "landscape" else "portrait"

    private fun defaultLayout(landscape: Boolean, cols: Int, rows: Int): DashboardLayout {
        val notes = listOf("notes", "calendar", "latency")
        val info = listOf("weather", "quote", "readingStats", "reading")
        val focus = listOf("pomodoro", "controls", "battery")
        val spots = when {
            // Large print: four big spots, every widget reachable by swiping.
            large && landscape -> listOf(
                listOf("clock") to intArrayOf(0, 0, 3, 2), notes + "quote" to intArrayOf(3, 0, 3, 2),
                info - "quote" to intArrayOf(0, 2, 3, 2), focus to intArrayOf(3, 2, 3, 2))
            large -> listOf(
                listOf("clock") to intArrayOf(0, 0, 4, 2), notes to intArrayOf(0, 2, 4, 2),
                info to intArrayOf(0, 4, 2, 2), focus to intArrayOf(2, 4, 2, 2))
            // Landscape is short: the clock gets a full-height column so its figure can stay large.
            landscape -> listOf(
                listOf("clock") to intArrayOf(0, 0, 2, 4), notes to intArrayOf(2, 0, 4, 2),
                info to intArrayOf(2, 2, 2, 2), focus to intArrayOf(4, 2, 2, 2))
            else -> listOf(
                listOf("clock") to intArrayOf(0, 0, 4, 2), notes to intArrayOf(0, 2, 4, 2),
                info to intArrayOf(0, 4, 2, 2), focus to intArrayOf(2, 4, 2, 2))
        }
        val page = Page(cols, rows)
        spots.forEachIndexed { i, (stack, g) -> page.tryUpdate(Placement(i + 1L, stack, g[0], g[1], g[2], g[3])) }
        return DashboardLayout(cols, rows, mutableListOf(page))
    }
}
