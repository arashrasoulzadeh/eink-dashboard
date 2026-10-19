package com.arashrasoulzadeh.einkdash.widgets

import android.content.Context
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.util.Fa
import com.arashrasoulzadeh.einkdash.util.Jalali
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ClockWidget : DashboardWidget {
    override val type = "clock"
    override val titleRes = R.string.widget_clock
    override val defaultW = 4
    override val defaultH = 2

    override fun createView(context: Context): WidgetView = object : WidgetView(context, 0) {
        val time = figure(84f, center = true)
        val date = line(17f, center = true)

        init { gravity = android.view.Gravity.CENTER }

        override fun refresh() {
            val now = Date()
            val cal = Calendar.getInstance()
            val j = Jalali.fromGregorian(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
            val locale = context.resources.configuration.locale
            time.text = num(SimpleDateFormat("HH:mm", Locale.US).format(now))
            val gregorian = SimpleDateFormat("EEEE d MMMM yyyy", locale).format(now)
            date.text = if (Fa.isFa(context)) num("${j.day} ${Jalali.monthsFa[j.month - 1]} ${j.year}") + "  ·  " + gregorian
            else gregorian + "  ·  " + "${j.day} ${Jalali.monthsEn[j.month - 1]} ${j.year}"
        }
    }
}
