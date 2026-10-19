package com.arashrasoulzadeh.einkdash.widgets

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.widget.LinearLayout
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.eink.Eink
import com.arashrasoulzadeh.einkdash.pomodoro.Pomodoro
import com.arashrasoulzadeh.einkdash.util.Ring

/**
 * Idle: four rings (1, 5, 15, 25 min); tap one to start that long.
 * Running: one progress ring with mm:ss left; tap it to stop.
 */
class PomodoroWidget : DashboardWidget {
    override val type = "pomodoro"
    override val titleRes = R.string.widget_pomodoro
    override val defaultW = 2
    override val defaultH = 2

    companion object {
        val QUICK = intArrayOf(1, 5, 15, 25)
    }

    override fun createView(context: Context): WidgetView = object : WidgetView(context, titleRes) {
        val ring = Ring(context)
        val quick = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val hint = line(12f, center = true)

        init {
            removeView(hint)
            fill(ring); fill(quick)
            addView(hint)
            ring.setOnClickListener { Pomodoro.stop(context); afterTap() }
            // Two rows of two so each ring stays big enough to tap on a 2x2 spot.
            QUICK.toList().chunked(2).forEach { pair ->
                quick.addView(LinearLayout(context).apply {
                    pair.forEach { min ->
                        addView(Ring(context).apply {
                            label = num(min.toString())
                            unit = context.getString(R.string.pomo_unit_min)
                            setOnClickListener { Pomodoro.start(context, Pomodoro.WORK, min); afterTap() }
                        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
                    }
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            }
        }

        private fun afterTap() {
            refresh(); Eink.fullRefresh(rootView)
            // Without this the host keeps its once-a-minute schedule and the countdown looks frozen.
            retick()
        }

        override fun refresh() {
            Pomodoro.checkFinished(context)
            val end = Pomodoro.endAt(context)
            val running = end != 0L
            ring.visibility = if (running) VISIBLE else GONE
            quick.visibility = if (running) GONE else VISIBLE
            if (!running) {
                label(R.string.widget_pomodoro)
                hint.setText(R.string.pomo_pick)
                return
            }
            val isBreak = Pomodoro.phase(context) == Pomodoro.BREAK
            val total = (if (isBreak) Pomodoro.breakMin(context) else Pomodoro.sessionMin(context)) * 60_000L
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
        fun mark(on: Boolean) = if (on) "☑  " else "☐  "
        val items = listOf(
            mark(Pomodoro.openReader(activity)) + activity.getString(R.string.pomo_open_reader),
            mark(Pomodoro.dnd(activity)) + activity.getString(R.string.pomo_dnd),
        )
        AlertDialog.Builder(activity)
            .setTitle(R.string.widget_pomodoro)
            .setItems(items.toTypedArray()) { _, i ->
                if (i == 0) Pomodoro.setOpenReader(activity, !Pomodoro.openReader(activity))
                else {
                    val on = !Pomodoro.dnd(activity)
                    Pomodoro.setDnd(activity, on)
                    if (on && android.os.Build.VERSION.SDK_INT >= 23) {
                        val nm = activity.getSystemService(android.app.NotificationManager::class.java)
                        if (!nm.isNotificationPolicyAccessGranted)
                            activity.startActivity(android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                    }
                }
            }
            .show()
        return true
    }
}
