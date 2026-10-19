package com.arashrasoulzadeh.einkdash.widgets

import android.content.Context
import android.view.Gravity
import android.widget.Toast
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.notes.NotesDb
import com.arashrasoulzadeh.einkdash.util.Fa
import com.arashrasoulzadeh.einkdash.util.Ui
import org.json.JSONArray

/** A Hafez couplet a day (opening bayt of each ghazal, bundled offline). Tap to save it to notes. */
class QuoteWidget : DashboardWidget {
    override val type = "quote"
    override val titleRes = R.string.widget_quote
    override val defaultW = 4
    override val defaultH = 2

    class Couplet(val number: Int, val first: String, val second: String)

    companion object {
        @Volatile private var cache: List<Couplet>? = null

        fun all(ctx: Context): List<Couplet> = cache ?: runCatching {
            val arr = JSONArray(ctx.assets.open("hafez.json").bufferedReader(Charsets.UTF_8).use { it.readText() })
            List(arr.length()) { i -> arr.getJSONObject(i).let { Couplet(it.getInt("n"), it.getString("a"), it.getString("b")) } }
        }.getOrDefault(emptyList()).also { cache = it }

        /** Same couplet all day, a different one tomorrow. */
        fun today(ctx: Context): Couplet? {
            val list = all(ctx)
            if (list.isEmpty()) return null
            val day = (System.currentTimeMillis() + java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis())) / 86_400_000L
            return list[(day % list.size).toInt()]
        }
    }

    override fun createView(context: Context): WidgetView = object : WidgetView(context, titleRes) {
        val first = Ui.text(context, 19f).apply { gravity = Gravity.CENTER; textDirection = TEXT_DIRECTION_RTL }
        val second = Ui.text(context, 19f).apply { gravity = Gravity.CENTER; textDirection = TEXT_DIRECTION_RTL }
        val source = line(12f, Ui.bold, center = true)

        init {
            removeView(source) // source goes under the couplet
            fill(android.widget.LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER
                addView(first); addView(Ui.gap(context, 10f)); addView(second)
            })
            addView(source)
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
            source.text = context.getString(R.string.quote_source, Fa.digits(context, c.number.toString())) +
                Fa.sep(context) + context.getString(R.string.quote_tap)
        }
    }
}
