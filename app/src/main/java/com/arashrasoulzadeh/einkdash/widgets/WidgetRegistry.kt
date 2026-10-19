package com.arashrasoulzadeh.einkdash.widgets

object WidgetRegistry {
    val all: List<DashboardWidget> = listOf(
        ClockWidget(),
        WeatherWidget(),
        BatteryWidget(),
        LatencyWidget(),
        ReadingWidget(),
        NotesWidget(),
        PomodoroWidget(),
        ReadingStatsWidget(),
        ControlsWidget(),
        CalendarWidget(),
        QuoteWidget(),
    )

    private val byType = all.associateBy { it.type }

    fun get(type: String): DashboardWidget? = byType[type]
}
