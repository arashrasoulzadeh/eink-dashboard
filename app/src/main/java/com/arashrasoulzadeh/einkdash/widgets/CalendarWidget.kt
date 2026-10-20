package com.arashrasoulzadeh.einkdash.widgets

import android.content.Context
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.notes.NotesDb
import com.arashrasoulzadeh.einkdash.util.Fa
import com.arashrasoulzadeh.einkdash.util.Jalali
import com.arashrasoulzadeh.einkdash.util.MonthGrid
import com.arashrasoulzadeh.einkdash.util.Icon
import com.arashrasoulzadeh.einkdash.util.Ui
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * This month: a Jalali month (weeks start Saturday) in Persian, a Gregorian month in English;
 * the other calendar's day numbers sit small under each day. Days with due todos are dotted.
 */
class CalendarWidget : DashboardWidget {
    override val type = "calendar"
    override val titleRes = R.string.widget_calendar
    override val icon = com.arashrasoulzadeh.einkdash.util.Icon.Kind.CALENDAR
    override val defaultW = 4
    override val defaultH = 2

    override fun createView(context: Context): WidgetView = object : WidgetView(context, 0) {
        /** Months away from the current one, changed with the ‹ › arrows. */
        var shift = 0
        val month = Ui.text(context, 16f, Ui.bold).apply { gravity = android.view.Gravity.CENTER }
        val grid: MonthGrid

        init {
            removeAllViews()
            setPadding((10 * dp).toInt(), (8 * dp).toInt(), (10 * dp).toInt(), (8 * dp).toInt())
            val arrow = (36 * dp).toInt()
            // Arrows keep their meaning in both directions: the start-side one goes back.
            addView(android.widget.LinearLayout(context).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
                addView(Icon(context, if (Fa.isFa(context)) Icon.Kind.NEXT else Icon.Kind.PREV).apply { setOnClickListener { shift--; changed() } }, android.widget.LinearLayout.LayoutParams(arrow, arrow))
                addView(month, android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(Icon(context, if (Fa.isFa(context)) Icon.Kind.PREV else Icon.Kind.NEXT).apply { setOnClickListener { shift++; changed() } }, android.widget.LinearLayout.LayoutParams(arrow, arrow))
            })
            grid = fill(MonthGrid(context))
        }

        override fun refresh() {
            val fa = Fa.isFa(context)
            val now = Calendar.getInstance()
            val realToday = MonthGrid.ymd(now)
            grid.today = realToday
            grid.marked = NotesDb.of(context).dueDays()
            grid.digits = { Fa.digits(context, it) }
            val locale = context.resources.configuration.locale
            val dayName = SimpleDateFormat("EEEEE", locale)
            val weekStart = if (fa) Calendar.SATURDAY else Calendar.getInstance(locale).firstDayOfWeek
            grid.weekdays = (0 until 7).map { i ->
                val c = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, (weekStart - 1 + i) % 7 + 1) }
                if (fa) "شیدسچپج"[i].toString() else dayName.format(c.time)
            }
            fun column(c: Calendar) = (c.get(Calendar.DAY_OF_WEEK) - weekStart + 7) % 7

            if (fa) {
                val jt = Jalali.fromGregorian(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH))
                val mIndex = jt.year * 12 + (jt.month - 1) + shift
                val j = Jalali.Date(mIndex / 12, mIndex % 12 + 1, 1)
                val len = Jalali.monthLength(j.year, j.month)
                grid.days = (1..len).map { d ->
                    val g = Jalali.toGregorian(j.year, j.month, d)
                    MonthGrid.Day(d, g.day, g.year * 10000 + g.month * 100 + g.day)
                }
                val first = Jalali.toGregorian(j.year, j.month, 1)
                grid.offset = column(Calendar.getInstance().apply { set(first.year, first.month - 1, first.day) })
                month.text = Fa.digits(context, "${Jalali.monthsFa[j.month - 1]} ${j.year}")
            } else {
                now.set(Calendar.DAY_OF_MONTH, 1); now.add(Calendar.MONTH, shift)
                val len = now.getActualMaximum(Calendar.DAY_OF_MONTH)
                grid.days = (1..len).map { d ->
                    val jd = Jalali.fromGregorian(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, d)
                    MonthGrid.Day(d, jd.day, now.get(Calendar.YEAR) * 10000 + (now.get(Calendar.MONTH) + 1) * 100 + d)
                }
                grid.offset = column((now.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) })
                val j1 = Jalali.fromGregorian(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, 1)
                val jn = Jalali.fromGregorian(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, len)
                month.text = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(now.time) +
                    " (" + Jalali.monthsEn[j1.month - 1] + "–" + Jalali.monthsEn[jn.month - 1] + ")"
            }
            grid.update()
        }
    }
}
