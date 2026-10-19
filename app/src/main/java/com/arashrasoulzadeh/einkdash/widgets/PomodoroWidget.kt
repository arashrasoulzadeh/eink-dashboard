package com.arashrasoulzadeh.einkdash.widgets

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.eink.Eink
import com.arashrasoulzadeh.einkdash.pomodoro.Pomodoro
import com.arashrasoulzadeh.einkdash.util.Ring

/** Progress ring with mm:ss left inside. Tap to start or stop. */
class PomodoroWidget : DashboardWidget {
    override val type = "pomodoro"
    override val titleRes = R.string.widget_pomodoro
    override val defaultW = 2
    override val defaultH = 2

    override fun createView(context: Context): WidgetView = object : WidgetView(context, titleRes) {
        val ring = fill(Ring(context))
        val hint = line(12f, center = true)

        init {
            setOnClickListener {
                if (Pomodoro.endAt(context) == 0L) Pomodoro.start(context, Pomodoro.WORK) else Pomodoro.stop(context)
                refresh(); Eink.fullRefresh(rootView)
                // Without this the host keeps its once-a-minute schedule and the countdown looks frozen.
                retick()
            }
        }

        override fun refresh() {
            Pomodoro.checkFinished(context)
            val end = Pomodoro.endAt(context)
            if (end == 0L) {
                label(R.string.widget_pomodoro)
                ring.progress = 0f
                ring.label = num("${Pomodoro.workMin(context)}")
                ring.unit = context.getString(R.string.pomo_unit_min)
                hint.setText(R.string.pomo_tap_start)
                return
            }
            val isBreak = Pomodoro.phase(context) == Pomodoro.BREAK
            val total = (if (isBreak) Pomodoro.breakMin(context) else Pomodoro.workMin(context)) * 60_000L
            val left = maxOf(0L, end - System.currentTimeMillis())
            label(R.string.widget_pomodoro, context.getString(if (isBreak) R.string.pomo_break else R.string.pomo_focus))
            ring.progress = 1f - left.toFloat() / total
            val secs = (left + 999) / 1000
            ring.label = num("%d:%02d".format(secs / 60, secs % 60))
            ring.unit = context.getString(R.string.pomo_unit_min_sec)
            hint.setText(R.string.pomo_tap_stop)
        }

        override fun nextDelayMs(now: Long): Long {
            val end = Pomodoro.endAt(context)
            if (end == 0L) return Long.MAX_VALUE
            // Tick on the second boundary of the countdown.
            return ((end - now) % 1000).let { if (it <= 0) 1000 else it } + 5
        }
    }

    override fun configure(activity: Activity): Boolean {
        val options = intArrayOf(15, 25, 45, 50)
        fun mark(on: Boolean) = if (on) "☑  " else "☐  "
        val items = options.map { (if (it == Pomodoro.workMin(activity)) "●  " else "○  ") + activity.getString(R.string.pomo_minutes, it) } +
            (mark(Pomodoro.openReader(activity)) + activity.getString(R.string.pomo_open_reader)) +
            (mark(Pomodoro.dnd(activity)) + activity.getString(R.string.pomo_dnd))
        AlertDialog.Builder(activity)
            .setTitle(R.string.pomo_length)
            .setItems(items.toTypedArray()) { _, i ->
                when {
                    i < options.size -> Pomodoro.setWorkMin(activity, options[i])
                    i == options.size -> Pomodoro.setOpenReader(activity, !Pomodoro.openReader(activity))
                    else -> {
                        val on = !Pomodoro.dnd(activity)
                        Pomodoro.setDnd(activity, on)
                        if (on && android.os.Build.VERSION.SDK_INT >= 23) {
                            val nm = activity.getSystemService(android.app.NotificationManager::class.java)
                            if (!nm.isNotificationPolicyAccessGranted)
                                activity.startActivity(android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                        }
                    }
                }
            }
            .show()
        return true
    }
}
