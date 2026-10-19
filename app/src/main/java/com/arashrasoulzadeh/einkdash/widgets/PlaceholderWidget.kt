package com.arashrasoulzadeh.einkdash.widgets

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.TextView

/** Stands in for a v1 widget until its real implementation lands. */
class PlaceholderWidget(
    override val type: String,
    override val titleRes: Int,
    override val defaultW: Int,
    override val defaultH: Int,
) : DashboardWidget {
    override fun createView(context: Context): View = TextView(context).apply {
        setText(titleRes)
        textSize = 20f
        gravity = Gravity.CENTER
    }
}
