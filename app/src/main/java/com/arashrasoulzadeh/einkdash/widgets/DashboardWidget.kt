package com.arashrasoulzadeh.einkdash.widgets

import android.app.Activity
import android.content.Context

/** A dashboard widget. Implementations are built into the app (no plugins). */
interface DashboardWidget {
    val type: String
    val titleRes: Int
    val defaultW: Int
    val defaultH: Int

    fun createView(context: Context): WidgetView

    /** Opens the widget's setup UI from edit mode. Returns false when there is nothing to set up. */
    fun configure(activity: Activity): Boolean = false
}
