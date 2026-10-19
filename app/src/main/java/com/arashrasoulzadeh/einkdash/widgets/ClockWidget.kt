package com.arashrasoulzadeh.einkdash.widgets

import android.content.Context
import android.view.Gravity
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.util.Fa
import com.arashrasoulzadeh.einkdash.util.FitText
import com.arashrasoulzadeh.einkdash.util.Jalali
import com.arashrasoulzadeh.einkdash.util.Meter
import com.arashrasoulzadeh.einkdash.util.Ui
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Hero clock: fitted time, both calendars, and a meter of how much of the day has passed. */
class ClockWidget : DashboardWidget {
    override val type = "clock"
    override val titleRes = R.string.widget_clock
    override val defaultW = 4
    override val defaultH = 2

    override fun createView(context: Context): WidgetView = object : WidgetView(context, 0) {
        val time = fill(FitText(context, Ui.light).apply { gravity = Gravity.CENTER })
        val date = line(16f, Ui.bold, center = true)
        val meter = Meter(context)
        val meterLabel = line(12f, center = true)

        init {
            addView(Ui.gap(context, 10f))
            addView(meter, LayoutParams(LayoutParams.MATCH_PARENT, (6 * dp).toInt()))
            removeView(meterLabel); addView(Ui.gap(context, 4f)); addView(meterLabel)
        }

        override fun refresh() {
            val now = Date()
            val cal = Calendar.getInstance()
            val j = Jalali.fromGregorian(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
            val locale = context.resources.configuration.locale
            time.text = num(SimpleDateFormat("HH:mm", Locale.US).format(now))
            val gregorian = SimpleDateFormat("EEEE d MMMM", locale).format(now)
            date.text = if (Fa.isFa(context)) num("${j.day} ${Jalali.monthsFa[j.month - 1]} ${j.year}") + "  " + Fa.sep(context) + "  " + gregorian
            else gregorian + "  " + Fa.sep(context) + "  " + "${j.day} ${Jalali.monthsEn[j.month - 1]} ${j.year}"
            val minutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            meter.progress = minutes / 1440f
            meterLabel.text = context.getString(R.string.clock_day_left, num(((1440 - minutes) / 60).toString()), num(((1440 - minutes) % 60).toString()))
        }
    }
}
