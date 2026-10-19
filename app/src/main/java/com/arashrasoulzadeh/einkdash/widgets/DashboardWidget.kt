package com.arashrasoulzadeh.einkdash.widgets

import android.content.Context
import android.view.View

/** A dashboard widget. Implementations are built into the app (no plugins). */
interface DashboardWidget {
    val type: String
    val titleRes: Int
    val defaultW: Int
    val defaultH: Int

    fun createView(context: Context): View

    /** Called by the refresh scheduler (phase 2+); must not animate. */
    fun update(view: View) {}
}
