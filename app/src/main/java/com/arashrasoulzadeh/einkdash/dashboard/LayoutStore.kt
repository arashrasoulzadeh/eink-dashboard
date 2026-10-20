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
        val (cols, rows) = if (landscape) 6 to 4 else 4 to 8
        return prefs.getString(key(landscape), null)
            ?.let { DashboardLayout.fromJson(it, cols, rows) }
            ?: defaultLayout(landscape, cols, rows)
    }

    fun save(orientation: Int, layout: DashboardLayout) {
        prefs.edit().putString(key(orientation == Configuration.ORIENTATION_LANDSCAPE), layout.toJson()).apply()
    }

    // v10: card design (4x8 portrait grid); bumping the key moves existing installs onto the new defaults.
    private fun key(landscape: Boolean) = (if (large) "large2_" else "layout10_") + if (landscape) "landscape" else "portrait"

    private fun defaultLayout(landscape: Boolean, cols: Int, rows: Int): DashboardLayout {
        // Spots are stacks: swipe to reach the rest. Positions follow the card mockup
        // (logical column 0 is the start side, i.e. the right in Persian).
        val quote = listOf("quote", "clock", "weather")
        val focus = listOf("pomodoro", "controls", "readingStats")
        val month = listOf("calendar", "notes", "latency")
        val habits = listOf("habits", "battery", "reading")
        val spots = when {
            large && landscape -> listOf(
                quote to intArrayOf(0, 0, 3, 2), focus to intArrayOf(0, 2, 3, 2),
                month + habits to intArrayOf(3, 0, 3, 3), listOf("shortcuts") to intArrayOf(3, 3, 3, 1))
            large -> listOf(
                quote to intArrayOf(0, 0, 4, 2), focus to intArrayOf(0, 2, 2, 3), month + habits to intArrayOf(2, 2, 2, 3),
                listOf("shortcuts") to intArrayOf(0, 5, 4, 1), listOf("motto", "weather") to intArrayOf(0, 6, 4, 2))
            landscape -> listOf(
                quote to intArrayOf(0, 0, 2, 2), focus to intArrayOf(0, 2, 2, 2),
                month to intArrayOf(2, 0, 2, 3), listOf("shortcuts") to intArrayOf(2, 3, 2, 1),
                habits to intArrayOf(4, 0, 2, 2), listOf("motto", "weather") to intArrayOf(4, 2, 2, 2))
            else -> listOf(
                quote to intArrayOf(0, 0, 2, 2), focus to intArrayOf(0, 2, 2, 3),
                month to intArrayOf(2, 0, 2, 3), habits to intArrayOf(2, 3, 2, 2),
                listOf("shortcuts") to intArrayOf(0, 5, 4, 1), listOf("motto", "weather") to intArrayOf(0, 6, 4, 2))
        }
        val page = Page(cols, rows)
        spots.forEachIndexed { i, (stack, g) -> page.tryUpdate(Placement(i + 1L, stack, g[0], g[1], g[2], g[3])) }
        return DashboardLayout(cols, rows, mutableListOf(page))
    }
}
