package com.arashrasoulzadeh.einkdash.widgets

import com.arashrasoulzadeh.einkdash.R

object WidgetRegistry {
    val all: List<DashboardWidget> = listOf(
        PlaceholderWidget("clock", R.string.widget_clock, 4, 2),
        PlaceholderWidget("weather", R.string.widget_weather, 2, 2),
        PlaceholderWidget("battery", R.string.widget_battery, 2, 1),
        PlaceholderWidget("latency", R.string.widget_latency, 2, 1),
        PlaceholderWidget("reading", R.string.widget_reading, 2, 2),
        PlaceholderWidget("notes", R.string.widget_notes, 2, 1),
        PlaceholderWidget("pomodoro", R.string.widget_pomodoro, 2, 1),
    )

    private val byType = all.associateBy { it.type }

    fun get(type: String): DashboardWidget? = byType[type]
}
