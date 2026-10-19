package com.arashrasoulzadeh.einkdash.widgets

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.notes.NotesActivity
import com.arashrasoulzadeh.einkdash.notes.NotesDb

class NotesWidget : DashboardWidget {
    override val type = "notes"
    override val titleRes = R.string.widget_notes
    override val defaultW = 2
    override val defaultH = 1

    override fun createView(context: Context): WidgetView = object : WidgetView(context, titleRes) {
        val body = line(16f).apply { setLineSpacing(0f, 1.25f) }

        init { setOnClickListener { open(context) } }

        override fun refresh() {
            val open = NotesDb(context).all().filter { !it.done }
            body.text = if (open.isEmpty()) context.getString(R.string.notes_empty)
            else open.take(6).joinToString("\n") { "• " + it.text }
        }
    }

    override fun configure(activity: Activity): Boolean { open(activity); return true }

    private fun open(context: Context) = context.startActivity(Intent(context, NotesActivity::class.java))
}
