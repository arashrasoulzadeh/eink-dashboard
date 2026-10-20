package com.arashrasoulzadeh.einkdash.widgets

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.view.Gravity
import android.widget.LinearLayout
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.notes.NotesActivity
import com.arashrasoulzadeh.einkdash.notes.NotesDb
import com.arashrasoulzadeh.einkdash.util.Ui

/** A ruled notebook page: one open item per line, empty lines fill the rest of the cell. */
class NotesWidget : DashboardWidget {
    override val type = "notes"
    override val titleRes = R.string.widget_notes
    override val icon = com.arashrasoulzadeh.einkdash.util.Icon.Kind.NOTE
    override val defaultW = 4
    override val defaultH = 4

    override fun createView(context: Context): WidgetView = object : WidgetView(context, titleRes, icon) {
        val lines = fill(LinearLayout(context).apply { orientation = LinearLayout.VERTICAL })
        val lineH = (44 * dp).toInt()

        init { setOnClickListener { open(context) } }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            // The number of ruled lines depends on the cell height; rebuild once it's known.
            post { refresh() }
        }

        override fun refresh() {
            val open = NotesDb.of(context).all().filter { !it.done }
            label(R.string.widget_notes, if (open.isEmpty()) null else num(open.size.toString()))
            val count = (lines.height / lineH).coerceAtLeast(1)
            lines.removeAllViews()
            for (i in 0 until count) {
                val note = open.getOrNull(i)
                val text = when {
                    note != null && i == count - 1 && open.size > count -> context.getString(R.string.notes_more, num((open.size - i).toString()))
                    note != null && note.due != 0 -> (if (note.due <= NotesDb.today()) "◆   " else "☐   ") + note.text +
                        com.arashrasoulzadeh.einkdash.util.Fa.sep(context) + NotesDb.dueLabel(context, note.due)
                    note != null -> "☐   " + note.text
                    i == 0 && open.isEmpty() -> context.getString(R.string.notes_empty)
                    else -> ""
                }
                lines.addView(Ui.text(context, 17f).apply {
                    this.text = text
                    gravity = Gravity.CENTER_VERTICAL
                    setSingleLine(); ellipsize = TextUtils.TruncateAt.END
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, lineH - (1 * dp).toInt()))
                lines.addView(Ui.rule(context))
            }
        }
    }

    override fun configure(activity: Activity): Boolean { open(activity); return true }

    private fun open(context: Context) = context.startActivity(Intent(context, NotesActivity::class.java))
}
