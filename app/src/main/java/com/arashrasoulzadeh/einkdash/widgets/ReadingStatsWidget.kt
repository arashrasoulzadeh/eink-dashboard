package com.arashrasoulzadeh.einkdash.widgets

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.reading.ReadingStats
import com.arashrasoulzadeh.einkdash.util.FitText
import com.arashrasoulzadeh.einkdash.util.Spark
import com.arashrasoulzadeh.einkdash.util.Ui
import java.text.SimpleDateFormat
import java.util.Calendar

/** Reading streak and a bar per day for the last week, fed by finished focus sessions. */
class ReadingStatsWidget : DashboardWidget {
    override val type = "readingStats"
    override val titleRes = R.string.widget_reading_stats
    override val icon = com.arashrasoulzadeh.einkdash.util.Icon.Kind.BOOK
    override val defaultW = 2
    override val defaultH = 2

    override fun createView(context: Context): WidgetView = object : WidgetView(context, titleRes, icon) {
        val streak = FitText(context, Ui.light, 60f).apply { gravity = Gravity.CENTER_VERTICAL }
        val today = Ui.text(context, 13f)
        val bars = Spark(context).apply { slots = 7 }
        val days = LinearLayout(context)

        init {
            fill(LinearLayout(context).apply {
                addView(streak, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL
                    addView(Ui.text(context, 14f, Ui.bold).apply { setText(R.string.stats_day_streak) })
                    addView(today)
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
            }, 1f)
            fill(bars, 0.9f)
            addView(days)
        }

        override fun refresh() {
            val s = ReadingStats(context)
            val week = s.week()
            streak.text = num(s.streak().toString())
            today.text = context.getString(R.string.stats_today, num(week.last().toString()))
            bars.values = week
            days.removeAllViews()
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
            val f = SimpleDateFormat("EEEEE", context.resources.configuration.locale)
            repeat(7) {
                days.addView(Ui.text(context, 11f).apply { text = f.format(c.time); gravity = Gravity.CENTER },
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                c.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    }
}
