package com.arashrasoulzadeh.einkdash.widgets

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.eink.Eink
import com.arashrasoulzadeh.einkdash.pomodoro.Pomodoro
import com.arashrasoulzadeh.einkdash.util.Icon
import com.arashrasoulzadeh.einkdash.util.Ring
import com.arashrasoulzadeh.einkdash.util.Ui

/**
 * Focus card: a progress ring with time remaining, a play/pause button and a reset button.
 * While idle, chips pick the length (1, 5, 15, 25 min).
 */
class PomodoroWidget : DashboardWidget {
    override val type = "pomodoro"
    override val titleRes = R.string.pomo_focus
    override val icon = Icon.Kind.TOMATO
    override val defaultW = 2
    override val defaultH = 3

    companion object {
        val QUICK = intArrayOf(1, 5, 15, 25)
    }

    override fun createView(context: Context): WidgetView = object : WidgetView(context, titleRes, icon) {
        val ring = fill(Ring(context))
        val chips = LinearLayout(context).apply { gravity = Gravity.CENTER }
        val play = Icon(context, Icon.Kind.PLAY, filled = true)
        val reset = Icon(context, Icon.Kind.RESET)

        init {
            QUICK.forEach { min ->
                chips.addView(Ui.text(context, 14f, Ui.bold).apply {
                    tag = min
                    text = num(min.toString())
                    gravity = Gravity.CENTER
                    Ui.onTap(this) { Pomodoro.setWorkMin(context, min); afterTap() }
                }, LinearLayout.LayoutParams(0, (30 * dp).toInt(), 1f).apply { marginStart = (3 * dp).toInt(); marginEnd = (3 * dp).toInt() })
            }
            addView(chips, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = (6 * dp).toInt() })
            val size = (46 * dp).toInt()
            addView(LinearLayout(context).apply {
                gravity = Gravity.CENTER
                addView(play, LinearLayout.LayoutParams(size, size))
                addView(reset.apply { background = Ui.outline(context, 23f, Ui.TINT, Ui.TINT); setPadding((10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt()) },
                    LinearLayout.LayoutParams(size, size).apply { marginStart = (18 * dp).toInt() })
            }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = (8 * dp).toInt() })

            Ui.onTap(play) {
                when {
                    Pomodoro.endAt(context) != 0L -> Pomodoro.pause(context)
                    Pomodoro.pausedLeft(context) > 0 -> Pomodoro.resume(context)
                    else -> Pomodoro.start(context, Pomodoro.WORK, Pomodoro.workMin(context))
                }
                afterTap()
            }
            Ui.onTap(reset) { Pomodoro.reset(context); afterTap() }
        }

        private fun afterTap() {
            // The play/pause icon changes shape: a partial update leaves its old outline, so refresh the card fully.
            refresh(); Eink.fullView(this)
            // Without this the host keeps its once-a-minute schedule and the countdown looks frozen.
            retick()
        }

        private fun mmss(ms: Long): String { val s = (ms + 999) / 1000; return num("%d:%02d".format(s / 60, s % 60)) }

        override fun refresh() {
            Pomodoro.checkFinished(context)
            val end = Pomodoro.endAt(context)
            val paused = Pomodoro.pausedLeft(context)
            val isBreak = Pomodoro.phase(context) == Pomodoro.BREAK
            val total = (if (isBreak) Pomodoro.breakMin(context) else Pomodoro.sessionMin(context)) * 60_000L
            ring.unit = context.getString(R.string.pomo_time_left)
            label(if (isBreak && (end != 0L || paused > 0)) R.string.pomo_break else R.string.pomo_focus)
            when {
                end != 0L -> {
                    val left = maxOf(0L, end - System.currentTimeMillis())
                    ring.progress = left.toFloat() / total
                    ring.label = mmss(left)
                    play.kind = Icon.Kind.PAUSE
                    chips.visibility = GONE
                }
                paused > 0 -> {
                    ring.progress = paused.toFloat() / total
                    ring.label = mmss(paused)
                    play.kind = Icon.Kind.PLAY
                    chips.visibility = GONE
                }
                else -> {
                    val m = Pomodoro.workMin(context)
                    ring.progress = 1f
                    ring.label = mmss(m * 60_000L)
                    play.kind = Icon.Kind.PLAY
                    chips.visibility = VISIBLE
                    for (i in 0 until chips.childCount) (chips.getChildAt(i) as android.widget.TextView).apply {
                        val on = tag == m
                        background = Ui.outline(context, 15f, if (on) Ui.INK else Ui.PAPER, if (on) Ui.INK else Ui.LINE)
                        setTextColor(if (on) Ui.PAPER else Ui.INK)
                    }
                }
            }
            play.invalidate()
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
