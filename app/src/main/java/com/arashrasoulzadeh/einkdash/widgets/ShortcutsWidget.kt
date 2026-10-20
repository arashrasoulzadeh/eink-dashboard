package com.arashrasoulzadeh.einkdash.widgets

import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.widget.LinearLayout
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.launcher.FullscreenActivity
import com.arashrasoulzadeh.einkdash.notes.NotesActivity
import com.arashrasoulzadeh.einkdash.settings.SettingsActivity
import com.arashrasoulzadeh.einkdash.util.Icon
import com.arashrasoulzadeh.einkdash.util.Ui

/** A row of icon tiles: settings, timer, habits, notes, library. Draws its own tiles, so no card. */
class ShortcutsWidget : DashboardWidget {
    override val type = "shortcuts"
    override val titleRes = R.string.widget_shortcuts
    override val cardless = true
    override val defaultW = 4
    override val defaultH = 1

    override fun createView(context: Context): WidgetView = object : WidgetView(context, 0) {
        init {
            // No title row for a tile strip.
            removeAllViews()
            setPadding(0, 0, 0, 0)
            val row = LinearLayout(context).apply { gravity = Gravity.CENTER }
            fun full(type: String) = Intent(context, FullscreenActivity::class.java).putExtra(FullscreenActivity.EXTRA_TYPE, type)
            listOf<Triple<Icon.Kind, Int, () -> Unit>>(
                Triple(Icon.Kind.BOOK, R.string.sc_library) { if (!ReadingWidget.launchReader(context)) context.startActivity(full("reading")) },
                Triple(Icon.Kind.NOTE, R.string.widget_notes) { context.startActivity(Intent(context, NotesActivity::class.java)) },
                Triple(Icon.Kind.CHECK, R.string.sc_habits) { context.startActivity(full("habits")) },
                Triple(Icon.Kind.TIMER, R.string.sc_timer) { context.startActivity(full("pomodoro")) },
                Triple(Icon.Kind.GEAR, R.string.settings) { context.startActivity(Intent(context, SettingsActivity::class.java)) },
            ).forEach { (kind, label, open) ->
                row.addView(LinearLayout(context).apply {
                    orientation = VERTICAL
                    gravity = Gravity.CENTER
                    Ui.onTap(this) { open() }
                    addView(LinearLayout(context).apply {
                        gravity = Gravity.CENTER
                        background = Ui.outline(context, 14f)
                        addView(Icon(context, kind), LayoutParams((26 * dp).toInt(), (26 * dp).toInt()))
                    }, LayoutParams((54 * dp).toInt(), (54 * dp).toInt()))
                    addView(Ui.text(context, 13f).apply { setText(label); gravity = Gravity.CENTER; setPadding(0, (5 * dp).toInt(), 0, 0); setSingleLine() })
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
            }
            addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }

        override fun refresh() {}
    }
}
