package com.arashrasoulzadeh.einkdash.widgets

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.eink.Eink
import com.arashrasoulzadeh.einkdash.pomodoro.Pomodoro

/** Tap to start a focus session, tap again to stop. Per-second display only in the last minute. */
class PomodoroWidget : DashboardWidget {
    override val type = "pomodoro"
    override val titleRes = R.string.widget_pomodoro
    override val defaultW = 2
    override val defaultH = 1

    override fun createView(context: Context): WidgetView = object : WidgetView(context, titleRes) {
        val body = figure()
        val hint = line(13f)

        init {
            setOnClickListener {
                if (Pomodoro.endAt(context) == 0L) Pomodoro.start(context, Pomodoro.WORK) else Pomodoro.stop(context)
                refresh(); Eink.fullRefresh(rootView)
            }
        }

        override fun refresh() {
            Pomodoro.checkFinished(context)
            val end = Pomodoro.endAt(context)
            if (end == 0L) {
                body.text = num("${Pomodoro.workMin(context)}:00")
                hint.setText(R.string.pomo_tap_start)
                return
            }
            val left = maxOf(0L, end - System.currentTimeMillis())
            val label = context.getString(if (Pomodoro.phase(context) == Pomodoro.BREAK) R.string.pomo_break else R.string.pomo_focus)
            body.text = if (left <= 60_000) num("0:%02d".format((left + 999) / 1000))
            else num("${(left + 59_999) / 60_000} min")
            hint.text = label + " · " + context.getString(R.string.pomo_tap_stop)
        }

        override fun nextDelayMs(now: Long): Long {
            val end = Pomodoro.endAt(context)
            if (end == 0L) return Long.MAX_VALUE
            val left = end - now
            return when {
                left <= 61_000 -> 1000L
                else -> (left - 60_000) % 60_000 + 1
            }
        }
    }

    override fun configure(activity: Activity): Boolean {
        val options = intArrayOf(15, 25, 45, 50)
        AlertDialog.Builder(activity)
            .setTitle(R.string.pomo_length)
            .setItems(options.map { activity.getString(R.string.pomo_minutes, it) }.toTypedArray()) { _, i ->
                Pomodoro.setWorkMin(activity, options[i])
            }
            .show()
        return true
    }
}
