package com.arashrasoulzadeh.einkdash.widgets

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.face.Couplets
import com.arashrasoulzadeh.einkdash.notes.NotesDb
import com.arashrasoulzadeh.einkdash.util.Fa
import com.arashrasoulzadeh.einkdash.util.Icon
import com.arashrasoulzadeh.einkdash.util.Ui

/** Couplet of the day (Hafez, Saadi, Molavi, Khayyam and more, bundled offline). Tap to save it to notes. */
class QuoteWidget : DashboardWidget {
    override val type = "quote"
    override val titleRes = R.string.widget_quote
    override val icon = Icon.Kind.LEAF
    override val defaultW = 2
    override val defaultH = 2

    companion object {
        /** Same couplet all day; uses the minute pool at the day's first minute. */
        fun today(ctx: Context): Couplets.Couplet? {
            val now = System.currentTimeMillis() + java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis())
            return Couplets.forMinute(ctx, now / 86_400_000L * 1440)
        }
    }

    override fun createView(context: Context): WidgetView = object : WidgetView(context, 0) {
        val first = Ui.text(context, 15f, Ui.bold).apply { textDirection = View.TEXT_DIRECTION_RTL; setLineSpacing(0f, 1.1f) }
        val second = Ui.text(context, 15f, Ui.bold).apply { textDirection = View.TEXT_DIRECTION_RTL; setLineSpacing(0f, 1.1f) }
        val poet = Ui.text(context, 12f).apply { setTextColor(Ui.MUTED) }

        init {
            removeAllViews()
            setPadding((14 * dp).toInt(), (14 * dp).toInt(), (14 * dp).toInt(), (12 * dp).toInt())
            addView(Icon(context, Icon.Kind.LEAF), LayoutParams((26 * dp).toInt(), (26 * dp).toInt()))
            fill(LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                addView(first); addView(Ui.gap(context, 6f)); addView(second)
            })
            addView(poet)
            setOnClickListener {
                val c = today(context) ?: return@setOnClickListener
                NotesDb.of(context).add(c.first + " / " + c.second)
                Toast.makeText(context, R.string.quote_saved, Toast.LENGTH_SHORT).show()
            }
        }

        override fun refresh() {
            val c = today(context) ?: return
            first.text = c.first
            second.text = c.second
            poet.text = "— " + c.poet
        }
    }
}
