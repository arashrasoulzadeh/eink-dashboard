package com.arashrasoulzadeh.einkdash.widgets

import android.content.Context
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.notes.NotesDb
import com.arashrasoulzadeh.einkdash.util.Fa
import com.arashrasoulzadeh.einkdash.util.Jalali
import com.arashrasoulzadeh.einkdash.util.MonthGrid
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
    override val defaultW = 4
    override val defaultH = 2

    override fun createView(context: Context): WidgetView = object : WidgetView(context, titleRes) {
        val grid = fill(MonthGrid(context))

        override fun refresh() {
            val fa = Fa.isFa(context)
            val now = Calendar.getInstance()
            val today = MonthGrid.ymd(now)
            grid.today = today
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
                val j = Jalali.fromGregorian(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH))
                val len = Jalali.monthLength(j.year, j.month)
                grid.days = (1..len).map { d ->
                    val g = Jalali.toGregorian(j.year, j.month, d)
                    MonthGrid.Day(d, g.day, g.year * 10000 + g.month * 100 + g.day)
                }
                val first = Jalali.toGregorian(j.year, j.month, 1)
                grid.offset = column(Calendar.getInstance().apply { set(first.year, first.month - 1, first.day) })
                // A Jalali month spans two Gregorian months; name both.
                val last = Jalali.toGregorian(j.year, j.month, len)
                val gName = SimpleDateFormat("MMMM", Locale("fa"))
                val g1 = gName.format(Calendar.getInstance().apply { set(first.year, first.month - 1, first.day) }.time) + "–" +
                    gName.format(Calendar.getInstance().apply { set(last.year, last.month - 1, last.day) }.time)
                label(R.string.widget_calendar, Fa.digits(context, "${Jalali.monthsFa[j.month - 1]} ${j.year}") + " (" + g1 + ")")
            } else {
                val len = now.getActualMaximum(Calendar.DAY_OF_MONTH)
                grid.days = (1..len).map { d ->
                    val jd = Jalali.fromGregorian(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, d)
                    MonthGrid.Day(d, jd.day, now.get(Calendar.YEAR) * 10000 + (now.get(Calendar.MONTH) + 1) * 100 + d)
                }
                grid.offset = column((now.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) })
                val j1 = Jalali.fromGregorian(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, 1)
                val jn = Jalali.fromGregorian(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, len)
                label(R.string.widget_calendar, SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(now.time) +
                    " (" + Jalali.monthsEn[j1.month - 1] + "–" + Jalali.monthsEn[jn.month - 1] + ")")
            }
            grid.update()
        }
    }
}
